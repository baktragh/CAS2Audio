package com.baktra.cas2audio.tapeimage;

import com.baktra.cas2audio.tapeimagefile.TapeImageChunk;

import java.util.List;

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


    public int getIndex() {
        return index;
    }

    public static ResumePoint getClosestResumePoint(int ip, List<ResumePoint> resumePoints) {

        /*Can be empty*/
        if (resumePoints == null || resumePoints.isEmpty()) {
            return null;
        }

        /*Begin with a fallback value*/
        int currentResumePointIndex = 0;

        /*Find the closest resume point*/
        for (int i = resumePoints.size() - 1; i >= 0; i--) {
            ResumePoint p = resumePoints.get(i);
            if (p.getResumeIp() <= ip) {
                currentResumePointIndex = i;
                break;
            }
        }

        /*Return the resume point found*/
        return resumePoints.get(currentResumePointIndex);
    }
}
