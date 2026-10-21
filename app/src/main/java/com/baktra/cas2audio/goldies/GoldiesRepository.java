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
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GoldiesRepository {

    public static final int ZIP_OK=0;
    public static final int ZIP_NOT_PRESENT=-1;
    public static final int ZIP_CORRUPTED=-2;
    private boolean zipDirty;

    private File localDirectory;

    private Map<TapeSystem, List<String>> repoMap;
    private Map<String,TapeSystem> dir2SystemMap;
    private List<TapeSystem> tapeSystems;

    public GoldiesRepository(File localDirectory) {
        this.localDirectory=localDirectory;
        repoMap = new HashMap<>();
        dir2SystemMap = new HashMap<>();
        tapeSystems = new ArrayList<>();
        this.zipDirty=true;
    }

    public void displayRepository(GoldiesRepositoryObserver observer) {

        /*If the zip not processed yet, get the entries*/
        if (zipDirty==true) {
            int result = processZip();
            if (result==ZIP_OK) {
                //zipDirty=false;
            }
            else {
                observer.onFailedRead();
                return;
            }
        }

        observer.onDisplay(repoMap,tapeSystems);
    }



    private int processZip() {

        repoMap.clear();
        dir2SystemMap.clear();
        tapeSystems.clear();

        try {
            File f = new File(localDirectory,"goldies.zip");
            //File f = new File("/sdcard/Download/goldies.zip");
            if (!f.exists() || !f.isFile()) return ZIP_NOT_PRESENT;

            /*Instantiate the zip file*/
            ZipFile zipFile = new ZipFile(f);

            processMapEntry(zipFile);

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

                /*Skip files in the root directory*/
                if (entryParentFile==null) continue;

                System.out.println(entryNameFile);
                System.out.println(entryParentFile);
                System.out.println("--");

                /*Get the tape system*/
                TapeSystem tsForEntry=dir2SystemMap.get(entryParentFile.toString());
                if (tsForEntry==null) continue;

                /*If new tape system*/
                if (repoMap.get(tsForEntry)==null) {
                    repoMap.put(tsForEntry,new ArrayList<>());
                }

                /*Place the entry name there*/
                repoMap.get(tsForEntry).add(entryNameFile.getName());
            }


        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            return ZIP_CORRUPTED;
        }

        return ZIP_OK;
    }

    private void processMapEntry(ZipFile zipFile) throws IOException {

        /*Get the mapping from directory name to system*/
        ZipEntry mapFileEntry = zipFile.getEntry("map.txt");
        InputStream isMap = zipFile.getInputStream(mapFileEntry);
        InputStreamReader isrMap = new InputStreamReader(isMap);
        BufferedReader brMap = new BufferedReader(isrMap);

        String line;
        while ((line=brMap.readLine())!=null) {
            line=line.trim();
            StringTokenizer tk = new StringTokenizer(line,",");

            String name = tk.nextToken();
            String country = tk.nextToken();
            String prefixCode = tk.nextToken();

            TapeSystem ts = new TapeSystem(name,country,prefixCode);

            dir2SystemMap.put(prefixCode,ts);
            tapeSystems.add(ts);
        }

        brMap.close();
        isrMap.close();
        isMap.close();


    }
}
