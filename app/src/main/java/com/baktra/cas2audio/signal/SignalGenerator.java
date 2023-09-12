package com.baktra.cas2audio.signal;

import com.baktra.cas2audio.CasTask;

/**
 * Electric signal generator
 */
public class SignalGenerator implements SampleConsumer {

    /*Instruction constants*/

    private static final int INSTR_NARROW = 0;
    private static final int INSTR_WIDE = 1;
    private static final int INSTR_PILOT = 2;
    private static final int INSTR_DATA = 3;
    private static final int INSTR_PILOTTONE = 4;
    public static final int INSTR_END = 7;

    private static final int INSTR_SYNC = 9;
    private static final int INSTR_SILENCE = 10;
    private static final int INSTR_SETUP = 11;
    private static final int INSTR_BLOCKSEP = 12;
    private static final int INSTR_STOP = 13;
    public static final int INSTR_PWMS = 14;
    public static final int INSTR_PWMC = 15;
    public static final int INSTR_PWMD = 16;
    public static final int INSTR_PWML = 17;
    public static final int INSTR_BAUD = 18;
    public static final int INSTR_STDDATA = 19;
    public static final int INSTR_FSK = 20;
    public static final int INSTR_FUJI = 21;
    private static final int INSTR_PAUSE = 22;
    private static final int INSTR_NOP = 99;

    /*Flag constants*/
    public static final int FLAG_POLARITY_10 = 1;
    public static final int FLAG_POLARITY_01 = 0;
    public static final int FLAG_ORDER_LH = 0;
    public static final int FLAG_ORDER_HL = 1;
    private boolean cInvertPolarity;

    public static class SignalGeneratorConfig {
        public int numChannels;
        public int bitsPerSample;
        public boolean signedSamples;
        public boolean rightChannelOnly;
        public int waveForm;
        public int bufferSize;
        public int amplitude;
        public int sampleRate;
        public String postProcessingString;
        public boolean doNotModulateStandard;
        public int terminalSilence;
        public int initialSilence;

        public boolean invertPolarity;
    }


    /*Postprocessing enabled flag*/
    private final SignalGeneratorConfig asConfig;

    /*Total number of instructions*/
    private final int genLength;

    private int ip;
    /*Current instruction*/

    private final int[] mem;
    /*Memory*/

    /**
     * Wide pulse
     */
    private byte[] WIDE_PULSE;
    /**
     * Short pulse
     */
    private byte[] NARROW_PULSE;
    /**
     * Pilot tone pulse
     */
    private byte[] PILOTTONE_PULSE;
    /**
     * Sync pulse
     */
    private byte[] SYNC_PULSE;
    /**
     * Silence 0.1 seconds
     */
    private byte[] SILENCE_SHORT;
    /**
     * Block separator
     */
    private byte[] BLOCKSEP;
    /**
     * Stop pulse
     */
    private byte[] STOP_PULSE;

    /*Auxiliary samples*/
    private byte[] LOW_SAMPLE;
    private byte[] HIGH_SAMPLE;
    private byte[] SILENCE_SAMPLE;

    /*Order of bits*/
    private boolean loHiOrder = false;

    /*Pwm instructions*/
    private int pwmPolarity;
    private boolean pwmLoHiOrder;
    private int pwmSampleRate;

    /*FSK related*/
    private FSKGenerator fskGenerator;

    /*Configuration copy*/
    private boolean cSigned;
    private int cBits;
    private int cSampleRate;
    private int cPulseVolume;
    private int cChannels;
    private boolean cSignalInRightChannelOnly;
    private int cBufferSize;
    private int cInitialSilence;
    private int cTerminalSilence;

    private int cHarmonic;

    /**
     * Signal writer
     */
    private SignalWriter signalWriter;
    private final CasTask parentTask;



    /**
     * Create new SignalGenerator
     *
     * @param dta Generator instructions
     * @param asc
     */
    public SignalGenerator(int[] dta, SignalGeneratorConfig asc, CasTask parentTask) {
        mem = dta;
        genLength = mem.length;
        asConfig = asc;
        this.parentTask = parentTask;
    }


