package com.owncloud.android.test.ui.actions;

import java.nio.charset.Charset;


public class UnicodeEncoder {
    private static final Charset M_UTF7 = Charset.forName("x-IMAP-mailbox-name");
    private static final Charset ASCII  = Charset.forName("US-ASCII");


    public static String encode(String text) {
        byte[] encoded = text.getBytes(M_UTF7);
        return new String(encoded, ASCII);
    }
}