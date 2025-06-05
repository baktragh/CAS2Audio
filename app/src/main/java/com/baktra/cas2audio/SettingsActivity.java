package com.baktra.cas2audio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;

public class SettingsActivity extends Activity {

    private UserSettings userSettings;

    public SettingsActivity() {
        super();
    }

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    protected final void onResume() {
        super.onResume();
        userSettings = (UserSettings) getIntent().getSerializableExtra("user_settings");
        setUI();

    }

    public void onConfirm(View view) {
        super.onStop();
        flushUIToSettings();
        setResult(RESULT_OK, new Intent().putExtra("user_settings", this.userSettings));
        finish();
    }

    public void onDefaults(View view) {
        userSettings = new UserSettings();
        setUI();
    }

    protected final void onDestroy() {
        super.onDestroy();
    }

    private final void setUI() {

        Switch s48Khz = findViewById(R.id.sw48kHz);
        Switch sMono = findViewById(R.id.swChannels);
        Switch sInvertPulses = findViewById(R.id.swInvertPulses);
        Switch sSquare = findViewById(R.id.swSquareWave);

        s48Khz.setChecked(userSettings.isDo48kHz());
        sMono.setChecked(userSettings.isDoMono());
        sInvertPulses.setChecked(userSettings.isDoInvertPolarity());
        sSquare.setChecked(userSettings.isDoSquareWave());

    }

    private final void flushUIToSettings() {
        Switch s48Khz = findViewById(R.id.sw48kHz);
        Switch sMono = findViewById(R.id.swChannels);
        Switch sInvertPulses = findViewById(R.id.swInvertPulses);
        Switch sSquare = findViewById(R.id.swSquareWave);

        userSettings.setDo48kHz(s48Khz.isChecked());
        userSettings.setDoMono(sMono.isChecked());
        userSettings.setDoInvertPolarity(sInvertPulses.isChecked());
        userSettings.setDoSquareWave(sSquare.isChecked());



    }


}
