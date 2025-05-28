package com.baktra.cas2audio;

import android.net.Uri;

import java.util.ArrayList;
import java.util.StringTokenizer;

final class RecentItem {
    Uri uri;
    String filename;

    RecentItem(Uri uri, String filename) {
        this.uri = uri;
        this.filename = filename;
    }

    public static String createPersistenceString(ArrayList<RecentItem> recentItems) {
        StringBuilder sb = new StringBuilder();

        for (RecentItem ri : recentItems) {
            String p1 = ri.uri.toString();
            String p2 = ri.filename.toString();
            sb.append("{");
            sb.append(p1);
            sb.append(",");
            sb.append(p2);
            sb.append("}");
            sb.append(";");
        }

        return sb.toString();
    }

    public static void parsePersistenceString(String s, ArrayList<RecentItem> recentItems) {
        recentItems.clear();
        StringTokenizer tk = new StringTokenizer(s, ";");

        while (tk.hasMoreTokens()) {
            String pair = tk.nextToken();
            if (pair.length() == 0) break;

            StringTokenizer tk2 = new StringTokenizer(pair, ",");
            String p1 = tk2.nextToken().replace("{", "");
            String p2 = tk2.nextToken().replace("}", "");

            recentItems.add(new RecentItem(Uri.parse(p1), p2));
        }


    }


    public String toString() {
        return filename;
    }
}
