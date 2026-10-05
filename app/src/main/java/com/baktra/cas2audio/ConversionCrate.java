package com.baktra.cas2audio;

import com.baktra.cas2audio.tapeimage.ResumePoint;

import java.util.ArrayList;

public class ConversionCrate {
    private int sampleRate;
    private int[] instructions;
    private ArrayList<ResumePoint> resumePoints;
    public ConversionCrate() {
        resumePoints = new ArrayList<>();
    }

    public void setInstructions(int[] instructions) {
        this.instructions = new int[instructions.length];
        System.arraycopy(instructions,0,this.instructions,0,instructions.length);
    }

    public void addResumePoint(ResumePoint rsp) {
        resumePoints.add(rsp);
    }

    public void listing() {

        System.out.println("Conversion Create");
        System.out.println("Instructions: "+instructions.toString());
        System.out.println("Resume Points: {");

        for (ResumePoint rsp:resumePoints) {
            System.out.println(rsp);
        }

        System.out.println("}");

    }

    public int[] getInstructions() {
        return instructions;
    }

    public int getSampleRate() {
        return this.sampleRate;
    }

    public ArrayList<ResumePoint> getResumePoints() {
        return resumePoints;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate=sampleRate;
    }
}
