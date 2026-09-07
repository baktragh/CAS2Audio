package com.baktra.cas2audio.signal;


/*import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;*/
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;



public class DummySignalWriter implements SignalWriter {


    private final int terminalSilence;
    private final int sampleRate;
    private final int numChannels;
    private final int bitsPerSample;
    private int terminationSignalCounter;

    private byte[] terminationSignal;

    private final int bitShift;

    private long numSamples;

    public DummySignalWriter(int bitsPerSample, int channels, int bufferSize, int sampleRate, int terminalSilence) {


        terminationSignalCounter = -1;
        terminationSignal = null;
        numSamples = 0;
        this.terminalSilence=terminalSilence;
        this.bitsPerSample=bitsPerSample;
        this.sampleRate=sampleRate;
        this.numChannels=channels;
        
        int bytesPerSample = (bitsPerSample / 8) * channels;

        switch (bytesPerSample) {
            case 4: {
                bitShift = 2;
                break;
            }
            case 2: {
                bitShift = 1;
                break;
            }
            case 1: {
                bitShift = 0;
                break;
            }
            default: {
                bitShift = 0;
            }
        }

    }

    @Override
    public void prepare() throws Exception {

    }



    @Override
    public void write(byte[] signal) throws Exception {
        numSamples += (signal.length >> bitShift);
    }

    @Override
    public void writeInitialSignal(byte[] signal) throws Exception {
    }

    @Override
    public void flush() throws Exception {
    }

    @Override
    public void prepareForClose() throws Exception {

    }

    @Override
    public void prepareForTerminationSignal(byte[] signal) {
        if (signal == null) {
            return;
        }

        if (signal.length == 0) {
            terminationSignalCounter = 0;
        } else {
            terminationSignalCounter = ((terminalSilence * sampleRate * numChannels * bitsPerSample) / 8) / signal.length;
        }
        terminationSignal = signal;
    }

    @Override
    public boolean writeTerminationSignal() throws Exception {
        if (terminationSignal == null || terminationSignalCounter < 1) {
            return false;
        }
        write(terminationSignal);
        terminationSignalCounter--;
        return true;
    }

    @Override
    public long getNumberOfSamples() {
        return numSamples;
    }

    @Override
    public void close() throws Exception {

    }

}
