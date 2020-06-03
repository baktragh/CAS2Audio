package com.baktra.cas2audio;


class Utils {

    /**
     * Get a message string for some Exception
     * @return message
     */
    public static String getExceptionMessage(Exception ex) {
        StringBuilder sb = new StringBuilder(128);
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
     * @return message
     */
    public static String getTitledExceptionMessage(String title, Exception ex) {
        String sb = "<HTML><B>" +
                title +
                "</B><BR>" +
                Utils.getExceptionMessage(ex) +
                "</HTML>";
        return sb;

    }

    private Utils() {
    }
}
