package com.baktra.cas2audio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {

    private CasTask casTask;
    private final String LN_SP;
    Uri currentUri;
    private boolean playbackInProgress;
    private PowerManager powerManager;
    File lastChooserDirectory;

    private final ArrayList<View> playBackViewsDisabled;
    private final ArrayList<View> playBackViewsEnabled;

    private static final class RecentItem {
        Uri uri;
        String filename;

        RecentItem(Uri uri, String filename) {
            this.uri = uri;
            this.filename=filename;
        }

        public static String createPersistenceString(ArrayList<RecentItem> recentItems) {
            StringBuilder sb = new StringBuilder();

            for(RecentItem ri:recentItems) {
                String p1 = ri.uri.toString();
                String p2 = ri.filename.toString();
                sb.append("{");
                sb.append(p1);
                sb.append(",");
                sb.append(p2);
                sb.append("}");
                sb.append(";");
            }

            return sb.toString();
        }

        public static void parsePersistenceString(String s,ArrayList<RecentItem> recentItems) {
            recentItems.clear();
            StringTokenizer tk = new StringTokenizer(s,";");

            while (tk.hasMoreTokens()) {
                String pair = tk.nextToken();
                if (pair.length()==0) break;

                StringTokenizer tk2 = new StringTokenizer(pair,",");
                String p1 = tk2.nextToken().replace("{","");
                String p2 = tk2.nextToken().replace("}","");

                recentItems.add(new RecentItem(Uri.parse(p1),p2));
            }


        }


        public String toString() {
            return filename;
        }
    }

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
    }


    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Widgets to be disabled during playback*/
        playBackViewsDisabled.add(getBrowseButton());
        playBackViewsDisabled.add(findViewById(R.id.btnPlay));
        playBackViewsDisabled.add(findViewById(R.id.swChannels));
        playBackViewsDisabled.add(findViewById(R.id.swSquareWave));
        playBackViewsDisabled.add(findViewById(R.id.sbVolume));
        playBackViewsDisabled.add(findViewById(R.id.tvAmplitude));
        playBackViewsDisabled.add(findViewById(R.id.sw48kHz));
        playBackViewsDisabled.add(findViewById(R.id.swInvertPulses));
        playBackViewsDisabled.add(findViewById(R.id.lvRecentItems));

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
        ListView lv = (ListView)findViewById(R.id.lvRecentItems);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                currentUri = recentItems.get(i).uri;
                updateUIForURI();
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
            if (u!=null && u.getPath()!=null) {
                String filename = extractFileNameFromURI(u);
                setCurrentFileName(filename);
                setPlayBackViewsEnabled(false);
                currentUri=u;
            }
            /*There was some intent, but no valid path selected.*/
            else {
                setCurrentFileName("CAS2Audio 1.0.3");
                msgText.setText(R.string.msg_notape);
                setPlayBackViewsEnabled(false);
                currentUri=null;
            }

        }
        /*Activity was resumed, we are still open with valid tape image, and no playback is in progress*/
        else {
            updateUIForURI();
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
        }
        catch (Exception e) {
            getMessageWidget().setText(R.string.msg_unable_to_open + ":" + LN_SP + Utils.getExceptionMessage(e));
            return;
        }

        int sampleRate = getSampleRate();
        /*Try to process the tape image*/
        try {
            TapeImageProcessor tip = new TapeImageProcessor();
            instructions = tip.convertItem(iStream, sampleRate, isShortenStandardLeader());
        }
        catch (Exception e) {
            getMessageWidget().setText(R.string.msg_unable_to_process + ":" + LN_SP + Utils.getExceptionMessage(e));
            return;
        }

        /*Create new background task*/
        try {
            casTask = new CasTask(instructions, this, isStereo(), isSquare(), getVolume(), sampleRate, isInvertPolarity());
        }
        catch (Exception e) {
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


    /*Browse for a tape image*/
    public final void onBrowseTapeImage(android.view.View view) {

        /*First, stop playing, this will set the controls*/
        onStopPlaying(view);

        /*Create basic chooser dialog*/
        ChooserDialog cDlg = new ChooserDialog(MainActivity.this)
                .withFilter(false, false, "cas", "CAS")
                .displayPath(true)
                .withResources(R.string.tit_choose_image, R.string.btn_choose, R.string.btn_cancel)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        currentUri = Uri.fromFile(pathFile);
                        updateUIForURI();
                        lastChooserDirectory = pathFile.getParentFile();
                    }
                });
        if (lastChooserDirectory != null && lastChooserDirectory.isDirectory() && lastChooserDirectory.list().length>0) {
            cDlg = cDlg.withStartFile(lastChooserDirectory.getAbsolutePath());
        }
        cDlg.build().show();

    }

    void updateUIForURI() {
        String filename = extractFileNameFromURI(currentUri);
        setCurrentFileName(filename);
        setPlayBackViewsEnabled(false);
        getMessageWidget().setText("");
        addRecentItem(currentUri,filename);
    }

    void addRecentItem(Uri uri,String filename) {
        RecentItem candidateItem = new RecentItem(uri,filename);

        /*Check if already there*/
        boolean found=false;
        for (RecentItem item:recentItems) {
            if (item.uri.equals(candidateItem.uri)) {
                found=true;
                break;
            }
        }
        /*If already there, just return*/
        if (found) return;

        /*Move to front*/
        recentItems.add(0,candidateItem);
        if (recentItems.size()>12) recentItems.remove(11);
        updateRecentItemsUI();

    }

    void updateRecentItemsUI() {
        ListView lvRecentItems = (ListView)findViewById(R.id.lvRecentItems);
        lvRecentItems.setAdapter(new ArrayAdapter<RecentItem>(this,R.layout.list_text,recentItems));
    }

    private TextView getMessageWidget() {
        return ((TextView) findViewById(R.id.mltMessages));
    }

    private boolean isStereo() {
        return ((Switch) findViewById(R.id.swChannels)).isChecked();
    }

    private boolean isSquare() {
        return ((Switch) findViewById(R.id.swSquareWave)).isChecked();
    }

    private boolean isInvertPolarity() {
        return ((Switch) findViewById(R.id.swInvertPulses)).isChecked();
    }

    private int getVolume() {
        return ((SeekBar) findViewById(R.id.sbVolume)).getProgress();
    }

    private ProgressBar getProgressBar() {
        return ((ProgressBar) findViewById(R.id.pbProgress));
    }

    private int getSampleRate() {
        boolean f48kHz = ((Switch) findViewById(R.id.sw48kHz)).isChecked();
        return f48kHz ? 48000 : 44100;
    }

    private boolean isShortenStandardLeader() {
        return ((Switch) findViewById(R.id.swShortLeader)).isChecked();
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

    public final PowerManager getPowerManager() {
        return powerManager;
    }

    private void restorePreferences() {
        SharedPreferences sPref = this.getPreferences(Context.MODE_PRIVATE);

        Switch channels = findViewById(R.id.swChannels);
        Switch squareWave = findViewById(R.id.swSquareWave);
        Switch s48kHz = findViewById(R.id.sw48kHz);
        Switch sShortLeader = findViewById(R.id.swShortLeader);
        Switch sInvertPulses = findViewById(R.id.swInvertPulses);
        SeekBar volume = findViewById(R.id.sbVolume);

        channels.setChecked(sPref.getBoolean("c2a_stereo", true));
        squareWave.setChecked(sPref.getBoolean("c2a_square", false));
        s48kHz.setChecked(sPref.getBoolean("c2a_48kHz", false));
        sShortLeader.setChecked(sPref.getBoolean("c2a_short_leader", false));
        sInvertPulses.setChecked(sPref.getBoolean("c2a_invert_pulses", false));

        volume.setProgress(sPref.getInt("c2a_volume", 5));
        lastChooserDirectory = new File(sPref.getString("c2a_last_dir", ""));

        RecentItem.parsePersistenceString(sPref.getString("c2a_recents",""),recentItems);
        updateRecentItemsUI();

    }

    private void storePreferences() {

        SharedPreferences sPref = this.getPreferences(Context.MODE_PRIVATE);

        Switch channels = findViewById(R.id.swChannels);
        Switch squareWave = findViewById(R.id.swSquareWave);
        Switch s48kHz = findViewById(R.id.sw48kHz);
        Switch sShortLeader = findViewById(R.id.swShortLeader);
        Switch sInvertPulses = findViewById(R.id.swInvertPulses);

        SeekBar volume = findViewById(R.id.sbVolume);

        SharedPreferences.Editor editor = sPref.edit();

        editor.putBoolean("c2a_stereo", channels.isChecked());
        editor.putBoolean("c2a_square", squareWave.isChecked());
        editor.putBoolean("c2a_48kHz", s48kHz.isChecked());
        editor.putBoolean("c2a_short_leader", sShortLeader.isChecked());
        editor.putBoolean("c2a_invert_pulses", sInvertPulses.isChecked());

        editor.putInt("c2a_volume", (byte) volume.getProgress());

        if (lastChooserDirectory != null) {
            editor.putString("c2a_last_dir", lastChooserDirectory.getAbsolutePath());
        }

        String recentString = RecentItem.createPersistenceString(recentItems);
        editor.putString("c2a_recents",recentString);

        editor.apply();

    }


}
