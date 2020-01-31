package com.baktra.cas2audio;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private CasTask casTask;
    private final String lnsp;

    public MainActivity() {
        super();
        lnsp=System.getProperty("line.separator");
        casTask=null;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    protected void onResume() {

        super.onResume();
        Intent intent = getIntent();
        Uri u = intent.getData();
        TextView msgText = getMessageWidget();

        if (u!=null && u.getPath()!=null) {
            String filename = new File(u.getPath()).getName();
            ((TextView) findViewById(R.id.textView)).setText(filename);
            msgText.setText("Click PLAY to interpret the tape image");
            findViewById(R.id.btnPlay).setEnabled(true);
        }
        else {
            ((TextView) findViewById(R.id.textView)).setText("CAS2Audio");
            msgText.setText("No tape image selected."+lnsp+"Go to your favorite file manager, select tape image, and then select this application to open it.");
            findViewById(R.id.btnPlay).setEnabled(false);

        }
    }

    protected void onStop() {
        super.onStop();

    }

    public void onPlay(View v) {

        /*Clear text and progress*/
        getMessageWidget().setText("");
        getProgressBar().setProgress(0);

        int[] instructions = null;
        InputStream iStream = null;

        /*Try to open the tape image - short, can be in  the even thread*/
        try {
            iStream = getContentResolver().openInputStream(getIntent().getData());
        }
        catch (Exception e) {
            getMessageWidget().setText("Unable to open the tape image:"+lnsp+Utils.getExceptionMessage(e));
            return;
        }

        /*Try to process the tape image*/
        try {
            TapeImageProcessor tip = new TapeImageProcessor();
            instructions = tip.convertItem(iStream,44100);
        }
        catch (Exception e) {
            getMessageWidget().setText("Unable to process the tape image:"+lnsp+Utils.getExceptionMessage(e));
            return;
        }

        /*Create new background task*/
        try {
            casTask = new CasTask(instructions,findViewById(R.id.btnPlay),findViewById(R.id.btnStop),isStereo(),getMessageWidget(),getProgressBar());
        }
        catch (Exception e) {
            getMessageWidget().setText(Utils.getExceptionMessage(e));
        }
        /*Execute the task*/
        casTask.execute();

    }

    public void onStopPlaying(View v) {
        if (casTask!=null) {
            casTask.cancel(true);
        }
    }

    protected TextView getMessageWidget() {
        return ((TextView) findViewById(R.id.mltMessages));
    }

    private boolean isStereo() {
        return ((Switch) findViewById(R.id.swChannels)).isSelected();
    }

    private ProgressBar getProgressBar() {
        return ((ProgressBar) findViewById(R.id.pbProgress));
    }



}
