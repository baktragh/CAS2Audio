package com.baktra.cas2audio.features.player.presentation;

import static com.baktra.cas2audio.features.player.domain.CasPlaybackHandler.STOP_REASON_PAUSE;
import static com.baktra.cas2audio.features.player.domain.CasPlaybackHandler.STOP_REASON_TERMINATE;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baktra.cas2audio.features.player.domain.CasPlaybackHandler;
import com.baktra.cas2audio.features.player.domain.CasPlaybackObserver;
import com.baktra.cas2audio.features.recent.domain.TapeImageRecents;
import com.baktra.cas2audio.features.settings.repository.SettingsRepository;
import com.baktra.cas2audio.features.settings.domain.UserSettings;
import com.baktra.cas2audio.features.player.domain.tapeimage.ResumePoint;
import com.baktra.cas2audio.features.player.domain.tapeimage.TapeImageCrate;
import com.baktra.cas2audio.features.player.domain.tapeimage.TapeImageOpener;

public class MainViewModel extends ViewModel implements CasPlaybackObserver {


    /*Repositories and services*/
    private SettingsRepository settingsRepository;
    private TapeImageOpener tapeImageOpener;
    private final CasPlaybackHandler casPlaybackHandler;

    enum PlayerState {
        EMPTY,
        TAPE_READY,
        PLAYING,
    }

    enum AlertType {
        NONE,
        CAS_UNREADABLE,
        PLAYER_EMPTY,
        PLAYBACK_ERROR

    }

    protected class UIState {

        private final PlayerState playerState;
        private final int progressValue;
        private final int playerSelectedResumePointIp;
        private final int userSelectedResumePointIp;

        private final TapeImageCrate currentTapeImageCrate;
        private final boolean chunkListVisible;

        private final AlertType alertType;
        private final Throwable alertException;


        public UIState(PlayerState playerState, int progressValue, int playerIp, int userIp, TapeImageCrate crate, boolean chunkListVisible, AlertType alertType, Throwable alertException) {
            this.playerState = playerState;
            this.progressValue = progressValue;
            this.playerSelectedResumePointIp = playerIp;
            this.userSelectedResumePointIp = userIp;
            this.currentTapeImageCrate = crate;
            this.chunkListVisible = chunkListVisible;
            this.alertType = alertType;
            this.alertException = alertException;
        }

        public PlayerState getPlayerState() {
            return this.playerState;
        }

        public int getProgressValue() {
            return this.progressValue;
        }

        public int getPlayerSelectedResumePointIp() {
            return this.playerSelectedResumePointIp;
        }

        public int getUserSelectedResumePointIp() {
            return this.userSelectedResumePointIp;
        }

        public boolean isChunkListVisible() {
            return this.chunkListVisible;
        }

        public TapeImageCrate getCurrentTapeImageCrate() {
            return this.currentTapeImageCrate;
        }

        public AlertType getAlertType() {
            return this.alertType;
        }

        public Throwable getAlertException() {
            return this.alertException;
        }


    }

    /*User interface state*/
    private MutableLiveData<UIState> uiState;


    public MainViewModel(SettingsRepository settingsRepo, TapeImageOpener tiOpener, CasPlaybackHandler cpHandler) {
        settingsRepository = settingsRepo;
        tapeImageOpener = tiOpener;
        casPlaybackHandler = cpHandler;
        uiState = new MutableLiveData<>();
        uiState.setValue(new UIState(PlayerState.EMPTY, 0, 0, 0, null, false, AlertType.NONE, null));
        casPlaybackHandler.setObserver(this);
        loadPreferences();

    }

