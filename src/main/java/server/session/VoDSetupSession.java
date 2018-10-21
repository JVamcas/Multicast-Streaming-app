package server.session;
/***
 * Created by the SetUpSession class,; It decides whether to send a patch stream with a multicast strema
 * or not
 *
 * Project: Adaptive Transcoding of multi-definition multicasted video stream
 * Dependecies: none
 * @author Petrus Kambala
 * Final year Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.apache.commons.io.FileUtils;
import server.core.Constants;

import java.io.*;
import java.net.Socket;

import static server.core.Constants.*;

public class VoDSetupSession extends Session {
    private Socket clientSocket;
    private String videoName;


    public VoDSetupSession(long sessionDuration, Socket clientSocket, String videoName) {
        super(sessionDuration);
        this.clientSocket = clientSocket;
        this.videoName = videoName;
    }

    @Override
    public void run() {

        try {
            DataInputStream fromClient = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream toClient = new DataOutputStream(clientSocket.getOutputStream());
            String videoPath = Constants.ROOTFOlDER + "/videos/" + videoName + "/" + videoName + ".mp4";

            int state = PATCH_OR_MULTICAST;
            byte[] multicastPlaylist = null;

            while (state != QUIT) {
                switch (state) {
                    case PATCH_OR_MULTICAST:
                        if (SessionManager.contains(videoPath))
                            state = PATCH;
                        else state = MULTICAST;
                        break;

                    case PATCH:
                        int num_of_segs = SessionManager.getVideoLastSegmentIndex(videoPath);
                        setInitialRequest(videoPath, num_of_segs);
                        multicastPlaylist = SessionManager.zipMulticastAndConfig(videoPath);
                        sendPatchStream(videoPath, clientSocket);
                        state = SEND_DATA_LEN;
                        break;
                    case MULTICAST:
                        setInitialRequest(videoPath, 0);
                        multicastPlaylist = SessionManager.zipMulticastAndConfig(videoPath);
                        state = SEND_DATA_LEN;
                        SessionManager.createMulticastSession(null, streamType.VOD, videoPath);
                        break;

                    case SEND_DATA_LEN:
                        toClient.writeInt(multicastPlaylist.length);
                        state = SEND_MULTICAST_PLAYLIST;
                        break;

                    case SEND_MULTICAST_PLAYLIST:
                        toClient.write(multicastPlaylist, 0, multicastPlaylist.length);
                        if(!SessionManager.contains(videoPath)){
                            fromClient.close();
                            toClient.close();
                            clientSocket.close();
                        }
                        state = QUIT;
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPatchStream(String videoName, Socket clientSocket) {
        try {
            new Thread(new PatchStreamSession(-1, videoName, clientSocket,
                    SessionManager.getVideoLastSegmentIndex(videoName))).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setInitialRequest(String videoName, int num_of_segs) {
        try {
            String multicastPath = new File(videoName).getParent() + "/multicastPlaylist.json";
            JsonReader reader = new JsonReader(new FileReader(multicastPath));
            JsonElement element = new JsonParser().parse(reader);
            JsonObject rootObj = element.getAsJsonObject();
            rootObj.getAsJsonObject().remove("SEGMENTS");
            rootObj.getAsJsonObject().addProperty("SEGMENTS", num_of_segs);
            FileUtils.writeStringToFile(new File(multicastPath), rootObj.toString(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
