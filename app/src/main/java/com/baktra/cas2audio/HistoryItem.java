package com.baktra.cas2audio;

import android.net.Uri;

import java.util.ArrayList;
import java.util.StringTokenizer;

final class HistoryItem {
    String filename;
    Uri uri;

    HistoryItem(Uri uri, String filename) {
        this.filename=filename;
        this.uri=uri;
    }





    public String toString() {
        return filename;
    }
}
