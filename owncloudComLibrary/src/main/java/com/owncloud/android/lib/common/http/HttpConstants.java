/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.lib.common.http;

/**
 * @author David González Verdugo
 */
public class HttpConstants {

    /***********************************************************************************************************
     *************************************************** HEADERS ***********************************************
     ***********************************************************************************************************/

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String COOKIE_HEADER = "Cookie";
    public static final String BEARER_AUTHORIZATION_KEY = "Bearer ";
    public static final String USER_AGENT_HEADER = "User-Agent";
    public static final String IF_MATCH_HEADER = "If-Match";
    public static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";
    public static final String OC_TOTAL_LENGTH_HEADER = "OC-Total-Length";
    public static final String OC_X_OC_MTIME_HEADER = "X-OC-Mtime";
    public static final String OC_X_REQUEST_ID = "X-Request-ID";
    public static final String LOCATION_HEADER = "Location";
    public static final String LOCATION_HEADER_LOWER = "location";
    public static final String CONTENT_TYPE_URLENCODED_UTF8 = "application/x-www-form-urlencoded; charset=utf-8";
    public static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";
    public static final String ACCEPT_ENCODING_IDENTITY = "identity";
    public static final String OC_FILE_REMOTE_ID = "OC-FileId";

    // OAuth
    public static final String OAUTH_HEADER_AUTHORIZATION_CODE = "code";
    public static final String OAUTH_HEADER_GRANT_TYPE = "grant_type";
    public static final String OAUTH_HEADER_REDIRECT_URI = "redirect_uri";
    public static final String OAUTH_HEADER_REFRESH_TOKEN = "refresh_token";
    public static final String OAUTH_HEADER_CODE_VERIFIER = "code_verifier";
    public static final String OAUTH_HEADER_SCOPE = "scope";

    /***********************************************************************************************************
     ************************************************ CONTENT TYPES ********************************************
     ***********************************************************************************************************/

    public static final String CONTENT_TYPE_XML = "application/xml";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_WWW_FORM = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_JRD_JSON = "application/jrd+json";

    /***********************************************************************************************************
     ************************************************ ARGUMENTS NAMES ********************************************
     ***********************************************************************************************************/

    public static final String PARAM_FORMAT = "format";

    /***********************************************************************************************************
     ************************************************ ARGUMENTS VALUES ********************************************
     ***********************************************************************************************************/

    public static final String VALUE_FORMAT = "json";

    /***********************************************************************************************************
     ************************************************ STATUS CODES *********************************************
     ***********************************************************************************************************/

    /**
     * 1xx Informational
     */

    // 100 Continue (HTTP/1.1 - RFC 2616)
    public static final int HTTP_CONTINUE = 100;
    // 101 Switching Protocols (HTTP/1.1 - RFC 2616)
    public static final int HTTP_SWITCHING_PROTOCOLS = 101;
    // 102 Processing (WebDAV - RFC 2518)
    public static final int HTTP_PROCESSING = 102;

    /**
     * 2xx Success
     */

    // 200 OK (HTTP/1.0 - RFC 1945)
    public static final int HTTP_OK = 200;
    // 201 Created (HTTP/1.0 - RFC 1945)
    public static final int HTTP_CREATED = 201;
    // 202 Accepted (HTTP/1.0 - RFC 1945)
    public static final int HTTP_ACCEPTED = 202;
    // 203 Non Authoritative Information (HTTP/1.1 - RFC 2616)
    public static final int HTTP_NON_AUTHORITATIVE_INFORMATION = 203;
    // 204 No Content</tt> (HTTP/1.0 - RFC 1945)
    public static final int HTTP_NO_CONTENT = 204;
    // 205 Reset Content</tt> (HTTP/1.1 - RFC 2616)
    public static final int HTTP_RESET_CONTENT = 205;
    // 206 Partial Content</tt> (HTTP/1.1 - RFC 2616)
    public static final int HTTP_PARTIAL_CONTENT = 206;
    //207 Multi-Status (WebDAV - RFC 2518) or 207 Partial Update OK (HTTP/1.1 - draft-ietf-http-v11-spec-rev-01?)
    public static final int HTTP_MULTI_STATUS = 207;

    /**
     * 3xx Redirection
     */

    // 300 Mutliple Choices</tt> (HTTP/1.1 - RFC 2616)
    public static final int HTTP_MULTIPLE_CHOICES = 300;
    // 301 Moved Permanently</tt> (HTTP/1.0 - RFC 1945)
    public static final int HTTP_MOVED_PERMANENTLY = 301;
    // 302 Moved Temporarily</tt> (Sometimes <tt>Found) (HTTP/1.0 - RFC 1945)
    public static final int HTTP_MOVED_TEMPORARILY = 302;
    // 303 See Other (HTTP/1.1 - RFC 2616)
    public static final int HTTP_SEE_OTHER = 303;
    // 304 Not Modified (HTTP/1.0 - RFC 1945)
    public static final int HTTP_NOT_MODIFIED = 304;
    // 305 Use Proxy (HTTP/1.1 - RFC 2616)
    public static final int HTTP_USE_PROXY = 305;
    // 307 Temporary Redirect (HTTP/1.1 - RFC 2616)
    public static final int HTTP_TEMPORARY_REDIRECT = 307;

