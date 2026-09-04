package com.baktra.cas2audio;

import android.net.Uri;

import java.util.ArrayList;
import java.util.StringTokenizer;

class TapeImageHistory {

    private final ArrayList<HistoryItem> historyItems;
    public static final int HISTORY_CAPACITY=24;

    TapeImageHistory() {
        historyItems = new ArrayList<>();
    }

    String createPersistenceString() {
        StringBuilder sb = new StringBuilder();

        for (HistoryItem ri : historyItems) {
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

    void addHistoryItem(Uri uri, String filename) {
        HistoryItem candidateItem = new HistoryItem(uri,filename);

        /*Check if already there*/
        boolean found = false;
        for (HistoryItem item : historyItems) {
            if (item.uri.toString().equals(candidateItem.uri.toString())) {
                found = true;
                break;
            }
        }
        /*If already there, just return*/
        if (found) return;

        /*Move to front*/
        historyItems.add(0, candidateItem);
        if (historyItems.size() > HISTORY_CAPACITY) historyItems.remove(HISTORY_CAPACITY-1);
    }

    void parsePersistenceString(String s) {
        historyItems.clear();
        StringTokenizer tk = new StringTokenizer(s, ";");

        while (tk.hasMoreTokens()) {
            String pair = tk.nextToken();
            if (pair.length() == 0) break;
            StringTokenizer tk2 = new StringTokenizer(pair,",");
            String p1 = tk2.nextToken().replace("{", "").replace("}", "");
            String p2 = tk2.nextToken().replace("{", "").replace("}", "");
            historyItems.add(new HistoryItem(Uri.parse(p1),p2));
        }
    }

    void clear() {
        historyItems.clear();
    }

    public HistoryItem[] getAsArray() {
        HistoryItem[] retVal = new HistoryItem[historyItems.size()];
        return historyItems.toArray(retVal);
    }
}
