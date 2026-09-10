package com.baktra.cas2audio.signal;

import com.baktra.cas2audio.CasTask;

public class SignalGenerator implements SampleConsumer {

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
    private DummySignalWriter dummySignalWriter;

    public int getLastIp() {
        return ip;
    }

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
        public int resumeIp;
    }


    private final SignalGeneratorConfig asConfig;

    private final int genLength;

    private int ip;

    private final int[] mem;

    private byte[] WIDE_PULSE;
    private byte[] NARROW_PULSE;
    private byte[] PILOTTONE_PULSE;
    private byte[] SYNC_PULSE;
    private byte[] SILENCE_SHORT;
    private byte[] BLOCKSEP;
    private byte[] STOP_PULSE;

    private byte[] LOW_SAMPLE;
    private byte[] HIGH_SAMPLE;
    private byte[] SILENCE_SAMPLE;

    private boolean loHiOrder = false;

    private int pwmPolarity;
    private boolean pwmLoHiOrder;
    private int pwmSampleRate;

    private FSKGenerator fskGenerator;

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
    private int cResumeIp;

    private SignalWriter currentSignalWriter;
    private SignalWriter audioSignalWriter;
    private final CasTask parentTask;

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

        /*Resume IP*/
        cResumeIp= asConfig.resumeIp;
    }


    private void prepare() throws Exception {

        /*Copy configuration*/
        copyConfiguration();

        /*Create signal writers, both real and dummy*/
        audioSignalWriter = new AudioSignalBufferedWriter(cBits, cChannels, cBufferSize, cSampleRate, cTerminalSilence);
        dummySignalWriter = new DummySignalWriter(cBits, cChannels, cBufferSize, cSampleRate, cTerminalSilence);

        /*Prepare writers*/
        audioSignalWriter.prepare();
        dummySignalWriter.prepare();

        /*Determine writer*/
        if (cResumeIp==-1) {
            currentSignalWriter = audioSignalWriter;
        }
        else {
            currentSignalWriter = dummySignalWriter;
        }

        System.out.println("Resume IP: "+cResumeIp);

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

    public void run() throws Exception {


        /*Prepare for output*/
        prepare();

        /*Initial signal*/
        for (int k = 0; k < cInitialSilence; k++) {
                currentSignalWriter.writeInitialSignal(SILENCE_SHORT);
            }


            ip = 0;
        /*Loop counter*/
        int op = INSTR_NOP;

            while (op != SignalGenerator.INSTR_END && !parentTask.isCancelled()) {

                /*Did we reach the resume point ?*/
                if (ip==cResumeIp) {
                    cResumeIp=-1;
                    currentSignalWriter=audioSignalWriter;
                }

                /*Show progress*/
                parentTask.setProgress(getStatusPercent());

                /*Determine what is the current operation and execute it*/
                op = mem[ip];

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
                            currentSignalWriter.write(SILENCE_SAMPLE);
                        }

                        cx = mem[ip];
                        /*Number of pairs*/
                        ip++;
                        for (int i = 0; i < cx; i++) {
                            byte[] pulse = PulseCreator.createPulse(cChannels, cPulseVolume, getPWMLength(mem[ip]), cBits, cSigned, 0, pwmPolarity, cSignalInRightChannelOnly, cHarmonic);
                            ip++;
                            for (int j = 0; j < mem[ip]; j++) {
                                currentSignalWriter.write(pulse);
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
                            currentSignalWriter.write(SILENCE_SAMPLE);
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
                                currentSignalWriter.write(high == true ? HIGH_SAMPLE : LOW_SAMPLE);
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
                currentSignalWriter.prepareForClose();
                currentSignalWriter.close();
            }
            /*Generator ended normally*/
            else {
                currentSignalWriter.prepareForTerminationSignal(SILENCE_SHORT);
                currentSignalWriter.prepareForClose();
                currentSignalWriter.close();
            }

        /*Everything OK*/
    }

    private void generatePilotTone(int num) throws Exception {

        for (int i = 0; i < num && !parentTask.isCancelled(); i++) {
            currentSignalWriter.write(PILOTTONE_PULSE);
        }

    }

    private void generateSync() throws Exception {
        currentSignalWriter.write(SYNC_PULSE);
    }

    private void generateSilence(int tenths) throws Exception {
        for (int p = 0; p < tenths && !parentTask.isCancelled(); p++) {
            currentSignalWriter.write(SILENCE_SHORT);
        }
    }

    private void generateBlockSep() throws Exception {
        currentSignalWriter.write(BLOCKSEP);
    }

    private void generateStop() throws Exception {
        currentSignalWriter.write(STOP_PULSE);
    }

    private void generateWide() throws Exception {
        currentSignalWriter.write(WIDE_PULSE);
    }

    private void generateNarrow() throws Exception {
        currentSignalWriter.write(NARROW_PULSE);
    }

    private void generateByte(int i) throws Exception {

        /*Bit order from the highest to the lowest*/
        if (loHiOrder == false) {

            for (int k = 0; k < 8; k++) {
                if ((i & 0x0000_0080) == 0) {
                    currentSignalWriter.write(NARROW_PULSE);
                } else {
                    currentSignalWriter.write(WIDE_PULSE);
                }

                /*Left shift*/
                i <<= 1;
            }
        } /*Bit order from the lowest to the highest*/ else {
            for (int k = 0; k < 8; k++) {
                if ((i & 0x0000_0001) == 0) {
                    currentSignalWriter.write(NARROW_PULSE);
                } else {
                    currentSignalWriter.write(WIDE_PULSE);
                }

                /*Shift to right*/
                i >>= 1;
            }
        }
    }

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
            else {
                polarity=SignalGenerator.FLAG_POLARITY_01;
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

    private void generateBAUD() {
        ip++;
        fskGenerator = new FSKGenerator(mem[ip], cSigned, cChannels, cBits, this, cPulseVolume, cSignalInRightChannelOnly, cSampleRate);
        ip++;
    }

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
    public void consumeSamples(byte[] b) throws Exception {
        currentSignalWriter.write(b);
    }


}
