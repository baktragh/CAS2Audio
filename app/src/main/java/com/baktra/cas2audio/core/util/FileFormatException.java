package com.baktra.cas2audio.core.util;


public class FileFormatException extends Exception {

    private final String message;

    public FileFormatException(String msg) {
        message = msg;
    }

    @Override
    public String getMessage() {
        if (message != null) {
            return message;
        } else {
            return "No message";
        }
    }

}
