package com.baktra.cas2audio;

import android.app.Application;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baktra.cas2audio.recent.TapeImageRecents;
import com.baktra.cas2audio.settings.SettingsRepository;
import com.baktra.cas2audio.settings.UserSettings;
import com.baktra.cas2audio.tapeimage.ResumePoint;
import com.baktra.cas2audio.tapeimage.TapeImageCrate;
import com.baktra.cas2audio.tapeimage.TapeImageOpener;

import java.io.File;

public class MainViewModel extends AndroidViewModel implements CasTaskObserver {


    private CasTask casTask;
    private MutableLiveData<TapeImageCrate> currentTapeImageCrate;


    private Cas2AudioApp appContext;

    private MutableLiveData<Boolean> chunkListVisibility;


    private MutableLiveData<Boolean> playBackState;


    private MutableLiveData<Integer> resumePointIp;

    public static final int STOP_REASON_STOP=0;
    public static final int STOP_REASON_PAUSE=1;
    private int stopReason;

    private MutableLiveData<Integer> progressValue;

    public SingleLiveEvent<CasTaskAlertCrate> getCasTaskAlert() {
        return this.casTaskAlert;
    }

    SingleLiveEvent<CasTaskAlertCrate> casTaskAlert;
    SingleLiveEvent<OpenAlertCrate> openAlert;




    public MainViewModel(Cas2AudioApp app) {
        super(app);
        casTask = null;
        playBackState = new MutableLiveData<>(Boolean.FALSE);
        progressValue = new MutableLiveData<>(new Integer(0));
        currentTapeImageCrate = new MutableLiveData<>(TapeImageCrate.getEmpty());
        resumePointIp = new MutableLiveData<>(new Integer(0));
        chunkListVisibility =new MutableLiveData<>(Boolean.FALSE);
        casTaskAlert = new SingleLiveEvent<>();
        openAlert = new SingleLiveEvent<>();
        loadPreferences();
        appContext=(Cas2AudioApp) app;
    }



    Exception createCasTask() {
       casTask = null;
       UserSettings us = getSettingsRepository().getUserSettings();
        try {
            casTask = new CasTask(
                    currentTapeImageCrate.getValue().getConvCrate().getInstructions(),
                    this,
                    !us.isDoMono(),
                    us.isDoSquareWave(),
                    us.getAmplitude(),
                    currentTapeImageCrate.getValue().getConvCrate().getSampleRate(),
                    us.isDoInvertPolarity(),
                    resumePointIp.getValue(),
                    ((Cas2AudioApp)getApplication()).getPowerManager()
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
        UserSettings us = getSettingsRepository().getUserSettings();
        return getTapeImageOpener().readTapeImage(uri,us.isDo48kHz());
    }

    public void onProgressUpdate(int value) {
        this.progressValue.setValue(value);
    }

    public void onResumePointUpdate(int resumeIp) {
        setResumePoint(resumeIp);
    }

    public void setResumePoint(int resumeIp) {

        /*Always set to zero for an empty tape image or explicit zero resume point*/
        if (resumeIp==0 || currentTapeImageCrate.getValue().isEmpty()) {
            this.resumePointIp.setValue(0);
            return;
        }

        /*Try getting the closest resume point*/
        ResumePoint rp = ResumePoint.getClosestResumePoint(resumeIp,currentTapeImageCrate.getValue().getConvCrate().getResumePoints());
        if (rp!=null) {
            this.resumePointIp.setValue(rp.getResumeIp());
        }
        /*If not available, fall back to 0*/
        else {
            this.resumePointIp.setValue(0);
        }
    }


    private void loadPreferences() {

        /*Restore user settings*/
        getSettingsRepository().loadSettings();
        /*Update the UI*/
        chunkListVisibility.setValue(getSettingsRepository().getUiPersistence().isChunkListVisible());
    }

    private void savePreferences() {

        /*Get state of the ui*/
        getSettingsRepository().getUiPersistence().updateChunkListVisible(chunkListVisibility.getValue());

        /*Save settings*/
        getSettingsRepository().saveSettings();
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
        casTaskAlert.setValue(new CasTaskAlertCrate(0,Utils.getExceptionMessage(e)));
    }


    UserSettings getUserSettings() {
        return getSettingsRepository().getUserSettings();
    }

    void updateUserSettings(UserSettings us) {
        getSettingsRepository().setUserSettings(us);
    }

    TapeImageRecents getTapeImageRecents() {
        return getSettingsRepository().getTapeImageRecents();
    }

    void setTapeImageRecentsString(String s) {
        getSettingsRepository().getTapeImageRecents().parsePersistenceString(s);
    }

    void setCurrentTapeImageCrate(TapeImageCrate tic) {
        currentTapeImageCrate.setValue(tic);
        setResumePoint(0);
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
        savePreferences();
        if (casTask!=null) {
            casTask.cancel(false);
        }
    }

    public void openTapeImage(Uri tapeImageUri,String fileName) {

        /*Open nothing*/
        if (tapeImageUri==null) {
            setCurrentTapeImageCrate(TapeImageCrate.getEmpty());
            return;
        }

        /*Try to open the tape image*/
        TapeImageOpener.ReadResult result = readTapeImage(tapeImageUri);

        /*If successfull, add to recents*/
        if (result.isSuccess()) {
            setCurrentTapeImageCrate(result.getTapeImageCrate());
            getTapeImageRecents().addRecentItem(tapeImageUri, fileName);
        }
        /*Otherwise, set up for just "empty" tape image*/
        else {
            /*The player has an empt tape image*/
            setCurrentTapeImageCrate(TapeImageCrate.getEmpty());

            /*Tell the user by scheduling an alert*/
            openAlert.setValue(new OpenAlertCrate(result.getFailureNature(), Utils.getExceptionMessage(result.getException())));
        }
    }

    public LiveData<OpenAlertCrate> getOpenAlert() {
        return openAlert;
    }

    private SettingsRepository getSettingsRepository() {
        return appContext.getSettingsRepository();
    }

    private TapeImageOpener getTapeImageOpener() {
        return appContext.getTapeImageOpener();
    }

}
