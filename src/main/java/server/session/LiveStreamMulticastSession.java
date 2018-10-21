package server.session;
/***
 * This class is responsible for streamoing live video streams
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

public class LiveStreamMulticastSession extends VoDMulticastSession {

    private String bitrate;
    private ContentPreparation contentPreparation;
    public LiveStreamMulticastSession(ContentPreparation contentPreparation,SessionThreadSynchronize synchronizer, String groupIpAddress, int socketNumber, String pathToAlternative) {
        super(synchronizer, groupIpAddress, socketNumber, pathToAlternative);
        this.contentPreparation = contentPreparation;
        bitrate = pathToAlternative.split("_")[2];
    }

    @Override
    public void run() {
        new Thread(timeKeeper).start();
        timeKeeper.setTimeOut(Constants.LIVE_STREAM_DURATION);
        long start = 0;
        while (isSessionActive()&&!getSynchronizer().otherLiveSessionTerminated()) {//stream last 10 mins
            String seg_name = "/seg_" + nextSegmentIndex + ".mp4";

            if (contentPreparation.getLiveStreamTranscodersMap().get(bitrate).getSegmentIndex() >= nextSegmentIndex) {
                segmentPreprocess(seg_name);
            }
        }
        closeStreaming();//close transmission of this alternatives
        timeKeeper.cancel();

    }
}
