package com.baktra.cas2audio.signal;


import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Buffered audio signal writer This signal writer uses internal buffer to
 * reduce number of round-trips between TS and operating system's native sound
 * facilities.
 */
public class AudioSignalBufferedWriter implements SignalWriter {

    private final byte[] internalBuffer;
    private int internalBufferAvail;
    private int internalBufferPos;
    
    protected final int terminalSilence;
    protected final int sampleRate;
    protected final int numChannels;
    protected final int bitsPerSample;

    /**
     *
     */
    protected AudioTrack track;

    /**
     *
     */
    protected final AudioFormat audioFormat;

    /**
     *
     */
    protected final int bufferSize;

    /**
     *
     */
    protected int terminationSignalCounter;

    /**
     *
     */
    protected byte[] terminationSignal;

    /**
     *
     */
    protected final int bitShift;

    /**
     *
     */
    protected long numSamples;

    /**
     *
     * @param bitsPerSample
     * @param channels
     * @param signed
     * @param bufferSize
     * @param sampleRate
     * @param terminalSilence
     */
    public AudioSignalBufferedWriter(int bitsPerSample, int channels, boolean signed, int bufferSize,int sampleRate,int terminalSilence) {
        this.bufferSize = bufferSize;

        AudioFormat.Builder afb = new AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(bitsPerSample==16?AudioFormat.ENCODING_PCM_16BIT:AudioFormat.ENCODING_PCM_8BIT);
        if (channels==1) {
            afb.setChannelMask(AudioFormat.CHANNEL_OUT_MONO);
        }
        else {
            afb.setChannelMask(AudioFormat.CHANNEL_OUT_FRONT_LEFT+AudioFormat.CHANNEL_OUT_FRONT_RIGHT);
        }
        audioFormat = afb.build();
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

        internalBuffer = new byte[bufferSize];
        internalBufferAvail = bufferSize;
        internalBufferPos = 0;
    }

    @Override
    public void prepare() throws Exception {

        AudioTrack.Builder builder = new AudioTrack.Builder();
        builder=builder.setAudioFormat(audioFormat);

        track = new AudioTrack(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
                audioFormat,bufferSize,AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
        track.play();
    }

    @Override
    public void write(byte[] signal) throws Exception {

        int remainingBytes = signal.length;
        int inputPos = 0;

        while (remainingBytes > 0) {

            /*How many bytes will fit into buffer*/
            int bytesToCopy = Math.min(internalBufferAvail, remainingBytes);

            /*Copy whatever fits into the buffer*/
            System.arraycopy(signal, inputPos, internalBuffer, internalBufferPos, bytesToCopy);
            inputPos += bytesToCopy;
            remainingBytes -= bytesToCopy;
            internalBufferPos += bytesToCopy;
            internalBufferAvail -= bytesToCopy;

            /*If buffer is full, then flush and reset buffer*/
            if (internalBufferAvail == 0) {
                track.write(internalBuffer, 0, internalBuffer.length);
                internalBufferPos = 0;
                internalBufferAvail = internalBuffer.length;
            }

        }

        numSamples += (signal.length >> bitShift);

    }

    @Override
    public void writeInitialSignal(byte[] signal) throws Exception {
        write(signal);
    }

    @Override
    public void flush() throws Exception {
        track.write(internalBuffer, 0, internalBufferPos);
        internalBufferPos = 0;
        internalBufferAvail = internalBuffer.length;
        //track.drain();
    }

    @Override
    public void prepareForClose() throws Exception {
        if (internalBufferPos != 0) {
            track.write(internalBuffer, 0, internalBuffer.length - internalBufferAvail);
        }
        track.stop();
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
        track.stop();
    }

}
