package com.baktra.cas2audio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;

public class SettingsActivity extends Activity {

    private UserSettings userSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    protected void onResume() {
        super.onResume();
        userSettings = (UserSettings) getIntent().getSerializableExtra("user_settings");
        setUI();

    }

    void onConfirm(View view) {
        onStop();
        flushUIToSettings();
        setResult(RESULT_OK, new Intent().putExtra("user_settings", userSettings));
        finish();
    }

    void onDefaults(View view) {
        userSettings = new UserSettings();
        setUI();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setUI() {

        Switch s48Khz = findViewById(R.id.sw48kHz);
        Switch sMono = findViewById(R.id.swChannels);
        Switch sInvertPulses = findViewById(R.id.swInvertPulses);
        Switch sSquare = findViewById(R.id.swSquareWave);
        SeekBar sbAmplitude = findViewById(R.id.sbAmplitude);

        s48Khz.setChecked(userSettings.isDo48kHz());
        sMono.setChecked(userSettings.isDoMono());
        sInvertPulses.setChecked(userSettings.isDoInvertPolarity());
        sSquare.setChecked(userSettings.isDoSquareWave());
        sbAmplitude.setProgress(userSettings.getAmplitude());

    }

    private void flushUIToSettings() {
        Switch s48Khz = findViewById(R.id.sw48kHz);
        Switch sMono = findViewById(R.id.swChannels);
        Switch sInvertPulses = findViewById(R.id.swInvertPulses);
        Switch sSquare = findViewById(R.id.swSquareWave);
        SeekBar sbAmplitude = findViewById(R.id.sbAmplitude);

        userSettings.setDo48kHz(s48Khz.isChecked());
        userSettings.setDoMono(sMono.isChecked());
        userSettings.setDoInvertPolarity(sInvertPulses.isChecked());
        userSettings.setDoSquareWave(sSquare.isChecked());
        userSettings.setAmplitude(sbAmplitude.getProgress());

    }


}
