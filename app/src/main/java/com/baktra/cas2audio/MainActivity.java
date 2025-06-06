package com.baktra.cas2audio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
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
        LN_SP = System.getProperty("line.separator");
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
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Widgets to be disabled during playback*/
        playBackViewsDisabled.add(getBrowseButton());
        playBackViewsDisabled.add(findViewById(R.id.btnPlay));
        playBackViewsDisabled.add(findViewById(R.id.sbVolume));
        playBackViewsDisabled.add(findViewById(R.id.tvAmplitude));
        playBackViewsDisabled.add(findViewById(R.id.lvRecentItems));
        playBackViewsDisabled.add(findViewById(R.id.btnSettings));
        playBackViewsDisabled.add(findViewById(R.id.btnClearHistory));

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

        /*Allow the Recent items to be clicked*/
        ListView lv = (ListView) findViewById(R.id.lvRecentItems);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                currentUri = recentItems.get(i).uri;
                updateUIForFile();
            }
        });


    }


    protected final void onResume() {

        super.onResume();
        TextView msgText = getMessageWidget();

        /*If playback in progress, keep components as they were*/
        if (playbackInProgress) return;

        /*If the current uri==null, then try to get input file from intent*/
        if (currentUri == null) {

            Intent intent = getIntent();
            Uri u = intent.getData();

            /*Valid path selected*/
            if (u != null && u.toString() != null) {
                String filename = extractFileNameFromURI(u);
                setCurrentFileName(filename);
                setPlayBackViewsEnabled(false);
                currentUri = u;
            }
            /*There was some intent, but no valid path selected.*/
            else {
                setCurrentFileName("CAS2Audio 1.0.5");
                msgText.setText(R.string.msg_notape);
                setPlayBackViewsEnabled(false);
                currentUri = null;
            }

        }
        /*Activity was resumed, we are still open with valid tape image, and no playback is in progress*/
        else {
            updateUIForFile();
        }

    }

    protected final void onStop() {
        super.onStop();
        storePreferences();
    }

    protected final void onDestroy() {
        super.onDestroy();
        if (casTask != null) {
            casTask.cancel(true);
        }
    }

    public final void onPlay(View v) {

        /*Clear text and progress*/
        getMessageWidget().setText("");
        getProgressBar().setProgress(0);

        int[] instructions;
        InputStream iStream;

        /*Check if anything was selected*/
        if (currentUri == null) {
            getMessageWidget().setText(R.string.msg_nothing_to_play);
            return;
        }


        /*Try to open the tape image - short, can be in  the even thread*/
        try {
            iStream = getContentResolver().openInputStream(currentUri);

        } catch (Exception e) {
            getMessageWidget().setText(R.string.msg_unable_to_open + ":" + LN_SP + Utils.getExceptionMessage(e));
            return;
        }

        int sampleRate = userSettings.isDo48kHz() ? 48000 : 44100;

        /*Try to process the tape image*/
        try {
            TapeImageProcessor tip = new TapeImageProcessor();
            instructions = tip.convertItem(iStream, sampleRate, false);
        } catch (Exception e) {
            getMessageWidget().setText(R.string.msg_unable_to_process + ":" + LN_SP + Utils.getExceptionMessage(e));
            return;
        }

        /*Create new background task*/
        try {
            casTask = new CasTask(instructions, this, !userSettings.isDoMono(), userSettings.isDoSquareWave(), getVolume(), sampleRate, userSettings.isDoInvertPolarity());
        } catch (Exception e) {
            getMessageWidget().setText(Utils.getExceptionMessage(e));
        }
        /*Execute the task*/
        casTask.execute();
        setPlaybackInProgress(true);

    }

    public final void onStopPlaying(View v) {
        if (casTask != null) {
            casTask.cancel(true);
        }
    }

    public final void onSettings(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra("user_settings", this.userSettings);
        startActivityForResult(intent, OPEN_SETTINGS);
    }

    private static final int PICK_CAS_FILE = 102;
    private static final int OPEN_SETTINGS =103;


    protected final void onActivityResult(int requestCode,
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
        else if (requestCode==PICK_CAS_FILE && resultCode==Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                /*Get permissions for that URI*/
                getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (uri!=null) {
                    currentUri=uri;
                    updateUIForFile();
                }
            }

        }

    }


    /*Browse for a tape image*/
    public final void onBrowseTapeImage(android.view.View view) {

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

    void updateUIForFile() {
        String filename = extractFileNameFromURI(currentUri);
        setCurrentFileName(filename);
        setPlayBackViewsEnabled(false);
        getMessageWidget().setText("");
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
        updateRecentItemsUI();

    }

    void updateRecentItemsUI() {
        ListView lvRecentItems = (ListView) findViewById(R.id.lvRecentItems);
        lvRecentItems.setAdapter(new ArrayAdapter<RecentItem>(this, R.layout.list_text, recentItems));
    }

    private TextView getMessageWidget() {
        return ((TextView) findViewById(R.id.mltMessages));
    }

    private int getVolume() {
        return ((SeekBar) findViewById(R.id.sbVolume)).getProgress();
    }

    private ProgressBar getProgressBar() {
        return ((ProgressBar) findViewById(R.id.pbProgress));
    }

    private Button getBrowseButton() {
        return ((Button) findViewById(R.id.btnBrowse));
    }

    private void setCurrentFileName(String filename) {
        setTitle(filename);
    }

    final void setPlaybackInProgress(boolean b) {
        playbackInProgress = b;
    }

    final void setPlayBackViewsEnabled(boolean b) {
        for (View v : playBackViewsDisabled) {
            v.setEnabled(!b);
        }
        for (View v : playBackViewsEnabled) {
            v.setEnabled(b);
        }
    }

    final void setErrorText(String s) {
        getMessageWidget().setText(s);
    }

    public final void setErrorText(int msgId) {
        getMessageWidget().setText(msgId);
    }

    final void setProgressBar(int value) {
        getProgressBar().setProgress(value);
    }

    private String extractFileNameFromURI(Uri uri) {
        String result = null;

        /*Let us have content URI*/
        if (uri.getScheme()!=null && uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
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

    public final PowerManager getPowerManager() {
        return powerManager;
    }

    private void restorePreferences() {
        SharedPreferences sPref = this.getPreferences(Context.MODE_PRIVATE);
        SeekBar volume = findViewById(R.id.sbVolume);
        volume.setProgress(sPref.getInt("c2a_volume", 5));
        lastChooserDirectory = new File(sPref.getString("c2a_last_dir", ""));
        try {
            RecentItem.parsePersistenceString(sPref.getString("c2a_recents", ""), recentItems);
        }
        catch (Exception e) {
            recentItems.clear();
        }
        updateRecentItemsUI();
        userSettings = UserSettings.createFromPersistentStorage(sPref);
    }

    private void storePreferences() {

        SharedPreferences sPref = this.getPreferences(Context.MODE_PRIVATE);
        SeekBar volume = findViewById(R.id.sbVolume);
        SharedPreferences.Editor editor = sPref.edit();

        /*Current state of the UI*/
        editor.putInt("c2a_volume", (byte) volume.getProgress());
        if (lastChooserDirectory != null) {
            editor.putString("c2a_last_dir", lastChooserDirectory.getAbsolutePath());
        }
        String recentString = RecentItem.createPersistenceString(recentItems);
        editor.putString("c2a_recents", recentString);
        editor.apply();

        /*General settings*/
        UserSettings.flushToPersistentStorage(userSettings, sPref);

    }

    public final void onEraseHistory(android.view.View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                MainActivity.this.recentItems.clear();
                updateRecentItemsUI();
            }
        });
        builder.setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        builder.setMessage(R.string.btn_clear_history);

        AlertDialog dialog = builder.create();
        dialog.show();

    }


}
