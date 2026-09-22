package com.baktra.cas2audio.signal;


interface SampleConsumer {

    void consumeSamples(byte[] b,int dataIndex) throws Exception;
}
