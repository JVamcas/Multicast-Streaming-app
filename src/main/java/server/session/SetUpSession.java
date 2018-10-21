package server.session;
/***
 * Created by the ServerMain classes to handle a client requests
 *
 * Project: Adaptive Transcoding of multi-definition multicasted video stream
 * Dependecies: none
 * @author Petrus Kambala
 * Final year Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */

import server.core.Constants;
import server.core.ContentPreparation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class SetUpSession extends Session {

    private Socket clientSocket;

    public SetUpSession(long sessionDuration, Socket clientSocket) {
        super(sessionDuration);
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            DataInputStream fromClient = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream toClient = new DataOutputStream(clientSocket.getOutputStream());
            while (fromClient.available() == 0) ;//wait for client data
            String clientData = fromClient.readUTF();
            switch (clientData) {
                case Constants.SNAP://if client requesting snapshots of available videos
                    byte[] snapshots = SessionManager.retrieveAvailableVideos();
                    toClient.writeInt(snapshots.length);
                    Thread.sleep(100);
                    toClient.write(snapshots,0,snapshots.length);
                    break;
                case Constants.LIVE_STREAM://if request for Live Stream
                    ContentPreparation live_cont = new ContentPreparation(Constants.streamType.LIVE);
                    new Thread(live_cont).start();//open webcam
                    while (!live_cont.multicastPlaylisReady);//if multicast playlist not yet created
                    byte[] multicastPlaylist = SessionManager.zipMulticastAndConfig(Constants.LIVE_VIDEO_PATH);
                    toClient.writeInt(multicastPlaylist.length);
                    toClient.write(multicastPlaylist,0, multicastPlaylist.length);

                    toClient.close();
                    fromClient.close();
                    SessionManager.createMulticastSession(live_cont,Constants.streamType.LIVE, Constants.LIVE_VIDEO_PATH);

                    break;
                default://if request for VoD video
                    SessionManager.createVoDSetupSession(clientSocket, clientData);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
