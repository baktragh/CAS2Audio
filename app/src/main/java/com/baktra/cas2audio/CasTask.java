package com.baktra.cas2audio;

import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.baktra.cas2audio.signal.SignalGenerator;

public class CasTask extends AsyncTask<Void,Integer,Void> {

    private final boolean stereo;
    private final boolean square;
    private final int volume;
    private int[] instructions;
    private Exception lastException;
    private MainActivity parentActivity;
    private int sampleRate;

    public CasTask(int[] instructions, TextView errorText, ProgressBar progressBar, MainActivity mainActivity, boolean stereo, boolean square, int volume,int sampleRate) {
        this.instructions=instructions;
        this.stereo=stereo;
        this.lastException=null;
        this.parentActivity=mainActivity;
        this.square=square;
        this.volume=volume;
        this.sampleRate=sampleRate;
    }

    @Override
    protected Void doInBackground(Void... voids) {

            try {
                SignalGenerator.SignalGeneratorConfig sgc = new SignalGenerator.SignalGeneratorConfig();
                sgc.amplitude=volume*10;
                sgc.bitsPerSample=16;
                sgc.doNotModulateStandard=false;
                sgc.initialSilence=1;
                sgc.numChannels=(stereo?2:1);
                sgc.postProcessingString="";
                sgc.rightChannelOnly=(stereo?true:false);
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
           parentActivity.setErrorText("Tape image processed succesfully");
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
