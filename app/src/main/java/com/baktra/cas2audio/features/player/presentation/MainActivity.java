package com.baktra.cas2audio.features.player.presentation;

import static com.baktra.cas2audio.features.player.domain.CasPlaybackHandler.STOP_REASON_PAUSE;
import static com.baktra.cas2audio.features.player.domain.CasPlaybackHandler.STOP_REASON_STOP;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.baktra.cas2audio.Cas2AudioApp;
import com.baktra.cas2audio.R;
import com.baktra.cas2audio.core.util.Utils;
import com.baktra.cas2audio.features.goldies.presentation.GoldiesActivity;
import com.baktra.cas2audio.features.recent.presentation.RecentActivity;
import com.baktra.cas2audio.features.settings.SettingsActivity;
import com.baktra.cas2audio.features.settings.domain.UserSettings;
import com.baktra.cas2audio.features.player.domain.tapeimage.ResumePoint;
import com.baktra.cas2audio.features.player.domain.tapeimage.TapeImageCrate;
import com.baktra.cas2audio.features.player.domain.tapeimage.TapeImageOpener;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;

public class MainActivity extends AppCompatActivity {

    private final ArrayList<View> playBackViewsDisabled;
    private final ArrayList<MenuItem> playBackMenuItemsDisabled;
    private final ArrayList<View> playBackViewsEnabled;
    private final ArrayList<MenuItem> playBackMenuItemsEnabled;

    private final String LN_SP;
    private MainViewModel viewModel;

    private ActivityResultLauncher<Intent> launchBrowse;
    private ActivityResultLauncher<Intent> launchSettings;
    private ActivityResultLauncher<Intent> launchRecents;

    private ActivityResultLauncher<Intent> launchGoldies;

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

        Cas2AudioApp c2aApp = (Cas2AudioApp) getApplication();
        MainViewModelFactory factory = new MainViewModelFactory(c2aApp.getSettingsRepository(), c2aApp.getTapeImageOpener(), c2aApp.getCasPlaybackHandler());

        /*Widgets to be disabled during playback*/
        playBackViewsDisabled.add(getBrowseButton());
        playBackViewsDisabled.add(findViewById(R.id.btnPlay));
        playBackViewsDisabled.add(findViewById(R.id.btnRecent));
        playBackViewsDisabled.add(findViewById(R.id.lvChunks));

        /*Widgets to be enabled during playback*/
        playBackViewsEnabled.add(findViewById(R.id.btnStop));
        playBackViewsEnabled.add(findViewById(R.id.btnPause));

        viewModel = new ViewModelProvider(this, factory).get(MainViewModel.class);
        setupModelObservers();

        /*Set the title*/
        setTitle("CAS2Audio 1.1.0 [alpha]");

        /*Initialize launchers*/
        initializeLaunchers();
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        MenuItem mi1 = menu.findItem(R.id.miSettings);
        MenuItem mi2 = menu.findItem(R.id.miGoldies);
        playBackMenuItemsDisabled.add(mi1);
        playBackMenuItemsDisabled.add(mi2);

