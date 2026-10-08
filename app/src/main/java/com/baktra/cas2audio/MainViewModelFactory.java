package com.baktra.cas2audio;

import android.app.Application;
import android.content.ContentResolver;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.baktra.cas2audio.settings.SettingsRepository;
import com.baktra.cas2audio.tapeimage.TapeImageOpener;

public class MainViewModelFactory implements ViewModelProvider.Factory {
    private final Cas2AudioApp app;

    public MainViewModelFactory(Cas2AudioApp app) {

        this.app=app;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(app);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}

