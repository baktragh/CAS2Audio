package com.baktra.cas2audio.signal;

/**
 *
 * @author  
 */
public class Double2SampleFactory {

    private final Double2Sample[] classArray = new Double2Sample[8];

    /**
     *
     */
    private final float amplitudeMultiplier;

    /**
     *
     */
    private final boolean rightOnly;

    Double2SampleFactory(int amplitude, boolean rightOnly) {
        classArray[7] = new Signed16Stereo();
        classArray[6] = new Signed16Mono();
        classArray[5] = new Signed8Stereo();
        classArray[4] = new Signed8Mono();
        classArray[3] = new Unsigned16Stereo();
        classArray[2] = new Unsigned16Mono();
        classArray[1] = new Unsigned8Stereo();
        classArray[0] = new Unsigned8Mono();

        this.amplitudeMultiplier = amplitude / 100.0f;
        this.rightOnly = rightOnly;
    }

    /**
     *
     * @param signed
     * @param bitsPerSample
     * @param numChannels
     * @return
     */
    public Double2Sample getDouble2Sample(boolean signed, int bitsPerSample, int numChannels) {

        int index = 0;
        if (signed == true) {
            index += 4;
        }
        if (bitsPerSample == 16) {
            index += 2;
        }
        if (numChannels == 2) {
            index += 1;
        }

        return classArray[index];

    }

    class Signed16Mono implements Double2Sample {

        @Override
        public void double2Sample(byte[] samples, int srcIndex, double[] doubleSamples) {

            double f;
            int s;

            for (int i = 0; i < doubleSamples.length; i++) {
                f = doubleSamples[i] * amplitudeMultiplier;
                f *= 32766.0f;

                s = (int) Math.round(f);

                samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                srcIndex++;
                samples[srcIndex] = ((byte) ((s & 0x0000_FF00) >> 8));
                srcIndex++;

            }

        }

    }

    class Signed16Stereo implements Double2Sample {

        @Override
        public void double2Sample(byte[] samples, int srcIndex, double[] doubleSamples) {
            double f;
            int s;

            for (int i = 0; i < doubleSamples.length; i++) {
                f = doubleSamples[i] * amplitudeMultiplier;
                f *= 32766.0f;

                s = (int) Math.round(f);

                if (rightOnly == false) {
                    samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                    srcIndex++;
                    samples[srcIndex] = ((byte) ((s & 0x0000_FF00) >> 8));
                    srcIndex++;
                } else {
                    samples[srcIndex] = 0;
                    srcIndex++;
                    samples[srcIndex] = 0;
                    srcIndex++;
                }

                samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                srcIndex++;
                samples[srcIndex] = ((byte) ((s & 0x0000_FF00) >> 8));
                srcIndex++;

            }
        }

    }

    class Signed8Mono implements Double2Sample {

        @Override
        public void double2Sample(byte[] samples, int srcIndex, double[] doubleSamples) {
            double f;
            int s;

            for (int i = 0; i < doubleSamples.length; i++) {
                f = doubleSamples[i] * amplitudeMultiplier;
                f *= 126.0f;

                s = (int) Math.round(f);

                samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                srcIndex++;

            }
        }

    }

    class Signed8Stereo implements Double2Sample {

        @Override
        public void double2Sample(byte[] samples, int srcIndex, double[] doubleSamples) {
            double f;
            int s;

            for (int i = 0; i < doubleSamples.length; i++) {
                f = doubleSamples[i] * amplitudeMultiplier;
                f *= 126.0f;

                s = (int) Math.round(f);

                if (rightOnly == false) {
                    samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                    srcIndex++;
                } else {
                    samples[srcIndex] = 0;
                    srcIndex++;
                }
                samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                srcIndex++;
            }
        }

    }

    class Unsigned16Mono implements Double2Sample {

        @Override
        public void double2Sample(byte[] samples, int srcIndex, double[] doubleSamples) {
            double f;
            int s;

            for (int i = 0; i < doubleSamples.length; i++) {
                f = doubleSamples[i] * amplitudeMultiplier;
                f += 1.0f;
                f *= 32766.0f;

                s = (int) Math.round(f);

                samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                srcIndex++;
                samples[srcIndex] = ((byte) ((s & 0x0000_FF00) >> 8));
                srcIndex++;

            }
        }

    }

    class Unsigned16Stereo implements Double2Sample {

        @Override
        public void double2Sample(byte[] samples, int srcIndex, double[] doubleSamples) {
            double f;
            int s;

            for (int i = 0; i < doubleSamples.length; i++) {
                f = doubleSamples[i] * amplitudeMultiplier;
                f += 1.0f;
                f *= 32766.0f;

                s = (int) Math.round(f);

                if (rightOnly == false) {
                    samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                    srcIndex++;
                    samples[srcIndex] = ((byte) ((s & 0x0000_FF00) >> 8));
                    srcIndex++;
                } else {
                    samples[srcIndex] = (byte) ((32_768 & 0x0000_00FF));
                    srcIndex++;
                    samples[srcIndex] = ((byte) ((32_768 & 0x0000_FF00) >> 8));
                    srcIndex++;
                }

                samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                srcIndex++;
                samples[srcIndex] = ((byte) ((s & 0x0000_FF00) >> 8));
                srcIndex++;

            }
        }

    }

    class Unsigned8Mono implements Double2Sample {

        @Override
        public void double2Sample(byte[] samples, int srcIndex, double[] doubleSamples) {
            double f;
            int s;

            for (int i = 0; i < doubleSamples.length; i++) {
                f = doubleSamples[i] * amplitudeMultiplier;
                f += 1.0f;
                f *= 126.0f;

                s = (int) Math.round(f);
                samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                srcIndex++;

            }
        }

    }

    class Unsigned8Stereo implements Double2Sample {

        @Override
        public void double2Sample(byte[] samples, int srcIndex, double[] doubleSamples) {
            double f;
            int s;

            for (int i = 0; i < doubleSamples.length; i++) {
                f = doubleSamples[i] * amplitudeMultiplier;
                f += 1.0f;
                f *= 126.0f;

                s = (int) Math.round(f);
                if (rightOnly == false) {
                    samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                    srcIndex++;
                } else {
                    samples[srcIndex] = (byte) ((128 & 0x0000_00FF));
                    srcIndex++;
                }
                samples[srcIndex] = (byte) ((s & 0x0000_00FF));
                srcIndex++;
            }
        }

    }
}
