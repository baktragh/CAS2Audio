package com.baktra.cas2audio.features.goldies;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baktra.cas2audio.features.settings.repository.SettingsRepository;

import java.util.List;
import java.util.Map;


public class GoldiesViewModel extends ViewModel implements GoldiesRepositoryObserver {

    enum MainState {
        EMPTY,
        ZIP_NOT_PRESENT_OR_CORRUPT,
        ZIP_DOWNLOAD_FAILED,
        LIST_POPULATED,
        ZIP_DOWNLOADING
    }
    protected static class UIState {

        private MainState mainState;
        private TapeSystem currentTapeSystem;
        private List<String> currentItems;
        private List<TapeSystem> tapeSystems;
        private Exception failureException;

        public UIState(MainState mainState, TapeSystem currentTapeSystem, List<String> currentItems,List<TapeSystem> tapeSystems,Exception failureException) {
            this.mainState=mainState;
            this.currentTapeSystem=currentTapeSystem;
            this.currentItems=currentItems;
            this.tapeSystems=tapeSystems;
            this.failureException=failureException;
        }

        public UIState(MainState mainState, TapeSystem currentTapeSystem, List<String> currentItems,List<TapeSystem> tapeSystems) {
            this(mainState,currentTapeSystem,currentItems,tapeSystems,null);
        }

        public MainState getMainState() {
            return mainState;
        }
        public TapeSystem getCurrentTapeSystem() {
            return this.currentTapeSystem;
        }

        public List<String> getCurrentItems() {
            return currentItems;
        }

        public List<TapeSystem> getTapeSystems() {
            return tapeSystems;
        }

        public Exception getFailureException() {
            return failureException;
        }
    }

    private GoldiesRepository goldiesRepo;
    private MutableLiveData<UIState> uiState;

    private Map<TapeSystem, List<String>> currentMap;
    private SettingsRepository settingsRepo;
    private String savedTapeSystemName;

    public GoldiesViewModel(GoldiesRepository goldiesRepo, SettingsRepository settingsRepo) {
        this.goldiesRepo=goldiesRepo;
        this.uiState = new MutableLiveData<>(new UIState(MainState.EMPTY,null,null,null));
        this.currentMap=null;
        this.settingsRepo=settingsRepo;
        this.savedTapeSystemName=goldiesRepo.loadLastSystem();

    }

    public void reDisplay() {
        goldiesRepo.displayRepository(this,true);
    }

    @Override
    public void onSuccessfullRefresh() {
    }

    @Override
    public void onFailedRefresh(Exception failureException) {
        UIState newState = new UIState(MainState.ZIP_DOWNLOAD_FAILED, null, null, null,failureException);
        uiState.setValue(newState);
    }

    public void onFailedRead(int reason,Exception failureException) {
        UIState newState = new UIState(MainState.ZIP_NOT_PRESENT_OR_CORRUPT,null,null,null,failureException);
        uiState.setValue(newState);
    }

    @Override
    public void onDisplay(Map<TapeSystem, List<String>> map,List<TapeSystem> tapeSystems) {

        currentMap=map;

        /*Determine if empty or populated*/
        if (tapeSystems.isEmpty()) {
            UIState newState = new UIState(MainState.EMPTY,null,null,null);
            uiState.setValue(newState);
            return;
        }

        /*Change the state to list populated, adjust current tape system, and
          transition to a new state
         */

        /*No candidate yet*/
        UIState currState = uiState.getValue();
        TapeSystem newTapeSystem=null;
        TapeSystem oldTapeSystem=currState.getCurrentTapeSystem();

        /*If no current system, try getting from the persistence data*/
        if (oldTapeSystem==null) {

            if (!savedTapeSystemName.isEmpty()) {
                for (TapeSystem oneSystem: tapeSystems) {
                    if (oneSystem.getName().equals(savedTapeSystemName)) {
                        newTapeSystem=oneSystem;
                        break;
                    }
                }
            }
        }
        /*Otherwise, use it*/
        else {
            newTapeSystem=currState.getCurrentTapeSystem();
        }

        /*Fall back to the first in the list*/
        if (newTapeSystem==null) {
            newTapeSystem = tapeSystems.get(0);
        }

        //System.out.println("+++ "+oldTapeSystem+">>"+newTapeSystem);


        List<String> lItems = map.get(newTapeSystem);
        UIState newState = new UIState(MainState.LIST_POPULATED, newTapeSystem, lItems,tapeSystems);
        uiState.setValue(newState);
    }

    public void updateCurrentTapeSystem(TapeSystem newSystem) {

        List<String> lItems = currentMap.get(newSystem);
        UIState newState = new UIState(MainState.LIST_POPULATED, newSystem, lItems,goldiesRepo.getTapeSystems());
        uiState.setValue(newState);
    }

    public LiveData<UIState> getUIState() {
        return this.uiState;
    }


    public int[] getTapeImageData(TapeSystem system, String fileName) {
        return goldiesRepo.getTapeImageData(system,fileName);
    }

    public void initiateRefresh() {
        UIState newState = new UIState(MainState.ZIP_DOWNLOADING,null,null,null,null);
        uiState.setValue(newState);
        goldiesRepo.refresh(this);
    }


    public void onCleared() {
        TapeSystem lastSystem = uiState.getValue().getCurrentTapeSystem();
        if (lastSystem!=null) {
           goldiesRepo.saveLastSystem(lastSystem);
        }
    }


}
