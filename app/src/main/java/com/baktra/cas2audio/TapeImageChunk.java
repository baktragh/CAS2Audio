package com.baktra.cas2audio;

import java.io.InputStream;

/**
 * Tape image chunk
 */
public interface TapeImageChunk {

    /**
     * Get type of chunk
     *
     * @return Four character string identifying the chunk
     */
    public String getType();

    /**
     * Get length of chunk
     *
     * @return Length of chunk in bytes
     */
    public int getLength();

    /**
     * Get chunk data
     *
     * @return Chunk data
     */
    public int[] getData();

    /**
     * Returns true if parent chunk is used to generate turbo
     *
     * @return true if the parent chunk is used to generate turbo
     */
    public boolean isGeneretedUsingParent();

    /**
     * Returns parent chunk or null when the chunk is not dependent
     *
     * @return Parent chunk or null when the chunk is not dependent
     */
    public TapeImageChunk getParent();

    /**
     *
     * @param s
     * @throws Exception
     */
    public void readFromStream(InputStream s) throws Exception;

    /**
     *
     * @param s
     * @throws Exception
     */
    public void writeToStream(java.io.DataOutputStream s) throws Exception;

    /**
     *
     * @param newAuxValue
     */
    public void setAux(int newAuxValue);
}
