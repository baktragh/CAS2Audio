package com.baktra.cas2audio.goldies;

import android.provider.Contacts;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Map;


public class GoldiesViewModel extends ViewModel implements GoldiesRepositoryObserver {

    protected static class UIState {
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

        public List<String> getCurrentItems() {
            return currentItems;
        }
    }

    private GoldiesRepository goldiesRepo;
    private MutableLiveData<UIState> uiState;

    public GoldiesViewModel(GoldiesRepository goldiesRepo) {
        this.goldiesRepo=goldiesRepo;
        this.uiState = new MutableLiveData<>(new UIState(true,null,null));
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

    public void onFailedRead() {
        System.out.println("Failed read");
    }

    @Override
    public void onDisplay(Map<TapeSystem, List<String>> map,List<TapeSystem> tapeSystems) {

        List<String> lItems = map.get(tapeSystems.get(0));
        UIState newState = new UIState(uiState.getValue().isRefreshEnabled(), tapeSystems.get(0), lItems);
        uiState.setValue(newState);
    }

    public LiveData<UIState> getUIState() {
        return this.uiState;
    }
}
