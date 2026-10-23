package com.baktra.cas2audio.features.recent.domain;

import android.net.Uri;

public final class RecentItem {
    private String filename;
    private Uri uri;

    RecentItem(Uri uri, String filename) {
        this.filename=filename;
        this.uri=uri;
    }

    public String toString() {
        return filename;
    }

    public String getFilename() {
        return this.filename;
    }

    public Uri getUri() {
        return this.uri;
    }
}
