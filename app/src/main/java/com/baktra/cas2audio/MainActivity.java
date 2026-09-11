package com.baktra.cas2audio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.baktra.cas2audio.tapeimage.TapeImage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private CasTask casTask;
    private ConversionCrate currentConversionCrate;

    private final String LN_SP;
    Uri currentUri;
    private boolean playbackInProgress;
    private PowerManager powerManager;
    File lastChooserDirectory;

    private UserSettings userSettings;

    private final ArrayList<View> playBackViewsDisabled;
    private final ArrayList<MenuItem> playBackMenuItemsDisabled;
    private final ArrayList<View> playBackViewsEnabled;
    private final ArrayList<MenuItem> playBackMenuItemsEnabled;

    private TapeImageHistory tapeImageHistory;

    public static final int STOP_REASON_STOP=0;
    public static final int STOP_REASON_PAUSE=1;
    private int stopReason;



    public MainActivity() {
        super();
        LN_SP = System.lineSeparator();
        casTask = null;
        currentUri = null;
        playbackInProgress = false;
        playBackViewsDisabled = new ArrayList<>(8);
        playBackViewsEnabled = new ArrayList<>(8);
        playBackMenuItemsDisabled = new ArrayList<>(1);
        playBackMenuItemsEnabled = new ArrayList<>(1);

        lastChooserDirectory = null;
        tapeImageHistory = new TapeImageHistory();
        userSettings = new UserSettings();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Widgets to be disabled during playback*/
        playBackViewsDisabled.add(getBrowseButton());
        playBackViewsDisabled.add(findViewById(R.id.btnPlay));
        playBackViewsDisabled.add(findViewById(R.id.btnRecent));

        /*Widgets to be enabled during playback*/
        playBackViewsEnabled.add(findViewById(R.id.btnStop));
        playBackViewsEnabled.add(findViewById(R.id.btnPause));

        /*Restore preferences from permanent storage*/
        restorePreferences();

        /*Try to get a power manager*/
        try {
            powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        } catch (Exception e) {
            powerManager = null;
            e.printStackTrace();
        }

        /*Set the title*/
        setTitle("CAS2Audio 1.0.6-test-02");
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        playBackMenuItemsDisabled.add(menu.findItem(R.id.miSettings));

        return true;
    }


    protected void onResume() {

        super.onResume();

        /*If playback in progress, keep components as they were*/
        if (playbackInProgress) return;

        /*If the current uri==null, then try to get input file from intent*/
        if (currentUri == null) {

            Intent intent = getIntent();
            Uri u = intent.getData();

            /*Valid path selected with intent*/
            if (u != null) {
                String filename = extractFileNameFromURI(u);
                setCurrentFileName(filename);
                setPlayBackViewsEnabled(false);
                currentUri = u;
            }
            /*There was some intent, but no valid path selected.*/
            else {
                setCurrentFileName("");
                setPlayBackViewsEnabled(false);
                currentUri = null;
            }

        }
        /*Activity was resumed, we are still open with valid tape image, and no playback is in progress*/
        else {
            updateUIForFile();
        }

    }

    protected void onStop() {
        super.onStop();
        storePreferences();
    }

    protected void onDestroy() {
        super.onDestroy();
        if (casTask != null) {
            casTask.cancel(true);
        }
    }

    public void onPlay(View v) {

        getProgressBar().setProgress(0);

        int[] instructions;
        InputStream iStream;

        /*Check if anything was selected*/
        if (currentUri == null || currentConversionCrate == null) {
            displaySimpleAlert(getString(R.string.msg_nothing_to_play_tit),getResources().getString(R.string.msg_nothing_to_play));
            return;
        }

        /*Create new background task*/
        try {
            casTask = new CasTask(
                    currentConversionCrate.getInstructions(),
                    this,
                    !userSettings.isDoMono(),
                    userSettings.isDoSquareWave(),
                    getVolume(),
                    currentConversionCrate.sampleRate,
                    userSettings.isDoInvertPolarity(),
                    getResumeIp()
            );
        } catch (Exception e) {
            displaySimpleAlert(getString(R.string.msg_unable_to_process_tit),Utils.getExceptionMessage(e));
        }

        /*Execute the task*/
        setPlaybackInProgress(true);
        changeTapePicture(true);
        casTask.execute();
    }



    private void setChunkDisplay(ArrayList<ResumePoint> resumePoints) {

        ListView lv = (ListView)findViewById(R.id.lvChunks);
        ResumePointAdapter rpa = new ResumePointAdapter(this,lv,resumePoints,0);
        lv.setAdapter(rpa);
        rpa.notifyDataSetChanged();

        if (lv.getOnItemClickListener()==null) {
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    onClickChunks(adapterView,view,i,l);
                }
            });
        }

    }

    public void onDisplayChunks(View v) {
        View lv = findViewById(R.id.lvChunks);

        int visibility = lv.getVisibility();
        if (visibility==View.VISIBLE) {
            visibility=View.INVISIBLE;
        }
        else {
            visibility=View.VISIBLE;
        }

        lv.setVisibility(visibility);
    }

    public void onClickChunks(AdapterView<?> adapterView, View view, int i, long l) {
        ListView lv = (ListView)adapterView;
        ResumePointAdapter rpa = (ResumePointAdapter) lv.getAdapter();
        rpa.setSelectedIndex(i);
    }

    private int getResumeIp() {
        ListView lv = (ListView)findViewById((R.id.lvChunks));
        ResumePointAdapter rpa = (ResumePointAdapter)lv.getAdapter();
        ResumePoint rp = (ResumePoint)rpa.getSelectedItem();
        if (rp==null) {
            return -1;
        }
        else {
            return rp.resumeIp;
        }
    }

    public void onStopPlaying(View v) {

        if (v==findViewById(R.id.btnPause)) {
            stopReason=STOP_REASON_PAUSE;
        }
        else {
            stopReason=STOP_REASON_STOP;
        }

        if (casTask !=null ) {
            casTask.cancel(true);
        }
    }

    public void onSettings(View v) {
        doSettings();

    }
    public void onSettings(MenuItem item) {
        doSettings();
    }
    public void doSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra("user_settings", this.userSettings);
        startActivityForResult(intent, OPEN_SETTINGS);
    }

    public void onRecent(View v) {
        Intent intent = new Intent(this, RecentActivity.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra("recent_items", tapeImageHistory.createPersistenceString());
        startActivityForResult(intent, OPEN_RECENT);
    }

    /*Browse for a tape image*/
    public void onBrowseTapeImage(android.view.View view) {

        /*Ask for document selection*/
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (lastChooserDirectory!=null && lastChooserDirectory.exists() && lastChooserDirectory.isDirectory()) {
            Uri pickerInitialUri = Uri.fromFile(lastChooserDirectory);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        }
        startActivityForResult(intent, PICK_CAS_FILE);

    }
    private static final int PICK_CAS_FILE = 102;
    private static final int OPEN_SETTINGS =103;
    private static final int OPEN_RECENT = 104;


    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /*Handle the settings activity*/
        if (requestCode==OPEN_SETTINGS && resultCode==Activity.RESULT_OK) {
            if (data != null) {
                this.userSettings = (UserSettings) data.getSerializableExtra("user_settings");
            }
        }

        /*Handle .CAS file pickup*/
        else if ((requestCode==PICK_CAS_FILE || requestCode==OPEN_RECENT) && resultCode==Activity.RESULT_OK) {
            if (data != null) {
                Uri candidateUri = data.getData();

                /*If no URI, just be done*/
                if (candidateUri==null) return;

                /*Check if valid tape image*/
                /*Try to open the tape image - short, can be in the event thread*/
                InputStream iStream=null;

                try  {
                    /*Get the persmission*/
                    getContentResolver().takePersistableUriPermission(candidateUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    /*Open for input stream*/
                    iStream = getContentResolver().openInputStream(candidateUri);
                    TapeImage ti = new TapeImage();
                    ti.parse(iStream);

                    /*Perform the conversion*/
                    TapeImageProcessor tip = new TapeImageProcessor();
                    currentConversionCrate = tip.convertItem(ti,userSettings.isDo48kHz()?48000:44100 , false);
                    setChunkDisplay(currentConversionCrate.resumePoints);

                } catch (Exception e) {
                    candidateUri=null;
                    currentConversionCrate=null;
                    setChunkDisplay(new ArrayList<>());
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });

                    String primaryReasonString;

                    if (e instanceof FileFormatException) {
                        primaryReasonString = getString(R.string.msg_file_not_tape_image);
                    }
                    else {
                        primaryReasonString = getString(R.string.msg_file_unable_open);
                    }
                    builder.setMessage(String.format("%s%n%s",primaryReasonString,Utils.getExceptionMessage(e)));
                    builder.setTitle(getString(R.string.msg_file_unable_open_tit));
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    e.printStackTrace();
                }

                finally {
                    try {
                        if (iStream != null) iStream.close();
                    }
                    catch(IOException ioe) {
                        /*Nothing we can do*/
                    }

                    /*Update the user interface*/
                    if (candidateUri!=null) {
                        currentUri=candidateUri;
                        updateUIForFile();
                    }
                }

            }

        }

    }




    void updateUIForFile() {
        String filename = extractFileNameFromURI(currentUri);
        setCurrentFileName(filename);
        setPlayBackViewsEnabled(false);
        tapeImageHistory.addHistoryItem(currentUri,filename);
    }


    private int getVolume() {
        return userSettings.getAmplitude();
    }

    private ProgressBar getProgressBar() {
        return findViewById(R.id.pbProgress);
    }

    private ImageButton getBrowseButton() {
        return findViewById(R.id.btnBrowse);
    }

    private void setCurrentFileName(String filename) {
        TextView tv = findViewById(R.id.tvTapeImageName);
        tv.setText(filename);
    }

    void setPlaybackInProgress(boolean b) {
        playbackInProgress = b;
    }

    void changeTapePicture(boolean isActive) {

        ImageView iv = findViewById(R.id.ivCassette);

        if (isActive) {
            iv.setImageDrawable(getResources().getDrawable(R.drawable.tape_animation));
            AnimationDrawable ad = (AnimationDrawable)iv.getDrawable();
            ad.start();
        }
        else {
            iv.setImageDrawable(getResources().getDrawable(R.drawable.tape_animation));
            if (iv.getDrawable() instanceof AnimationDrawable) {
                AnimationDrawable ad = (AnimationDrawable) iv.getDrawable();
                ad.stop();
            }
            iv.setImageDrawable(getResources().getDrawable(R.drawable.tape_inactive));
        }
    }

    void setPlayBackViewsEnabled(boolean b) {
        for (View v : playBackViewsDisabled) {
            v.setEnabled(!b);
        }
        for (View v : playBackViewsEnabled) {
            v.setEnabled(b);
        }

        for (MenuItem mi: playBackMenuItemsDisabled) {
            mi.setEnabled(!b);
        }
        for (MenuItem mi: playBackMenuItemsEnabled) {
            mi.setEnabled(b);
        }
    }

    public void displayPostTaskAlert(int titleId, String msg) {
        displaySimpleAlert(getResources().getString(titleId),msg);
    }

    void setProgressBar(int value) {
        getProgressBar().setProgress(value);
    }

    void setResumePoint(int ip) {
        ListView lv = (ListView)findViewById(R.id.lvChunks);
        ResumePointAdapter rpa = (ResumePointAdapter)lv.getAdapter();

        if (stopReason==STOP_REASON_PAUSE) {
            rpa.setResumePoint(ip);
        }
        else {
            rpa.setResumePoint(0);
        }
    }

    private String extractFileNameFromURI(Uri uri) {
        String result = null;

        /*Let us have content URI*/
        if (uri.getScheme()!=null && uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst() &&cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)>=0) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        /*If not content URI, consider it a file*/
        if (result == null) {
            File f = new File(uri.toString());
            result = f.getName();
        }
        return result;

    }

    public PowerManager getPowerManager() {
        return powerManager;
    }

    private void restorePreferences() {
        SharedPreferences sPref = this.getPreferences(Context.MODE_PRIVATE);
        lastChooserDirectory = new File(sPref.getString("c2a_last_dir", ""));
        try {
            tapeImageHistory.parsePersistenceString(sPref.getString("c2a_recents", ""));
        }
        catch (Exception e) {
            tapeImageHistory.clear();
        }
        userSettings = UserSettings.createFromPersistentStorage(sPref);
        findViewById(R.id.lvChunks).setVisibility(sPref.getInt("c2a_chunks",View.INVISIBLE));
    }

    private void storePreferences() {

        SharedPreferences sPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();

        /*Current state of the UI*/
        if (lastChooserDirectory != null) {
            editor.putString("c2a_last_dir", lastChooserDirectory.getAbsolutePath());
        }
        String recentString = tapeImageHistory.createPersistenceString();
        editor.putString("c2a_recents", recentString);
        editor.putInt("c2a_chunks",findViewById(R.id.lvChunks).getVisibility());
        editor.apply();

        /*General settings*/
        UserSettings.flushToPersistentStorage(userSettings, sPref);

    }

    private void displaySimpleAlert(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onAbout(MenuItem mi) {
        Toast.makeText(getApplicationContext(),"CAS2Audio by BAKTRA Software",Toast.LENGTH_LONG).show();
    }



}
