package com.baktra.cas2audio;

import static com.baktra.cas2audio.MainViewModel.STOP_REASON_PAUSE;
import static com.baktra.cas2audio.MainViewModel.STOP_REASON_STOP;

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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.baktra.cas2audio.tapeimage.TapeImage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final ArrayList<View> playBackViewsDisabled;
    private final ArrayList<MenuItem> playBackMenuItemsDisabled;
    private final ArrayList<View> playBackViewsEnabled;
    private final ArrayList<MenuItem> playBackMenuItemsEnabled;

    private final String LN_SP;
    private MainViewModel viewModel;



    public MainActivity() {
        super();
        LN_SP = System.lineSeparator();
        playBackViewsDisabled = new ArrayList<>(8);
        playBackViewsEnabled = new ArrayList<>(8);
        playBackMenuItemsDisabled = new ArrayList<>(1);
        playBackMenuItemsEnabled = new ArrayList<>(1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //System.out.println("MainActivity::onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        SharedPreferences sharedPrefs = getSharedPreferences("c2a_prefs", Context.MODE_PRIVATE);
        MainViewModelFactory factory = new MainViewModelFactory(sharedPrefs);
        viewModel = new ViewModelProvider(this,factory).get(MainViewModel.class);

        setupModelObservers();

        /*Widgets to be disabled during playback*/
        playBackViewsDisabled.add(getBrowseButton());
        playBackViewsDisabled.add(findViewById(R.id.btnPlay));
        playBackViewsDisabled.add(findViewById(R.id.btnRecent));
        playBackViewsDisabled.add(findViewById(R.id.lvChunks));

        /*Widgets to be enabled during playback*/
        playBackViewsEnabled.add(findViewById(R.id.btnStop));
        playBackViewsEnabled.add(findViewById(R.id.btnPause));

        /*Try to get a power manager*/
        try {
            viewModel.setPowerManager((PowerManager)getApplicationContext().getSystemService(POWER_SERVICE));
        } catch (Exception e) {
            viewModel.setPowerManager(null);
            e.printStackTrace();
        }

        /*Set the title*/
        setTitle("CAS2Audio 1.0.9");
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        playBackMenuItemsDisabled.add(menu.findItem(R.id.miSettings));

        return true;
    }


    protected void onResume() {
        //System.out.println("MainActivity::onResume()");
        super.onResume();
    }

    protected void onStop() {
        //System.out.println("MainActivity::onStop()");
        super.onStop();

    }

    protected void onPause() {
        //System.out.println("MainActivity::onPause()");
        closeOptionsMenu();
        super.onPause();
    }

    protected void onDestroy() {
        closeOptionsMenu();
        super.onDestroy();
    }


    private void setupModelObservers() {

        /*Chunk list visibility*/
        viewModel.getChunkListVisibility().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer newVisibility) {
                ListView lv = (ListView)findViewById(R.id.lvChunks);
                lv.setVisibility(newVisibility);
            }
        });

        /*Playback controls enable/disable*/
        viewModel.getPlayBackState().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean newState) {

                /*UI elements enabled or disabled*/
                boolean b = newState.booleanValue();

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

                /*Cassette image animation*/
                ImageView iv = findViewById(R.id.ivCassette);

                if (newState) {
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

                /*In any case, reset the progress bar*/
                getProgressBar().setProgress(0);
            }
        });

        /*Progress*/
        viewModel.getProgressValue().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer newProgressValue) {
                ProgressBar pb = (ProgressBar)findViewById(R.id.pbProgress);
                pb.setProgress(newProgressValue);
            }
        });

    }


    public void onPlay(View v) {

        /*Check if anything was selected*/
        if (viewModel.getCurrentUri() == null || viewModel.getCurrentConversionCrate() == null) {
            displaySimpleAlert(getString(R.string.msg_nothing_to_play_tit),getResources().getString(R.string.msg_nothing_to_play));
            return;
        }

        /*Create new background task*/
        Exception e = viewModel.createCasTask();
        if (e!=null) {
            displaySimpleAlert(getString(R.string.msg_unable_to_process_tit),Utils.getExceptionMessage(e));
        }
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
        viewModel.flipChunkListVisibility();
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

        int stopReason;

        if (v==findViewById(R.id.btnPause)) {
            stopReason=STOP_REASON_PAUSE;
        }
        else {
            stopReason=STOP_REASON_STOP;
        }

        viewModel.stopCasTask(stopReason);


    }
    public void onSettings(MenuItem item) {
        doSettings();
    }
    public void doSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra("user_settings", viewModel.getUserSettings());
        startActivityForResult(intent, OPEN_SETTINGS);
    }

    public void onRecent(View v) {
        Intent intent = new Intent(this, RecentActivity.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra("recent_items", viewModel.getTapeImageRecents().createPersistenceString());
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

        File lastDir = viewModel.getLastChooserDirectory();

        if (lastDir!=null && lastDir.exists() && lastDir.isDirectory()) {
            Uri pickerInitialUri = Uri.fromFile(lastDir);
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
                this.viewModel.setUserSettings((UserSettings) data.getSerializableExtra("user_settings"));
            }
            return;
        }

        /*Pre-handle the Recent activity*/
        if (requestCode==OPEN_RECENT) {
            if (data != null) {
                String recentString = data.getStringExtra("recents");
                if (recentString!=null) {
                    this.viewModel.setTapeImageRecentsString(recentString);
                }
            }
        }

        /*Handle .CAS file pickup*/
        if ((requestCode==PICK_CAS_FILE || requestCode==OPEN_RECENT) && resultCode==Activity.RESULT_OK) {
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
                    ConversionCrate cc = tip.convertItem(ti,viewModel.getUserSettings().isDo48kHz()?48000:44100 , false);
                    viewModel.setCurrentConversionCrate(cc);


                } catch (Exception e) {
                    candidateUri=null;
                    viewModel.setCurrentConversionCrate(null);
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

                    /*Update the user interface, and recents*/
                    if (candidateUri!=null) {
                       viewModel.setCurrentUri(candidateUri);
                       viewModel.getTapeImageRecents().addRecentItem(candidateUri,extractFileNameFromURI(candidateUri));
                       updateUIForFile();
                    }
                }

            }

        }

    }




    void updateUIForFile() {
        String filename = extractFileNameFromURI(viewModel.getCurrentUri());
        setCurrentFileName(filename);
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


    public void displayPostTaskAlert(int titleId, String msg) {
        displaySimpleAlert(getResources().getString(titleId),msg);
    }

    void setProgressBar(int value) {
        getProgressBar().setProgress(value);
    }

    void setResumePointProgress(int ip) {
        if (ip==-1) return;
        ListView lv = (ListView)findViewById(R.id.lvChunks);
        ResumePointAdapter rpa = (ResumePointAdapter)lv.getAdapter();
        rpa.setResumePoint(ip,true);
    }

    void observeResumePoint(int ip,int stopReason) {
        ListView lv = (ListView)findViewById(R.id.lvChunks);
        ResumePointAdapter rpa = (ResumePointAdapter)lv.getAdapter();

        if (stopReason==STOP_REASON_PAUSE) {
            rpa.setResumePoint(ip,false);
        }
        else {
            rpa.setResumePoint(0,false);
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

    private void observePreferences(SharedPreferences sPref) {
        findViewById(R.id.lvChunks).setVisibility(sPref.getInt("c2a_chunks", View.INVISIBLE));
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
        Toast.makeText(this,getString(R.string.toast_about),Toast.LENGTH_LONG).show();
    }



}
