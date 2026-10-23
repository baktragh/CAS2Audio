package com.baktra.cas2audio.features.player.domain.tapeimage;

import com.baktra.cas2audio.features.player.domain.ConversionCrate;

public class TapeImageCrate {

    private ConversionCrate convCrate;
    private String fileName;

    private boolean isEmpty;

    private TapeImageCrate() {
        convCrate=null;
        fileName =null;
        isEmpty=true;
    }

    public static TapeImageCrate getEmpty() {
        TapeImageCrate tic = new TapeImageCrate();
        return tic;
    }

    public static TapeImageCrate getFull(ConversionCrate cc,String fileName) {
        TapeImageCrate tic = new TapeImageCrate();
        tic.convCrate=cc;
        tic.fileName =fileName;
        tic.isEmpty=false;
        return tic;
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    public ConversionCrate getConvCrate() {
        return convCrate;
    }

    public String getFileName() {
        return this.fileName;
    }
}
