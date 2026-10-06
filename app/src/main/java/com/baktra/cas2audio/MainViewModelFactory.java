package com.baktra.cas2audio;

import android.content.ContentResolver;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class MainViewModelFactory implements ViewModelProvider.Factory {
    private final SharedPreferences sharedPreferences;
    private final ContentResolver contentResolver;

    public MainViewModelFactory(SharedPreferences sharedPreferences,ContentResolver contentResolver) {
        this.sharedPreferences = sharedPreferences;
        this.contentResolver=contentResolver;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(sharedPreferences,contentResolver);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}

