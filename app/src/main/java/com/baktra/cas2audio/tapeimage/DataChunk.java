package com.baktra.cas2audio.tapeimage;

import com.baktra.cas2audio.FileFormatException;

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
     * @return
     */
    public final int getAux() {
        return aux;
    }

    @Override
    public final String getType() {
        return type;
    }

    @Override
    public final int getLength() {
        return length;
    }

    @Override
    public final int[] getData() {
        return data;
    }

    @Override
    public final boolean isGeneratedUsingParent() {
        return true;
    }

    /**
     *
     * @param s
     * @throws Exception
     */
    @Override
    public final void readFromStream(InputStream s) throws Exception {

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
    public final void writeToStream(java.io.DataOutputStream s) throws Exception {
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
    public final String toString() {
        return "data: [" + aux + "] (" + length + ")";
    }

    @Override
    public final TapeImageChunk getParent() {
        return parent;
    }

    /**
     *
     * @param newAuxValue
     */
    @Override
    public final void setAux(int newAuxValue) {
        aux = newAuxValue;
    }

}
