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
    private Exception lastException;
    private final MainViewModel parentModel;
    private final int sampleRate;
    private PowerManager.WakeLock wakeLock;

    SignalGenerator sg;

    public static final int WAKELOCK_TIMEOUT = 120 * 60 * 1000;

    public CasTask(int[] instructions, MainViewModel mainView, boolean stereo, boolean square, int volume, int sampleRate,boolean invertPolarity,int resumeIp) {
        this.instructions=instructions;
        this.stereo=stereo;
        this.lastException = null;
        this.parentModel = mainView;
        this.square = square;
        this.volume=volume;
        this.sampleRate=sampleRate;
        this.invertPolarity=invertPolarity;
        this.wakeLock = null;
        this.resumeIp=resumeIp;
        this.sg=null;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        try {
            PowerManager pm = parentModel.getPowerManager();
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
                sgc.resumeIp = resumeIp;
                sg = new SignalGenerator(instructions,sgc,this);
                sg.run();
                parentModel.setProgressValue(100);
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

            parentModel.setProgressValue(progress[0]);
            if (progress[1] != -1) {
                parentModel.setResumePoint(progress[1]);
            }


    }

    protected void onPostExecute(Void v) {

        /*If the parent activity still exists, full termination*/
        //parentModel.setControlsForTermination();

        if (lastException != null) {
            //parentModel.get().displayPostTaskAlert(R.string.msg_unable_to_process_tit,Utils.getExceptionMessage(lastException));
            lastException.printStackTrace();
        }

        parentModel.handlePlaybackEndedNormal();

        //parentModel.setResumePoint(0);
    }

    protected void onCancelled() {

        /*Calculate possible resume point*/
        int lastIp = 0;
        if (sg!=null) lastIp=sg.getLastIp();

        /*If the parent activity still exists, full cancellation*/

        if (lastException != null) {
            //parentModel.get().displayPostTaskAlert(R.string.msg_unable_to_process_tit,Utils.getExceptionMessage(lastException));
            lastException.printStackTrace();
        }
        parentModel.handlePlaybackCancelled(lastIp);

    }


    protected void onPreExecute() {

    }

    public void setProgress(int statusPercent,int ip) {
        publishProgress(statusPercent,ip);
    }
}
