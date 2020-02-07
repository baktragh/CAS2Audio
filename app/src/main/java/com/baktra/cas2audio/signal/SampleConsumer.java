package com.baktra.cas2audio.signal;


public interface SampleConsumer {

    void consumeSamples(byte[] b) throws Exception;
}
