package server.core;
/***
 * This class is responsible for transcoding a video into various alternatives and segmenting
 * each alternative into multiple 2s video segments
 *
 * Project: Adaptive Transcoding of multi-definition multicasted video stream
 * Dependecies: ffmpeg
 * @author Petrus Kambala
 * Final year Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Transcode_Segment implements Runnable {

    private String videoFileName;
    private String bitrate;

    public Transcode_Segment(String videoFileName, String bitrate) {
        this.videoFileName = videoFileName;
        this.bitrate = bitrate;
    }

    @Override
    public void run() {
        String parentFolder = new File(videoFileName).getParent();
        String alternativeName = parentFolder + "/alternative_" + bitrate;
        String segmentName = alternativeName+"/seg_%d.mp4";
        String playListName = alternativeName+"/playlist.m3u8";
        String bufsize = String.valueOf(Integer.parseInt(bitrate.split("k")[0])/2);

        String transSeg_command = "ffmpeg -y -err_detect ignore_err -i "+videoFileName+" -r "+Constants.FPS+" -g " +Constants.VoD_GOP+
                " -s " +Constants.VIDEO_RES+
                " -vsync 1 -c:v h264 -b:v "+bitrate+" -minrate "+bitrate+" -maxrate "+bitrate+" -bufsize "+bufsize+" -c:a aac" +
                " -ac 2 -ar 48k -flags +cgop -f segment -preset superfast " +
                "-segment_format mp4 -segment_time "+Constants.VoD_SEG_DURATION+" "+segmentName;

        try {

            String createAlternativeFolderCommand = "mkdir "+alternativeName;

            ProcessBuilder pb1 = new ProcessBuilder(createAlternativeFolderCommand.split(" "));
            pb1.redirectErrorStream(true);
            Process p1 = pb1.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p1.getInputStream()));
            while ((reader.readLine())!=null);
            p1.waitFor();
            closeStream(p1,reader);

            ProcessBuilder pb2 = new ProcessBuilder(transSeg_command.split(" "));
            //pb2.redirectErrorStream(true);
            Process p2 = pb2.start();
            reader = new BufferedReader(new InputStreamReader(p2.getInputStream()));
            while ((reader.readLine())!=null);
            System.out.println(IOUtils.toString(p2.getErrorStream(),"utf-8"));
            p2.waitFor();
            closeStream(p2,reader);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }private void closeStream(Process process, BufferedReader reader) {
        try {
            process.getErrorStream().close();
            process.getInputStream().close();
            process.getOutputStream().close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