        mi1.setEnabled(!(viewModel.isPlaying()));
        mi2.setEnabled(!(viewModel.isPlaying()));
        return true;
    }


    protected void onResume() {
        super.onResume();
    }

    protected void onStop() {
        super.onStop();

    }

    protected void onPause() {
        closeOptionsMenu();
        super.onPause();
    }

    protected void onDestroy() {
        closeOptionsMenu();
        super.onDestroy();
    }


    private void handlePlayerControls(boolean isPlaying) {

        for (View v : playBackViewsDisabled) {
            v.setEnabled(!isPlaying);
        }
        for (View v : playBackViewsEnabled) {
            v.setEnabled(isPlaying);
        }

        for (MenuItem mi : playBackMenuItemsDisabled) {
            mi.setEnabled(!isPlaying);
        }
        for (MenuItem mi : playBackMenuItemsEnabled) {
            mi.setEnabled(isPlaying);
        }

        /*Cassette image animation*/
        ImageView iv = findViewById(R.id.ivCassette);

        if (isPlaying) {
            iv.setImageDrawable(getResources().getDrawable(R.drawable.tape_animation, null));
            AnimationDrawable ad = (AnimationDrawable) iv.getDrawable();
            ad.start();
        } else {
            iv.setImageDrawable(getResources().getDrawable(R.drawable.tape_animation, null));
            if (iv.getDrawable() instanceof AnimationDrawable) {
                AnimationDrawable ad = (AnimationDrawable) iv.getDrawable();
                ad.stop();
            }
            iv.setImageDrawable(getResources().getDrawable(R.drawable.tape_inactive, null));
        }
    }


    private Optional<Boolean> oldVisibility = Optional.empty();
    private Optional<Integer> oldProgress = Optional.empty();
    private Optional<Integer> oldPlayerResumeIp = Optional.empty();
    private Optional<Integer> oldUserResumeIp = Optional.empty();

    private void setupModelObservers() {


        viewModel.getUIState().observe(this, new Observer<MainViewModel.UIState>() {
            @Override
            public void onChanged(MainViewModel.UIState newState) {

                boolean isPlaying = (newState.getPlayerState() == MainViewModel.PlayerState.PLAYING);

                /*Handle chunk list visibility*/

                if (oldVisibility.isEmpty() || oldVisibility.get()!=newState.isChunkListVisible()) {
                    ListView lv = (ListView) findViewById(R.id.lvChunks);
                    lv.setVisibility(newState.isChunkListVisible() ? View.VISIBLE : View.INVISIBLE);
                    oldVisibility=Optional.of(newState.isChunkListVisible());
                }

                /*Handle progress*/
                if (oldProgress.isEmpty() || oldProgress.get()!=newState.getProgressValue()) {
                    getProgressBar().setProgress(newState.getProgressValue());
                    oldProgress=Optional.of(newState.getProgressValue());
                }

                /*Handle playback state, playing/not playing*/
                if (newState.isPlaybackChanged()) {
                    handlePlayerControls(isPlaying);
                }

                /*Handle tape image opened, not opened*/
                if (newState.isCrateChanged()) {
                    if (newState.getPlayerState() == MainViewModel.PlayerState.TAPE_READY || newState.getPlayerState() == MainViewModel.PlayerState.PLAYING) {
                        TextView tv = findViewById(R.id.tvTapeImageName);
                        tv.setText(newState.getCurrentTapeImageCrate().getFileName());
                        setChunkDisplay(newState.getCurrentTapeImageCrate().getConvCrate().getResumePoints());
                    } else {
                        TextView tv = findViewById(R.id.tvTapeImageName);
                        tv.setText("");
                        setChunkDisplay(new ArrayList<>());
                    }
                }

                /*Handle player resume point*/
                if (isPlaying || newState.isPlaybackChanged() || newState.isCrateChanged()) {

                    int playerIp = newState.getPlayerSelectedResumePointIp();

                    if (oldPlayerResumeIp.isEmpty() || oldPlayerResumeIp.get()!=playerIp) {
                        updatePlayerResumePoint(playerIp);
                        oldPlayerResumeIp=Optional.of(playerIp);
                    }
                }

                /*Handle user resume point*/
                if (!isPlaying) {
                    int userIp = newState.getUserSelectedResumePointIp();
                    if (oldUserResumeIp.isEmpty() || oldUserResumeIp.get() != userIp) {
                        updateUserSelectedResumePoint(userIp);
                        oldUserResumeIp = Optional.of(userIp);
                    }
                }


                /*Handle alerts*/
                if (newState.getAlertType()!= MainViewModel.AlertType.NONE) {

                    if (newState.getAlertType() == MainViewModel.AlertType.PLAYBACK_ERROR) {
                        displaySimpleAlert(getString(R.string.msg_unable_to_process_tit), Utils.getExceptionMessage(newState.getAlertException()));
                    } else if (newState.getAlertType() == MainViewModel.AlertType.CAS_UNREADABLE) {
                        displaySimpleAlert(getString(R.string.msg_file_unable_open_tit), Utils.getExceptionMessage(newState.getAlertException()));
                    } else if (newState.getAlertType() == MainViewModel.AlertType.PLAYER_EMPTY) {
                        displaySimpleAlert(getString(R.string.msg_nothing_to_play_tit), getString(R.string.msg_nothing_to_play));
                    }
                }

            }
        });

    }


    public void onPlay(View v) {
        viewModel.startPlayback();
    }

    private void setChunkDisplay(ArrayList<ResumePoint> resumePoints) {

        ListView lv = (ListView) findViewById(R.id.lvChunks);
        ResumePointAdapter rpa = new ResumePointAdapter(this, lv, resumePoints, 0);
        lv.setAdapter(rpa);
        rpa.notifyDataSetChanged();

        if (lv.getOnItemClickListener() == null) {
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    onClickChunks(adapterView, view, i, l);
                }
            });
        }

    }

    public void onDisplayChunks(View v) {
        viewModel.flipChunkListVisibility();
    }

    public void onClickChunks(AdapterView<?> adapterView, View view, int i, long l) {
        ListView lv = (ListView) adapterView;
        ResumePointAdapter rpa = (ResumePointAdapter) lv.getAdapter();
        ResumePoint p = (ResumePoint) rpa.getItem(i);
        if (p != null) {
            viewModel.setUserResumePoint(p.getResumeIp());
        }
    }

    public void onStopPlaying(View v) {

        int stopReason;

        if (v == findViewById(R.id.btnPause)) {
            stopReason = STOP_REASON_PAUSE;
        } else {
            stopReason = STOP_REASON_STOP;
        }

        viewModel.stopPlayback(stopReason);


    }

    public void onSettings(MenuItem item) {
        doSettings();
    }

    public void doSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra("user_settings", viewModel.getUserSettings());
        launchSettings.launch(intent);
    }

    public void onRecent(View v) {
        Intent intent = new Intent(this, RecentActivity.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra("recent_items", viewModel.getTapeImageRecents().createPersistenceString());
        launchRecents.launch(intent);
    }

    public void onGoldies(MenuItem i) {
        Intent intent = new Intent(this, GoldiesActivity.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        launchGoldies.launch(intent);
    }

    /*Browse for a tape image*/
    public void onBrowseTapeImage(android.view.View view) {

        /*Ask for document selection*/
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        launchBrowse.launch(intent);

    }

    private void processBrowseResult(ActivityResult o) {
        if (o.getResultCode() != Activity.RESULT_OK || o.getData() == null || o.getData().getData() == null)
            return;
        openTapeImageUri(o.getData().getData());

    }

    private void processRecentsResult(ActivityResult o) {

        if (o.getResultCode() != Activity.RESULT_OK || o.getData() == null) return;

        String recentString = o.getData().getStringExtra("recents");
        if (recentString != null) {
            this.viewModel.setTapeImageRecentsString(recentString);
        }

        if (o.getData().getData() != null) {
            openTapeImageUri(o.getData().getData());
        }
    }

    private void openTapeImageUri(Uri uri) {
        String fileName = null;
        fileName = extractFileNameFromURI(uri);
        viewModel.openTapeImageFromURI(uri, fileName);
    }

    private void processSettingsResult(ActivityResult o) {

        /*Handle the settings activity*/
        if (o.getResultCode() != Activity.RESULT_OK || o.getData() == null) return;
        UserSettings us = IntentCompat.getSerializableExtra(o.getData(), "user_settings", UserSettings.class);
        this.viewModel.updateUserSettings(us);
    }

    private void processGoldiesResult(ActivityResult o) {

        /*Handle the Goldies Activity*/
        if (o.getResultCode() != Activity.RESULT_OK || o.getData() == null) return;

        Intent intentData = o.getData();
        viewModel.openTapeImageFromArray(intentData.getIntArrayExtra("tape_image_data"), intentData.getStringExtra("tape_image_name"));

    }

    private void initializeLaunchers() {

        launchBrowse = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {
                        processBrowseResult(o);
                    }
                }
        );


        launchRecents = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {
                        processRecentsResult(o);
                    }
                }
        );
        launchSettings = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {
                        processSettingsResult(o);
                    }
                }
        );

        launchGoldies = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {
                        processGoldiesResult(o);
                    }
                }
        );


    }


    private ProgressBar getProgressBar() {
        return findViewById(R.id.pbProgress);
    }

    private ImageButton getBrowseButton() {
        return findViewById(R.id.btnBrowse);
    }


    void updateUserSelectedResumePoint(int resumeIp) {
        if (resumeIp == -1) return;
        ListView lv = (ListView) findViewById(R.id.lvChunks);
        ResumePointAdapter rpa = (ResumePointAdapter) lv.getAdapter();
        if (rpa==null) return;
        rpa.setUserResumePoint(resumeIp);
    }

    void updatePlayerResumePoint(int resumeIp) {
        if (resumeIp == -1) return;
        ListView lv = (ListView) findViewById(R.id.lvChunks);
        ResumePointAdapter rpa = (ResumePointAdapter) lv.getAdapter();
        if (rpa==null) return;
        rpa.setPlayerResumePoint(resumeIp);
    }

    private String extractFileNameFromURI(Uri uri) {
        String result = null;

        /*Let us have content URI*/
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst() && cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) >= 0) {
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

    private void displaySimpleAlert(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                viewModel.dismissAlert();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onAbout(MenuItem mi) {
        Toast.makeText(this, getString(R.string.toast_about), Toast.LENGTH_LONG).show();
    }

    public void onExit(MenuItem mi) {
        finish();
    }


}
