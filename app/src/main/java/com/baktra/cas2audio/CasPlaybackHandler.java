package com.baktra.cas2audio;

import android.os.PowerManager;

import java.util.Optional;

public class CasPlaybackHandler {

    private final Optional<PowerManager> powerManager;
    public CasPlaybackHandler(Optional<PowerManager> pm) {
        this.powerManager = pm;
        casTask=Optional.empty();
    }

    private Optional<CasTask> casTask;
    public static final int STOP_REASON_STOP=0;
    public static final int STOP_REASON_PAUSE=1;
    public static final int STOP_REASON_TERMINATE=2;
    private int stopReason;

    private CasPlaybackObserver observer;

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
            casTask=Optional.empty();
            throw e;
        }
    }

    public void play() {
        if (casTask.isPresent()) {
            casTask.get().execute();
        }
    }

    public void stop(int stopReason) {
        this.stopReason=stopReason;
        if (casTask.isPresent()) {
            casTask.get().cancel(false);
        }
        casTask=Optional.empty();
    }

    public void processSuccess() {
        casTask=Optional.empty();
        publishPlaybackSuccess();
    }

    public void processCancellation(int resumeIp) {
        casTask=Optional.empty();
        publishPlaybackCancellation(resumeIp);
    }

    public void processFailure(Exception e) {
        casTask=Optional.empty();
        publishPlaybackFailure(e);
    }


    public void publishProgress(Integer progress, Integer resumeIp) {
        observer.onProgressUpdate(progress);
        if (resumeIp != -1) {
            observer.onResumePointUpdate(resumeIp);
        }
    }

    public void publishPlaybackFailure(Exception e) {
        observer.onFailedPlayback(e);
    }

    public void publishPlaybackSuccess() {
        observer.onSuccessfulPlayback();
    }

    public void publishPlaybackCancellation(int resumeIp) {
        observer.onCancelledPlayback(resumeIp,stopReason);
    }



}
