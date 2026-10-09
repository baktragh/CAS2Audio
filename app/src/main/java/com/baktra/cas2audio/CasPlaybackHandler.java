package com.baktra.cas2audio;

import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CasPlaybackHandler {

    private final Optional<PowerManager> powerManager;


    private Optional<CasTask> casTask;
    private Optional<Future<Void>> taskFuture;

    public static final int STOP_REASON_STOP=0;
    public static final int STOP_REASON_PAUSE=1;
    public static final int STOP_REASON_TERMINATE=2;

    private int stopReason;

    private CasPlaybackObserver observer;

    public CasPlaybackHandler(Optional<PowerManager> pm) {
        this.powerManager = pm;
        clearTask();
    }

    private void clearTask() {
        casTask=Optional.empty();
        taskFuture=Optional.empty();
    }

    public void prepare(int[] instructions, CasPlaybackObserver observer, boolean stereo, boolean square, int volume, int sampleRate, boolean invertPolarity, int resumeIp) throws Exception{

        this.observer=observer;

        try {
            casTask = Optional.of(new CasTask(
                    instructions,
                    this,
                    stereo,
                    square,
                    volume,
                    sampleRate,
                    invertPolarity,
                    resumeIp,
                    powerManager
            ));
        } catch (Exception e) {
           clearTask();
            throw e;
        }
    }

    public void play() {
        if (casTask.isPresent()) {
            ExecutorService es = Executors.newSingleThreadExecutor();
            taskFuture=Optional.of(es.submit(casTask.get()));
        }
    }

    public void stop(int stopReason) {

        /*Keep reason*/
        this.stopReason=stopReason;

        /*Not task, do nothing*/
        if (casTask.isEmpty() || taskFuture.isEmpty()) return;
        CasTask ct = casTask.get();

        /*Request cancellation*/
        ct.cancel();
    }

    public void processSuccess() {
        clearTask();
        publishPlaybackSuccess();
    }

    public void processCancellation(int resumeIp) {
        clearTask();
        publishPlaybackCancellation(resumeIp);
    }

    public void processFailure(Throwable e) {
       clearTask();
        publishPlaybackFailure(e);
    }


    public void publishProgress(Integer progress, Integer resumeIp) {

        Looper mainLooper = Looper.getMainLooper();
        Handler mainHandler = new Handler(mainLooper);

        mainHandler.post(new Runnable() {
            public void run() {
                observer.onProgressUpdate(progress);
                if (resumeIp != -1) {
                    observer.onResumePointUpdate(resumeIp);
                }
            }
        });

    }

    public void publishPlaybackFailure(Throwable e) {
        Looper mainLooper = Looper.getMainLooper();
        Handler mainHandler = new Handler(mainLooper);

        mainHandler.post(new Runnable() {
            public void run() {
                observer.onFailedPlayback(e);
            }
        });

    }

    public void publishPlaybackSuccess() {

        Looper mainLooper = Looper.getMainLooper();
        Handler mainHandler = new Handler(mainLooper);

        mainHandler.post(new Runnable() {
            public void run() {
                observer.onSuccessfulPlayback();
            }
        });

    }

    public void publishPlaybackCancellation(int resumeIp) {
        Looper mainLooper = Looper.getMainLooper();
        Handler mainHandler = new Handler(mainLooper);

        mainHandler.post(new Runnable() {
            public void run() {
                observer.onCancelledPlayback(resumeIp,stopReason);
            }
        });
    }


}
