package client.core;

/**
 * This is the client application driver class, it automatically connects to the server once you run it to get a
 * list of available VoD video snapshots
 *
 * Project: Adaptive transcoding of multi-definition multicasted video stream
 * Dependencies: none
 * @author Petrus Kambala
 * Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static client.core.Constants.*;

public class ClientMain extends Application {


    public static volatile VideoPlayer videoPlayer;
    public static String CLIENT_IP_ADDRESS;
    public static void main(String[] args) {
        System.out.println("\n\nClient Application Starting...................!!!!\n\n");
        Scanner s = new Scanner(System.in);
        System.out.println("Enter the IP Address of the interface to be used by the client application:");
        CLIENT_IP_ADDRESS = s.nextLine().trim();
        System.setProperty("java.net.preferIPv4Stack","true");
        System.out.println("\n\nClient application is ready!!");
        s.close();
        byte[] snapshots = fetchAvailableVideosSnapshot();
        new Thread(() -> launch(null)).start();//create videoplayer instance
        while (videoPlayer == null) ;
        processAvailableVideoSnapshot(snapshots);//populate video player
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        videoPlayer = new VideoPlayer(primaryStage);
    }

    public static byte[] fetchAvailableVideosSnapshot() {
        byte[] snapshot = null;
        DataInputStream fromServer;
        DataOutputStream toServer;
        Socket clientSocket;
        try {
            clientSocket = new Socket(Constants.HOST, Constants.PORT_NUMBER);
            toServer = new DataOutputStream(clientSocket.getOutputStream());
            fromServer = new DataInputStream(clientSocket.getInputStream());

            int state = -1;
            int data_len = 0;
            while (state != QUIT) {
                switch (state) {
                    case REQUEST_SNAP:
                        toServer.writeUTF(SNAP);
                        state = WAIT_DATA_LEN;
                        break;
                    case WAIT_DATA_LEN:
                        if (fromServer.available() > 0) {
                            data_len = fromServer.readInt();
                            state = RECEIVE_SNAP;
                        }
                        break;
                    case RECEIVE_SNAP:
                        if (fromServer.available() > 0) {
                            snapshot = readData(fromServer, data_len);
                            state = QUIT;
                        }
                        break;
                    default:
                        state = REQUEST_SNAP;
                        break;
                }
            }
            toServer.close();
            fromServer.close();
            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return snapshot;
    }

    public static byte[] readData(InputStream inputStream, int datalen) {
        byte[] buffer = new byte[0];
        byte[] temp_buffer;
        int iteration = datalen / Constants.PKT_SIZE;
        int reminder = datalen % Constants.PKT_SIZE;
        try {
            for (int i = 0; i < iteration; i++) {
                temp_buffer = new byte[Constants.PKT_SIZE];
                IOUtils.read(inputStream, temp_buffer);
                buffer = ArrayUtils.addAll(buffer, temp_buffer);
            }
            temp_buffer = new byte[reminder];
            IOUtils.read(inputStream, temp_buffer);
            buffer = ArrayUtils.addAll(buffer, temp_buffer);
        } catch (Exception e) {
        }
        return buffer;
    }

    /***
     * Process a byte array containing a zip file of video snapshots from the server
     * @param snapshots the buffer
     */
    private static void processAvailableVideoSnapshot(byte[] snapshots) {
        String destination = Constants.ROOTFOLDER + "/landing_page";
        extractZIp(snapshots, destination);
        File[] files = new File(destination).listFiles();
        try {
            for (File entry : files) {
                String videoName = entry.getName().split("\\.")[0];
                Image image = SwingFXUtils.toFXImage(ImageIO.read(entry), null);
                videoPlayer.addVideoSnapshot(image, videoName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }//TODO remember you cannot be in two channels at same time
    }

    /***
     * Unzip the file
     * @param file byte array of the zip file
     * @param destination path to folder where to unzip
     */
    public static void extractZIp(byte[] file, String destination) {
        ZipInputStream zipstream = new ZipInputStream(new ByteArrayInputStream(file));
        ZipEntry entry;
        try {
            while ((entry = zipstream.getNextEntry()) != null) {
                String fileName = destination + "/" + entry.getName();
                FileOutputStream out = new FileOutputStream(fileName);
                byte[] buffer = new byte[20240];
                int byteRead;

                while ((byteRead = zipstream.read(buffer)) != -1) {
                    out.write(buffer, 0, byteRead);
                }
                out.close();
                zipstream.closeEntry();
            }
            zipstream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}