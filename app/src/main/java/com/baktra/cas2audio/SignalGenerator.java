package com.baktra.cas2audio;

import java.io.*;

/**
 * Electric signal generator
 */
public class SignalGenerator implements Runnable, SampleConsumer {

    /*Output types*/
    public static final int TYPE_AUDIO = 1;

    /*Cancellation*/
    public static final int CANCEL_NO = 0;
    public static final int CANCEL_YES = 1;

    /*Instruction constants*/

    public static final int INSTR_NARROW = 0;
    public static final int INSTR_WIDE = 1;
    public static final int INSTR_PILOT = 2;
    public static final int INSTR_DATA = 3;
    public static final int INSTR_PILOTTONE = 4;
    public static final int INSTR_END = 7;

    public static final int INSTR_SYNC = 9;
    public static final int INSTR_SILENCE = 10;
    public static final int INSTR_SETUP = 11;
    public static final int INSTR_BLOCKSEP = 12;
    public static final int INSTR_STOP = 13;
    public static final int INSTR_PWMS = 14;
    public static final int INSTR_PWMC = 15;
    public static final int INSTR_PWMD = 16;
    public static final int INSTR_PWML = 17;
    public static final int INSTR_BAUD = 18;
    public static final int INSTR_STDDATA = 19;
    public static final int INSTR_FSK = 20;
    public static final int INSTR_FUJI = 21;
    public static final int INSTR_PAUSE = 22;
    public static final int INSTR_NOP = 99;

