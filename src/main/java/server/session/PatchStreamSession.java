package server.session;
/***
 * This class is responsible for streaming the patch stream
 *
 * Project: Adaptive Transcoding of multi-definition multicasted video stream
 * Dependecies: none
 * @author Petrus Kambala
 * Final year Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */
import org.apache.commons.io.FileUtils;
import server.core.Constants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;

import static server.core.Constants.*;

public class PatchStreamSession extends Session {
    private DataInputStream fromClient;
    private DataOutputStream toClient;
    private int lastindex;
    private String videoPath;
    private Socket clientSocket;

    public PatchStreamSession(long sessionDuration, String videoPath, Socket clientSocket, int lastIndex) throws Exception {
        super(sessionDuration);
        fromClient = new DataInputStream(clientSocket.getInputStream());
        toClient = new DataOutputStream(clientSocket.getOutputStream());
        this.lastindex = lastIndex;
        this.videoPath = new File(videoPath).getParent();
        this.clientSocket = clientSocket;
    }

    /***
     * Sends patch stream to the client,
     * Client must request segment using alternative_<bitrate>/seg_<segment Index>.mp4
     */
    @Override
    public void run() {
        try {
            int state = AWAIT_CLIENT_REQUEST, segmentIndex = -1;
            String clientRequest = "";
            while (!(lastindex <= segmentIndex)) {
                switch (state) {
                    case AWAIT_CLIENT_REQUEST:
                        if (fromClient.available() > 0) {
                            clientRequest = fromClient.readUTF();
                            String[] temp = clientRequest.split("_");
                            segmentIndex = Integer.parseInt(temp[temp.length - 1].split("\\.")[0]);
                            state = SEND_DATA;
                        }
                        break;
                    case SEND_DATA:
                        String segmentUrl = videoPath + "/" + clientRequest;
                        byte[] segment = FileUtils.readFileToByteArray(new File(segmentUrl));
                        toClient.writeUTF(SEG_START+"#"+segment.length);
                        toClient.write(segment, 0, segment.length); //send next segment
                        state = AWAIT_CLIENT_REQUEST;
                        Thread.sleep(1000);
                        break;
                }
            }
            toClient.close();
            fromClient.close();
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
