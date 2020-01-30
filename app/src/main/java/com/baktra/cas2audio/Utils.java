package com.baktra.cas2audio;

import java.io.*;
import java.util.*;


public class Utils {

    /**
     * Get a message string for some Exception
     * @return 
     */
    public static String getExceptionMessage(Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.getClass().getName());
        String m = ex.getMessage();
        if (m != null) {
            sb.append(':');
            sb.append(' ');
            sb.append(m);
        }
        return sb.toString();
 }

    /**
     * Get HTML formatted message for some exception with title specified
     * @return 
     */
    public static String getTitledExceptionMessage(String title, Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML><B>");
        sb.append(title);
        sb.append("</B><BR>");
        sb.append(Utils.getExceptionMessage(ex));
        sb.append("</HTML>");
        return sb.toString();

    }


    /**
     *
     * @param byteArray
     * @return
     */
    public static int[] getAsIntArray(byte[] byteArray) {
        return getAsIntArray(byteArray, byteArray.length);
    }

    /**
     *
     * @param byteArray
     * @param numBytes
     * @return
     */
    public static int[] getAsIntArray(byte[] byteArray, int numBytes) {

        int[] intArray = new int[numBytes];

        byte b;

        for (int i = 0; i < numBytes; i++) {
            /*Orezani znamenka*/
            b = byteArray[i];
            intArray[i] = (b < 0) ? b + 256 : b;
        }

        return intArray;
    }


    private Utils() {
    }
}
