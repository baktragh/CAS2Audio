package com.baktra.cas2audio.signal;

/**
 * Electric signal writer
 *
 * @author michael
 */
interface SignalWriter {

    /**
     * Prepare signal writer
     *
     * @throws java.lang.Exception
     *
     */
    void prepare() throws Exception;

    /**
     * Write initial signal
     *
     * @param signal Signal
     * @throws Exception
     */
    void writeInitialSignal(byte[] signal) throws Exception;

    /**
     * Write termination signal
     *
     * @return True when this method should be called again, false otherwise.
     * @throws Exception
     *
     */
    boolean writeTerminationSignal() throws Exception;

    /**
     * Write data
     *
     * @param signal
     * @throws Exception
     */
    void write(byte[] signal) throws Exception;

    /**
     * Prepare for closing
     *
     * @throws Exception
     */
    void prepareForClose() throws Exception;

    /**
     * Close writer
     *
     * @throws Exception
     */
    void close() throws Exception;

    /**
     * Flush data instantly
     *
     * @throws java.lang.Exception
     */
    void flush() throws Exception;

    /**
     * Prepare for terminal silence
     *
     * @param signal Terminal signal
     */
    void prepareForTerminationSignal(byte[] signal);

    /**
     * Get total number of generated samples
     *
     * @return Number of samples
     */
    long getNumberOfSamples();

}
