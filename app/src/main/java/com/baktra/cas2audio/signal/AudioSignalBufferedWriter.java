package com.baktra.cas2audio.signal;


/*import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;*/
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
    
    private final int terminalSilence;
    private final int sampleRate;
    private final int numChannels;
    private final int bitsPerSample;

    /**
     *
     */
    private AudioTrack track;

    /*private final AudioFormat audioFormat;*/

    /**
     *
     */
    private final int bufferSize;

    /**
     *
     */
    private int terminationSignalCounter;

    /**
     *
     */
    private byte[] terminationSignal;

    /**
     *
     */
    private final int bitShift;

    /**
     *
     */
    private long numSamples;

    /**
     *  @param bitsPerSample
     * @param channels
     * @param bufferSize
     * @param sampleRate
     * @param terminalSilence
     */
    public AudioSignalBufferedWriter(int bitsPerSample, int channels, int bufferSize, int sampleRate, int terminalSilence) {
        this.bufferSize = bufferSize;

        /* Audio format - new style*/
        /*audioFormat = getAudioFormat(sampleRate,bitsPerSample.channels);*/


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
    public final void prepare() throws Exception {

        track = getOldStyleAudioTrack();
        track.play();
    }

    private AudioTrack getOldStyleAudioTrack() {

        AudioTrack oldTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                numChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                bitsPerSample == 8 ? AudioFormat.ENCODING_PCM_8BIT : AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
        );

        return oldTrack;

    }

    /*private AudioTrack getNewStyleAudioTrack() {

        AudioTrack.Builder builder = new AudioTrack.Builder();
        builder=builder.setAudioFormat(audioFormat);
        return new AudioTrack(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
                audioFormat, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
    }*/

    /*private AudioFormat getAudioFormat(int sampleRate,int bitsPerChannel,int numChannels) {
        AudioFormat.Builder afb = new AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(bitsPerSample==16?AudioFormat.ENCODING_PCM_16BIT:AudioFormat.ENCODING_PCM_8BIT);
        if (channels==1) {
            afb.setChannelMask(AudioFormat.CHANNEL_OUT_MONO);
        }
        else {
            afb.setChannelMask(AudioFormat.CHANNEL_OUT_FRONT_LEFT+AudioFormat.CHANNEL_OUT_FRONT_RIGHT);
        }
        return afb.build();

    }*/

    @Override
    public final void write(byte[] signal) throws Exception {

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
    public final void writeInitialSignal(byte[] signal) throws Exception {
        write(signal);
    }

    @Override
    public final void flush() throws Exception {
        track.write(internalBuffer, 0, internalBufferPos);
        internalBufferPos = 0;
        internalBufferAvail = internalBuffer.length;
        //track.drain();
    }

    @Override
    public final void prepareForClose() throws Exception {
        if (internalBufferPos != 0) {
            track.write(internalBuffer, 0, internalBuffer.length - internalBufferAvail);
        }
        track.stop();
    }

    @Override
    public final void prepareForTerminationSignal(byte[] signal) {
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
    public final boolean writeTerminationSignal() throws Exception {
        if (terminationSignal == null || terminationSignalCounter < 1) {
            return false;
        }
        write(terminationSignal);
        terminationSignalCounter--;
        return true;
    }

    @Override
    public final long getNumberOfSamples() {
        return numSamples;
    }

    @Override
    public final void close() throws Exception {
        track.stop();
    }

}
