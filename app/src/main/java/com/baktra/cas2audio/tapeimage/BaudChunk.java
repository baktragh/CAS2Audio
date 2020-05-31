package com.baktra.cas2audio.tapeimage;

import com.baktra.cas2audio.FileFormatException;

import java.io.InputStream;

/**
 * baud tape image chunk
 */
public class BaudChunk implements TapeImageChunk {

    /**
     * Chunk type
     */
    private static final String type = "baud";
    /**
     * Chunk length
     */
    private int length;
    /**
     * Baud rate
     */
    private int baudRate;
    /**
     * Data
     */
    private int[] data;

    /**
     *
     */
    public BaudChunk() {

    }

    /**
     *
     * @param baudRate
     */
    public BaudChunk(int baudRate) {
        length = 0;
        this.baudRate = baudRate;
        data = new int[0];
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
     * @return
     */
    public int getBaudRate() {
        return baudRate;
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
        int baudLo = s.read();
        int baudHi = s.read();

        if (lengthLo == -1 || lengthHi == -1 || baudLo == -1 || baudHi == -1) {
            throw new FileFormatException("Truncated baud chunk header");
        }

        length = lengthLo + (256 * lengthHi);
        baudRate = baudLo + (256 * baudHi);

        /*Read data*/
        data = new int[length];
        for (int i = 0; i < length; i++) {
            int b = s.read();
            if (b == -1) {
                throw new FileFormatException("Truncated baud chunk data");
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
        s.writeBytes("baud");
        s.write(length % 256);
        s.write(length / 256);
        s.write(baudRate % 256);
        s.write(baudRate / 256);
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
        return "baud: [" + baudRate + " bd] (" + length + ")";
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
        baudRate = newAuxValue;
    }

}
