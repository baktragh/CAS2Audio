package com.baktra.cas2audio.goldies;

import static com.baktra.cas2audio.CasPlaybackHandler.STOP_REASON_TERMINATE;

import android.provider.Contacts;

import androidx.compose.ui.text.font.FontVariation;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baktra.cas2audio.settings.SettingsRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;


public class GoldiesViewModel extends ViewModel implements GoldiesRepositoryObserver {

    enum MainState {
        EMPTY,
        ZIP_NOT_PRESENT,
        ZIP_DOWNLOAD_FAILED,
        LIST_POPULATED
    }
    protected static class UIState {

        private MainState mainState;
        private TapeSystem currentTapeSystem;
        private List<String> currentItems;
        private List<TapeSystem> tapeSystems;

        public UIState(MainState mainState, TapeSystem currentTapeSystem, List<String> currentItems,List<TapeSystem> tapeSystems) {
            this.mainState=mainState;
            this.currentTapeSystem=currentTapeSystem;
            this.currentItems=currentItems;
            this.tapeSystems=tapeSystems;
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
    }

    private GoldiesRepository goldiesRepo;
    private MutableLiveData<UIState> uiState;

    private Map<TapeSystem, List<String>> currentMap;
    private SettingsRepository settingsRepo;
    private GoldiesPersistence persistence;

    public GoldiesViewModel(GoldiesRepository goldiesRepo, SettingsRepository settingsRepo) {
        this.goldiesRepo=goldiesRepo;
        this.uiState = new MutableLiveData<>(new UIState(MainState.EMPTY,null,null,null));
        this.currentMap=null;
        this.settingsRepo=settingsRepo;
        persistence = settingsRepo.loadGoldiesPersistence();
    }

    public void reDisplay() {
        goldiesRepo.displayRepository(this);
    }

    @Override
    public void onSuccessfullRefresh() {
        System.out.println("Successful refresh");
    }

    @Override
    public void onFailedRefresh() {
        System.out.println("Failed refresh");
    }

    public void onFailedRead(int reason) {
        UIState newState = new UIState(MainState.ZIP_NOT_PRESENT,null,null,null);
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

            if (!persistence.getLastSystem().isEmpty()) {
                for (TapeSystem oneSystem: tapeSystems) {
                    if (oneSystem.getName().equals(persistence.getLastSystem())) {
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

        System.out.println("+++ "+oldTapeSystem+">>"+newTapeSystem);


        List<String> lItems = map.get(newTapeSystem);
        UIState newState = new UIState(MainState.LIST_POPULATED, newTapeSystem, lItems,tapeSystems);
        uiState.setValue(newState);
    }

    public void updateCurrentTapeSystem(TapeSystem newSystem) {

        List<String> lItems = currentMap.get(newSystem);
        UIState newState = new UIState(MainState.LIST_POPULATED, newSystem, lItems,uiState.getValue().getTapeSystems());
        uiState.setValue(newState);
    }

    public LiveData<UIState> getUIState() {
        return this.uiState;
    }

    public void onCleared() {
        TapeSystem lastSystem = uiState.getValue().getCurrentTapeSystem();
        if (lastSystem!=null) {
            persistence.setLastSystem(lastSystem.getName());
        }
        settingsRepo.saveGoldiesPersistence(persistence);
    }


}
