package com.baktra.cas2audio;

import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

final class RecentItem {
    String filename;
    Uri uri;

    RecentItem(Uri uri,String filename) {
        this.filename=filename;
        this.uri=uri;
    }

    public static String createPersistenceString(ArrayList<RecentItem> recentItems) {
        StringBuilder sb = new StringBuilder();

        for (RecentItem ri : recentItems) {
            String p1 = ri.uri.toString();
            String p2 = ri.filename;
            sb.append("{");
            sb.append(p1);
            sb.append(',');
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
            StringTokenizer tk2 = new StringTokenizer(pair,",");
            String p1 = tk2.nextToken().replace("{", "").replace("}", "");
            String p2 = tk2.nextToken().replace("{", "").replace("}", "");
            recentItems.add(new RecentItem(Uri.parse(p1),p2));
        }
    }


    public String toString() {
        return filename;
    }
}
