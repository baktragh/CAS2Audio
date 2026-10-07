package com.baktra.cas2audio.settings;

import android.content.SharedPreferences;

import java.io.File;

public class UIPersistence {

    boolean chunkListVisible;

    public UIPersistence() {
        chunkListVisible =false;
    }

    public static UIPersistence load(SharedPreferences sharedPrefs) {

        UIPersistence uiP = new UIPersistence();
        try {

            uiP.chunkListVisible =(sharedPrefs.getBoolean("c2a_chunks_v", false));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            return uiP;
        }
    }

    public static void save(UIPersistence uiP,SharedPreferences.Editor editor) {
        editor.putBoolean("c2a_chunks_v",uiP.chunkListVisible);
    }

    public boolean isChunkListVisible() {
        return this.chunkListVisible;
    }

    public void updateChunkListVisible(boolean newVisibility) {
        this.chunkListVisible=newVisibility;
    }
}
