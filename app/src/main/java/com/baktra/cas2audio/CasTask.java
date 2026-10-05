package com.baktra.cas2audio;

import android.os.AsyncTask;
import android.os.PowerManager;

import com.baktra.cas2audio.signal.SignalGenerator;

public class CasTask extends AsyncTask<Void,Integer,Void> {

    private final boolean stereo;
    private final boolean square;
    private final int volume;
    private final int[] instructions;
    private final boolean invertPolarity;
    private final int resumeIp;
    private final PowerManager powerManager;
    private Exception lastException;
    private final CasTaskObserver taskObserver;
    private final int sampleRate;
    private PowerManager.WakeLock wakeLock;

    SignalGenerator sg;

    public static final int WAKELOCK_TIMEOUT = 120 * 60 * 1000;

    public CasTask(int[] instructions, CasTaskObserver observer, boolean stereo, boolean square, int volume, int sampleRate,boolean invertPolarity,int resumeIp,PowerManager pm) {
        this.instructions=instructions;
        this.stereo=stereo;
        this.lastException = null;
        this.taskObserver = observer;
        this.square = square;
        this.volume=volume;
        this.sampleRate=sampleRate;
        this.invertPolarity=invertPolarity;
        this.wakeLock = null;
        this.resumeIp=resumeIp;
        this.sg=null;
        this.powerManager=pm;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        try {

            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CAS2Audio::TaskWakeLock");
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
                sgc.resumeIp = resumeIp;
                sg = new SignalGenerator(instructions,sgc,this);
                sg.run();
                setProgress(0,-1);
            }
            catch (Exception e) {
                e.printStackTrace();
                lastException=e;
            } finally {
                if (wakeLock != null) wakeLock.release();
            }


        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {

            taskObserver.onProgressUpdate(progress[0]);
            if (progress[1] != -1) {
                taskObserver.onResumePointUpdate(progress[1]);
            }

    }

    protected void onPostExecute(Void v) {

        if (lastException != null) {
            taskObserver.onFailedPlayback(lastException);
            lastException.printStackTrace();
        }
        taskObserver.onSuccessfulPlayback();

    }

    protected void onCancelled() {

        /*Calculate possible resume point*/
        int lastIp = 0;
        if (sg!=null) lastIp=sg.getLastIp();

        if (lastException != null) {
            taskObserver.onFailedPlayback(lastException);
            lastException.printStackTrace();
        }
        taskObserver.onCancelledPlayback(lastIp);
    }


    protected void onPreExecute() {

    }

    public void setProgress(int statusPercent,int ip) {
        publishProgress(statusPercent,ip);
    }
}
