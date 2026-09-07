package com.baktra.cas2audio;

import com.baktra.cas2audio.tapeimage.TapeImageChunk;

public class ResumePoint {

    int index;
    int resumeIp;
    TapeImageChunk chunk;

    public ResumePoint(int index, int ip, TapeImageChunk chunk ) {
        this.index=index;
        this.resumeIp =ip;
        this.chunk=chunk;
    }

    public String toString() {
        return String.format("%04d: %s, %04d ",index,chunk.toString(),resumeIp);
    }



}
