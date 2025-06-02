package com.baktra.cas2audio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

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
        userSettings = (UserSettings)getIntent().getSerializableExtra("user_settings");
        setUI();

    }

    protected final void onStop() {
        System.out.println("Stopping settings");
        super.onStop();
        flushUIToSettings();
        setResult(RESULT_OK,new Intent().putExtra("user_settings",this.userSettings));
        System.out.println("Result was set");
        finish();
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

        System.out.println("Flushed");

    }


}
