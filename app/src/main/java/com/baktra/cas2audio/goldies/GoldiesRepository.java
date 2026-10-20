package com.baktra.cas2audio.goldies;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GoldiesRepository {

    public static final int ZIP_OK=0;
    public static final int ZIP_NOT_PRESENT=-1;
    public static final int ZIP_CORRUPTED=-2;
    private boolean zipDirty;

    private File localDirectory;

    private Map<String, List<String>> zipMap;
    private Map<String,TapeSystem> dir2SystemMap;

    public GoldiesRepository(File localDirectory) {
        this.localDirectory=localDirectory;
        zipMap = new HashMap<>();
        this.zipDirty=true;
    }

    public void displayRepository(GoldiesRepositoryObserver observer) {

        /*If the zip not processed yet, get the entries*/
        if (zipDirty==true) {
            int result = processZip();
            if (result==ZIP_OK) {
                zipDirty=false;
            }
            else {
                observer.onFailedRead();
            }
        }

        /*Prepare the map for the observer*/

        /*Create a new map*/
        Map<TapeSystem,List<String>> repoMap = new HashMap<>();
        Set<Map.Entry<String,TapeSystem>> entries = dir2SystemMap.entrySet();

        /*Put all tape systems in the new map, with empty list of entry names*/
        for (Map.Entry<String,TapeSystem> oneEntry:entries) {
            repoMap.put(oneEntry.getValue(),new ArrayList<>());
        }

        Set<Map.Entry<String,List<String>>> zipMapEntries = zipMap.entrySet();

        /*For each directory in the zip, add to the map entry with matching tape system*/
        for (Map.Entry<String,List<String>> oneEntry: zipMapEntries) {
            /*Get the tape system*/
            TapeSystem t = dir2SystemMap.get(oneEntry.getKey());
            repoMap.get(t).addAll(oneEntry.getValue());
        }

        observer.onDisplay(repoMap);

    }



    private int processZip() {

        zipMap.clear();

        try {
            File f = new File(localDirectory,"goldies.zip");
            if (!f.exists() || !f.isFile()) return ZIP_NOT_PRESENT;

            /*Instantiate the zip file*/
            ZipFile zipFile = new ZipFile(f);

            /*Get all entries*/
            Enumeration<ZipEntry> allEntries = (Enumeration<ZipEntry>) zipFile.entries();

            /*Process the entries*/
            while (allEntries.hasMoreElements()) {
                ZipEntry oneEntry = allEntries.nextElement();

                /*Skip directories*/
                if (oneEntry.isDirectory()) continue;

                /*Files*/
                File entryNameFile = new File(oneEntry.getName());
                File entryParentFile = entryNameFile.getParentFile();

                /*If new directory, place it there*/
                if (zipMap.get(entryParentFile.getName())==null) {
                    zipMap.put(entryParentFile.getName(),new ArrayList<>());
                }
                /*Place the entry there*/
                zipMap.get(entryParentFile.getName()).add(entryNameFile.getName());
            }

            /*Get the mapping from directory name to system*/
            ZipEntry mapFileEntry = zipFile.getEntry("map.txt");
            InputStream isMap = zipFile.getInputStream(mapFileEntry);
            InputStreamReader isrMap = new InputStreamReader(isMap);
            BufferedReader brMap = new BufferedReader(isrMap);

            processMap(brMap);

            brMap.close();
            isrMap.close();
            isMap.close();
        }
        catch (IOException ioe) {
            return ZIP_CORRUPTED;
        }

        return ZIP_OK;
    }

    private void processMap(BufferedReader br) throws IOException {

        String line;
        while ((line=br.readLine())!=null) {
            line=line.trim();
            StringTokenizer tk = new StringTokenizer(line,",");

            String name = tk.nextToken();
            String country = tk.nextToken();
            String prefixCode = tk.nextToken();

            dir2SystemMap.put(prefixCode,new TapeSystem(name,country,prefixCode));
        }

    }
}
