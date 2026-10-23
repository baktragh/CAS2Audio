package com.baktra.cas2audio.features.player.domain.signal;


public interface Double2Sample {

    /**
     * Convert double values to samples
     *
     * @param samples Samples
     * @param srcIndex Source index
     * @param doubleSamples Double values
     */
    void double2Sample(byte[] samples, int srcIndex, double[] doubleSamples);

}
