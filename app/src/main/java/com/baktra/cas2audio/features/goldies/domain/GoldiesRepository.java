package com.baktra.cas2audio.features.goldies.domain;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.baktra.cas2audio.core.util.InstructionStream;
import com.baktra.cas2audio.core.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Future<Integer> currentFuture;

    public GoldiesRepository(File localDirectory, SharedPreferences prefs) {
        this.localDirectory=localDirectory;
        repoMap = new HashMap<>();
        dir2SystemMap = new HashMap<>();
        tapeSystems = new ArrayList<>();
        this.zipDirty=true;
        this.prefs=prefs;
        this.currentFuture=null;
    }

    public void displayRepository(GoldiesRepositoryObserver observer,boolean force) {

        if (force) {
            zipDirty=true;
        }

        /*If the zip not processed yet, get the entries*/
        if (zipDirty==true) {
            try {
                processZip();
                zipDirty=false;
                observer.onDisplay(repoMap,tapeSystems);
            }
            catch (Exception e) {
                if (e instanceof FileNotFoundException) {
                    observer.onFailedRead(ZIP_NOT_PRESENT,e);
                }
                else {
                    observer.onFailedRead(ZIP_CORRUPTED,e);
                }
            }
        }
    }


    private void processZip() throws Exception {

        repoMap.clear();
        dir2SystemMap.clear();
        tapeSystems.clear();

        File f = new File(localDirectory, "goldies.zip");
        if (!f.exists() || !f.isFile()) throw new FileNotFoundException(f.getAbsolutePath());

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
            if (entryParentFile == null) continue;

            /*Get the tape system*/
            TapeSystem tsForEntry = dir2SystemMap.get(entryParentFile.toString());
            if (tsForEntry == null) continue;

            /*If new tape system*/
            if (repoMap.get(tsForEntry) == null) {
                repoMap.put(tsForEntry, new ArrayList<>());
            }

            /*Place the entry name there*/
            repoMap.get(tsForEntry).add(entryNameFile.getName());
        }

        zipFile.close();


    }

    private void processMapEntry(ZipFile zipFile) throws IOException {

        try {
            /*Get the mapping from directory name to system*/
            ZipEntry mapFileEntry = zipFile.getEntry("map.txt");
            InputStream isMap = zipFile.getInputStream(mapFileEntry);
            InputStreamReader isrMap = new InputStreamReader(isMap);
            BufferedReader brMap = new BufferedReader(isrMap);

            String line;
            while ((line = brMap.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

                StringTokenizer tk = new StringTokenizer(line, ",");

                String name = tk.nextToken();
                String country = tk.nextToken();
                String prefixCode = tk.nextToken();

                TapeSystem ts = new TapeSystem(name, country, prefixCode);

                dir2SystemMap.put(prefixCode, ts);
                tapeSystems.add(ts);
            }

            brMap.close();
            isrMap.close();
            isMap.close();
        }
        catch (Exception e) {
            throw new IOException("Error when processing mapfile: "+ Utils.getExceptionMessage(e));
        }
    }

    public int[] getTapeImageData(TapeSystem system,String fileName) {

        /*Instantiate the zip file*/
        try {
            File f = new File(localDirectory, "goldies.zip");
            ZipFile zipFile = new ZipFile(f);
            if (!f.exists() || !f.isFile()) return null;

            ZipEntry entry = zipFile.getEntry(system.getPrefixCode()+"/"+fileName);
            InputStream is = zipFile.getInputStream(entry);

            InstructionStream tiBytes = new InstructionStream();
            int oneByte;
            while ((oneByte=is.read())!=-1) {
                tiBytes.add(oneByte);
            }

            /*Close both stream and the ZIP file*/
            is.close();
            zipFile.close();

            /*Return the array*/
            return tiBytes.getInstructions();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }

    public List<TapeSystem> getTapeSystems() {
        return tapeSystems;
    }

    public void saveLastSystem(TapeSystem system) {

        String sName = system.getName();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("c2a_goldies_last_system", sName);
        editor.apply();

    }

    public String loadLastSystem() {
        return  prefs.getString("c2a_goldies_last_system","");
    }

    public void refresh(GoldiesRepositoryObserver observer) {

        if (currentFuture!=null) return;

       zipDirty=true;
       currentFuture = executor.submit(new Callable<Integer>() {

            public Integer call()  {

                try {

                    HttpURLConnection connection = null;
                    InputStream inputStream = null;
                    FileOutputStream outputStream = null;


                    URL url = new URL("https://baktra.wz.cz/packages/goldies.zip");

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                        File outputFile = new File(localDirectory, "goldies2.zip");

                        if (outputFile.exists()) {
                            outputFile.delete();
                        }

                        inputStream = connection.getInputStream();
                        outputStream = new FileOutputStream(outputFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }

                        /*Swap the output file with the final file*/
                        File finalFile = new File(localDirectory, "goldies.zip");
                        if (finalFile.exists()) {
                            finalFile.delete();
                        }
                        outputFile.renameTo(finalFile);

                    } else {
                        throw new IOException("Unexpected server response: " + connection.getResponseCode());
                    }

                    // Ensure streams and connections close cleanly
                    try {
                        if (outputStream != null) outputStream.close();
                    } catch (Exception ignored) {
                    }
                    try {
                        if (inputStream != null) inputStream.close();
                    } catch (Exception ignored) {
                    }
                    if (connection != null) connection.disconnect();

                }
                /*Exception was thrown, handle it. The observer must be called on main thread*/
                catch (Exception totalException) {
                    totalException.printStackTrace();
                    Handler h = new Handler(Looper.getMainLooper());
                    h.post(new Runnable() {
                        public void run() {
                            observer.onFailedRefresh(totalException);
                        }
                    });
                    currentFuture=null;
                    return 0;
                }
                /*Normal execution, the observer must be called on main thread*/
                Handler h = new Handler(Looper.getMainLooper());
                h.post(new Runnable() {
                    public void run() {
                        observer.onSuccessfullRefresh();
                    }
                });
                currentFuture=null;
                return 0;
            }
        });



    }

    public void cancelRefresh() {
        if (currentFuture==null) return;
        currentFuture.cancel(true);
    }



}
