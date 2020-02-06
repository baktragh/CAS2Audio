package com.baktra.cas2audio.signal;

/**
 * FSK generator Supports standard FSK tape system Lookup table based.
 */
public class FSKGenerator {

    private static final int MARK_FREQUENCY = 5_327;
    private static final int SPACE_FREQUENCY = 3_995;
    private final int MARK_DEGREES_PER_SAMPLE; 
    private final int SPACE_DEGREES_PER_SAMPLE; 
    protected final int sampleRate;

    /**
     *
     */
    protected int samplesPerMarkOrSpace;
    private final int byteMultiplier;
    private byte[][] sampleSineTable;

    /**
     *
     */
    protected final SampleConsumer consumer;
    private final Double2Sample f2s;

    /*Sine wave generation*/
    private int angle;

    /**
     * Create new FSK generator
     *
     * @param baudRate Baud rate
     * @param signed Signed samples
     * @param numChannels Number of channels
     * @param bitsPerSample Bits per sample (8 or 16)
     * @param c Consumer of the generated samples
     * @param amplitude Signal amplitude
     * @param rightOnly Signal in right channel only
     */
    public FSKGenerator(int baudRate, boolean signed, int numChannels, int bitsPerSample, SampleConsumer c, int amplitude, boolean rightOnly,int sampleRate) {

        this.consumer = c;
        this.sampleRate=sampleRate;
        
        MARK_DEGREES_PER_SAMPLE = (MARK_FREQUENCY * 3_600) / sampleRate;
        SPACE_DEGREES_PER_SAMPLE = (SPACE_FREQUENCY * 3_600) / sampleRate;

        Double2SampleFactory fact = new Double2SampleFactory(amplitude, rightOnly);
        this.f2s = fact.getDouble2Sample(signed, bitsPerSample, numChannels);

        /*Some calculations*/
        samplesPerMarkOrSpace = sampleRate / baudRate;
        byteMultiplier = numChannels * ((bitsPerSample == 8) ? 1 : 2);
        angle = 0;

        createSineTable();

    }

    /**
     * Generate inter-record gap
     *
     * @param milis Duration in miliseconds
     * @throws Exception
     */
    public void generateIRG(int milis) throws Exception {

        /*Total number of samples for mark tone*/
        int counter = 0;
        int max = milis * sampleRate / 1_000;

        /*Generate marks repeatedly*/
        while (counter < max) {
            generateMarkOrSpace(MARK_DEGREES_PER_SAMPLE);
            counter += samplesPerMarkOrSpace;
        }

    }

    /**
     * Generate block of data
     *
     * @param data Data
     * @throws Exception
     */
    public void generateData(int[] data) throws Exception {

        int dataByte;
        int p;

        for (int i = 0; i < data.length; i++) {

            dataByte = data[i];

            /*Start bit*/
            generateMarkOrSpace(SPACE_DEGREES_PER_SAMPLE);

            /*Data bits, LSB to MSB*/
            int mask = 1;
            for (int k = 0; k < 8; k++) {
                p = dataByte & mask;
                if (p != 0) {
                    generateMarkOrSpace(MARK_DEGREES_PER_SAMPLE);
                } else {
                    generateMarkOrSpace(SPACE_DEGREES_PER_SAMPLE);
                }
                mask <<= 1;
            }

            /*Stop bit*/
            generateMarkOrSpace(MARK_DEGREES_PER_SAMPLE);

        }

    }

    /**
     * Generate mark or space
     *
     * @param degreesPerSample How many degrees per sample
     * @throws Exception
     */
    private void generateMarkOrSpace(int degreesPerSample) throws Exception {

        for (int i = 0; i < samplesPerMarkOrSpace; i++) {
            consumer.consumeSamples(sampleSineTable[angle]);
            angle += degreesPerSample;
            if (angle > 3_599) {
                angle -= 3_600;
            }
        }

    }

    /**
     * Generate 0 and 1
     *
     * @param oneOrZero 0 for space, 1 for mark
     * @param duration Duration in tenths of miliseconds
     */
    void generateFSK(int[] durations, int offset, int length) throws Exception {

        int maxIndex = offset + length;
        int durationInSamples;
        int dps;
        boolean mark = false;

        for (int i = offset; i < maxIndex; i++) {

            durationInSamples = (durations[i] * sampleRate) / 10_000;
            if (mark == true) {
                dps = MARK_DEGREES_PER_SAMPLE;
            } else {
                dps = SPACE_DEGREES_PER_SAMPLE;
            }

            for (int k = 0; k < durationInSamples; k++) {
                consumer.consumeSamples(sampleSineTable[angle]);
                angle += dps;
                if (angle > 3_599) {
                    angle -= 3_600;
                }
            }
            mark = !mark;
        }
    }

    void resetAngle() {
        angle = 0;
    }

    /**
     * Precalculate lookup table
     */
    private void createSineTable() {

        /*Allocate table*/
        sampleSineTable = new byte[3_600][byteMultiplier];
        double[] d = new double[1];

        /*Calculate values for 3600 degrees*/
        double step = Math.PI * 2 / 3600.0d;
        double currentAngle = 0.0d;

        for (int i = 0; i < 3_600; i++) {
            d[0] = Math.sin(currentAngle);
            currentAngle += step;
            f2s.double2Sample(sampleSineTable[i], 0, d);
        }

    }

}
