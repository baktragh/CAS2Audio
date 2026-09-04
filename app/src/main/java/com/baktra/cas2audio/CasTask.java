package com.baktra.cas2audio;

import android.os.AsyncTask;
import android.os.PowerManager;

import com.baktra.cas2audio.signal.SignalGenerator;

import java.lang.ref.WeakReference;

public class CasTask extends AsyncTask<Void,Integer,Void> {

    private final boolean stereo;
    private final boolean square;
    private final int volume;
    private final int[] instructions;
    private final boolean invertPolarity;
    private Exception lastException;
    private final WeakReference<MainActivity> parentActivity;
    private final int sampleRate;
    private PowerManager.WakeLock wakeLock;

    public static final int WAKELOCK_TIMEOUT = 120 * 60 * 1000;

    public CasTask(int[] instructions, MainActivity mainActivity, boolean stereo, boolean square, int volume, int sampleRate,boolean invertPolarity) {
        this.instructions=instructions;
        this.stereo=stereo;
        this.lastException = null;
        this.parentActivity = new WeakReference<>(mainActivity);
        this.square = square;
        this.volume=volume;
        this.sampleRate=sampleRate;
        this.invertPolarity=invertPolarity;
        this.wakeLock = null;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        try {
            PowerManager pm = parentActivity.get().getPowerManager();
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CAS2Audio::TaskWakeLock");
                wakeLock.acquire(WAKELOCK_TIMEOUT);
            } else {
                wakeLock = null;
            }
        } catch (Exception e) {
            wakeLock = null;
            e.printStackTrace();
        }

            try {
                SignalGenerator.SignalGeneratorConfig sgc = new SignalGenerator.SignalGeneratorConfig();
                sgc.amplitude=volume*10;
                sgc.bitsPerSample=16;
                sgc.doNotModulateStandard=false;
                sgc.initialSilence=1;
                sgc.numChannels=(stereo?2:1);
                sgc.postProcessingString="";
                sgc.rightChannelOnly = (stereo);
                sgc.sampleRate=sampleRate;
                sgc.bufferSize=sgc.sampleRate;
                sgc.signedSamples=true;
                sgc.terminalSilence=1;
                sgc.waveForm=square?0:-1;
                sgc.invertPolarity=invertPolarity;

                SignalGenerator sg = new SignalGenerator(instructions,sgc,this);
                sg.run();
            }
            catch (Exception e) {
                e.printStackTrace();
                lastException=e;
            } finally {
                if (wakeLock != null) wakeLock.release();
            }

            setProgress(100);
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        parentActivity.get().setProgressBar(progress[0]);
    }

    protected void onPostExecute(Void v) {
        setControlsForTermination();
        if (lastException != null) {
            parentActivity.get().displayPostTaskAlert(R.string.msg_unable_to_process_tit,Utils.getExceptionMessage(lastException));
            lastException.printStackTrace();
        }
        parentActivity.get().setPlaybackInProgress(false);
        parentActivity.get().changeTapePicture(false);

    }

    protected void onCancelled() {
        setControlsForTermination();
        if (lastException != null) {
            parentActivity.get().displayPostTaskAlert(R.string.msg_unable_to_process_tit,Utils.getExceptionMessage(lastException));
            lastException.printStackTrace();
        } else {
            parentActivity.get().displayPostTaskAlert(R.string.msg_proc_cancel_tit,parentActivity.get().getString(R.string.msg_proc_cancel));
        }
        parentActivity.get().setProgressBar(0);
        parentActivity.get().setPlaybackInProgress(false);
        parentActivity.get().changeTapePicture(false);
    }

    private void setControlsForTermination() {
        parentActivity.get().setPlayBackViewsEnabled(false);
        parentActivity.get().setProgressBar(0);
    }

    protected void onPreExecute() {
        parentActivity.get().setPlayBackViewsEnabled(true);
    }

    public void setProgress(int statusPercent) {
        publishProgress(statusPercent);
    }
}
