package com.baktra.cas2audio;

import java.util.ArrayList;

public class ConversionCrate {
    int sampleRate;
    int[] instructions;
    ArrayList<ResumePoint> resumePoints;
    ConversionCrate() {
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
}
