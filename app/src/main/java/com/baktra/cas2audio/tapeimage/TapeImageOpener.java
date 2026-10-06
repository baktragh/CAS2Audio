package com.baktra.cas2audio.tapeimage;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import com.baktra.cas2audio.ConversionCrate;
import com.baktra.cas2audio.FileFormatException;
import com.baktra.cas2audio.tapeimagefile.TapeImage;

import java.io.InputStream;

public class TapeImageOpener {

    public static class ReadResult {
        boolean success;
        int failureNature;
        Exception exception;
        TapeImageCrate tapeImageCrate;

        public static final int FAILURE_NONE=0;
        public static final int FAILURE_NOT_TAPEIMAGE=1;
        public static final int FAILURE_CANNOT_OPEN=2;

        public ReadResult(final boolean success, final int failureNature, final Exception exception,TapeImageCrate tapeImageCrate) {
            this.success = success;
            this.failureNature = failureNature;
            this.exception = exception;
            this.tapeImageCrate=tapeImageCrate;
        }

        public TapeImageCrate getTapeImageCrate() {
            return this.tapeImageCrate;
        }

        public boolean isSuccess() {
            return this.success;
        }

        public int getFailureNature() {
            return this.failureNature;
        }

        public Exception getException() {
            return this.exception;
        }


    }

    private final ContentResolver resolver;

    public TapeImageOpener(ContentResolver resolver) {
        this.resolver=resolver;
    }

    public ReadResult readTapeImage(Uri uri,boolean do48Khz) {
        try  {
            /*Get the persmission*/
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            /*Open for input stream*/
            InputStream iStream = resolver.openInputStream(uri);
            TapeImage ti = new TapeImage();
            ti.parse(iStream);

            /*Perform the conversion*/
            TapeImageProcessor tip = new TapeImageProcessor();
            ConversionCrate cc = tip.convertItem(ti,do48Khz?48000:44100);

            TapeImageCrate tic = TapeImageCrate.getFull(cc,uri);

            return new ReadResult(true,ReadResult.FAILURE_NONE,null,tic);

        } catch (Exception e) {

            /*Assume cannot open*/
            int failureReason=ReadResult.FAILURE_CANNOT_OPEN;

            /*Special case, not a tape image*/
            if (e instanceof FileFormatException) {
                failureReason = ReadResult.FAILURE_NOT_TAPEIMAGE;
            }

            return new ReadResult(false,failureReason,e,null);

        }

    }


}
