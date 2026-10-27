package com.baktra.cas2audio.features.player.domain.tapeimage;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import com.baktra.cas2audio.features.player.domain.ConversionCrate;
import com.baktra.cas2audio.core.util.FileFormatException;
import com.baktra.cas2audio.core.util.Utils;
import com.baktra.cas2audio.features.player.domain.tapeimagefile.TapeImageFile;

import java.io.ByteArrayInputStream;
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

    public ReadResult readTapeImage(Uri uri,int[] tapeImageBytes,boolean do48Khz,String fileName) {
        try  {


            TapeImageFile ti = new TapeImageFile();

            /*Read from URI*/
            if (uri!=null) {
                /*Get the permission*/
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                /*Open for input stream*/
                InputStream iStream = resolver.openInputStream(uri);
                ti.parse(iStream);
            }
            /*Read from array*/
            else {
                ByteArrayInputStream bais = new ByteArrayInputStream(Utils.convertIntsToBytes(tapeImageBytes));
                ti.parse(bais);
            }

            /*Perform the conversion*/
            TapeImageProcessor tip = new TapeImageProcessor();
            ConversionCrate cc = tip.convertItem(ti,do48Khz?48000:44100);

            TapeImageCrate tic = TapeImageCrate.getFull(cc,fileName);

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
