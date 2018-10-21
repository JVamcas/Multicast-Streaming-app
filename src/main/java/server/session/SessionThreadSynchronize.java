package server.session;
/***
 * Created by the SessionManager class to synchronise the streaming session instances
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static server.core.Constants.streamType.LIVE;
import static server.session.SessionManager.parseMulticastPlaylist;

public class SessionThreadSynchronize extends Session {
    private SessionManager.Alternatives[] alternatives;
    private Constants.streamType streamType;
    private String pathToVideo;
    private List<Session> sessionThreadList;
    private volatile List<Integer> segmentSendStatusList;
    private AtomicInteger nextSegmentIndex = new AtomicInteger(-1);
    private ContentPreparation contentPreparation;
    SessionThreadSynchronize(ContentPreparation contentPreparation,long sessionDuration, String pathToVideo, Constants.streamType streamType) {
        super(sessionDuration);
        segmentSendStatusList = new ArrayList<>();
        this.alternatives = parseMulticastPlaylist(pathToVideo);
        this.streamType = streamType;
        this.pathToVideo = pathToVideo;
        sessionThreadList = new ArrayList<>();
        this.contentPreparation  =contentPreparation;
    }

    @Override
    public void run() {
        for (SessionManager.Alternatives alt : alternatives) {
            String pathToAlternative = new File(pathToVideo).getParent() + "/alternative_" + alt.getBITRATE();
            String groupIp = alt.getGROUP_IP_ADDRESS();
            int multicastSocketNumber = Integer.parseInt(alt.getSOCKET_NUMBER());
            Session session;
            if (streamType == LIVE)
                session = new LiveStreamMulticastSession(contentPreparation,this, groupIp, multicastSocketNumber, pathToAlternative);
            else
                session = new VoDMulticastSession(this, groupIp, multicastSocketNumber, pathToAlternative);
            addSessionThread(session);
            new Thread(session).start();
        }

        if (streamType == LIVE) {
            while (true) {
                boolean state = true;
                for (Session session : sessionThreadList)
                    state = state && (((LiveStreamMulticastSession) session).getChannel().getView().size() >= 1);
                if (state) {//wait for all the channels to start before starting WebCam
                    new Thread(new ContentPreparation.WebCamVideoFeed()).start();
                    break;
                }
            }
        }
        new Thread(timeKeeper).start();
        addSeg_Index(-1);

        while (true) {
            if (ready() && previousSegmentSent()) {
                increSegmentIndex();
            }
            if (terminate()) {
                nextSegmentIndex.getAndSet(-1);
                SessionManager.removeStream(pathToVideo);
                if(streamType == Constants.streamType.LIVE)
                    ContentPreparation.terminateLiveStream();
                break;
            }
        }
    }

    public synchronized AtomicInteger getNextSegmentIndex() {
        return nextSegmentIndex;
    }

    public synchronized void increSegmentIndex() {
        nextSegmentIndex.addAndGet(1);
    }

    private boolean ready() {
        boolean state = true;
        for (Session session : sessionThreadList)
            state = state && session.isReady();
        return state;
    }

    private boolean terminate() {
        boolean state = true;
        for (Session session : sessionThreadList)
            state = state && session.threadState == Constants.threadState.TERMINATE;
        return state;
    }

    /***
     *
     * @return true if one of the transmission SessionThread terminated
     */
    public synchronized boolean otherLiveSessionTerminated(){
        for (Session session:sessionThreadList){
            if(session.threadState == Constants.threadState.TERMINATE)
                return true;
        }
        return false;
    }
    public synchronized void addSessionThread(Session session){
        sessionThreadList.add(session);
    }

    boolean clientAvailable() {
        for (Session session : sessionThreadList) {
            if (((VoDMulticastSession) session).getChannel().getView().size() > 1)
                return true;
        }
        return false;
    }

    public synchronized void addSeg_Index(int seg_index) {
        segmentSendStatusList.add(seg_index);
    }

    public synchronized boolean previousSegmentSent() {
        return segmentSendStatusList.contains(nextSegmentIndex.get());
    }
}
