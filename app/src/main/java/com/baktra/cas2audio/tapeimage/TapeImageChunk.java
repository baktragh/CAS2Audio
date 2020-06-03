package com.baktra.cas2audio.tapeimage;

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
    String getType();

    /**
     * Get length of chunk
     *
     * @return Length of chunk in bytes
     */
    int getLength();

    /**
     * Get chunk data
     *
     * @return Chunk data
     */
    int[] getData();

    /**
     * Returns true if parent chunk is used to generate turbo
     *
     * @return true if the parent chunk is used to generate turbo
     */
    boolean isGeneratedUsingParent();

    /**
     * Returns parent chunk or null when the chunk is not dependent
     *
     * @return Parent chunk or null when the chunk is not dependent
     */
    TapeImageChunk getParent();

    /**
     *
     * @param s
     * @throws Exception
     */
    void readFromStream(InputStream s) throws Exception;

    /**
     *
     * @param s
     * @throws Exception
     */
    void writeToStream(java.io.DataOutputStream s) throws Exception;

    /**
     *
     * @param newAuxValue
     */
    void setAux(int newAuxValue);
}
