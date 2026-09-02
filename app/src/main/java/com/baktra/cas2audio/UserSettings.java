package com.baktra.cas2audio;

import android.content.SharedPreferences;

import java.io.Serializable;

class UserSettings implements Serializable {

    private boolean doMono = false;
    private boolean do48kHz = false;
    private boolean doSquareWave = false;
    private boolean doInvertPolarity = false;

    private int amplitude = 50;

    public UserSettings(final boolean doMono, final boolean do48kHz, final boolean doSquareWave, final boolean doInvertPolarity, final int amplitude) {
        this.doMono = doMono;
        this.do48kHz = do48kHz;
        this.doSquareWave = doSquareWave;
        this.doInvertPolarity = doInvertPolarity;
        this.amplitude=amplitude;
    }

    public UserSettings() {

    }

    public boolean isDoMono() {
        return this.doMono;
    }

    public boolean isDo48kHz() {
        return this.do48kHz;
    }

    public boolean isDoSquareWave() {
        return this.doSquareWave;
    }

    public boolean isDoInvertPolarity() {
        return this.doInvertPolarity;
    }

    public int getAmplitude() {return this.amplitude;}

    public void setDoMono(final boolean doMono) {
        this.doMono = doMono;
    }

    public void setDo48kHz(final boolean do48kHz) {
        this.do48kHz = do48kHz;
    }

    public void setDoSquareWave(final boolean doSquareWave) {
        this.doSquareWave = doSquareWave;
    }

    public void setDoInvertPolarity(final boolean doInvertPolarity) {
        this.doInvertPolarity = doInvertPolarity;
    }

    public void setAmplitude(final int amplitude) {this.amplitude = amplitude;}

    public static UserSettings createFromPersistentStorage(SharedPreferences sPref) {
        UserSettings s = new UserSettings();

        s.do48kHz = sPref.getBoolean("c2a_48kHz", false);
        s.doMono = sPref.getBoolean("c2a_mono", false);
        s.doSquareWave = sPref.getBoolean("c2a_square", false);
        s.doInvertPolarity = sPref.getBoolean("c2a_invert_pulses", false);
        s.amplitude = sPref.getInt("c2a_amplitude",50);

        return s;
    }

    protected static void flushToPersistentStorage(UserSettings s, SharedPreferences sPref) {

        SharedPreferences.Editor editor = sPref.edit();
        editor.putBoolean("c2a_48kHz", s.do48kHz);
        editor.putBoolean("c2a_mono", s.doMono);
        editor.putBoolean("c2a_square", s.doSquareWave);
        editor.putBoolean("c2a_invert_pulses", s.doInvertPolarity);
        editor.putInt("c2a_amplitude",s.amplitude);
        editor.apply();
    }

    public String toString() {
        return "Settings:" + do48kHz + "," + doMono + "," + doSquareWave + "," + doInvertPolarity+","+amplitude;

    }
}
