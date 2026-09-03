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
import android.view.View;
import android.widget.*;

import com.baktra.cas2audio.tapeimage.TapeImage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private CasTask casTask;
    private final String LN_SP;
    Uri currentUri;
    private boolean playbackInProgress;
    private PowerManager powerManager;
    File lastChooserDirectory;

    private UserSettings userSettings;

    private final ArrayList<View> playBackViewsDisabled;
    private final ArrayList<View> playBackViewsEnabled;

    private final ArrayList<RecentItem> recentItems;

    public MainActivity() {
        super();
        LN_SP = System.lineSeparator();
        casTask = null;
        currentUri = null;
        playbackInProgress = false;
        playBackViewsDisabled = new ArrayList<>(8);
        playBackViewsEnabled = new ArrayList<>(8);
        lastChooserDirectory = null;
        recentItems = new ArrayList<>();
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
        playBackViewsDisabled.add(findViewById(R.id.btnSettings));

        /*Widgets to be enabled during playback*/
        playBackViewsEnabled.add(findViewById(R.id.btnStop));

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
        setTitle("CAS2Audio 1.0.5");

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
        if (currentUri == null) {
            displaySimpleAlert(getString(R.string.msg_nothing_to_play_tit),getResources().getString(R.string.msg_nothing_to_play));
            return;
        }

        /*Try to open the tape image - short, can be in  the even thread*/
        try {
            iStream = getContentResolver().openInputStream(currentUri);

        } catch (Exception e) {
            displaySimpleAlert(getString(R.string.msg_unable_to_open_tit),getResources().getString(R.string.msg_unable_to_open)+":" + LN_SP + Utils.getExceptionMessage(e));
            return;
        }

        int sampleRate = userSettings.isDo48kHz() ? 48000 : 44100;

        /*Try to process the tape image*/
        try {
            TapeImageProcessor tip = new TapeImageProcessor();
            instructions = tip.convertItem(iStream, sampleRate, false);
        } catch (Exception e) {
            displaySimpleAlert(getString(R.string.msg_unable_to_process_tit),getResources().getString(R.string.msg_unable_to_process)+":" + LN_SP + Utils.getExceptionMessage(e));
            return;
        }

        /*Create new background task*/
        try {
            casTask = new CasTask(instructions, this, !userSettings.isDoMono(), userSettings.isDoSquareWave(), getVolume(), sampleRate, userSettings.isDoInvertPolarity());
        } catch (Exception e) {
            displaySimpleAlert(getString(R.string.msg_unable_to_process_tit),Utils.getExceptionMessage(e));
        }
        /*Execute the task*/
        casTask.execute();
        setPlaybackInProgress(true);
        changeTapePicture(true);

    }

    public void onStopPlaying(View v) {
        if (casTask != null) {
            casTask.cancel(true);
        }
    }

    public void onSettings(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra("user_settings", this.userSettings);
        startActivityForResult(intent, OPEN_SETTINGS);
    }

    public void onRecent(View v) {
        Intent intent = new Intent(this, RecentActivity.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra("recent_items", RecentItem.createPersistenceString(recentItems));
        startActivityForResult(intent, OPEN_RECENT);
    }

    /*Browse for a tape image*/
    public void onBrowseTapeImage(android.view.View view) {

        /*First, stop playing, this will set the controls*/
        onStopPlaying(view);

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

                /*Get permissions for that URI*/
                getContentResolver().takePersistableUriPermission(candidateUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);

                /*Check if valid tape image*/
                /*Try to open the tape image - short, can be in  the even thread*/
                InputStream iStream=null;

                try  {
                    iStream = getContentResolver().openInputStream(candidateUri);
                    TapeImage ti = new TapeImage();
                    ti.parse(iStream);

                } catch (Exception e) {
                    candidateUri=null;
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    builder.setMessage(String.format("%s%n%s",getString(R.string.msg_not_a_tape_image),Utils.getExceptionMessage(e)));
                    builder.setTitle(getString(R.string.msg_not_a_tape_image_tit));
                    AlertDialog dialog = builder.create();
                    dialog.show();
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
        addRecentItem(currentUri,filename);
    }

    void addRecentItem(Uri uri, String filename) {
        RecentItem candidateItem = new RecentItem(uri,filename);

        /*Check if already there*/
        boolean found = false;
        for (RecentItem item : recentItems) {
            if (item.uri.toString().equals(candidateItem.uri.toString())) {
                found = true;
                break;
            }
        }
        /*If already there, just return*/
        if (found) return;

        /*Move to front*/
        recentItems.add(0, candidateItem);
        if (recentItems.size() > 12) recentItems.remove(11);
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
    }

    public void displayPostTaskAlert(int titleId, String msg) {
        displaySimpleAlert(getResources().getString(titleId),msg);
    }

    void setProgressBar(int value) {
        getProgressBar().setProgress(value);
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
            RecentItem.parsePersistenceString(sPref.getString("c2a_recents", ""), recentItems);
        }
        catch (Exception e) {
            recentItems.clear();
        }
        userSettings = UserSettings.createFromPersistentStorage(sPref);
    }

    private void storePreferences() {

        SharedPreferences sPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();

        /*Current state of the UI*/
        if (lastChooserDirectory != null) {
            editor.putString("c2a_last_dir", lastChooserDirectory.getAbsolutePath());
        }
        String recentString = RecentItem.createPersistenceString(recentItems);
        editor.putString("c2a_recents", recentString);
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



}
