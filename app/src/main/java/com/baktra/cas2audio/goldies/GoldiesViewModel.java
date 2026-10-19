package com.baktra.cas2audio.goldies;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Map;


public class GoldiesViewModel extends ViewModel implements GoldiesRepoObserver {

    private static class UIState {
        boolean isRefreshEnabled;
        TapeSystem currentTapeSystem;

        List<String> currentItems;

        public UIState(boolean isRefreshEnabled, TapeSystem currentTapeSystem, List<String> currentItems) {
            this.isRefreshEnabled = isRefreshEnabled;
            this.currentTapeSystem=currentTapeSystem;
            this.currentItems=currentItems;
        }

        public boolean isRefreshEnabled() {
            return this.isRefreshEnabled;
        }

        public TapeSystem getCurrentTapeSystem() {
            return this.currentTapeSystem;
        }
    }

    private GoldiesRepository goldiesRepo;
    private MutableLiveData<UIState> uiState;



    @Override
    public void onSuccessfullRefresh() {

    }

    @Override
    public void onFailedRefresh() {

    }

    @Override
    public void onDisplay(Map<TapeSystem, List<String>> map) {

        List<String> lItems = map.get(uiState.getValue().getCurrentTapeSystem());
        UIState newState = new UIState(uiState.getValue().isRefreshEnabled(), uiState.getValue().getCurrentTapeSystem(), lItems);
        uiState.setValue(newState);
    }
}
