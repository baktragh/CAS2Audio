package com.baktra.cas2audio.goldies;

import android.content.SharedPreferences;

import java.util.Collection;

public class GoldiesPersistence {

    private String lastSystemSelected;

    public static void save(GoldiesPersistence p, SharedPreferences.Editor editor) {
        editor.putString("c2a_goldies_last_system", p.lastSystemSelected==null?"":p.lastSystemSelected);
        editor.apply();
    }

    public static GoldiesPersistence load(SharedPreferences prefs) {
        GoldiesPersistence gp = new GoldiesPersistence();
        gp.lastSystemSelected=prefs.getString("c2a_goldies_last_system","");
        return gp;
    }

    public String getLastSystem() {
        return lastSystemSelected;
    }

    public void setLastSystem(String name) {
        lastSystemSelected=name;
    }
}