    void startPlayback() {



        if (uiState.getValue().playerState == PlayerState.EMPTY) {

            /*Update UI state to ready, with error*/
            UIState oldState = uiState.getValue();
            UIState newState = new UIState
                    (oldState.playerState,
                            oldState.progressValue,
                            oldState.playerSelectedResumePointIp,
                            oldState.userSelectedResumePointIp,
                            oldState.currentTapeImageCrate,
                            oldState.chunkListVisible,
                            AlertType.PLAYER_EMPTY,
                            null
                    );
            uiState.setValue(newState);
            return;
        }

        try {

            UserSettings us = getUserSettings();
            getCasPlaybackHandler().prepare(
                    uiState.getValue().currentTapeImageCrate.getConvCrate().getInstructions(),
                    !us.isDoMono(),
                    us.isDoSquareWave(),
                    us.getAmplitude(),
                    us.isDo48kHz() ? 48000 : 44100,
                    us.isDoInvertPolarity(),
                    uiState.getValue().userSelectedResumePointIp
            );
        } catch (Exception e) {

            /*Update UI state to ready, with error*/
            UIState oldState = uiState.getValue();
            UIState newState = new UIState
                    (PlayerState.TAPE_READY,
                            oldState.progressValue,
                            oldState.playerSelectedResumePointIp,
                            oldState.userSelectedResumePointIp,
                            oldState.currentTapeImageCrate,
                            oldState.chunkListVisible,
                            AlertType.PLAYBACK_ERROR,
                            e
                    );
            uiState.setValue(newState);
            return;
        }

        /*Update UI state to playing*/
        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (PlayerState.PLAYING,
                        oldState.progressValue,
                        oldState.playerSelectedResumePointIp,
                        oldState.userSelectedResumePointIp,
                        oldState.currentTapeImageCrate,
                        oldState.chunkListVisible,
                        AlertType.NONE,
                        null
                );
        uiState.setValue(newState);

        getCasPlaybackHandler().play();


    }

    public void stopPlayback(int reason) {
        getCasPlaybackHandler().stop(reason);
    }

    TapeImageOpener.ReadResult readTapeImage(Uri uri, int[] tapeImageBytes, String fileName) {
        UserSettings us = getSettingsRepository().getUserSettings();
        return getTapeImageOpener().readTapeImage(uri, tapeImageBytes, us.isDo48kHz(), fileName);
    }

    public void onProgressUpdate(int value) {
        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (oldState.playerState,
                        value,
                        oldState.playerSelectedResumePointIp,
                        oldState.userSelectedResumePointIp,
                        oldState.currentTapeImageCrate,
                        oldState.chunkListVisible,
                        oldState.alertType,
                        oldState.alertException
                );
        uiState.setValue(newState);
    }

    public void onResumePointUpdate(int resumeIp) {

        /*Check if possible*/
        int newResumePoint = calculateResumePoint(resumeIp);

        /*The player changes the resume IP*/
        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (oldState.playerState,
                        oldState.progressValue,
                        newResumePoint,
                        oldState.userSelectedResumePointIp,
                        oldState.currentTapeImageCrate,
                        oldState.chunkListVisible,
                        oldState.alertType,
                        oldState.alertException
                );
        uiState.setValue(newState);

    }

    public void setUserResumePoint(int resumeIp) {


        int newResumePoint = calculateResumePoint(resumeIp);

        /*The user changes the resume IP*/
        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (oldState.playerState,
                        oldState.progressValue,
                        oldState.playerSelectedResumePointIp,
                        newResumePoint,
                        oldState.currentTapeImageCrate,
                        oldState.chunkListVisible,
                        oldState.alertType,
                        oldState.alertException
                );
        uiState.setValue(newState);

    }


    private void loadPreferences() {

        /*Restore user settings*/
        getSettingsRepository().loadSettings();
        /*Update the UI*/
        boolean chunkListVisibility = (getSettingsRepository().getUiPersistence().isChunkListVisible());

        /*The user changes the resume IP*/
        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (oldState.playerState,
                        oldState.progressValue,
                        oldState.playerSelectedResumePointIp,
                        oldState.userSelectedResumePointIp,
                        oldState.currentTapeImageCrate,
                        chunkListVisibility,
                        oldState.alertType,
                        oldState.alertException
                );
        uiState.setValue(newState);
    }

    private void savePreferences() {

        /*Get state of the ui*/
        getSettingsRepository().getUiPersistence().updateChunkListVisible(uiState.getValue().chunkListVisible);

        /*Save settings*/
        getSettingsRepository().saveSettings();


    }

    private int calculateResumePoint(int resumeIp) {
        int newResumePoint = 0;
        if (uiState.getValue().playerState == PlayerState.EMPTY) {
            newResumePoint = 0;
        } else {
            ResumePoint rp = ResumePoint.getClosestResumePoint(resumeIp, uiState.getValue().currentTapeImageCrate.getConvCrate().getResumePoints());
            if (rp != null) {
                newResumePoint = rp.getResumeIp();
            } else {
                newResumePoint = 0;
            }
        }

        return newResumePoint;
    }


    public void onSuccessfulPlayback() {

        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (PlayerState.TAPE_READY,
                        0,
                        0,
                        0,
                        oldState.currentTapeImageCrate,
                        oldState.chunkListVisible,
                        oldState.alertType,
                        oldState.alertException
                );
        uiState.setValue(newState);
    }

