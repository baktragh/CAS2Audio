package com.baktra.cas2audio;

import android.app.Application;
import android.content.Context;
import android.os.PowerManager;

import com.baktra.cas2audio.settings.SettingsRepository;
import com.baktra.cas2audio.tapeimage.TapeImageOpener;

public class Cas2AudioApp extends Application {

    private SettingsRepository settingsRepository;
    private PowerManager powerManager;
    private TapeImageOpener tapeImageOpener;

    public void onCreate() {

        super.onCreate();
        settingsRepository = new SettingsRepository(getSharedPreferences("c2a_prefs", Context.MODE_PRIVATE));
        tapeImageOpener = new TapeImageOpener(getContentResolver());

        try {
            powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        }
        catch(Exception e) {
            powerManager=null;
        }

    }


    public PowerManager getPowerManager() {
        return powerManager;
    }

    public SettingsRepository getSettingsRepository() {
        return this.settingsRepository;
    }

    public TapeImageOpener getTapeImageOpener() {
        return this.tapeImageOpener;
    }
}
