package com.baktra.cas2audio.signal;

/**
 * Pulse Creator
 *
 */
class PulseCreator {

    /**
     *
     */
    public static final int IDX_BAUDRATE = 0;

    /**
     *
     */
    public static final int IDX_HEAD = 1;

    /**
     *
     */
    public static final int IDX_LONG = 2;

    /**
     *
     */
    public static final int IDX_SHORT = 3;

    /**
     *
     */
    public static final int IDX_SYNC = 4;

    /**
     *
     */
    private static final int SPECIAL_NONE = 0;

    /**
     *
     */
    public static final int SPECIAL_SILENCE = 1;

    /**
     *
     */
    public static final int SPECIAL_LOW = 2;

    /**
     *
     */
    public static final int SPECIAL_HIGH = 3;

    /**
     * Pilot tone prolongation, number of pulses increases with transfer speed
     *
     * @param newWidth New transfer speed
     * @param origWidth Base transfer speed
     * @param numPulses Number of pilot tone pulses for base transfer speed
     * @return Number of pilot tone pulses after prolongation
     */
    public static int prolongatePilotTone(int numPulses, int origWidth, int newWidth) {

        double origTime = origWidth * (double) numPulses;
        double newPulses = origTime / (double) newWidth;
        return (int) Math.round(newPulses);
    }

    /**
     * Create pulse
     *
     * @param channels Number of channels
     * @param volume Volume (in percent)
     * @param width Pulse width
     * @param bits Bits per sample
     * @param signed Signed or unsigned
     * @param pulseType Type of pulse
     * @param polarity Polarity of the pulse
     * @param rightOnly Signal in the right channel only
     * @param harmonic Which harmonic to use. 0 Rectangular pulse, 1 Sine wave,
     * other - nth harmonic, -1 auto
     * @return Array with samples
     */
    public static byte[] createPulse(int channels, int volume, int width, int bits, boolean signed, int pulseType, int polarity, boolean rightOnly, int harmonic) {

        /*Harmonic pulses only for non-specials*/
        if (pulseType == SPECIAL_NONE) {

            if (harmonic == -1) {
                if (width <= 6) {
                    harmonic = 0;
                } else {
                    harmonic = 1;
                }
            }

            if (harmonic > 0) {
                return createHarmonicPulse(channels, volume, width, bits, signed, polarity, rightOnly, harmonic);
            }
        }

        int lo = 0;
        int hi = 0;
        int minlo = 0;
        int maxhi = 0;
        int med = 0;

        int[] pulse = new int[width];

        double multip = volume / 100.0d;

        if (signed == true) {
            if (bits == 16) {
                lo = -32_768;
                hi = 32_767;
                med = 0;
            } else {
                lo = -128;
                hi = 127;
                med = 0;
            }
        } else if (bits == 16) {
            lo = 0;
            hi = 65_535;
            med = 32_768;
        } else {
            lo = 0;
            hi = 255;
            med = 128;
        }

        byte bMedHi = ((byte) (med & 0xFF));
        byte bMedLo = ((byte) ((med & 0xFF00) >> 8));

        maxhi = hi;
        minlo = lo;

        if (signed == true) {
            hi = (int) (hi * multip);
            lo = (int) (lo * multip);
        } else {
            hi = (maxhi / 2) + ((int) ((maxhi / 2) * multip));
            lo = maxhi - hi;
        }

        /*Protect against overflow*/
        if (hi > maxhi) {
            hi = maxhi;
        }
        if (lo < minlo) {
            lo = minlo;
        }

        /*Special pulses*/
        switch (pulseType) {
            case SPECIAL_SILENCE: {
                hi = med;
                lo = med;
                break;
            }
            case SPECIAL_LOW: {
                hi = lo;
                break;
            }
            case SPECIAL_HIGH: {
                lo = hi;
            }

        }

        /*Base values*/
        if (polarity == 1) {
            for (int i = 0; i < width / 2; i++) {
                pulse[i] = hi;
            }
            for (int i = width / 2; i < width; i++) {
                pulse[i] = lo;
            }
        } else {
            for (int i = 0; i < width / 2; i++) {
                pulse[i] = lo;
            }
            for (int i = width / 2; i < width; i++) {
                pulse[i] = hi;
            }
        }

        /*Conversion to array of bytes*/
        byte[] retVal = null;
        int pos = 0;

        /*Stereo*/
        if (channels == 2) {
            if (bits == 16) {
                retVal = new byte[width * 4];
                for (int i = 0; i < pulse.length; i++) {
                    byte blo = (byte) ((pulse[i] & 0x0000_FF00) >> 8);
                    byte bhi = (byte) ((pulse[i] & 0x0000_00FF));
                    if (rightOnly == false) {
                        retVal[pos + 0] = bhi;
                        retVal[pos + 1] = blo;
                    } else {
                        retVal[pos + 0] = bMedHi;
                        retVal[pos + 1] = bMedLo;
                    }
                    retVal[pos + 2] = bhi;
                    retVal[pos + 3] = blo;
                    pos += 4;
                }
            } else {
                retVal = new byte[pulse.length * 2];
                for (int i = 0; i < pulse.length; i++) {
                    byte bhi = (byte) ((pulse[i] & 0x0000_00FF));
                    if (rightOnly == false) {
                        retVal[pos] = bhi;
                    } else {
                        retVal[pos] = bMedHi;
                    }

                    retVal[pos + 1] = bhi;
                    pos += 2;
                }
            }
        } /*Mono*/ else if (bits == 16) {
            retVal = new byte[width * 2];
            for (int i = 0; i < pulse.length; i++) {
                byte blo = (byte) ((pulse[i] & 0x0000_FF00) >> 8);
                byte bhi = (byte) ((pulse[i] & 0x0000_00FF));
                retVal[pos] = bhi;
                retVal[pos + 1] = blo;
                pos += 2;
            }
        } else {
            retVal = new byte[pulse.length];
            for (int i = 0; i < pulse.length; i++) {
                byte bhi = (byte) ((pulse[i] & 0x0000_00FF));
                retVal[pos] = bhi;
                pos += 1;
            }
        }

        return retVal;
    }

    private static byte[] createHarmonicPulse(int channels, int volume, int length, int bits, boolean signed, int polarity, boolean rightOnly, int harmonic) {
        Double2SampleFactory factory = new Double2SampleFactory(volume, rightOnly);
        Double2Sample d2s = factory.getDouble2Sample(signed, bits, channels);

        double twoPi = Math.PI * 2;

        double[] doubleSamples = new double[length];
        byte[] byteSamples = new byte[channels * (bits / 8) * length];
        double tempValue;

        if (polarity == SignalGenerator.FLAG_POLARITY_10) {

            for (int i = 0; i < length; i++) {
                double angle = ((double) i / length) * twoPi;
                tempValue = 0.0d;
                for (int h = 1; h <= harmonic; h += 2) {
                    tempValue += Math.sin(h * angle) / h;
                }
                doubleSamples[i] = tempValue;
            }
        } else {
            for (int i = 0; i < length; i++) {
                double angle = ((double) i / length) * twoPi;
                tempValue = 0.0d;
                for (int h = 1; h <= harmonic; h += 2) {
                    tempValue += Math.sin(h * (angle + Math.PI)) / h;
                }
                doubleSamples[i] = tempValue;
            }
        }

        d2s.double2Sample(byteSamples, 0, doubleSamples);

        return byteSamples;

    }

    private PulseCreator() {
    }

}
