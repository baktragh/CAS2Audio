package com.baktra.cas2audio;

/**
 * Electric signal writer
 *
 * @author michael
 */
public interface SignalWriter {

    /**
     * Prepare signal writer
     *
     * @throws java.lang.Exception
     *
     */
    public void prepare() throws Exception;

    /**
     * Write initial signal
     *
     * @param signal Signal
     * @throws Exception
     */
    public void writeInitialSignal(byte[] signal) throws Exception;

    /**
     * Write termination signal
     *
     * @return True when this method should be called again, false otherwise.
     * @throws Exception
     *
     */
    public boolean writeTerminationSignal() throws Exception;

    /**
     * Write data
     *
     * @param signal
     * @throws Exception
     */
    public void write(byte[] signal) throws Exception;

    /**
     * Prepare for closing
     *
     * @throws Exception
     */
    public void prepareForClose() throws Exception;

    /**
     * Close writer
     *
     * @throws Exception
     */
    public void close() throws Exception;

    /**
     * Flush data instantly
     *
     * @throws java.lang.Exception
     */
    public void flush() throws Exception;

    /**
     * Prepare for terminal silence
     *
     * @param signal Terminal signal
     */
    public void prepareForTerminationSignal(byte[] signal);

    /**
     * Get total number of generated samples
     *
     * @return Number of samples
     */
    public long getNumberOfSamples();

}