    /**
     * 4xx Client Error
     */

    // 400 Bad Request (HTTP/1.1 - RFC 2616)
    public static final int HTTP_BAD_REQUEST = 400;
    // 401 Unauthorized (HTTP/1.0 - RFC 1945)
    public static final int HTTP_UNAUTHORIZED = 401;
    // 402 Payment Required (HTTP/1.1 - RFC 2616)
    public static final int HTTP_PAYMENT_REQUIRED = 402;
    // 403 Forbidden (HTTP/1.0 - RFC 1945)
    public static final int HTTP_FORBIDDEN = 403;
    // 404 Not Found (HTTP/1.0 - RFC 1945)
    public static final int HTTP_NOT_FOUND = 404;
    // 405 Method Not Allowed (HTTP/1.1 - RFC 2616)
    public static final int HTTP_METHOD_NOT_ALLOWED = 405;
    // 406 Not Acceptable (HTTP/1.1 - RFC 2616)
    public static final int HTTP_NOT_ACCEPTABLE = 406;
    // 407 Proxy Authentication Required (HTTP/1.1 - RFC 2616)
    public static final int HTTP_PROXY_AUTHENTICATION_REQUIRED = 407;
    // 408 Request Timeout (HTTP/1.1 - RFC 2616)
    public static final int HTTP_REQUEST_TIMEOUT = 408;
    // 409 Conflict (HTTP/1.1 - RFC 2616)
    public static final int HTTP_CONFLICT = 409;
    // 410 Gone (HTTP/1.1 - RFC 2616)
    public static final int HTTP_GONE = 410;
    // 411 Length Required (HTTP/1.1 - RFC 2616)
    public static final int HTTP_LENGTH_REQUIRED = 411;
    // 412 Precondition Failed (HTTP/1.1 - RFC 2616)
    public static final int HTTP_PRECONDITION_FAILED = 412;
    // 413 Request Entity Too Large (HTTP/1.1 - RFC 2616)
    public static final int HTTP_REQUEST_TOO_LONG = 413;
    // 414 Request-URI Too Long (HTTP/1.1 - RFC 2616)
    public static final int HTTP_REQUEST_URI_TOO_LONG = 414;
    // 415 Unsupported Media Type (HTTP/1.1 - RFC 2616)
    public static final int HTTP_UNSUPPORTED_MEDIA_TYPE = 415;
    // 416 Requested Range Not Satisfiable (HTTP/1.1 - RFC 2616)
    public static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    // 417 Expectation Failed (HTTP/1.1 - RFC 2616)
    public static final int HTTP_EXPECTATION_FAILED = 417;
    // 419 Insufficient Space on Resource (WebDAV - draft-ietf-webdav-protocol-05?)
    // or <tt>419 Proxy Reauthentication Required (HTTP/1.1 drafts?)
    public static final int HTTP_INSUFFICIENT_SPACE_ON_RESOURCE = 419;
    // 420 Method Failure (WebDAV - draft-ietf-webdav-protocol-05?)
    public static final int HTTP_METHOD_FAILURE = 420;
    // 422 Unprocessable Entity (WebDAV - RFC 2518)
    public static final int HTTP_UNPROCESSABLE_ENTITY = 422;
    // 423 Locked (WebDAV - RFC 2518)
    public static final int HTTP_LOCKED = 423;
    // 424 Failed Dependency (WebDAV - RFC 2518)
    public static final int HTTP_FAILED_DEPENDENCY = 424;
    public static final int HTTP_TOO_EARLY = 425;

    /**
     * 5xx Client Error
     */

    // 500 Server Error (HTTP/1.0 - RFC 1945)
    public static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    // 501 Not Implemented (HTTP/1.0 - RFC 1945)
    public static final int HTTP_NOT_IMPLEMENTED = 501;
    // 502 Bad Gateway (HTTP/1.0 - RFC 1945)
    public static final int HTTP_BAD_GATEWAY = 502;
    // 503 Service Unavailable (HTTP/1.0 - RFC 1945)
    public static final int HTTP_SERVICE_UNAVAILABLE = 503;
    // 504 Gateway Timeout (HTTP/1.1 - RFC 2616)
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    // 505 HTTP Version Not Supported (HTTP/1.1 - RFC 2616)
    public static final int HTTP_HTTP_VERSION_NOT_SUPPORTED = 505;
    // 507 Insufficient Storage (WebDAV - RFC 2518)
    public static final int HTTP_INSUFFICIENT_STORAGE = 507;

    /***********************************************************************************************************
     *************************************************** TIMEOUTS **********************************************
     ***********************************************************************************************************/

    /**
     * Default timeout for waiting data from the server
     */
    public static final int DEFAULT_DATA_TIMEOUT = 60000;

    /**
     * Default timeout for establishing a connection
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
}
