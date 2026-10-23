package com.baktra.cas2audio.features.goldies;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.baktra.cas2audio.features.settings.repository.SettingsRepository;

public class GoldiesViewModelFactory implements ViewModelProvider.Factory {

        private final GoldiesRepository goldiesRepo;
        private final SettingsRepository settingsRepo;

        public GoldiesViewModelFactory(GoldiesRepository goldiesRepo, SettingsRepository settingsRepo) {
            this.goldiesRepo=goldiesRepo;
            this.settingsRepo=settingsRepo;
        }

        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {

            if (modelClass.isAssignableFrom(GoldiesViewModel.class)) {
                return (T) new GoldiesViewModel(goldiesRepo,settingsRepo);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }

}