    public void onCancelledPlayback(int resIp, int stopReason) {

        int newResumePoint = 0;
        if (stopReason == STOP_REASON_PAUSE) {
            newResumePoint = calculateResumePoint(resIp);
        } else {
            newResumePoint = 0;
        }
        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (PlayerState.TAPE_READY,
                        0,
                        newResumePoint,
                        newResumePoint,
                        oldState.currentTapeImageCrate,
                        oldState.chunkListVisible,
                        oldState.alertType,
                        oldState.alertException
                );
        uiState.setValue(newState);

    }

    public void onFailedPlayback(Throwable e) {
        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (PlayerState.TAPE_READY,
                        0,
                        0,
                        0,
                        oldState.currentTapeImageCrate,
                        oldState.chunkListVisible,
                        AlertType.PLAYBACK_ERROR,
                        e
                );
        uiState.setValue(newState);
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

    public void flipChunkListVisibility() {

        boolean newVisibility = !uiState.getValue().chunkListVisible;
        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (oldState.playerState,
                        oldState.progressValue,
                        oldState.playerSelectedResumePointIp,
                        oldState.userSelectedResumePointIp,
                        oldState.currentTapeImageCrate,
                        newVisibility,
                        oldState.alertType,
                        oldState.alertException

                );
        uiState.setValue(newState);

    }


    @Override
    public void onCleared() {
        super.onCleared();
        savePreferences();
        getCasPlaybackHandler().stop(STOP_REASON_TERMINATE);
        getCasPlaybackHandler().removeObserver();
    }

    private void setEmptyState() {
        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (PlayerState.EMPTY,
                        oldState.progressValue,
                        0,
                        0,
                        null,
                        oldState.chunkListVisible,
                        oldState.alertType,
                        oldState.alertException
                );
        uiState.setValue(newState);
    }

    private void setEmptyStateWithError(AlertType alertType, Throwable alertException) {
        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (PlayerState.EMPTY,
                        oldState.progressValue,
                        0,
                        0,
                        null,
                        oldState.chunkListVisible,
                        alertType,
                        alertException
                );
        uiState.setValue(newState);
    }

    public void openTapeImageFromURI(Uri tapeImageUri, String fileName) {

        /*Open nothing*/
        if (tapeImageUri == null) {
            setEmptyState();
            return;
        }

        /*Try to open the tape image*/
        TapeImageOpener.ReadResult result = readTapeImage(tapeImageUri, null, fileName);
        processOpenedImage(result, true, tapeImageUri, fileName);

    }

    public void openTapeImageFromArray(int[] tapeImageBytes, String fileName) {

        if (tapeImageBytes == null) {
            setEmptyState();
            return;
        }

        TapeImageOpener.ReadResult result = readTapeImage(null, tapeImageBytes, fileName);
        processOpenedImage(result, false, null, fileName);
    }

    private void processOpenedImage(TapeImageOpener.ReadResult result, boolean addToRecents, Uri tapeImageUri, String fileName) {
        /*If successful, add to recents*/
        if (result.isSuccess()) {
            if (addToRecents && tapeImageUri != null) {
                getTapeImageRecents().addRecentItem(tapeImageUri, fileName);
            }
            UIState oldState = uiState.getValue();
            UIState newState = new UIState
                    (PlayerState.TAPE_READY,
                            oldState.progressValue,
                            0,
                            0,
                            result.getTapeImageCrate(),
                            oldState.chunkListVisible,
                            AlertType.NONE,
                            null
                    );
            uiState.setValue(newState);
        }
        /*Otherwise, set up for just "empty" tape image*/
        else {
            setEmptyStateWithError(AlertType.CAS_UNREADABLE, result.getException());
        }
    }

    public void dismissAlert() {
        UIState oldState = uiState.getValue();
        UIState newState = new UIState
                (oldState.playerState,
                        oldState.progressValue,
                        oldState.playerSelectedResumePointIp,
                        oldState.userSelectedResumePointIp,
                        oldState.currentTapeImageCrate,
                        oldState.chunkListVisible,
                        AlertType.NONE,
                        null
                );
        uiState.setValue(newState);
    }

    public boolean isPlaying() {
        return uiState.getValue().playerState == PlayerState.PLAYING;
    }

    public LiveData<UIState> getUIState() {
        return this.uiState;
    }


    private SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    private TapeImageOpener getTapeImageOpener() {
        return tapeImageOpener;
    }

    private CasPlaybackHandler getCasPlaybackHandler() {
        return this.casPlaybackHandler;
    }


}
