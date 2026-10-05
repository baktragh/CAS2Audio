package com.baktra.cas2audio.tapeimage;

import com.baktra.cas2audio.tapeimagefile.TapeImageChunk;

public class ResumePoint {

    private int index;

    public int getResumeIp() {
        return this.resumeIp;
    }

    private int resumeIp;
    private TapeImageChunk chunk;

    public ResumePoint(int index, int ip, TapeImageChunk chunk ) {
        this.index=index;
        this.resumeIp =ip;
        this.chunk=chunk;
    }

    public String toString() {
        return String.format("%04d: %s, %04d ",index,chunk.toString(),resumeIp);
    }

    public String toUIString() {
        return String.format("%04d: %s",index,chunk.toString());
    }


    public long getIndex() {
        return index;
    }
}
