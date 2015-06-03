
/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */
package com.owncloud.android.lib.common.operations;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parser for Invalid Character server exception
 * @author masensio
 */
public class InvalidCharacterExceptionParser {

    private static final String EXCEPTION_STRING = "OC\\Connector\\Sabre\\Exception\\InvalidPath";
	private static final String EXCEPTION_UPLOAD_STRING = "OCP\\Files\\InvalidPathException";

    // No namespaces
	private static final String ns = null;

    // Nodes for XML Parser
    private static final String NODE_ERROR = "d:error";
	private static final String NODE_EXCEPTION = "s:exception";
    /**
	 * Parse is as an Invalid Path Exception
	 * @param is
	 * @return if The exception is an Invalid Char Exception
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	public boolean parseXMLResponse(InputStream is) throws XmlPullParserException,
            IOException {
        boolean result = false;

		try {
			// XMLPullParser
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);

			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(is, null);
			parser.nextTag();
			result = readError(parser);

		} finally {
			is.close();
		}
		return result;
	}

	/**
	 * Parse OCS node
	 * @param parser
	 * @return List of ShareRemoteFiles
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private boolean readError (XmlPullParser parser) throws XmlPullParserException, IOException {
		String exception = "";
		parser.require(XmlPullParser.START_TAG,  ns , NODE_ERROR);
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// read NODE_EXCEPTION
			if (name.equalsIgnoreCase(NODE_EXCEPTION)) {
				exception = readText(parser);
			} else {
				skip(parser);
			}

		}
		return exception.equalsIgnoreCase(EXCEPTION_STRING) ||
				exception.equalsIgnoreCase(EXCEPTION_UPLOAD_STRING);


	}

	/**
	 * Skip tags in parser procedure
	 * @param parser
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}

    	/**
	 * Read the text from a node
	 * @param parser
	 * @return Text of the node
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}
}
