/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.baktra.cas2audio.tapeimage;

import com.baktra.cas2audio.FileFormatException;

import java.io.InputStream;


/**
 * FSK Chunk
 *
 */
public class FSKChunk implements TapeImageChunk {

    /**
     * Chunk type
     */
    private static final String type = "fsk ";
    /**
     * Chunk length
     */
    private int length;
    /**
     * Chunk data
     */
    private int[] data;
    /**
     * Auxiliary value - IRG length
     */
    private int aux;

    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int[] getData() {
        return data;
    }

    @Override
    public boolean isGeneratedUsingParent() {
        return false;
    }

    @Override
    public TapeImageChunk getParent() {
        return null;
    }

    /**
     *
     * @param s
     * @throws Exception
     */
    @Override
    public void readFromStream(InputStream s) throws Exception {
        int lengthLo = s.read();
        int lengthHi = s.read();

        int auxLo = s.read();
        int auxHi = s.read();

        if (lengthLo == -1 || lengthHi == -1 || auxLo == -1 || auxHi == -1) {
            throw new FileFormatException("Truncated fsk chunk header");
        }

        length = lengthLo + 256 * lengthHi;
        aux = auxLo + 256 * auxHi;

        data = new int[length];
        for (int i = 0; i < length; i++) {
            data[i] = s.read();
            if (data[i] == -1) {
                throw new FileFormatException("Truncated fsk chunk data");
            }
        }

    }

    /**
     *
     * @param s
     * @throws Exception
     */
    @Override
    public void writeToStream(java.io.DataOutputStream s) throws Exception {
        s.writeBytes("fsk ");
        s.write(length % 256);
        s.write(length / 256);
        s.write(aux % 256);
        s.write(aux / 256);
        for (int i = 0; i < data.length; i++) {
            s.write(data[i]);
        }
    }

    /**
     *
     * @return
     */
    public int getAux() {
        return aux;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "fsk: [" + Integer.toString(aux) + " ms] (" + Integer.toString(length) + ")";
    }

    /**
     *
     * @param newAuxValue
     */
    @Override
    public void setAux(int newAuxValue) {
        aux = newAuxValue;
    }

}
