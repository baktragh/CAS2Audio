package com.baktra.cas2audio.features.player.domain.signal;


interface SampleConsumer {

    void consumeSamples(byte[] b,int dataIndex) throws Exception;
}
