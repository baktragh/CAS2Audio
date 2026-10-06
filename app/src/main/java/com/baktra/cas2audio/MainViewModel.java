package com.baktra.cas2audio;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baktra.cas2audio.recent.TapeImageRecents;
import com.baktra.cas2audio.settings.UserSettings;
import com.baktra.cas2audio.tapeimage.ResumePoint;
import com.baktra.cas2audio.tapeimage.TapeImageCrate;
import com.baktra.cas2audio.tapeimage.TapeImageOpener;

import java.io.File;

public class MainViewModel extends ViewModel implements CasTaskObserver {

    private SharedPreferences sharedPreferences;

    private UserSettings userSettings;

    private TapeImageRecents tapeImageRecents;


    private CasTask casTask;
    private MutableLiveData<TapeImageCrate> currentTapeImageCrate;


    private MutableLiveData<Boolean> chunkListVisibility;

    private MutableLiveData<Boolean> playBackState;
    File lastChooserDirectory;

    private MutableLiveData<Integer> resumePointIp;

    public static final int STOP_REASON_STOP=0;
    public static final int STOP_REASON_PAUSE=1;
    private int stopReason;

    private MutableLiveData<Integer> progressValue;

    public SingleLiveEvent<CasTaskAlertCrate> getCasAlert() {
        return this.casAlert;
    }

    SingleLiveEvent<CasTaskAlertCrate> casAlert;

    private TapeImageOpener tapeImageOpener;


    public MainViewModel(SharedPreferences sPref, ContentResolver contentResolver) {
        casTask = null;
        playBackState = new MutableLiveData<>(Boolean.FALSE);
        progressValue = new MutableLiveData<>(new Integer(0));
        currentTapeImageCrate = new MutableLiveData<>(TapeImageCrate.getEmpty());
        resumePointIp = new MutableLiveData<>(new Integer(0));
        lastChooserDirectory = null;
        tapeImageRecents = new TapeImageRecents();
        userSettings = new UserSettings();
        chunkListVisibility =new MutableLiveData<>(Boolean.FALSE);
        sharedPreferences=sPref;
        restorePreferences();
        casAlert = new SingleLiveEvent<>();
        tapeImageOpener = new TapeImageOpener(contentResolver);
    }



    Exception createCasTask(PowerManager pm) {
       casTask = null;
        try {
            casTask = new CasTask(
                    currentTapeImageCrate.getValue().getConvCrate().getInstructions(),
                    this,
                    !userSettings.isDoMono(),
                    userSettings.isDoSquareWave(),
                    userSettings.getAmplitude(),
                    currentTapeImageCrate.getValue().getConvCrate().getSampleRate(),
                    userSettings.isDoInvertPolarity(),
                    resumePointIp.getValue(),
                    pm
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
        stopReason=reason;
        if (casTask !=null ) {
            casTask.cancel(false);
        }
    }

    TapeImageOpener.ReadResult readTapeImage(Uri uri) {
        return this.tapeImageOpener.readTapeImage(uri,userSettings.isDo48kHz());
    }

    public void onProgressUpdate(int value) {
        this.progressValue.setValue(value);
    }

    public void onResumePointUpdate(int resumeIp) {
        setResumePoint(resumeIp);
    }

    public void setResumePoint(int resumeIp) {

        ResumePoint rp = ResumePoint.getClosestResumePoint(resumeIp,currentTapeImageCrate.getValue().getConvCrate().getResumePoints());
        if (rp!=null) {
            this.resumePointIp.setValue(rp.getResumeIp());
        }
        else {
            this.resumePointIp.setValue(0);
        }
    }


    private void restorePreferences() {

        /*Restore user settings*/
        userSettings = UserSettings.createFromPersistentStorage(sharedPreferences);

        /*Restore recents*/
        try {
            tapeImageRecents.parsePersistenceString(sharedPreferences.getString("c2a_recents", ""));
        }
        catch (Exception e1) {
            tapeImageRecents.clear();
        }

        /*Restore state of selected controls. If something fails, allow continuation*/
        try {

            lastChooserDirectory = new File(sharedPreferences.getString("c2a_last_dir", ""));
            chunkListVisibility.setValue(sharedPreferences.getBoolean("c2a_chunks_v", false));
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }

    }


    public void onSuccessfulPlayback() {

        playBackState.setValue(new Boolean(false));
        setResumePoint(0);
        casTask=null;
    }

    public void onCancelledPlayback(int resIp) {
        playBackState.setValue(new Boolean(false));
        if (stopReason==STOP_REASON_PAUSE) {
            setResumePoint(resIp);
        }
        else {
            setResumePoint(0);
        }
        casTask=null;
    }

    public void onFailedPlayback(Exception e) {
        playBackState.setValue(new Boolean(false));
        setResumePoint(0);
        casTask=null;
        casAlert.setValue(new CasTaskAlertCrate(0,Utils.getExceptionMessage(e)));
    }

    private void storePreferences() {

        SharedPreferences.Editor editor = sharedPreferences.edit();

        /*Current state of the UI*/
        if (lastChooserDirectory != null) {
            editor.putString("c2a_last_dir", lastChooserDirectory.getAbsolutePath());
        }
        String recentString = tapeImageRecents.createPersistenceString();
        editor.putString("c2a_recents", recentString);
        editor.putBoolean("c2a_chunks_v", chunkListVisibility.getValue());
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

    void setCurrentTapeImageCrate(TapeImageCrate tic) {
        currentTapeImageCrate.setValue(tic);
    }

    public void flipChunkListVisibility() {

        chunkListVisibility.setValue(!chunkListVisibility.getValue());

    }

    public LiveData<Boolean> getChunkListVisibility() {
        return chunkListVisibility;
    }

    public LiveData<Boolean> getPlayBackState() {
        return playBackState;
    }

    public LiveData<Integer> getProgressValue() {
        return progressValue;
    }

    public LiveData<TapeImageCrate> getCurrentTapeImageCrate() {return currentTapeImageCrate;}
    public LiveData<Integer> getResumePointIp() {return resumePointIp;}


    @Override
    public void onCleared() {
        //System.out.println("MainViewModel::onCleared()");
        storePreferences();
        if (casTask!=null) {
            casTask.cancel(false);
        }
    }

}
