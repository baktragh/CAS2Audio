package com.baktra.cas2audio;

import android.os.AsyncTask;
import android.os.PowerManager;

import com.baktra.cas2audio.signal.SignalGenerator;

public class CasTask extends AsyncTask<Void,Integer,Void> {

    private final boolean stereo;
    private final boolean square;
    private final int volume;
    private final int[] instructions;
    private Exception lastException;
    private final MainActivity parentActivity;
    private final int sampleRate;
    private PowerManager.WakeLock wakeLock;

    public CasTask(int[] instructions, MainActivity mainActivity, boolean stereo, boolean square, int volume, int sampleRate) {
        this.instructions=instructions;
        this.stereo=stereo;
        this.lastException=null;
        this.parentActivity=mainActivity;
        this.square=square;
        this.volume=volume;
        this.sampleRate=sampleRate;
        this.wakeLock = null;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        try {
            PowerManager pm = parentActivity.getPowerManager();
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CAS2Audio::TaskWakeLock");
                wakeLock.acquire(120 * 60 * 1000);
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
        parentActivity.setProgressBar(progress[0]);
    }

    protected void onPostExecute(Void v) {
       setControlsForTermination();
       if (lastException!=null) {
            parentActivity.setErrorText(Utils.getExceptionMessage(lastException));
            lastException.printStackTrace();
       }
       else {
           parentActivity.setErrorText("Tape image processed successfully");
       }
       parentActivity.setPlaybackInProgress(false);
    }

    protected void onCancelled() {
        setControlsForTermination();
        if (lastException!=null) {
            parentActivity.setErrorText(Utils.getExceptionMessage(lastException));
            lastException.printStackTrace();
        }
        else {
            parentActivity.setErrorText("Tape image processing cancelled");
        }
        parentActivity.setProgressBar(0);
        parentActivity.setPlaybackInProgress(false);

    }

    private void setControlsForTermination() {
        parentActivity.setPlayBackViewsEnabled(false);
    }

    protected void onPreExecute() {
        parentActivity.setPlayBackViewsEnabled(true);
    }

    public void setProgress(int statusPercent) {
        publishProgress(statusPercent);
    }
}
