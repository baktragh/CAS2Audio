package com.baktra.cas2audio;

public interface CasTaskObserver {

    public void onSuccessfulPlayback();
    public void onCancelledPlayback(int resumeIp);

    public void onProgressUpdate(int value);
    public void onResumePointUpdate(int resumeIp);

    public void onFailedPlayback(Exception e);

}
