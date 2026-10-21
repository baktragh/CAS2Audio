package com.baktra.cas2audio.goldies;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class GoldiesViewModelFactory implements ViewModelProvider.Factory {

        private final GoldiesRepository goldiesRepo;

        public GoldiesViewModelFactory(GoldiesRepository goldiesRepo) {
            this.goldiesRepo=goldiesRepo;
        }

        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {

            if (modelClass.isAssignableFrom(GoldiesViewModel.class)) {
                return (T) new GoldiesViewModel(goldiesRepo);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }

}
