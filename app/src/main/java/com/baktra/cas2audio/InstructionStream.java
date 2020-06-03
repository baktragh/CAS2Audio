package com.baktra.cas2audio;

/**
 * Auto-growing storage for integers
 *
 */
class InstructionStream {

    private int[] storage;
    private int pointer;

    /**
     * Create new stream with default initial capacity
     */
    public InstructionStream() {
        this(1_024);
    }

    /**
     * Create new instruction stream
     *
     * @param initialCapacity Initial capacity
     */
    private InstructionStream(int initialCapacity) {
        storage = new int[initialCapacity];
        pointer = 0;
    }

    /**
     * Add single instruction
     *
     * @param instruction
     */
    public final void add(int instruction) {

        /* Check capacity*/
        if (pointer == storage.length - 1) {
            increaseCapacity(512);
        }
        /* Add instruction*/
        storage[pointer] = instruction;
        pointer++;

    }

    /**
     * Add instructions
     *
     * @param instructions Instructions
     */
    public final void add(int[] instructions) {

        /* Check capacity*/
        int free = storage.length - 1 - pointer;
        if (free < instructions.length) {
            increaseCapacity(instructions.length * 2);
        }
        System.arraycopy(instructions, 0, storage, pointer, instructions.length);
        pointer += instructions.length;
    }

    /**
     * @return
     */
    public final int[] getInstructions() {
        int[] instructions = new int[pointer];
        System.arraycopy(storage, 0, instructions, 0, instructions.length);
        return instructions;
    }

    private void increaseCapacity(int increment) {
        int newSize = storage.length + increment;
        int[] newStorage = new int[newSize];
        System.arraycopy(storage, 0, newStorage, 0, pointer);
        storage = null;
        storage = newStorage;

    }

}
