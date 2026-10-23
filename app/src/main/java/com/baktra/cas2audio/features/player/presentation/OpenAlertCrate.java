package com.baktra.cas2audio.features.player.presentation;

public class OpenAlertCrate {

    private int reason;
    private String details;

    public OpenAlertCrate(int reason,String details) {
        this.reason=reason;
        this.details=details;
    }

    public int getReason() {
        return this.reason;
    }

    public String getDetails() {
        return this.details;
    }
}
