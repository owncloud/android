package com.owncloud.android.lib.resources.shares;

import java.util.ArrayList;

public class ShareParserResult {
    private ArrayList<OCShare> shares;
    private String parserMessage;

    public ShareParserResult(ArrayList<OCShare> shares, String parserMessage) {
        this.shares = shares;
        this.parserMessage = parserMessage;
    }

    public ArrayList<OCShare> getShares() {
        return shares;
    }

    public String getParserMessage() {
        return parserMessage;
    }
}