package com.baktra.cas2audio.features.player.presentation;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.baktra.cas2audio.features.player.domain.CasPlaybackHandler;
import com.baktra.cas2audio.features.settings.repository.SettingsRepository;
import com.baktra.cas2audio.features.player.domain.tapeimage.TapeImageOpener;

public class MainViewModelFactory implements ViewModelProvider.Factory {


    private final SettingsRepository settingsRepository;
    private final TapeImageOpener tapeImageOpener;

    private final CasPlaybackHandler casPlaybackHandler;

    public MainViewModelFactory(SettingsRepository sr, TapeImageOpener to,CasPlaybackHandler cph) {

        this.settingsRepository=sr;
        this.tapeImageOpener=to;
        this.casPlaybackHandler=cph;
                
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(settingsRepository,tapeImageOpener,casPlaybackHandler);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}

