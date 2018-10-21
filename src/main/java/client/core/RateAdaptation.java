package client.core;
/**
 * This class is responsible for adapting the bitrate of the received video segments to the client network
 * throughput, you can use the buffer-based method or the throughput-based method
 *
 * Project: Adaptive transcoding of multi-definition multicasted video stream
 * Dependencies: none
 * @author Petrus Kambala
 * Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */

import client.session.StreamSession;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;

import static client.core.Constants.*;

public class RateAdaptation {

    private List<Integer> bitrateList;
    private StringBuilder volumeBuiler = new StringBuilder();
    private StringBuilder T_i_Builder = new StringBuilder();
    private StringBuilder R_i_Builder = new StringBuilder();
    private StringBuilder timeBuilder = new StringBuilder();
    private int time = 1, prev_T_i = 0, bit_index = 0, T_0;

    public RateAdaptation(Alternatives[] alternatives) {
        bitrateList = new ArrayList<>();
        for (Alternatives alt : alternatives) {
            String bitrate = alt.getBITRATE();
            bitrateList.add(Integer.parseInt(bitrate.split("k")[0]));
            Collections.sort(bitrateList);
        }
    }

    public String adaptRate(int segmentSize, double duration, String Bitrate) {
        int T_i = (int) Math.round((segmentSize / 1000.0) / duration);
        int R_i = Integer.parseInt(Bitrate.split("k")[0]);

        int volume = ClientMain.videoPlayer.getStreamSession().vp_controller.size();

        this.volumeBuiler.append(volume).append(" ");
        this.T_i_Builder.append(T_i).append(" ");
        timeBuilder.append(time++ * 2).append(" ");
        return bufferbased(volume, R_i, T_i);
        //return throughputBased(T_i);

    }

    private int estimateBitrate(int throughput) {
        Map<Integer, Integer> bitrateMap = new HashMap<>();
        for (Integer bitrate : bitrateList) {
            if (bitrate < throughput)
                bitrateMap.put(Math.abs(throughput - bitrate), bitrate);
        }
        if (bitrateMap.isEmpty())
            return StreamSession.min_bitrate;

        int min_diff = Collections.min(bitrateMap.keySet());
        return bitrateMap.get(min_diff);
    }

    /***
     * Buffer based rate adaptation algorithm
     * @param volume level of the buffer
     * @param R_i bitrate of current segment
     * @param T_i throughput of network during download of current segment
     * @return the estimated bitrate of next segment
     */
    private String bufferbased(int volume, int R_i, int T_i) {

        if (volume <= BUF_1) bit_index = 0; //[9] buffer based plus throughput

        else if (volume <= BUF_2 && R_i >= 0.7* T_i) bit_index-=2;

        else if (volume >= BUF_3 && R_i <= T_i) bit_index++;

        bit_index = bit_index < 0 ? 0 : bit_index == bitrateList.size() ? bitrateList.size() - 1 : bit_index;
        return bitrateList.get(bit_index) + "k";
    }

    /***
     * Use throughput based method
     * @param T_i throughput for current segment
     * @return the estimated bitrate of next segment
     */
    private String throughputBased(int T_i) {

        Double T_est = ((1 - Constants.SENSITIVITY) * T_0 + Constants.SENSITIVITY * T_i);
        Double R_est = T_est * SCALER;
        T_0 = T_i;
        return estimateBitrate(R_est.intValue()) + "k";
    }


    public void writeTofile() {
        try {
            String toFile = T_i_Builder.toString() + "\n" + R_i_Builder.toString() + "\n" + volumeBuiler.toString() + "\n" +
                    timeBuilder.toString();

            FileUtils.writeStringToFile(new File(Constants.RESULTS), toFile, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateBitrate(String bitrate) {
        this.R_i_Builder.append(bitrate.split("k")[0]).append(" ");
    }
}