package com.baktra.cas2audio;

import android.app.Application;
import android.content.Context;
import android.os.PowerManager;

import com.baktra.cas2audio.settings.SettingsRepository;
import com.baktra.cas2audio.tapeimage.TapeImageOpener;

import java.util.Optional;

public class Cas2AudioApp extends Application {

    private SettingsRepository settingsRepository;
    private PowerManager powerManager;
    private TapeImageOpener tapeImageOpener;
    private CasPlaybackHandler casPlaybackHandler;

    public void onCreate() {

        super.onCreate();
        settingsRepository = new SettingsRepository(getSharedPreferences("c2a_prefs", Context.MODE_PRIVATE));
        tapeImageOpener = new TapeImageOpener(getContentResolver());

        Optional<PowerManager> pm =Optional.empty();
        try {
            PowerManager p = (PowerManager) getSystemService(POWER_SERVICE);
            pm=Optional.of(p);
        }
        catch(Exception e) {
           pm=Optional.empty();
        }
        casPlaybackHandler = new CasPlaybackHandler(pm);

    }


    public SettingsRepository getSettingsRepository() {
        return this.settingsRepository;
    }

    public TapeImageOpener getTapeImageOpener() {
        return this.tapeImageOpener;
    }

    public CasPlaybackHandler getCasPlaybackHandler() {
        return casPlaybackHandler;
    }
}
