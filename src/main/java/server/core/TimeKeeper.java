package server.core;

/***
 * This class provide timing requirements used by the server
 *
 * Project: Adaptive Transcoding of multi-definition multicasted video stream
 * Dependecies: none
 * @author Petrus Kambala
 * Final year Electrical and Computer Engineering Student
 * University of Cape Town
 * Sept 2018
 */
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class TimeKeeper implements Runnable {

    private AtomicLong timeElapsed;
    private AtomicLong timeOut;
    private Timer timer = new Timer();

    public TimeKeeper(Long time) {
        timeOut = new AtomicLong(time);
        timeElapsed = new AtomicLong(0);
    }

    @Override
    public void run() {
        start();
    }

    public AtomicLong getTimeElapsed() {
        return timeElapsed;
    }

    private void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timeElapsed.addAndGet(1);
                timeOut.getAndSet(timeOut.get() <= 0 ? 0 : timeOut.decrementAndGet());
            }
        }, 0, 1);
    }

    public void cancel() {
        timer.cancel();
    }

    public AtomicLong getTimeOut() {
        return timeOut;
    }
    public void setTimeOut(long time){
        timeOut.getAndSet(time);
    }
}