    /*Flag constants*/
    public static final int FLAG_POLARITY_10 = 1;
    public static final int FLAG_POLARITY_01 = 0;
    public static final int FLAG_ORDER_LH = 0;
    public static final int FLAG_ORDER_HL = 1;


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
    }


    /*Postprocessing enabled flag*/
    private final boolean wavePostProcessingEnabled;
    private final SignalGeneratorConfig asConfig;

    /*Total number of instructions*/
    private final int genLength;

    private int ip;
    /*Index of current instruction*/
    private int cx;
    /*Loop counter*/
    private int op;
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

    /*Output file*/
    private final String outFile;
    /*Cancel type*/
    private volatile int cancelType;
    /*Postprocessing run flag*/
    private boolean postProcessRunning = false;


    /*Configuration copy*/
    private boolean cSigned;
    private int cBits;
    private int cSampleRate;
    private String cPostProcessingString;
    private int cPulseVolume;
    private int cChannels;
    private boolean cSignalInRightChannelOnly;
    private int cBufferSize;
    private int cInitialSilence;
    private int cTerminalSilence;

    private boolean cDoNotModulateStandardRecords;
    private int cHarmonic;

    /*Error message*/
    private String errorMessage;
    /**
     * Signal writer
     */
    private SignalWriter signalWriter;

    /*Output type, WAVE or AUDIO*/
    private final int outputType;

    /*Pause and repeat*/
    private boolean resumeFromPause;
    private final boolean repeatOutput;

    /*Dummy*/
    private final boolean isDummy;

    /**
     * Create new SignalGenerator
     *
     * @param outFile Output file
     * @param dta Generator instructions
     * @param postProEnabled Postprocessing enabled
     * @param outputType Output type WAVE or AUDIO
     * @param repeat Indicates whether the output should be repeated
     * @param dummy
     * @param asc
     */
    public SignalGenerator(String outFile, int[] dta, boolean postProEnabled, int outputType, boolean repeat, boolean dummy,SignalGeneratorConfig asc) {
        mem = dta;
        cancelType = CANCEL_NO;
        genLength = mem.length;
        errorMessage = null;
        wavePostProcessingEnabled = postProEnabled;
        this.outputType = outputType;
        resumeFromPause = false;
        repeatOutput = repeat;
        this.outFile = outFile;
        isDummy = dummy;
        asConfig = asc;
    }


    private void copyConfiguration() {

        cBits = asConfig.bitsPerSample;

        cPostProcessingString = asConfig.postProcessingString;
        cChannels = asConfig.numChannels;
        cPulseVolume = asConfig.amplitude;
        cSignalInRightChannelOnly = asConfig.rightChannelOnly;
        cDoNotModulateStandardRecords = asConfig.doNotModulateStandard;
        cHarmonic = asConfig.waveForm;
        cInitialSilence = asConfig.initialSilence;
        cTerminalSilence = asConfig.terminalSilence;
        cSampleRate = asConfig.sampleRate;

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
     * @throws java.lang.Exception
     */
    public void prepare() throws Exception {

        /*Copy configuration*/
        copyConfiguration();



            if (!isDummy) {
                signalWriter = new AudioSignalBufferedWriter(cBits, cChannels, cSigned, cBufferSize, cSampleRate, cTerminalSilence);
            }



        /*Prepare writer*/
        signalWriter.prepare(outFile);

        /*Create silence*/
        SILENCE_SHORT = PulseCreator.createPulse(cChannels, cPulseVolume, cSampleRate/10, cBits, cSigned, 1, 1, cSignalInRightChannelOnly, 0);
        BLOCKSEP = PulseCreator.createPulse(cChannels, cPulseVolume, cSampleRate/44, cBits, cSigned, 1, 1, cSignalInRightChannelOnly, 0);

        /*Reasonable default pieces of signal*/
        LOW_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_LOW, SignalGenerator.FLAG_POLARITY_01, cSignalInRightChannelOnly, 0);
        HIGH_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_HIGH, SignalGenerator.FLAG_POLARITY_01, cSignalInRightChannelOnly, 0);
        SILENCE_SAMPLE = PulseCreator.createPulse(cChannels, cPulseVolume, 1, cBits, cSigned, PulseCreator.SPECIAL_SILENCE, SignalGenerator.FLAG_POLARITY_01, cSignalInRightChannelOnly, 0);

    }

    /*Main Thread -----------------------------------------------------------*/
    /**
     * **********************************************************************
     */
    @Override
    public void run() {



        /*Prepare for output*/
        try {
            prepare();
        } catch (Exception e) {
            errorMessage = "Error when preparing audio output: "
                    + Utils.getExceptionMessage(e);

            e.printStackTrace();
            return;
        }

        /*Start writing signal*/
        try {

            /*Initial signal*/
            for (int k = 0; k < cInitialSilence; k++) {
                signalWriter.writeInitialSignal(SILENCE_SHORT);
            }

            ip = 0;
            op = INSTR_NOP;

            while (op != SignalGenerator.INSTR_END && cancelType == CANCEL_NO) {

                op = mem[ip];


                /*Execution of instructions*/
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

                        for (int i = 0; i < cx && cancelType == CANCEL_NO; i++) {
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
                        if (repeatOutput == true) {
                            ip = 0;
                            op = INSTR_NOP;
                            continue;
                        } else {
                            break;
                        }
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
                        int silence = getPWMMilis2Samples(mem[ip]);
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
                        for (int i = 0; i < cx && cancelType == CANCEL_NO; i++) {
                            generateByte(mem[ip]);
                            ip++;
                        }

                        break;
                    }

                    case SignalGenerator.INSTR_PWML: {
                        ip++;
                        /*Silence*/
                        int silence = getPWMMilis2Samples(mem[ip]);
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
            if (cancelType != CANCEL_NO) {
                signalWriter.prepareForClose();
                signalWriter.close();
            } /*Generator ended normally*/ else {

                /*Terminal signal - silence*/
                signalWriter.prepareForTerminationSignal(SILENCE_SHORT);
                while (cancelType == CANCEL_NO && signalWriter.writeTerminationSignal()) {

                }

                signalWriter.prepareForClose();
                signalWriter.close();
                //dumpInstructions();

            }

            /*Everything OK*/

        } catch (Exception e) {

            errorMessage = Utils.getExceptionMessage(e);
            try {
                e.printStackTrace();
                signalWriter.close();
            } catch (Exception e1) {
                errorMessage = Utils.getExceptionMessage(e1);
                e1.printStackTrace();
            } finally {
            }
        } finally {


        }

    }

    /**
     * Generate pilot tone
     *
     * @param num Number of pilot tone pulses
     * @throws Exception
     */
    private void generatePilotTone(int num) throws Exception {

        for (int i = 0; i < num && cancelType == CANCEL_NO; i++) {
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
        for (int p = 0; p < tenths && cancelType == CANCEL_NO; p++) {
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
     * Generuje 0 nebo 1
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

    /*Implementing Generator*/

    /**
     *
     * @return
     */

    public boolean isDirect() {
        return false;
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

        /*Create new pulses for the settings*/
        PILOTTONE_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, mem[tmp + 0], dab, das, 0, polarity, cSignalInRightChannelOnly, cHarmonic);
        WIDE_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, mem[tmp + 1], dab, das, 0, polarity, cSignalInRightChannelOnly, cHarmonic);
        NARROW_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, mem[tmp + 2], dab, das, 0, polarity, cSignalInRightChannelOnly, cHarmonic);
        SYNC_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, mem[tmp + 3], dab, das, 0, polarity, cSignalInRightChannelOnly, cHarmonic);
        STOP_PULSE = PulseCreator.createPulse(cChannels, cPulseVolume, mem[tmp + 4], dab, das, 0, polarity, cSignalInRightChannelOnly, cHarmonic);

    }

    /*Cancel generator*/

    public synchronized void cancelOperation(int cncType) {
        cancelType = cncType;
        resumeFromPause = true;
        this.notifyAll();
    }

    public int getStatusPercent() {
        double d;
        d = ip;
        d /= genLength;
        d *= 100;
        if (d > 100) {
            d = 100.0;
        }
        return (int) Math.round(d);
    }

    /**
     *
     * @return
     */
    public int getCancelledType() {
        return cancelType;
    }



    public String getErrorMessage() {
        return errorMessage;
    }

    private int getPWMLength(int i) {
        return (int) Math.round(i * (double) cSampleRate / pwmSampleRate);
    }

    private int getPWMMilis2Samples(int i) {
        return (int) Math.round(i * (((double)cSampleRate)/1000));
    }

    /**
     * Generate BAUD instruction 00 OPCODE 01 BAUDRATE
     */
    private void generateBAUD() {
        ip++;
        fskGenerator = new FSKGenerator(mem[ip], cSigned, cChannels, cBits, this, cPulseVolume, cSignalInRightChannelOnly,cSampleRate);
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

        /*Prepare data for fsk genetator*/
        int data[] = new int[dataLen];
        System.arraycopy(mem, ip, data, 0, dataLen);

        /*Increase instruction pointer*/
        ip += dataLen;

        int numPieces = irgLen / 2_000;
        int remainder = irgLen % 2_000;

        /*Generate IRG in pieces of 2 seconds*/
        for (int i = 0; i < numPieces; i++) {
            fskGenerator.generateIRG(2_000);
            if (cancelType != CANCEL_NO) {
                return;
            }
        }
        /*Remainder of IRG*/
        fskGenerator.generateIRG(remainder);
        if (cancelType != CANCEL_NO) {
            return;
        }

        /*Generate data*/
        fskGenerator.generateData(data);

    }

    /**
     * Generate FSK instruction 00 IRG 01 LENGTH 02 ... DATA (0 and 1 durations
     * in 0.1 miliseconds)
     *
     * @throws Exception
     */
    private void generateFSK() throws Exception {
        ip++;
        int irgLen = mem[ip];
        ip++;
        int dataLen = mem[ip];
        ip++;

        /*To be on the safe side*/
        if (fskGenerator == null) {

                fskGenerator = new FSKGenerator(600, cSigned, cChannels, cBits, this, cPulseVolume, cSignalInRightChannelOnly,cSampleRate);

        }
        fskGenerator.resetAngle();
        fskGenerator.generateIRG(irgLen);
        fskGenerator.generateFSK(mem, ip, dataLen);
        ip += dataLen;
    }

    /**
     *
     * @param b
     * @throws Exception
     */
    @Override
    public void consumeSamples(byte[] b) throws Exception {
        signalWriter.write(b);
    }

}
