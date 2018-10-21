package server.session;
/***
 * Created by the VoDSetUPSession class to stream VoD video streams
 *
 * Project: Adaptive Transcoding of multi-definition multicasted video stream
 * Dependecies: none
 * @author Petrus Kambala
 * Final year Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.protocols.UDP;
import server.core.Constants;
import server.core.ServerMain;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static server.core.Constants.*;
import static server.core.Constants.threadState.*;


public class VoDMulticastSession extends Session {

    private String pathToVideoAlternative;
    private JChannel channel;
    int nextSegmentIndex = 0;
    public JChannel getChannel() {
        return channel;
    }



    public SessionThreadSynchronize getSynchronizer() {
        return synchronizer;
    }

    private SessionThreadSynchronize synchronizer;
    private String videoPath;

    public VoDMulticastSession(SessionThreadSynchronize synchronizer, String groupIpAddress, int serverSocketNumber, String pathToVideoAlternative) {
        super(0);
        this.pathToVideoAlternative = pathToVideoAlternative;

        try {
            String videoName = pathToVideoAlternative.split("/")[3]+".mp4";
            videoPath = new File(pathToVideoAlternative).getParent()+"/"+videoName;

            this.synchronizer = synchronizer;
            channel = new JChannel(Constants.CONFIG_FILE);
            channel.setDiscardOwnMessages(true);
            UDP udp = channel.getProtocolStack().getBottomProtocol();
            udp.setBindAddress(InetAddress.getByName(ServerMain.serverIPAddress));
            udp.setMulticastAddress(InetAddress.getByName(groupIpAddress));
            udp.setMulticastPort(serverSocketNumber);
            String bitrate = pathToVideoAlternative.split("/")[4].split("_")[1];
            String cluster_Name = new File(pathToVideoAlternative).getParent().split("/")[3] + "_" + bitrate;
            channel.connect(cluster_Name);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        File alternativeDir = new File(pathToVideoAlternative);
        File[] segmentFile = alternativeDir.listFiles((dir, name) -> name.endsWith(".mp4"));

        Arrays.sort(segmentFile, (o1, o2) -> {
            int n1 = Integer.parseInt(o1.getName().split("_")[1].split("\\.")[0]);
            int n2 = Integer.parseInt(o2.getName().split("_")[1].split("\\.")[0]);
            return n1 - n2;
        });

        try {

            for (File file : segmentFile) {
                segmentPreprocess(file.getName());
            }

            closeStreaming();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void closeStreaming(){
        try{
        while (true) {
            if (synchronizer.previousSegmentSent()) {
                Message msg = new Message(null, STREAM_END.getBytes()).setFlag(Message.Flag.RSVP);
                Thread.sleep(1500);
                channel.send(msg);
                while (true) {
                    if (channel.getView().size() < 2) {//wait for client to leave cluster first before leaving
                        channel.disconnect();
                        channel.close();
                        break;
                    }
                }
                threadState = TERMINATE;
                break;
            }
        }}
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void segmentPreprocess(String segmentName) {
        threadState = READY;
        while (threadState != DONE) {
            switch (threadState) {
                case READY:
                    synchronized (this) {
                        if (synchronizer.getNextSegmentIndex().get() == nextSegmentIndex)//wait other thread to be ready
                            threadState = SEND;
                        else if(synchronizer.otherLiveSessionTerminated())
                            threadState = DONE;//terminate if one of other threads terminated
                    }
                    break;
                case SEND:
                    if (synchronizer.clientAvailable()) {//make sure atleast one cluster has >1 members
                        nextSegmentIndex++;
                        threadState = DONE;
                        if (channel.getView().size() > 1) {//prevent sending if you're alone in cluster
                            sendSegment(segmentName);
                            synchronizer.addSeg_Index(synchronizer.getNextSegmentIndex().get());//add this index to the lists of segments send
                            SessionManager.updateVideoLastSegmentIndex(videoPath,nextSegmentIndex);
                        }
                    }
                    break;
            }
        }
    }

    private void sendSegment(String segmentName) {
        try {
            int state = START;
            while (state != QUIT) {
                switch (state) {
                    case START://sync with clients, about to send data
                        Message msg = new Message(null, SEG_START.getBytes()).setFlag(Message.Flag.RSVP);
                        channel.send(msg);
                        state = SEND_DATA;
                        break;
                    case SEND_DATA:
                        String segmentFile = pathToVideoAlternative + "/" + segmentName;
                        byte[] segmentByte = Files.readAllBytes(Paths.get(new File(segmentFile).getPath()));
                        Message message = new Message(null, segmentByte).setFlag(Message.Flag.RSVP);
                        channel.send(message);
                        state = QUIT;
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
