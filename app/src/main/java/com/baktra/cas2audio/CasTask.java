package com.baktra.cas2audio;

import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;

import java.io.InputStream;

public class CasTask extends AsyncTask<Void,Integer,Void> {

    private InputStream is;
    private View startView;
    private View stopView;

    public CasTask(InputStream stream, View startView, View stopView) {
        this.is=stream;
        this.startView=startView;
        this.stopView=stopView;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        System.out.println("Starting in background");


            TapeImageProcessor tip = new TapeImageProcessor();
            try {
                int[] instructions = tip.convertItem(is, 44100);
                SignalGenerator.SignalGeneratorConfig sgc = new SignalGenerator.SignalGeneratorConfig();
                sgc.amplitude=90;
                sgc.bitsPerSample=16;
                sgc.bufferSize=44100;
                sgc.doNotModulateStandard=false;
                sgc.initialSilence=1;
                sgc.numChannels=2;
                sgc.postProcessingString="";
                sgc.rightChannelOnly=false;
                sgc.sampleRate=44100;
                sgc.signedSamples=true;
                sgc.terminalSilence=1;
                sgc.waveForm=-1;

                SignalGenerator sg = new SignalGenerator("",instructions,false,SignalGenerator.TYPE_AUDIO,false,false,sgc);
                sg.run();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Doing in background");

        System.out.println("Done in background");

        return null;
    }

    protected void onPostExecute(Void v) {
       setControlsForTermination();
    }

    protected void onCancelled() {
        setControlsForTermination();
    }

    private void setControlsForTermination() {
        stopView.setEnabled(false);
        startView.setEnabled(true);
    }

    protected void onPreExecute() {
        stopView.setEnabled(true);
        startView.setEnabled(false);
    }
}
