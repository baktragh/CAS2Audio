package com.baktra.cas2audio.signal;


public interface SampleConsumer {

    public void consumeSamples(byte[] b) throws Exception;
}
