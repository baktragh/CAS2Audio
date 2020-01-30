package com.baktra.cas2audio;

import java.io.InputStream;


/**
 *
 * @author  
 */
public class DataChunk implements TapeImageChunk {

    /**
     * Chunk type
     */
    private static final String type = "data";
    /**
     * Chunk length
     */
    private int length;
    /**
     * Chunk AUX
     */
    private int aux;
    /**
     * Chunk data
     */
    private int[] data;
    /**
     * Parent chunk
     */
    private final TapeImageChunk parent;

    /**
     *
     * @param parent
     */
    public DataChunk(TapeImageChunk parent) {
        this.parent = parent;
    }

    /**
     *
     * @return
     */
    public int getAux() {
        return aux;
    }

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
    public boolean isGeneretedUsingParent() {
        return true;
    }

    /**
     *
     * @param s
     * @throws Exception
     */
    @Override
    public void readFromStream(InputStream s) throws Exception {

        /*Read length and baud rate*/
        int lengthLo = s.read();
        int lengthHi = s.read();
        int auxLo = s.read();
        int auxHi = s.read();

        if (lengthLo == -1 || lengthHi == -1 || auxLo == -1 || auxHi == -1) {
            throw new FileFormatException("Truncated data chunk header");
        }

        length = lengthLo + (256 * lengthHi);
        aux = auxLo + (256 * auxHi);

        /*Read data*/
        data = new int[length];
        for (int i = 0; i < length; i++) {
            int b = s.read();
            if (b == -1) {
                throw new FileFormatException("Truncated data chunk data");
            }
            data[i] = b;
        }
    }

    /**
     *
     * @param s
     * @throws Exception
     */
    @Override
    public void writeToStream(java.io.DataOutputStream s) throws Exception {
        s.writeBytes("data");
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
    @Override
    public String toString() {
        return "data: [" + Integer.toString(aux) + "] (" + Integer.toString(length) + ")";
    }

    @Override
    public TapeImageChunk getParent() {
        return parent;
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
