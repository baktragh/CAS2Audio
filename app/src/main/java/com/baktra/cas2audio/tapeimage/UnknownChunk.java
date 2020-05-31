package com.baktra.cas2audio.tapeimage;

import com.baktra.cas2audio.FileFormatException;

import java.io.InputStream;

/**
 *
 * @author  
 */
public class UnknownChunk implements TapeImageChunk {

    private final String type;
    private int length;
    private int[] data;
    private int aux;

    /**
     *
     * @param type
     */
    public UnknownChunk(String type) {
        this.type = type;
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
    public boolean isGeneratedUsingParent() {
        return false;
    }

    /**
     *
     * @param s
     * @throws Exception
     */
    @Override
    public void readFromStream(InputStream s) throws Exception {

        /*Read length and aux*/
        int lengthLo = s.read();
        int lengthHi = s.read();
        int auxLo = s.read();
        int auxHi = s.read();

        if (lengthLo == -1 || lengthHi == -1 || auxLo == -1 || auxHi == -1) {
            throw new FileFormatException("Truncated " + type + " chunk header");
        }

        length = lengthLo + (256 * lengthHi);
        aux = auxLo + (256 * auxHi);

        /*Read data*/
        data = new int[length];
        for (int i = 0; i < length; i++) {
            int b = s.read();
            if (b == -1) {
                throw new FileFormatException("Truncated " + type + " chunk data");
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
        s.writeBytes(type);
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
        return type + ": [" + aux + "] (" + length + ")";
    }

    @Override
    public TapeImageChunk getParent() {
        return null;
    }

    /**
     *
     * @param newAuxValue
     */
    @Override
    public void setAux(int newAuxValue) {

    }

}
