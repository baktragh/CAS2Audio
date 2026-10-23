package com.baktra.cas2audio.features.player.presentation;

import static com.baktra.cas2audio.features.player.domain.CasPlaybackHandler.STOP_REASON_PAUSE;
import static com.baktra.cas2audio.features.player.domain.CasPlaybackHandler.STOP_REASON_TERMINATE;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baktra.cas2audio.core.util.Utils;
import com.baktra.cas2audio.features.player.domain.CasPlaybackHandler;
import com.baktra.cas2audio.features.player.domain.CasPlaybackObserver;
import com.baktra.cas2audio.features.recent.domain.TapeImageRecents;
import com.baktra.cas2audio.features.settings.repository.SettingsRepository;
import com.baktra.cas2audio.features.settings.domain.UserSettings;
import com.baktra.cas2audio.features.player.domain.tapeimage.ResumePoint;
import com.baktra.cas2audio.features.player.domain.tapeimage.TapeImageCrate;
import com.baktra.cas2audio.features.player.domain.tapeimage.TapeImageOpener;

public class MainViewModel extends ViewModel implements CasPlaybackObserver {

    /*User interface state*/
    private MutableLiveData<TapeImageCrate> currentTapeImageCrate;
    private MutableLiveData<Boolean> chunkListVisibility;
    private MutableLiveData<Boolean> playBackState;
    private MutableLiveData<Integer> resumePointIp;
    private MutableLiveData<Integer> progressValue;
    private SingleLiveEvent<CasTaskAlertCrate> casTaskAlert;
    private SingleLiveEvent<OpenAlertCrate> openAlert;


    /*Repositories and services*/
    private SettingsRepository settingsRepository;
    private TapeImageOpener tapeImageOpener;
    private final CasPlaybackHandler casPlaybackHandler;


    public MainViewModel(SettingsRepository settingsRepo,TapeImageOpener tiOpener,CasPlaybackHandler cpHandler) {
        playBackState = new MutableLiveData<>(Boolean.FALSE);
        progressValue = new MutableLiveData<>(0);
        currentTapeImageCrate = new MutableLiveData<>(TapeImageCrate.getEmpty());
        resumePointIp = new MutableLiveData<>(0);
        chunkListVisibility =new MutableLiveData<>(Boolean.FALSE);
        casTaskAlert = new SingleLiveEvent<>();
        openAlert = new SingleLiveEvent<>();
        settingsRepository=settingsRepo;
        tapeImageOpener=tiOpener;
        casPlaybackHandler = cpHandler;
        loadPreferences();
    }

    Exception startPlayback() {
       try {

           UserSettings us = getUserSettings();
            getCasPlaybackHandler().prepare(
                    currentTapeImageCrate.getValue().getConvCrate().getInstructions(),
                    this,
                    !us.isDoMono(),
                    us.isDoSquareWave(),
                    us.getAmplitude(),
                    us.isDo48kHz()?48000:44100,
                    us.isDoInvertPolarity(),
                    resumePointIp.getValue()
                    );
       }
       catch (Exception e) {
           return e;
       }

       playBackState.setValue(true);
       getCasPlaybackHandler().play();
       return null;

    }

    public void stopPlayback(int reason) {
        getCasPlaybackHandler().stop(reason);
    }

    TapeImageOpener.ReadResult readTapeImage(Uri uri,int[] tapeImageBytes,String fileName) {
        UserSettings us = getSettingsRepository().getUserSettings();
        return getTapeImageOpener().readTapeImage(uri,tapeImageBytes,us.isDo48kHz(),fileName);
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
        playBackState.setValue(false);
        setResumePoint(0);
    }

    public void onCancelledPlayback(int resIp,int stopReason) {
        playBackState.setValue(false);
        if (stopReason==STOP_REASON_PAUSE) {
            setResumePoint(resIp);
        }
        else {
            setResumePoint(0);
        }

    }

    public void onFailedPlayback(Throwable e) {
        playBackState.setValue(false);
        setResumePoint(0);
        casTaskAlert.setValue(new CasTaskAlertCrate(0, Utils.getExceptionMessage(e)));
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
        getCasPlaybackHandler().stop(STOP_REASON_TERMINATE);
    }

    public void openTapeImageFromURI(Uri tapeImageUri, String fileName) {

        /*Open nothing*/
        if (tapeImageUri==null) {
            setCurrentTapeImageCrate(TapeImageCrate.getEmpty());
            return;
        }

        /*Try to open the tape image*/
        TapeImageOpener.ReadResult result = readTapeImage(tapeImageUri,null,fileName);
        processOpenedImage(result,false,tapeImageUri,fileName);

    }

    public void openTapeImageFromArray(int[] tapeImageBytes,String fileName) {
        if (tapeImageBytes==null) {
            setCurrentTapeImageCrate(TapeImageCrate.getEmpty());
            return;
        }

        TapeImageOpener.ReadResult result = readTapeImage(null,tapeImageBytes,fileName);
        processOpenedImage(result,false,null,fileName);
    }

    private void processOpenedImage(TapeImageOpener.ReadResult result, boolean addToRecents,Uri tapeImageUri,String fileName) {
        /*If successfull, add to recents*/
        if (result.isSuccess()) {
            setCurrentTapeImageCrate(result.getTapeImageCrate());
            if (addToRecents && tapeImageUri!=null) {
                getTapeImageRecents().addRecentItem(tapeImageUri, fileName);
            }
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
    public SingleLiveEvent<CasTaskAlertCrate> getCasTaskAlert() {
        return this.casTaskAlert;
    }

    private SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    private TapeImageOpener getTapeImageOpener() {
        return tapeImageOpener;
    }

    public CasPlaybackHandler getCasPlaybackHandler() {
        return this.casPlaybackHandler;
    }
}
