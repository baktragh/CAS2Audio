package com.baktra.cas2audio.tapeimage;

import com.baktra.cas2audio.FileFormatException;
import com.baktra.cas2audio.TapeImageProcessor;

import java.io.*;
import java.util.*;



/**
 *
 * @author  
 */
public class TapeImage {

    private final ArrayList<TapeImageChunk> chunkList;
    

    public TapeImage() {
        chunkList = new ArrayList<>(32);
        
    }

    /**
     * @return
     */
    public final int[] getChunkBalance() {

        int[] retVal = new int[2];
        int l = chunkList.size();
        TapeImageChunk chunk;

        retVal[0] = l;
        /*Total*/
        retVal[1] = 0;
        /*Unsupported*/

        boolean lastParentSupported = false;

        Iterator it = chunkList.iterator();
        while (it.hasNext()) {
            chunk = (TapeImageChunk) it.next();

            /*Parent chunk ?*/
            if (chunk.isGeneratedUsingParent() == false) {
                lastParentSupported = TapeImageProcessor.isChunkSupported(chunk);
            }

            if (lastParentSupported == false) {
                retVal[1] += 1;
            }
        }

        return retVal;
    }

    public final void parse(InputStream is) throws Exception {

        BufferedInputStream bis = null;

        char[] chunkTypeChars = new char[4];
        int[] chunkTypeInts = new int[4];

        try {

            /*
              Open tape image file
             */
            bis = new BufferedInputStream(is);

            boolean isFujiRead = false;
            TapeImageChunk lastBaudOrTrChunk = null;
            TapeImageChunk lastPWMSChunk = null;

            READLOOP:
            while (true) {

                /*Read next chunk while checking for EOF*/
                for (int i = 0; i < 4; i++) {

                    chunkTypeInts[i] = bis.read();

                    if (chunkTypeInts[i] == -1) {
                        if (i == 0) {
                            break READLOOP;
                        } else {
                            throw new FileFormatException("Trailing bytes in tape image");
                        }
                    }
                }

                /*Determine chunk type*/
                for (int i = 0; i < 4; i++) {
                    chunkTypeChars[i] = (char) chunkTypeInts[i];
                }
                String chunkType = new String(chunkTypeChars);


                /*Create new instance of appropriate chunks*/

                /*FUJI chunk*/
                if (chunkType.equals("FUJI")) {
                    FujiChunk fujiChunk = new FujiChunk();
                    fujiChunk.readFromStream(bis);
                    chunkList.add(fujiChunk);
                    isFujiRead = true;
                    continue;
                }

                /*Other chunks only after FUJI chunk*/
                if (isFujiRead == false) {
                    throw new FileFormatException("FUJI chunk not found in tape image");
                }

                /*Baud chunk*/
                if (chunkType.equals("baud")) {
                    BaudChunk baudChunk = new BaudChunk();
                    baudChunk.readFromStream(bis);
                    chunkList.add(baudChunk);
                    lastBaudOrTrChunk = baudChunk;
                    continue;

                }

                if (chunkType.equals("fsk ")) {
                    FSKChunk fskChunk = new FSKChunk();
                    fskChunk.readFromStream(bis);
                    chunkList.add(fskChunk);
                    continue;
                }

                /*Data chunk*/
                if (chunkType.equals("data")) {

                    TapeImageChunk parent;

                    /*If there is no preceding chunk, then we must create virtual parent*/
                    if (lastBaudOrTrChunk == null) {
                        parent = new BaudChunk(600);
                    } else {
                        parent = lastBaudOrTrChunk;
                    }

                    DataChunk dataChunk = new DataChunk(parent);
                    dataChunk.readFromStream(bis);
                    chunkList.add(dataChunk);
                    continue;
                }

                if (chunkType.equals("pwms")) {
                    PWMChunk pwmChunk = new PWMChunk(chunkType, null);
                    pwmChunk.readFromStream(bis);
                    lastPWMSChunk = pwmChunk;
                    chunkList.add(pwmChunk);
                    continue;
                }

                if (chunkType.equals("pwmc") || chunkType.equals("pwmd") || chunkType.equals("pwml")) {

                    TapeImageChunk parent = lastPWMSChunk;

                    if (parent == null) {
                        parent = PWMChunk.createDummyPWMS(44_100);
                    }

                    PWMChunk pwmChunk = new PWMChunk(chunkType, parent);
                    pwmChunk.readFromStream(bis);
                    chunkList.add(pwmChunk);
                    continue;
                }


                /*Other chunks - they must be considered independent chunks*/
                UnknownChunk unknownChunk = new UnknownChunk(chunkType);
                unknownChunk.readFromStream(bis);
                chunkList.add(unknownChunk);
                lastBaudOrTrChunk = unknownChunk;

            }
        } finally {

            /*
              Attempt to close tape image file
             */
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException closeException) {
                closeException.printStackTrace();
            }

        }
    }

    /**
     *
     * @return
     */
    public final int getChunkCount() {
        return chunkList.size();
    }

    /**
     *
     * @param index
     * @return
     */
    public final TapeImageChunk getChunkAt(int index) {
        return chunkList.get(index);
    }

    /**
     *
     * @return
     */
    public final String[] getListing() {

        int l = getChunkCount();
        String[] listing = new String[l];

        for (int i = 0; i < l; i++) {
            listing[i] = getChunkAt(i).toString();
        }

        return listing;
    }
}