    private void copyConfiguration() {

        cBits = asConfig.bitsPerSample;
        cChannels = asConfig.numChannels;
        cPulseVolume = asConfig.amplitude;
        cSignalInRightChannelOnly = asConfig.rightChannelOnly;
        cHarmonic = asConfig.waveForm;
        cInitialSilence = asConfig.initialSilence;
        cTerminalSilence = asConfig.terminalSilence;
        cSampleRate = asConfig.sampleRate;
        cInvertPolarity = asConfig.invertPolarity;

        cSigned = asConfig.signedSamples;

        /*Correct the buffer size to at least sample rate*/
        if (cBufferSize < cSampleRate) {
            cBufferSize = cSampleRate;
        }

        /*Correct the buffer size to be a multiple of the sample rate*/
        if ((cBufferSize % cSampleRate) != 0) {
            cBufferSize = ((cBufferSize / cSampleRate) + 1) * cSampleRate;
        }

    }


    /**
     * Prepare to generate signal
     *
     * @throws java.lang.Exception when preparing for generation goes wrong
     */
    private void prepare() throws Exception {

        /*Copy configuration*/
        copyConfiguration();


        signalWriter = new AudioSignalBufferedWriter(cBits, cChannels, cBufferSize, cSampleRate, cTerminalSilence);
        /*Prepare writer*/
        signalWriter.prepare();

        /*Create silence*/
        SILENCE_SHORT = PulseCreator.createPulse(cChannels, cPulseVolume, cSampleRate / 10, cBits, cSigned, 1, 1, cSignalInRightChannelOnly, 0);
        BLOCKSEP = PulseCreator.createPulse(cChannels, cPulseVolume, cSampleRate / 44, cBits, cSigned, 1, 1, cSignalInRightChannelOnly, 0);

        /*Reasonable default pieces of signal*/
        if (!cInvertPolarity) {
            LOW_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_LOW, SignalGenerator.FLAG_POLARITY_01, cSignalInRightChannelOnly, 0);
            HIGH_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_HIGH, SignalGenerator.FLAG_POLARITY_01, cSignalInRightChannelOnly, 0);
            SILENCE_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_SILENCE, SignalGenerator.FLAG_POLARITY_01, cSignalInRightChannelOnly, 0);
        }
        else {
            LOW_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_HIGH, SignalGenerator.FLAG_POLARITY_10, cSignalInRightChannelOnly, 0);
            HIGH_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_LOW, SignalGenerator.FLAG_POLARITY_10, cSignalInRightChannelOnly, 0);
            SILENCE_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_SILENCE, SignalGenerator.FLAG_POLARITY_10, cSignalInRightChannelOnly, 0);
        }

    }

    /**
     * Generate the signal
     *
     * @throws Exception When anything fails
     */
    public final void run() throws Exception {


        /*Prepare for output*/
        prepare();


        /*Start writing signal*/

        /*Initial signal*/
        for (int k = 0; k < cInitialSilence; k++) {
                signalWriter.writeInitialSignal(SILENCE_SHORT);
            }

            int lastIp=0;
            ip = 0;
        /*Loop counter*/
        int op = INSTR_NOP;

            while (op != SignalGenerator.INSTR_END && !parentTask.isCancelled()) {

                op = mem[ip];

                /*Show progress every 2nd instruction*/
                if (ip-lastIp>=2) {
                    parentTask.setProgress(getStatusPercent());
                    lastIp=ip;
                }

                /*Execution of instructions*/
                /*Index of current instruction*/
                int cx;
                switch (op) {

                    /*Narrow pulse*/
                    case SignalGenerator.INSTR_NARROW: {
                        generateNarrow();
                        ip++;
                        break;
                    }
                    /*Wide pulse*/
                    case SignalGenerator.INSTR_WIDE: {
                        generateWide();
                        ip++;
                        break;
                    }
                    /*Pilot tone pulse*/
                    case SignalGenerator.INSTR_PILOT: {
                        generatePilotTone(1);
                        ip++;
                        break;
                    }
                    /*Sync pulse*/
                    case SignalGenerator.INSTR_SYNC: {
                        generateSync();
                        ip++;
                        break;
                    }
                    /*Data*/
                    case SignalGenerator.INSTR_DATA: {
                        ip++;
                        cx = mem[ip];
                        ip++;

                        for (int i = 0; i < cx && !parentTask.isCancelled(); i++) {
                            generateByte(mem[ip]);
                            ip++;
                        }
                        break;

                    }
                    /*Pilot tone*/
                    case SignalGenerator.INSTR_PILOTTONE: {
                        ip++;
                        cx = mem[ip];
                        generatePilotTone(cx);
                        ip++;
                        break;
                    }

                    /*End*/
                    case SignalGenerator.INSTR_END: {
                        break;
                    }


                    /*Silence in tenths of seconds, or pause when negative */
                    case SignalGenerator.INSTR_SILENCE: {
                        ip++;
                        int silence = mem[ip];

                        if (silence < 0) {
                            silence = -mem[ip];

                        }
                        generateSilence(silence);
                        ip++;
                        break;
                    }

                    case SignalGenerator.INSTR_PAUSE: {
                        ip++;
                        break;
                    }

                    /*Setup*/
                    case SignalGenerator.INSTR_SETUP: {
                        handleSetup();
                        ip += 9;
                        break;
                    }

                    /*Block separator*/
                    case SignalGenerator.INSTR_BLOCKSEP: {
                        generateBlockSep();
                        ip++;
                        break;
                    }

                    /*Stop pulse*/
                    case SignalGenerator.INSTR_STOP: {
                        generateStop();
                        ip++;
                        break;
                    }

                    /*PWMS*/
                    case SignalGenerator.INSTR_PWMS: {
                        ip++;
                        pwmPolarity = mem[ip];
                        ip++;
                        pwmLoHiOrder = (mem[ip] != SignalGenerator.FLAG_ORDER_HL);
                        ip++;
                        pwmSampleRate = mem[ip];
                        ip++;

                        /*Create levels*/
                        LOW_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_LOW, pwmPolarity, cSignalInRightChannelOnly, 0);
                        HIGH_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_HIGH, pwmPolarity, cSignalInRightChannelOnly, 0);
                        SILENCE_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_SILENCE, pwmPolarity, cSignalInRightChannelOnly, 0);
                        break;
                    }

                    /*PWMC*/
                    case SignalGenerator.INSTR_PWMC: {
                        ip++;
                        int silence = getPWMMillis2Samples(mem[ip]);
                        ip++;
                        for (int i = 0; i < silence; i++) {
                            signalWriter.write(SILENCE_SAMPLE);
                        }

                        cx = mem[ip];
                        /*Number of pairs*/
                        ip++;
                        for (int i = 0; i < cx; i++) {
                            byte[] pulse = PulseCreator.createPulse(cChannels, cPulseVolume, getPWMLength(mem[ip]), cBits, cSigned, 0, pwmPolarity, cSignalInRightChannelOnly, cHarmonic);
                            ip++;
                            for (int j = 0; j < mem[ip]; j++) {
                                signalWriter.write(pulse);
                            }
                            ip++;
                        }
                        break;
                    }

                    case SignalGenerator.INSTR_PWMD: {
                        ip++;
                        cx = mem[ip];
                        /*Bytes of data*/

                        /*Create pulses*/
                        ip++;
                        NARROW_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, getPWMLength(mem[ip]), cBits, cSigned, 0, pwmPolarity, cSignalInRightChannelOnly, cHarmonic);
                        ip++;
                        WIDE_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, getPWMLength(mem[ip]), cBits, cSigned, 0, pwmPolarity, cSignalInRightChannelOnly, cHarmonic);
                        ip++;
                        /*Set order*/
                        loHiOrder = pwmLoHiOrder;

                        /*Generate data itself*/
                        for (int i = 0; i < cx && !parentTask.isCancelled(); i++) {
                            generateByte(mem[ip]);
                            ip++;
                        }

                        break;
                    }

                    case SignalGenerator.INSTR_PWML: {
                        ip++;
                        /*Silence*/
                        int silence = getPWMMillis2Samples(mem[ip]);
                        ip++;
                        for (int i = 0; i < silence; i++) {
                            signalWriter.write(SILENCE_SAMPLE);
                        }
                        /*Number of lengths*/
                        cx = mem[ip];
                        ip++;

                        int sampleCount;
                        boolean high;
                        high = pwmPolarity == SignalGenerator.FLAG_ORDER_HL;

                        /*States*/
                        for (int i = 0; i < cx; i++) {
                            sampleCount = getPWMLength(mem[ip]);
                            ip++;
                            for (int j = 0; j < sampleCount; j++) {
                                signalWriter.write(high == true ? HIGH_SAMPLE : LOW_SAMPLE);
                            }
                            high = !high;
                        }

                        break;
                    }
                    case SignalGenerator.INSTR_BAUD: {
                        generateBAUD();
                        break;
                    }
                    case SignalGenerator.INSTR_STDDATA: {
                        generateSTDDATA();
                        break;
                    }
                    case SignalGenerator.INSTR_FSK: {
                        generateFSK();
                        break;
                    }
                    case SignalGenerator.INSTR_FUJI: {
                        /*Skip opcode*/
                        ip++;
                        /*Get length*/
                        cx = mem[ip];
                        /*Skip data*/
                        ip++;
                        ip += cx;
                        break;
                    }

                }/*End of switch*/

            }


            /*Generator canceled*/
            if (parentTask.isCancelled()) {
                signalWriter.prepareForClose();
                signalWriter.close();
            }
            /*Generator ended normally*/
            else {
                signalWriter.prepareForTerminationSignal(SILENCE_SHORT);
                signalWriter.prepareForClose();
                signalWriter.close();
            }

        /*Everything OK*/
    }

    /**
     * Generate pilot tone
     *
     * @param num Number of pilot tone pulses
     * @throws Exception When pilot tone generation fails
     */
    private void generatePilotTone(int num) throws Exception {

        for (int i = 0; i < num && !parentTask.isCancelled(); i++) {
            signalWriter.write(PILOTTONE_PULSE);
        }

    }

    /**
     * Generate sync pulse
     */
    private void generateSync() throws Exception {
        signalWriter.write(SYNC_PULSE);
    }

    /**
     * Generate silence
     *
     * @param tenths Number of 0.1 second ticks
     * @throws Exception
     */
    private void generateSilence(int tenths) throws Exception {
        for (int p = 0; p < tenths && !parentTask.isCancelled(); p++) {
            signalWriter.write(SILENCE_SHORT);
        }
    }

    /**
     * Generate block separator
     */
    private void generateBlockSep() throws Exception {
        signalWriter.write(BLOCKSEP);
    }

    /**
     * Generate stop pulse
     */
    private void generateStop() throws Exception {
        signalWriter.write(STOP_PULSE);
    }

    /**
     * Generates 0 or 1
     */
    private void generateWide() throws Exception {
        signalWriter.write(WIDE_PULSE);
    }

    private void generateNarrow() throws Exception {
        signalWriter.write(NARROW_PULSE);
    }

    /**
     * Generate byte
     *
     * @param i Byte to be generated
     * @throws Exception
     */
    private void generateByte(int i) throws Exception {

        /*Bit order from highest to lowest*/
        if (loHiOrder == false) {

            for (int k = 0; k < 8; k++) {
                if ((i & 0x0000_0080) == 0) {
                    signalWriter.write(NARROW_PULSE);
                } else {
                    signalWriter.write(WIDE_PULSE);
                }

                /*Left shift*/
                i <<= 1;
            }
        } /*Bit order from lowest to highest*/ else {
            for (int k = 0; k < 8; k++) {
                if ((i & 0x0000_0001) == 0) {
                    signalWriter.write(NARROW_PULSE);
                } else {
                    signalWriter.write(WIDE_PULSE);
                }

                /*Shift to right*/
                i >>= 1;
            }
        }
    }

    /**
     * Handle setup instruction
     */
    private void handleSetup() {

        int dab = cBits;
        boolean das = cSigned;

        int tmp = ip + 1;

        /*Polarity and byte order*/
        int polarity = mem[tmp + 5];
        loHiOrder = mem[tmp + 6] != SignalGenerator.FLAG_ORDER_HL;

        if (cInvertPolarity) {
            if (polarity==SignalGenerator.FLAG_POLARITY_01) {
                polarity=SignalGenerator.FLAG_POLARITY_10;
            }
            if (polarity==SignalGenerator.FLAG_POLARITY_01) {
                polarity=SignalGenerator.FLAG_POLARITY_10;
            }
        }

        /*Create new pulses for the settings*/
        PILOTTONE_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, mem[tmp + 0], dab, das, 0, polarity, cSignalInRightChannelOnly, cHarmonic);
        WIDE_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, mem[tmp + 1], dab, das, 0, polarity, cSignalInRightChannelOnly, cHarmonic);
        NARROW_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, mem[tmp + 2], dab, das, 0, polarity, cSignalInRightChannelOnly, cHarmonic);
        SYNC_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, mem[tmp + 3], dab, das, 0, polarity, cSignalInRightChannelOnly, cHarmonic);
        STOP_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, mem[tmp + 4], dab, das, 0, polarity, cSignalInRightChannelOnly, cHarmonic);

    }

    private int getStatusPercent() {
        float d;
        d = ip;
        d /= genLength;
        d *= 100;
        if (d > 100) {
            d = 100.0f;
        }
        return Math.round(d);
    }


    private int getPWMLength(int i) {
        return (int) Math.round(i * (double) cSampleRate / pwmSampleRate);
    }

    private int getPWMMillis2Samples(int i) {
        return (int) Math.round(i * (((double) cSampleRate) / 1000));
    }

    /**
     * Generate BAUD instruction 00 OPCODE 01 BAUDRATE
     */
    private void generateBAUD() {
        ip++;
        fskGenerator = new FSKGenerator(mem[ip], cSigned, cChannels, cBits, this, cPulseVolume, cSignalInRightChannelOnly, cSampleRate);
        ip++;
    }

    /**
     * Generate STDDATA instruction 00 OPCODE 01 IRG LENGTH 02 DATA LENGTH 03
     * ... DATA
     */
    private void generateSTDDATA() throws Exception {
        ip++;

        /*Get IRG length*/
        int irgLen = mem[ip];
        ip++;
        /*Ged data length*/
        int dataLen = mem[ip];
        ip++;

        /*Prepare data for fsk generator*/
        int[] data = new int[dataLen];
        System.arraycopy(mem, ip, data, 0, dataLen);

        /*Increase instruction pointer*/
        ip += dataLen;

        int numPieces = irgLen / 2_000;
        int remainder = irgLen % 2_000;

        /*Generate IRG in pieces of 2 seconds*/
        for (int i = 0; i < numPieces; i++) {
            fskGenerator.generateIRG(2_000);
            if (parentTask.isCancelled()) {
                return;
            }
        }
        /*Remainder of IRG*/
        fskGenerator.generateIRG(remainder);
        if (parentTask.isCancelled()) {
            return;
        }

        /*Generate data*/
        fskGenerator.generateData(data);

    }

    /**
     * Generate FSK instruction 00 IRG 01 LENGTH 02 ... DATA (0 and 1 durations
     * in 0.1 milliseconds)
     *
     * @throws Exception when FSK generation fails
     */
    private void generateFSK() throws Exception {
        ip++;
        int irgLen = mem[ip];
        ip++;
        int dataLen = mem[ip];
        ip++;

        /*To be on the safe side*/
        if (fskGenerator == null) {

            fskGenerator = new FSKGenerator(600, cSigned, cChannels, cBits, this, cPulseVolume, cSignalInRightChannelOnly, cSampleRate);

        }
        fskGenerator.resetAngle();
        fskGenerator.generateIRG(irgLen);
        fskGenerator.generateFSK(mem, ip, dataLen);
        ip += dataLen;
    }

    @Override
    public final void consumeSamples(byte[] b) throws Exception {
        signalWriter.write(b);
    }


}
