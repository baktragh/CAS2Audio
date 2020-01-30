package com.baktra.cas2audio;


public interface Double2Sample {

    /**
     * Convert double values to samples
     *
     * @param samples Samples
     * @param srcIndex Source index
     * @param doubleSamples Double values
     */
    public void double2Sample(byte[] samples, int srcIndex, double[] doubleSamples);

}
