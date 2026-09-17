package com.baktra.cas2audio;

import android.net.Uri;

import java.util.ArrayList;
import java.util.StringTokenizer;

class TapeImageRecents {

    private final ArrayList<RecentItem> recentItems;
    public static final int RECENT_CAPACITY =24;

    TapeImageRecents() {
        recentItems = new ArrayList<>();
    }

    String createPersistenceString() {
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

    void addRecentItem(Uri uri, String filename) {
        RecentItem candidateItem = new RecentItem(uri,filename);

        /*Check if already there*/
        boolean found = false;
        for (RecentItem item : recentItems) {
            if (item.uri.toString().equals(candidateItem.uri.toString())) {
                found = true;
                break;
            }
        }
        /*If already there, just return*/
        if (found) return;

        /*Move to front*/
        recentItems.add(0, candidateItem);
        if (recentItems.size() > RECENT_CAPACITY) recentItems.remove(RECENT_CAPACITY -1);
    }

    void parsePersistenceString(String s) {
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

    void clear() {
        recentItems.clear();
    }

    public RecentItem[] getAsArray() {
        RecentItem[] retVal = new RecentItem[recentItems.size()];
        return recentItems.toArray(retVal);
    }
}
