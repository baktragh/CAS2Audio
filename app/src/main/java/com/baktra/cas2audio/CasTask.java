package com.baktra.cas2audio;

import android.os.AsyncTask;
import android.os.PowerManager;

import com.baktra.cas2audio.signal.SignalGenerator;

import java.util.Optional;
import java.util.concurrent.Callable;

public class CasTask implements Callable<Void> {

    private final boolean stereo;
    private final boolean square;
    private final int volume;
    private final int[] instructions;
    private final boolean invertPolarity;
    private final int resumeIp;
    private final Optional<PowerManager> powerManager;
    private Exception lastException;
    private final CasPlaybackHandler handler;
    private final int sampleRate;
    private Optional<PowerManager.WakeLock> wakeLock;

    private volatile boolean cancelRequest;

    SignalGenerator sg;

    public static final int RESULT_OK = 0;
    public static final int RESULT_ERROR = -1;

    public static final int WAKELOCK_TIMEOUT = 120 * 60 * 1000;

    public CasTask(int[] instructions, CasPlaybackHandler handler, boolean stereo, boolean square, int volume, int sampleRate, boolean invertPolarity, int resumeIp, Optional<PowerManager> pm) {
        this.instructions=instructions;
        this.stereo=stereo;
        this.lastException = null;
        this.handler = handler;
        this.square = square;
        this.volume=volume;
        this.sampleRate=sampleRate;
        this.invertPolarity=invertPolarity;
        this.wakeLock = Optional.empty();
        this.resumeIp=resumeIp;
        this.sg=null;
        this.powerManager=pm;
        this.cancelRequest=false;

    }


    public Void call() {

        /*Setup wake lock. If it fails, processing continues*/
        try {

            if (powerManager.isPresent()) {
                wakeLock = Optional.of(powerManager.get().newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CAS2Audio::TaskWakeLock"));
                wakeLock.get().acquire(WAKELOCK_TIMEOUT);
            } else {
                wakeLock = Optional.empty();
            }
        } catch (Exception e) {
            wakeLock = Optional.empty();
            e.printStackTrace();
        }

        try {
            SignalGenerator.SignalGeneratorConfig sgc = new SignalGenerator.SignalGeneratorConfig();
            sgc.amplitude = volume * 10;
            sgc.bitsPerSample = 16;
            sgc.doNotModulateStandard = false;
            sgc.initialSilence = 1;
            sgc.numChannels = (stereo ? 2 : 1);
            sgc.postProcessingString = "";
            sgc.rightChannelOnly = (stereo);
            sgc.sampleRate = sampleRate;
            sgc.bufferSize = sgc.sampleRate;
            sgc.signedSamples = true;
            sgc.terminalSilence = 1;
            sgc.waveForm = square ? 0 : -1;
            sgc.invertPolarity = invertPolarity;
            sgc.resumeIp = resumeIp;
            sg = new SignalGenerator(instructions, sgc, this);
            sg.run();
            setProgress(0, -1);
        } catch (Exception e) {
            e.printStackTrace();
            lastException = e;
        } finally {
            if (wakeLock.isPresent()) wakeLock.get().release();
        }

        /*Handle the termination by calling methods of the handler.
         The methods are called within this background thread. It is responsibility
         of the handler to publish the results to the UI using the main thread.
         */

        if (lastException != null) {
            handler.processFailure(lastException);
        }
        else if (isCancelled()) {
            int lastIp = 0;
            if (sg!=null) lastIp=sg.getLastIp();
            handler.processCancellation(lastIp);
        }
        else {
            handler.processSuccess();
        }

        return null;



    }

    /*The handler must ensure that publishing to the UI is done on the main thread*/
    public void setProgress(int statusPercent,int ip) {
        handler.publishProgress(statusPercent,ip);
    }

    public void cancel() {
        cancelRequest=true;
    }

    public boolean isCancelled() {
        return cancelRequest;
    }
}
