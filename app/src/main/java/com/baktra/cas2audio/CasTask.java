package com.baktra.cas2audio;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CasTask extends AsyncTask<Void,Integer,Void> {

    private final boolean stereo;
    private int[] instructions;
    private View startView;
    private View stopView;
    private Exception lastException;
    private TextView errorText;
    private ProgressBar progressBar;

    public CasTask(int[] instructions, View startView, View stopView, boolean stereo, TextView errorText,ProgressBar progressBar) {
        this.instructions=instructions;
        this.startView=startView;
        this.stopView=stopView;
        this.stereo=stereo;
        this.lastException=null;
        this.errorText=errorText;
        this.progressBar=progressBar;
    }

    @Override
    protected Void doInBackground(Void... voids) {

            try {
                SignalGenerator.SignalGeneratorConfig sgc = new SignalGenerator.SignalGeneratorConfig();
                sgc.amplitude=75;
                sgc.bitsPerSample=16;
                sgc.doNotModulateStandard=false;
                sgc.initialSilence=1;
                sgc.numChannels=(stereo?2:1);
                sgc.postProcessingString="";
                sgc.rightChannelOnly=(stereo?true:false);
                sgc.sampleRate=44100;
                sgc.bufferSize=sgc.sampleRate;
                sgc.signedSamples=true;
                sgc.terminalSilence=1;
                sgc.waveForm=-1;

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
        progressBar.setProgress(progress[0]);
    }

    protected void onPostExecute(Void v) {
       setControlsForTermination();
       if (lastException!=null) {
            errorText.setText(Utils.getExceptionMessage(lastException));
            lastException.printStackTrace();
       }
       else {
           errorText.setText("Tape image processed succesfully");
       }
    }

    protected void onCancelled() {
        setControlsForTermination();
        if (lastException!=null) {
            errorText.setText(Utils.getExceptionMessage(lastException));
            lastException.printStackTrace();
        }
        else {
            errorText.setText("Tape image processing cancelled");
        }
        progressBar.setProgress(0);
    }

    private void setControlsForTermination() {
        stopView.setEnabled(false);
        startView.setEnabled(true);
    }

    protected void onPreExecute() {
        stopView.setEnabled(true);
        startView.setEnabled(false);
    }


    public void setProgress(int statusPercent) {
        publishProgress(statusPercent);
    }
}
