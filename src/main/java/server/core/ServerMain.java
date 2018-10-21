package server.core;

/***
 *This is the driver class for the Server application
 *
 * Project: Adaptive Transcoding of multi-definition multicasted video stream
 * Dependecies: none
 * @author Petrus Kambala
 * Final year Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */

import server.session.SessionManager;
import server.session.SetUpSession;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServerMain {
    public static String serverIPAddress, webcam;
    public static void main(String[] args) {
        try {new SessionManager();

            Scanner s = new Scanner(System.in);
            System.out.println("Multicast Streaming server.");
            System.out.println("Enter the IP address of the interface to use: \n");
            serverIPAddress = s.nextLine().trim();
            System.out.println("Enter the name of your webcam: normally \"/dev/video0/\":");
            webcam = s.nextLine().trim();
            System.out.println("Server is ready.");
            System.setProperty("java.net.preferIPv4Stack" , "true");
            InetAddress addr = InetAddress.getByName(ServerMain.serverIPAddress);
            ServerSocket serverSocket = new ServerSocket(Constants.SERVER_SOCKET_NUMBER, 0, addr);
            new Thread(new ContentPreparation(Constants.streamType.VOD)).start();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new SetUpSession(-1, clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
