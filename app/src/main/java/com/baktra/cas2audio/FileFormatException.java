package com.baktra.cas2audio;


public class FileFormatException extends Exception {

    private final String message;

    public FileFormatException(String msg) {
        message = msg;
    }

    @Override
    public final String getMessage() {
        if (message != null) {
            return message;
        } else {
            return "No message";
        }
    }

}
