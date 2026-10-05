package com.baktra.cas2audio.tapeimage;

import android.net.Uri;

import com.baktra.cas2audio.ConversionCrate;

public class TapeImageCrate {

    private ConversionCrate convCrate;
    private Uri uri;

    private boolean isEmpty;

    private TapeImageCrate() {
        convCrate=null;
        uri=null;
        isEmpty=true;
    }

    public static TapeImageCrate getEmpty() {
        TapeImageCrate tic = new TapeImageCrate();
        return tic;
    }

    public static TapeImageCrate getFull(ConversionCrate cc,Uri uri) {
        TapeImageCrate tic = new TapeImageCrate();
        tic.convCrate=cc;
        tic.uri=uri;
        tic.isEmpty=false;
        return tic;
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    public ConversionCrate getConvCrate() {
        return convCrate;
    }

    public Uri getUri() {
        return this.uri;
    }
}
