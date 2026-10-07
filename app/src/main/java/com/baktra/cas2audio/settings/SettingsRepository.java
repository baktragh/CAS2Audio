package com.baktra.cas2audio.settings;

import android.content.SharedPreferences;

import com.baktra.cas2audio.recent.TapeImageRecents;

public class SettingsRepository {

    private SharedPreferences sharedPrefs;
    private UserSettings userSettings;
    private UIPersistence uiPersistence;

    private TapeImageRecents tiRecents;

    public SettingsRepository(SharedPreferences sharedPrefs) {
        this.sharedPrefs=sharedPrefs;
        userSettings = new UserSettings();
        uiPersistence = new UIPersistence();
        tiRecents = new TapeImageRecents();
    }

    public void saveSettings() {

        SharedPreferences.Editor editor = sharedPrefs.edit();
        UserSettings.save(userSettings,editor);
        UIPersistence.save(uiPersistence,editor);
        TapeImageRecents.save(tiRecents,editor);

        editor.apply();
    }

    public void loadSettings() {
        userSettings = UserSettings.load(sharedPrefs);
        uiPersistence = UIPersistence.load(sharedPrefs);
        tiRecents = TapeImageRecents.load(sharedPrefs);
    }

    public UserSettings getUserSettings() {
        return this.userSettings;
    }

    public UIPersistence getUiPersistence() {
        return this.uiPersistence;
    }

    public void setUserSettings(UserSettings us) {
        this.userSettings=us;
    }

    public TapeImageRecents getTapeImageRecents() {
        return this.tiRecents;
    }
}
