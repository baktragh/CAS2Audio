package com.baktra.cas2audio;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private CasTask casTask;
    private final String lnsp;
    private static final int BROWSE_REQUEST_CODE = 100;
    private Uri currentUri;
    private boolean playbackInProgress;

    private ArrayList<View> playBackViewsDisabled;
    private ArrayList<View> playBackViewsEnabled;

    public MainActivity() {
        super();
        lnsp=System.getProperty("line.separator");
        casTask=null;
        currentUri=null;
        playbackInProgress=false;
        playBackViewsDisabled = new ArrayList<>();
        playBackViewsEnabled = new ArrayList<>();



    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playBackViewsDisabled.add(getBrowseButton());
        playBackViewsDisabled.add(findViewById(R.id.btnPlay));
        playBackViewsDisabled.add(findViewById(R.id.swChannels));
        playBackViewsDisabled.add(findViewById(R.id.swSquareWave));
        playBackViewsDisabled.add(findViewById(R.id.sbVolume));
        playBackViewsEnabled.add(findViewById(R.id.btnStop));


    }

    protected void onResume() {

        super.onResume();
        TextView msgText = getMessageWidget();

        /*If playback in progress, keep components as they were*/
        if (playbackInProgress) return;

        /*If the current uri==null, then try to get input file from intent*/
        if (currentUri==null) {

            Intent intent = getIntent();
            Uri u = intent.getData();

            /*Valid path selected*/
            if (u!=null && u.getPath()!=null) {
                String filename = extractFileNameFromURI(u);
                setCurrentFileName(filename);
                setPlayBackViewsEnabled(false);
                currentUri=u;
            }
            /*There was some intent, but no valid path selected*/
            else {
                setCurrentFileName("CAS2Audio");
                msgText.setText(
                        "No tape image selected. Click the 'Browse for tape image...' button, or "+
                                "Go to your favorite file manager, select a tape image, and then select this application to open it."
                );
                setPlayBackViewsEnabled(false);
                currentUri=null;
            }

        }
        /*Activity was resumed, we are still open with valid tape image, and no playback is in progress*/
        else {
            String filename = extractFileNameFromURI(currentUri);
            setCurrentFileName(filename);
            setPlayBackViewsEnabled(false);
            msgText.setText("");
        }

    }

    protected void onStop() {
        super.onStop();
    }

    protected void onDestroy() {
        super.onDestroy();
        if (casTask!=null) {
            casTask.cancel(true);
        }
    }

    public void onPlay(View v) {

        /*Clear text and progress*/
        getMessageWidget().setText("");
        getProgressBar().setProgress(0);

        int[] instructions = null;
        InputStream iStream = null;

        /*Check if anything was selected*/
        if (currentUri==null) {
            getMessageWidget().setText("Unable to play. No tape image was selected");
            return;
        }


        /*Try to open the tape image - short, can be in  the even thread*/
        try {
            iStream = getContentResolver().openInputStream(currentUri);
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
            casTask = new CasTask(instructions,getMessageWidget(),getProgressBar(),this,isStereo(),isSquare(),getVolume());
        }
        catch (Exception e) {
            getMessageWidget().setText(Utils.getExceptionMessage(e));
        }
        /*Execute the task*/
        casTask.execute();
        setPlaybackInProgress(true);

    }

    public void onStopPlaying(View v) {
        if (casTask!=null) {
            casTask.cancel(true);
        }
    }

    /*Browse for a tape image*/
    public void onBrowseTapeImage(android.view.View view) {

        /*First, stop playing, this will set the controls*/
        onStopPlaying(view);

        /*Open a picker*/
        Intent browseIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        browseIntent.setType("*/*");
        startActivityForResult(browseIntent, BROWSE_REQUEST_CODE);

        /*And then let the activity resume with the selected file*/
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        switch (requestCode) {
            case BROWSE_REQUEST_CODE: {
                if (resultCode==RESULT_OK && resultData !=null) {
                    Uri resUri = resultData.getData();
                    currentUri=resUri;
                }
                break;
            }
        }
    }

    protected TextView getMessageWidget() {
        return ((TextView) findViewById(R.id.mltMessages));
    }

    private boolean isStereo() {
        return ((Switch) findViewById(R.id.swChannels)).isSelected();
    }
    private boolean isSquare() { return ((Switch) findViewById(R.id.swSquareWave)).isSelected();}
    private int getVolume() { return ((SeekBar) findViewById(R.id.sbVolume)).getProgress();}
    private ProgressBar getProgressBar() {
        return ((ProgressBar) findViewById(R.id.pbProgress));
    }

    private Button getBrowseButton() {
        return ((Button) findViewById(R.id.btnBrowse));
    }

    private void setCurrentFileName(String filename) {
        ((TextView) findViewById(R.id.textView)).setText(filename);
    }

    void setPlaybackInProgress(boolean b) {
        playbackInProgress=b;
    }

    void setPlayBackViewsEnabled(boolean b) {
        for (View v:playBackViewsDisabled) {
            v.setEnabled(!b);
        }
        for (View v:playBackViewsEnabled) {
            v.setEnabled(b);
        }
    }

    void setErrorText(String s) {
        getMessageWidget().setText(s);
    }

    void setProgressBar(int value) {
        getProgressBar().setProgress(value);
    }

    private String extractFileNameFromURI(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            File f = new File (uri.getPath());
            result = f.getName();
        }
        return result;

    }


}
