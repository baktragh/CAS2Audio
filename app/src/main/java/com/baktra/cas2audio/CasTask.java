package com.baktra.cas2audio;

import android.os.AsyncTask;
import android.view.View;

public class CasTask extends AsyncTask<Void,Integer,Void> {

    private String filename;
    private View startView;
    private View stopView;

    public CasTask(String filename,View startView,View stopView) {
        this.filename=filename;
        this.startView=startView;
        this.stopView=stopView;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        System.out.println("Starting in background");

        while(!isCancelled()) {

            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ie) {
                /*What we can do?*/
            }
            System.out.println("Doing in background");
        }

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
