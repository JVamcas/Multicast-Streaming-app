package server.core;

/***
 * This class makes use of the Transcode_Segment class to prepare VoD videos for streaming
 * It makes use of WebCamVideoFeed and LiveStreamTranscode helper classes to prepare Live stream videos for streaming
 *
 * Project: Adaptive Transcoding of multi-definition multicasted video stream
 * Dependecies: ffmpeg, v4l2 and alsa linux utilities
 * @author Petrus Kambala
 * Final year Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import server.session.SessionManager;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ContentPreparation implements Runnable {

    private Constants.streamType whichContent;
    private static Process webcamProcess;
    private volatile static boolean stop_liveFeed;
    public volatile boolean multicastPlaylisReady = false;

    private Map<String,LiveStreamTranscoder> liveStreamTranscodersMap;

    public ContentPreparation(Constants.streamType whichContent) {
        this.whichContent = whichContent;
        liveStreamTranscodersMap = new HashMap<>();
        stop_liveFeed  = false;
    }

    public void VodContent() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Constants.ROOTFOlDER + "/videoUrl.txt"));
            String videoPath;
            while ((videoPath = reader.readLine()) != null) {
                String parentFolder = new File(videoPath).getParent();
                if (!isReady(parentFolder)) {//avoid re-transcode
                    for (String bitrate : SessionManager.ipAddress.keySet()) {
                        new Thread(new Transcode_Segment(videoPath, bitrate)).start();
                    }

                    new IndexingModule().createMulticastPlaylist(0.0,new File(videoPath).getParent(),null);
                }
                extractScreenShot(videoPath);
            }reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void liveStreamContent() throws IOException {
        FileUtils.cleanDirectory(new File(Constants.WEBCAM_FEED));//trash all the live feeds
        FileUtils.cleanDirectory(new File(Constants.LIVE_STREAM_PATH));//trash all the alternative dirs

        String fileName = Constants.ROOTFOlDER + "/live_stream";
        BufferedReader reader = new BufferedReader(new FileReader(Constants.LIVE_IP));
        Gson gson = new Gson();
        Map<String, String> ipAddressBitrateMap = gson.fromJson(reader, Map.class);
        reader.close();

        //create live multicast playlist
        new IndexingModule().createMulticastPlaylist(0.0,Constants.LIVE_STREAM_PATH, ipAddressBitrateMap);

        //transcode segment for respective bitrate
        for (String bitrate : ipAddressBitrateMap.keySet()) {
            String pathToAlternative = fileName + "/streams/alternative_" + bitrate;
            LiveStreamTranscoder transcoder = new LiveStreamTranscoder(bitrate, pathToAlternative);
            liveStreamTranscodersMap.put(bitrate,transcoder);
            new Thread(transcoder).start();
        }
    }

    @Override
    public void run() {
        if (whichContent == Constants.streamType.VOD) {
            VodContent();
        } else {
            try {
                liveStreamContent();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class IndexingModule {
        private void createMulticastPlaylist(Double videoDUration,String videoParentDir, Map<String, String>ipAddressMap) throws IOException {

            Map<String, String> bitrateMap = ipAddressMap==null?SessionManager.ipAddress:ipAddressMap;
            StringBuilder builder = new StringBuilder("{\"STREAM DURATION\":")
            .append(videoDUration.intValue()).append(",\"alternatives\":[");
            for (String bitrate : bitrateMap.keySet()) {
                builder.append("{\"BITRATE\":\"").append(bitrate).append("\"")
                .append(",\"SOCKET_NUMBER\":\"").append(SessionManager.allocateSocketNumber())
                        .append("\"")
                .append(",\"GROUP_IP_ADDRESS\":\"").append(bitrateMap.get(bitrate))
                        .append("\"")
                .append(",\"CLUSTER_NAME\":\"").append(videoParentDir.split("/")[3])
                        .append("_").append(bitrate).append("\"},");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append("]}");
            File file = new File(videoParentDir+"/multicastPlaylist.json");
            FileUtils.writeStringToFile(file,builder.toString(),"utf-8");
            multicastPlaylisReady = true;
        }
    }

    private boolean isReady(String videoParentDir) {
        return new File(videoParentDir + "/multicastPlaylist.json").exists();
    }

    public void extractScreenShot(String videoPath) {
        String parentDir = new File(videoPath).getParent();
        String screenshotName = parentDir.split("/")[3];
        String destination = Constants.ROOTFOlDER + "/landing_page/" + screenshotName + ".jpeg";
        String command = "ffmpeg -y -i " + videoPath + " -ss 00:00:03 -vframes 1 " + destination;
        execCommand(command);
    }

    public static class WebCamVideoFeed implements Runnable {
        @Override
        public void run() {
            String transFromWebCamCommand = "ffmpeg -y -thread_queue_size 65000 -f alsa -i default -thread_queue_size 65000 " +
                    "-f v4l2 -i "+ServerMain.webcam+
                    " -c:v h264 -sc_threshold 0 -force_key_frames expr:lte(t,n_forced*2) -f segment -segment_format mp4 -segment_time 2"+
                    " -preset superfast -tune zerolatency " +
                    Constants.WEBCAM_FEED + "/seg_%d.mp4 ";

            try {
                execCommand(transFromWebCamCommand);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /***
     * Terminate process retrieving video from webcam and sub process transcoding
     */
    public static void terminateLiveStream() {
        webcamProcess.destroy();//stop getting video feed from camera
        stop_liveFeed = true; //stop transcoding
        SessionManager.liveSession  = false;//you can now request new live stream
    }

    public class LiveStreamTranscoder implements Runnable {

        String bitrate;
        String pathToAlternative;
        private volatile int segmentIndex = -1;

        private LiveStreamTranscoder(String bitrate, String pathToAlternative) {
            this.bitrate = bitrate;
            this.pathToAlternative = pathToAlternative;
            new File(pathToAlternative).mkdirs();//create alternative
        }

        @Override
        public void run() {
            int seg_index = 0;

            try {
                while (!stop_liveFeed) {
                    String out_seg_name = pathToAlternative + "/seg_" + seg_index + ".mp4";
                    String in_seg_name = Constants.WEBCAM_FEED + "/seg_" + seg_index + ".mp4";
                    String next_seg = Constants.WEBCAM_FEED + "/seg_"+ (seg_index + 1) + ".mp4";
                    String bufsize = (Integer.parseInt(bitrate.split("k")[0])/2)+"k";

                    String transcodeCommand = "ffmpeg -y -err_detect ignore_err -i " + in_seg_name
                            + " -s "+Constants.VIDEO_RES+
                            " -vsync 1 -c:v h264 -b:v " + bitrate + " -minrate "+bitrate+" -maxrate "+bitrate+
                            " -bufsize "+bufsize+" -c:a aac -ac 2 -ar 48k -preset superfast -strict -2 "+ out_seg_name;

                    if (new File(next_seg).exists()) {//ensure the segment exists in the dir
                        execCommand(transcodeCommand);
                        increSegmentIndex();
                        seg_index++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        public synchronized void increSegmentIndex(){
            segmentIndex ++;
        }
        public int getSegmentIndex(){
            return segmentIndex;
        }
    }

    private static void execCommand(String Command) {
        ProcessBuilder pb = new ProcessBuilder(Command.split(" "));
        pb.redirectErrorStream(true);
        BufferedReader reader;
        try {
            Process process = pb.start();
            if (ArrayUtils.contains(Command.split(" "), "v4l2"))//if this is the process extracting from webcam
                webcamProcess = process;

            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (reader.readLine() != null) ;
            process.waitFor();
            closeStream(process, reader);
        } catch (Exception e) {
        }
    }

    private static void closeStream(Process process, BufferedReader reader) {
        try {
            process.getErrorStream().close();
            process.getInputStream().close();
            process.getOutputStream().close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String,LiveStreamTranscoder> getLiveStreamTranscodersMap() {
        return liveStreamTranscodersMap;
    }
}
