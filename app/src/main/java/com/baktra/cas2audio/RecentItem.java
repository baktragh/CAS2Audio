package com.baktra.cas2audio;

import android.net.Uri;

final class RecentItem {
    String filename;
    Uri uri;

    RecentItem(Uri uri, String filename) {
        this.filename=filename;
        this.uri=uri;
    }

    public String toString() {
        return filename;
    }
}
