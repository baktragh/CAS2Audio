package com.baktra.cas2audio;

public class CasTaskAlertCrate {
    private int titleId;

    public String getMessage() {
        return this.message;
    }

    public int getTitle() {
        return this.titleId;
    }

    private String message;

    public CasTaskAlertCrate(final int title, final String message) {
        this.titleId = title;
        this.message = message;
    }
}
