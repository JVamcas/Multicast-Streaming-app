package server.session;

/***
 * Provides utiltiy methods for the sessions classes, and is responsible for creating and
 * coordinating the streaming sessions
 *
 * Project: Adaptive Transcoding of multi-definition multicasted video stream
 * Dependecies: none
 * @author Petrus Kambala
 * Final year Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import server.core.Constants;
import server.core.ContentPreparation;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SessionManager {

    public static Map<String, String> ipAddress;
    public static int NUMBER_OF_ALTERNATIVES = 0;
    private static volatile Map<String,Integer> currentStreamsMap = new HashMap<>();
    private static Properties serverProperties;
    public static boolean liveSession;

    public SessionManager() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Constants.ROOTFOlDER + "/multicastIp.json"));
            Gson gson = new Gson();
            ipAddress = gson.fromJson(reader, Map.class);
            NUMBER_OF_ALTERNATIVES = ipAddress.size();
            serverProperties = new Properties();
            String fileName = Constants.ROOTFOlDER + "/server.config";
            InputStream stream = new FileInputStream(fileName);
            serverProperties.load(stream);
            reader.close();
            stream.close();//TODO
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createVoDSetupSession(Socket clientSocket, String videoName) {
        new Thread(new VoDSetupSession(Session.INIT_SESSION_TIME, clientSocket, videoName)).start();
    }

    /***
     *
     * @param pathToVideo full path to the video to be streamed
     */
    public static void createMulticastSession(ContentPreparation content,Constants.streamType streamType, String pathToVideo) {
        if (streamType == Constants.streamType.LIVE && liveSession)
            return; //do nothing if live session requested while already running

        if (streamType == Constants.streamType.VOD) {
            if (contains(pathToVideo))
                return;//if VoD stream for this video already running ignore request
            updateVideoLastSegmentIndex(pathToVideo,-1);
        }
        new Thread(new SessionThreadSynchronize(content,-1, pathToVideo, streamType)).start();
    }

    /***
     * Synchronise the threads streaming this video
     */


    /***
     * Parse playlist file for an alternative video stream
     *
     * @param playlistName full path to the playlist
     * @return a map of the <segmentname, duration>
     */
    public static LinkedHashMap<String, Double> parsePlayListFile(String playlistName) {
        LinkedHashMap<String, Double> segments = new LinkedHashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(playlistName));
            String line;
            Double duration = 0.0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#EXTINF"))
                    duration = Double.parseDouble(line.split(":")[1].split(",")[0]);
                else if (!line.startsWith("#"))
                    segments.put(line, duration);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return segments;
    }

    /**
     * Removes the video stream that has ended*
     *
     * @param videoName full path to the video
     */
    public static synchronized void removeStream(String videoName) {
        currentStreamsMap.remove(videoName);
    }

    public static synchronized void updateVideoLastSegmentIndex(String videoName, int newIndex){
        currentStreamsMap.put(videoName,newIndex);
    }
    public static synchronized boolean contains(String videoName){
        return currentStreamsMap.containsKey(videoName);
    }
    public static synchronized int getVideoLastSegmentIndex(String videoName){
        if(currentStreamsMap.size()==0)
            return 0;
        return currentStreamsMap.get(videoName);
    }


    public static int allocateSocketNumber() {
        int socket_num = 0;
        try {
            socket_num = Integer.parseInt(serverProperties.getProperty("NEXT_SOCKET_NUMBER"));
            serverProperties.setProperty("NEXT_SOCKET_NUMBER", String.valueOf(++socket_num));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return socket_num;
    }

    /***
     *
     * @param videoFIleName full path to the video requested
     * @return array of alternatives from where to stream from
     */
    public static Alternatives[] parseMulticastPlaylist(String videoFIleName) {
        JsonReader reader;
        Alternatives[] alternatives = null;
        try {
            reader = new JsonReader(new FileReader(new File(videoFIleName).getParent() + "/multicastPlaylist.json"));
            JsonElement element = new JsonParser().parse(reader);
            String ip_address = element.getAsJsonObject().get("alternatives").toString();
            alternatives = new Gson().fromJson(ip_address, Alternatives[].class);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return alternatives;
    }

    public static byte[] retrieveAvailableVideos() {
        File[] files = new File(Constants.ROOTFOlDER + "/landing_page").listFiles();
        if(files ==null)return null;
        return createZipFile(Arrays.asList(files));
    }

    /***
     * Create a zip of files passsed to it in a list
     * @param files list of files
     * @return byte array of the zip file
     */
    private static byte[] createZipFile(List<File> files) {
        File zipfile = new File(Constants.ROOTFOlDER + "/Zips/page.zip");
        byte[] filesbyte = null;
        try {
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipfile));
            for (File f : files) {
                ZipEntry entry = new ZipEntry(f.getName());
                byte[] filebyte = Files.readAllBytes(Paths.get(f.getPath()));
                zout.putNextEntry(entry);
                zout.write(filebyte);
                zout.closeEntry();
            }
            filesbyte = Files.readAllBytes(Paths.get(zipfile.getPath()));
            zout.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return filesbyte;
    }

    /***
     * Create a zip file of the multicast playlist and the config file using the createZipFile<List<FIle>
     *     as the helper</>
     * @param videoFilePath video name
     * @return byte array of the zip file
     */
    public static byte[] zipMulticastAndConfig(String videoFilePath){
        List<File> files = new ArrayList<>();
        File multicastFile = new File(new File(videoFilePath).getParent() + "/multicastPlaylist.json");
        files.add(multicastFile);
        files.add(new File(Constants.CONFIG_FILE));
        return createZipFile(files);
    }


    class Alternatives {
        String BITRATE;
        String SOCKET_NUMBER;
        String GROUP_IP_ADDRESS;

        public String getCLUSTER_NAME() {
            return CLUSTER_NAME;
        }

        public void setCLUSTER_NAME(String CLUSTER_NAME) {
            this.CLUSTER_NAME = CLUSTER_NAME;
        }

        String CLUSTER_NAME;

        public String getBITRATE() {
            return BITRATE;
        }

        public void setBITRATE(String BITRATE) {
            this.BITRATE = BITRATE;
        }

        public String getSOCKET_NUMBER() {
            return SOCKET_NUMBER;
        }

        public void setSOCKET_NUMBER(String SOCKET_NUMBER) {
            this.SOCKET_NUMBER = SOCKET_NUMBER;
        }

        public String getGROUP_IP_ADDRESS() {
            return GROUP_IP_ADDRESS;
        }

        public void setGROUP_IP_ADDRESS(String GROUP_IP_ADDRESS) {
            this.GROUP_IP_ADDRESS = GROUP_IP_ADDRESS;
        }
    }
}
