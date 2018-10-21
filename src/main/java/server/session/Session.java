package server.session;
/***
 * The super class for all the streaming Sessions,
 *
 * Project: Adaptive Transcoding of multi-definition multicasted video stream
 * Dependecies: none
 * @author Petrus Kambala
 * Final year Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */
import server.core.Constants;
import server.core.TimeKeeper;

public abstract class Session implements Runnable {

    public static int SESSION_CAN_TIMEOUT = 1;
    public static int INIT_SESSION_TIME = 100000;
    TimeKeeper timeKeeper;
    public volatile Constants.threadState threadState;

    public Session(long sessionDuration) {
        timeKeeper = new TimeKeeper(sessionDuration);
    }

    public boolean isSessionActive() {
        return timeKeeper.getTimeOut().get() > 0;
    }

    public synchronized boolean isReady(){
        return threadState == Constants.threadState.READY;
    }
}
