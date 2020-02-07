package com.baktra.cas2audio;

import com.baktra.cas2audio.signal.SignalGenerator;
import com.baktra.cas2audio.tapeimage.BaudChunk;
import com.baktra.cas2audio.tapeimage.DataChunk;
import com.baktra.cas2audio.tapeimage.FSKChunk;
import com.baktra.cas2audio.tapeimage.FujiChunk;
import com.baktra.cas2audio.tapeimage.PWMChunk;
import com.baktra.cas2audio.tapeimage.TapeImage;
import com.baktra.cas2audio.tapeimage.TapeImageChunk;

import java.io.InputStream;

public class TapeImageProcessor {


    public static boolean isChunkSupported(TapeImageChunk chunk) {
        return true;
    }

    /*Convert tape image to the signal generator instructions*/
    public int[] convertItem(InputStream iStream, int sampleRate) throws Exception {

        InstructionStream is = new InstructionStream();

        TapeImage ti = new TapeImage();
        ti.parse(iStream);

        TapeImageChunk chunk;
        TapeImageChunk parent;
        int l = ti.getChunkCount();

        for (int i = 0; i < l; i++) {

            chunk = ti.getChunkAt(i);
            parent = chunk.getParent();
            addInstructionsForChunk(is, chunk);
        }

        is.add(SignalGenerator.INSTR_END);
        return is.getInstructions();
    }



    private void addInstructionsForChunk(InstructionStream is, TapeImageChunk chunk) throws Exception {

        if (chunk.getType().equals("pwmc")) {
            processPWMC(is, (PWMChunk) chunk);
            return;
        }

        if (chunk.getType().equals("pwmd")) {
            processPWMD(is, (PWMChunk) chunk);
            return;
        }

        if (chunk.getType().equals("pwml")) {
            processPWML(is, (PWMChunk) chunk);
        }

        if (chunk.getType().equals("pwms")) {
            processPWMS(is, (PWMChunk) chunk);
            return;
        }

        if (chunk.getType().equals("baud")) {
            processBAUD(is, (BaudChunk) chunk);
        }
        if (chunk.getType().equals("data")) {
            processDATA(is, (DataChunk) chunk);
        }

        if (chunk.getType().equals("fsk ")) {
            processFSK(is, (FSKChunk) chunk);
        }
        if (chunk.getType().equals("FUJI")) {
            processFUJI(is, (FujiChunk) chunk);
        }

    }

    private void processPWMS(InstructionStream is, PWMChunk chunk) throws Exception {

        /*Opcode*/
        is.add(SignalGenerator.INSTR_PWMS);
        /*Polarity*/
        if ((chunk.getAuxLo() & 0x0000_0002) == 2) {
            is.add(SignalGenerator.FLAG_POLARITY_10);
        } else {
            is.add(SignalGenerator.FLAG_POLARITY_01);
        }
        /*Bit order*/
        if ((chunk.getAuxLo() & 0x0000_0004) == 4) {
            is.add(SignalGenerator.FLAG_ORDER_HL);
        } else {
            is.add(SignalGenerator.FLAG_ORDER_LH);
        }
        /*Sample rate*/
        int[] data = chunk.getData();
        is.add(data[0] + data[1] * 256);
    }

    private void processPWMC(InstructionStream is, PWMChunk chunk) throws Exception {

        /*Opcode*/
        is.add(SignalGenerator.INSTR_PWMC);
        /*Silence ms*/
        is.add(chunk.getAux());
        /*Number of pairs*/
        int numPairs = chunk.getLength() / 3;
        is.add(numPairs);
        /*Pairs*/
        int pos = 0;
        int[] data = chunk.getData();
        for (int i = 0; i < numPairs; i++) {
            is.add(data[pos]);
            pos++;
            is.add(data[pos] + 256 * data[pos + 1]);
            pos += 2;
        }

    }

    private void processPWMD(InstructionStream is, PWMChunk chunk) throws Exception {

        /*Opcode*/
        is.add(SignalGenerator.INSTR_PWMD);
        /*Number of bytes*/
        is.add(chunk.getLength());
        /*Narrow pulse*/
        is.add(chunk.getAuxLo());
        /*Wide pulse*/
        is.add(chunk.getAuxHi());

        /*Data itself*/
        int[] data = chunk.getData();
        is.add(data);

    }

    private void processPWML(InstructionStream is, PWMChunk chunk) throws Exception {

        /*Opcode*/
        is.add(SignalGenerator.INSTR_PWML);
        /*Silence*/
        is.add(chunk.getAux());

        /*Number of lengths*/
        is.add(chunk.getLength() / 2);

        /*Data*/
        int[] data = chunk.getData();
        for (int i = 0; i < data.length; i += 2) {
            is.add(data[i] + 256 * data[i + 1]);
        }

    }

    private void processDATA(InstructionStream is, DataChunk chunk) throws Exception {
        is.add(SignalGenerator.INSTR_STDDATA);
        is.add(chunk.getAux());
        is.add(chunk.getLength());
        /*Data*/
        int[] data = chunk.getData();
        is.add(data);

    }

    private void processBAUD(InstructionStream is, BaudChunk chunk) throws Exception {
        is.add(SignalGenerator.INSTR_BAUD);
        is.add(chunk.getBaudRate());
    }

    private void processFSK(InstructionStream is, FSKChunk chunk) throws Exception {
        is.add(SignalGenerator.INSTR_FSK);
        is.add(chunk.getAux());
        is.add(chunk.getLength() / 2);
        int[] data = chunk.getData();
        for (int i = 0; i < data.length; i += 2) {
            int duration = data[i] + (data[i + 1] * 256);
            is.add(duration);
        }
    }

    private void processFUJI(InstructionStream is, FujiChunk chunk) throws Exception {
        is.add(SignalGenerator.INSTR_FUJI);
        is.add(chunk.getLength());
        int[] data = chunk.getData();
        is.add(data);

    }

}

