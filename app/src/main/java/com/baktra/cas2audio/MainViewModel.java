package com.baktra.cas2audio;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;
import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;

public class MainViewModel extends ViewModel {

    private SharedPreferences sharedPreferences;
    private PowerManager powerManager;

    private UserSettings userSettings;

    private TapeImageRecents tapeImageRecents;


    private CasTask casTask;
    private ConversionCrate currentConversionCrate;
    Uri currentUri;

    private MutableLiveData<Integer> chunkListVisibility;

    private MutableLiveData<Boolean> playBackState;
    File lastChooserDirectory;


    public static final int STOP_REASON_STOP=0;
    public static final int STOP_REASON_PAUSE=1;
    private int stopReason;

    private MutableLiveData<Integer> progressValue;
    private int resumeIp;

    public MainViewModel(SharedPreferences sPref) {
        casTask = null;
        currentUri = null;
        playBackState = new MutableLiveData<>(Boolean.FALSE);
        progressValue = new MutableLiveData<>(new Integer(0));
        lastChooserDirectory = null;
        tapeImageRecents = new TapeImageRecents();
        userSettings = new UserSettings();
        powerManager = null;
        chunkListVisibility =new MutableLiveData<>(View.INVISIBLE);
        sharedPreferences=sPref;
        restorePreferences();
    }


    void setPowerManager(PowerManager p) {
        if (powerManager==null) {
            powerManager=p;
        }
    }

    Uri getCurrentUri() {
        return currentUri;
    }
    ConversionCrate getCurrentConversionCrate() {
        return currentConversionCrate;
    }

    Exception createCasTask() {
       casTask = null;
        try {
            casTask = new CasTask(
                    currentConversionCrate.getInstructions(),
                    this,
                    !userSettings.isDoMono(),
                    userSettings.isDoSquareWave(),
                    userSettings.getAmplitude(),
                    currentConversionCrate.sampleRate,
                    userSettings.isDoInvertPolarity(),
                    resumeIp
            );
            playBackState.setValue(new Boolean(true));
            casTask.execute();
            return null;
        } catch (Exception e) {
            casTask=null;
            return e;
        }
    }

    void stopCasTask(int reason) {

        if (casTask !=null ) {
            casTask.cancel(false);
        }
        stopReason=reason;

    }

    void setProgressValue(int value) {
        this.progressValue.setValue(value);
    }

    void setResumePoint(int value) {
        this.resumeIp = value;
    }


    private void restorePreferences() {

        lastChooserDirectory = new File(sharedPreferences.getString("c2a_last_dir", ""));
        try {
            tapeImageRecents.parsePersistenceString(sharedPreferences.getString("c2a_recents", ""));
        }
        catch (Exception e) {
            tapeImageRecents.clear();
        }
        userSettings = UserSettings.createFromPersistentStorage(sharedPreferences);

    }

    void handlePlaybackEndedNormal() {
        playBackState.setValue(new Boolean(false));
    }

    void handlePlaybackCancelled(int resumeIp) {
        playBackState.setValue(new Boolean(false));
    }

    private void storePreferences() {

        SharedPreferences.Editor editor = sharedPreferences.edit();

        /*Current state of the UI*/
        if (lastChooserDirectory != null) {
            editor.putString("c2a_last_dir", lastChooserDirectory.getAbsolutePath());
        }
        String recentString = tapeImageRecents.createPersistenceString();
        editor.putString("c2a_recents", recentString);
        editor.putInt("c2a_chunks", chunkListVisibility.getValue());
        editor.apply();

        /*General settings*/
        UserSettings.flushToPersistentStorage(userSettings, sharedPreferences);

    }

    UserSettings getUserSettings() {
        return userSettings;
    }

    void setUserSettings(UserSettings us) {
        userSettings=us;
    }

    TapeImageRecents getTapeImageRecents() {
        return tapeImageRecents;
    }

    void setTapeImageRecentsString(String s) {
        tapeImageRecents.parsePersistenceString(s);
    }

    File getLastChooserDirectory() {
        return lastChooserDirectory;
    }

    void setCurrentConversionCrate(ConversionCrate cc) {
        currentConversionCrate=cc;
    }

    void setCurrentUri(Uri u) {
        currentUri=u;
    }


    public PowerManager getPowerManager() {
        return powerManager;
    }

    public void flipChunkListVisibility() {

        if (chunkListVisibility.getValue()==View.VISIBLE) {
            chunkListVisibility.setValue(View.INVISIBLE);
        }
        else {
            chunkListVisibility.setValue(View.VISIBLE);
        }

    }

    public MutableLiveData<Integer> getChunkListVisibility() {
        return chunkListVisibility;
    }

    public MutableLiveData<Boolean> getPlayBackState() {
        return playBackState;
    }

    public MutableLiveData<Integer> getProgressValue() {
        return progressValue;
    }


    @Override
    public void onCleared() {
        System.out.println("MainViewModel::onCleared()");
        storePreferences();
    }

}
