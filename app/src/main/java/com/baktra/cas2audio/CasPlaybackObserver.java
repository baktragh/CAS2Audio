package com.baktra.cas2audio;

public interface CasPlaybackObserver {

    public void onSuccessfulPlayback();
    public void onCancelledPlayback(int resumeIp,int stopReason);

    public void onProgressUpdate(int value);
    public void onResumePointUpdate(int resumeIp);

    public void onFailedPlayback(Exception e);

}
