package client.session;
/**
 * This class handles video streaming, either for patch stream or multicast streams
 * It understands the streaming protocols used by the server
 * It handles connections to the server once a user clicks on a "live stream" button or on any VoD video snapshot
 *
 * Project: Adaptive transcoding of multi-definition multicasted video stream
 * Dependencies: ffmpeg for re-encoding the video segments
 *
 * @author Petrus Kambala
 * Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */
import client.core.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.protocols.UDP;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static client.core.Constants.*;

public class StreamSession {

    private Alternatives[] alternatives;//alternatives from which to stream
    private String cur_bit, prev_bit;
    private int SEG_INDEX = 0;

    private String videoName;
    private volatile boolean terminate = false;
    public VideoPlayer.VP_Controller vp_controller;
    private RateAdaptation rateAdaptation;
    private Constants.stream_type stream_type;
    private JChannel channel;
    private Map<String, JChannel> channelMap;
    private long start_time = 0;
    private volatile boolean sessionEnd = false, unicastStreamEnd;
    private int segments;
    private long streamDuration;
    public static long start_tim = 0;

    public static int max_bitrate, min_bitrate, starting_volume;
    private volatile ObservableList<Message> multicastStreamList, unicastStreamList, tempStreamList;

    public StreamSession(String videoName, Constants.stream_type stream_type) {
        min_bitrate = stream_type == Constants.stream_type.LIVE ? LIVE_DEFAULT_BITRATE : VoD_DEFAULT_MULTICAST;
        max_bitrate = stream_type == Constants.stream_type.LIVE ? LIVE_MAX_BITRATE : VoD_MAX_BITRATE;

        starting_volume = stream_type == Constants.stream_type.LIVE ? Constants.VoD_VOL : Constants.VoD_VOL;
        multicastStreamList = FXCollections.observableArrayList();
        unicastStreamList = FXCollections.observableArrayList();
        tempStreamList = FXCollections.observableArrayList();

        cur_bit = min_bitrate + "k";
        this.videoName = videoName;
        vp_controller = ClientMain.videoPlayer.new VP_Controller();
        this.stream_type = stream_type;
        try {
            //Remove segments from old sessions
            FileUtils.cleanDirectory(new File(Constants.LIVE_STREAM_PATH));
            FileUtils.cleanDirectory(new File(Constants.ROOTFOLDER + "/videos"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialize() {
        try {
            Socket clientSocket = new Socket(Constants.HOST, Constants.PORT_NUMBER);
            DataOutputStream toServer = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream fromServer = new DataInputStream(clientSocket.getInputStream());
            int data_len = 0;
            int state = REQUEST_VIDEO;
            while (state != QUIT && !getTerminate()) {
                switch (state) {
                    case REQUEST_VIDEO:
                        toServer.writeUTF(videoName);
                        state = WAIT_DATA_LEN;
                        break;
                    case WAIT_DATA_LEN:
                        if(fromServer.available() > 0){
                            data_len = fromServer.readInt();
                            state = WAIT_MULTICAST_GROUP;
                        }
                        break;
                    case WAIT_MULTICAST_GROUP:
                        if (fromServer.available() > 0) {//if multicast group ip addresses received
                            prepareStreaming(IOUtils.readFully(fromServer,data_len),clientSocket);
                            if (segments == 0) {//if this is not patch
                                fromServer.close();
                                toServer.close();
                                clientSocket.close();
                            }
                            state = QUIT;
                        }
                        break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareStreaming(byte[] configs, Socket clientSocket) {
        try {
            String destination = Constants.ROOTFOLDER + "/configs";
            FileUtils.cleanDirectory(new File(destination));//remove old configs file
            ClientMain.extractZIp(configs, destination);

            String multicastPlaylist = "";
            for (File file : new File(destination).listFiles()) {
                if (file.getName().endsWith(".json"))
                    multicastPlaylist = IOUtils.toString(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            }
            parserMulticastPlaylist(multicastPlaylist);
            createChannelMap();
            rateAdaptation = new RateAdaptation(alternatives);

            tempStreamList = segments <= 0 ? multicastStreamList : unicastStreamList;
            unicastStreamEnd = segments <= 0;

            receiveMulticastStream();
            if(!unicastStreamEnd)
                new Thread(new UnicastStream(new DataInputStream(clientSocket.getInputStream()),
                        new DataOutputStream(clientSocket.getOutputStream()))).start();



            proces_segments();
            channelsClose();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createChannelMap() {
        channelMap = new HashMap<>();
        try {
            for (Alternatives alt : alternatives) {
                JChannel channel = new JChannel(Constants.CONFIG_FILE);
                UDP udp = channel.getProtocolStack().getBottomProtocol();
                udp.setBindAddress(InetAddress.getByName(ClientMain.CLIENT_IP_ADDRESS));
                udp.setMulticastAddress(InetAddress.getByName(alt.getGROUP_IP_ADDRESS()));
                udp.setMulticastPort(Integer.parseInt(alt.getSOCKET_NUMBER()));
                channel.setName(alt.getCLUSTER_NAME());
                channelMap.put(alt.getBITRATE(), channel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void channelsClose() {
        for (JChannel channel : channelMap.values()) {//stream has ended
            channel.disconnect();
            channel.close();
        }
    }

    private class UnicastStream implements Runnable {

        DataInputStream fromServer;
        DataOutputStream toServer;
        int nextSegment = 0;

        public UnicastStream(DataInputStream fromServer, DataOutputStream toServer) {
            this.fromServer = fromServer;
            this.toServer = toServer;
        }

        @Override
        public void run() {
            String state = REQUEST_SEG;
            String cur_bitrate = VoD_DEFAULT_MULTICAST + "k";
            long start_time = 0;
            String data = "";
            try {
                while (true) {
                    switch (state) {
                        case REQUEST_SEG:
                            String videoName = ("alternative_" + cur_bitrate + "/seg_" + nextSegment + ".mp4");
                            toServer.writeUTF(videoName);
                            state = SEG_START;
                            break;
                        case SEG_START:
                            if (fromServer.available() > 0) {
                                data = fromServer.readUTF();
                                if (data.split("#")[0].trim().equals(SEG_START)) {
                                    start_time = System.currentTimeMillis();
                                    state = RECEIVE_SEG;
                                }
                            }
                            break;
                        case RECEIVE_SEG:
                            int data_len = Integer.parseInt(data.split("#")[1].trim());
                            byte[] segmentByte = IOUtils.readFully(fromServer,data_len);
                            long end = System.currentTimeMillis();
                            cur_bitrate = rateAdaptation.adaptRate(segmentByte.length * 8, (end - start_time) / 1e3, cur_bitrate);
                            addUnicastMessage(new Message(null, segmentByte));
                            nextSegment++;
                            state = REQUEST_SEG;
                            break;
                    }
                    if (nextSegment == segments) {
                        while (tempStreamListSize()!= 0);//wait templist to be emptied
                        synchronized (this) {
                            tempStreamList = multicastStreamList;
                            unicastStreamEnd = true;
                            fromServer.close();
                            toServer.close();
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void receiveMulticastStream() {
        if (cur_bit.equals(prev_bit))//prevent joining same channel
            return;
        if (!unicastStreamEnd)//if not end of unicast stream don't switch multicast groups
            cur_bit = prev_bit = "960k";


        if (null != channel)
            channel.disconnect();

        try {
            for (String bitrate : channelMap.keySet()) {
                if (bitrate.equals(cur_bit)) {
                    channel = channelMap.get(bitrate);
                    channel.connect(channel.getName());
                    channel.receiver(new ReceiverAdapter() {
                        @Override
                        public void receive(Message msg) {
                            try {
                                byte[] payload = ArrayUtils.clone(msg.getBuffer());
                                String message = IOUtils.toString(payload, "utf-8").trim();
                                switch (message) {
                                    case SEG_START:
                                        start_time = System.currentTimeMillis();
                                        break;
                                    case STREAM_END:
                                        channelsClose();//terminate all the channels
                                        sessionEnd = true;
                                        addMulticastMessage(null);
                                        rateAdaptation.writeTofile();
                                        break;
                                    default:
                                        double duration = (System.currentTimeMillis() - start_time) / 1e3;
                                        addMulticastMessage(new Message(null, payload));
                                        rateAdaptation.updateBitrate(cur_bit);
                                        prev_bit = cur_bit;
                                        if (unicastStreamEnd)
                                            cur_bit = rateAdaptation.adaptRate(payload.length * 8, duration, cur_bit);
                                        receiveMulticastStream();
                                        break;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    System.out.println(channel.getView().size());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * Receives segments from the server as a sequence of packets of 1400bytes
     */
    private void proces_segments() {
        boolean started = false;
        while ((!sessionEnd || tempStreamListSize() > 0) && !getTerminate()) {
            try {
                if (tempStreamListSize() > 0) {
                    Message msg = getTempStreamList();
                    if (msg == null) {//terminate streaming
                        vp_controller.addMedia(null);
                        return;
                    }
                    String Output = stream_type == Constants.stream_type.LIVE ? LIVE_STREAM_PATH : VOD_PATH;
                    String Input = stream_type == Constants.stream_type.LIVE ? LIVE_STREAM_PATH : VOD_PATH;
                    Output += "/output_" + SEG_INDEX + ".mp4";
                    Input += "/input_" + SEG_INDEX + ".mp4";

                    Files.write(Paths.get(Input), msg.getBuffer());
                    encodeSegment(Input, Output);
                    Media media = new Media(new File(Output).toURI().toString());
                    vp_controller.addMedia(new MediaPlayer(media));

                    if (!started && vp_controller.size() > starting_volume){
                        System.out.println("start time: "+(System.currentTimeMillis() -start_tim)/1e3);
                        Platform.runLater(new Thread(vp_controller::updateVideo));
                        started = true;
                    }
                    SEG_INDEX++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void addMulticastMessage(Message msg) {
        multicastStreamList.add(msg);
    }

    private synchronized void addUnicastMessage(Message msg) {
        unicastStreamList.add(msg);
    }

    private synchronized Message getTempStreamList() {
        return tempStreamList.remove(0);
    }

    private synchronized int tempStreamListSize() {
        return tempStreamList.size();
    }

    private void parserMulticastPlaylist(String playlist) {
        JsonElement element = new JsonParser().parse(playlist);
        String alt_ipAddress = element.getAsJsonObject().get("alternatives").toString();
        if(stream_type == Constants.stream_type.VOD) {
            segments = Integer.parseInt(element.getAsJsonObject().get("SEGMENTS").toString());
            streamDuration = Long.parseLong(element.getAsJsonObject().get("STREAM DURATION").toString());
        }
        vp_controller.updateTime();
        alternatives = new Gson().fromJson(alt_ipAddress, Alternatives[].class);
    }

    /***
     * Re-encode the assembeled segment for playback
     * @param input input file containing raw segment
     * @param output output file containing the encoded segment
     */
    private void encodeSegment(String input, String output) {
        String command = "ffmpeg -y -err_detect ignore_err -i " + input + " -r " + Constants.FPS + " -s " + Constants.RESOL +
                " -c:a h264 -c:a aac -bsf:a aac_adtstoasc -strict -2 " + output;
        try {
            ProcessBuilder pb = new ProcessBuilder(command.split(" "));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (reader.readLine() != null) ;
            System.out.println(IOUtils.toString(p.getErrorStream(), "utf-8"));
            p.waitFor();
            p.getErrorStream().close();
            p.getOutputStream().close();
            p.getInputStream().close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void terminateStream() {
        setTerminate(true);
        vp_controller.stopVideoPlayer();
    }

    private synchronized void setTerminate(boolean newState) {
        terminate = newState;
    }

    private synchronized boolean getTerminate() {
        return terminate;
    }

    public long getStreamDuration() {
        return streamDuration;
    }

    public void decreStreamDuration() {
        this.streamDuration -=1;
    }
}