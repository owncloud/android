#Troubleshooting

## Log Files

Effectively debugging software requires as much relevant information as can be
obtained. To assist the ownCloud support personnel, please try to provide as
many relevant logs as possible. Log output can help with tracking down
problems and, if you report a bug, log output can help to resolve an issue more
quickly.

Here you have a description about how to get relevant information to be handled in case of any issue. You can ship this information via mail (android-app@owncloud.com), creating a new issue in the [open GitHub ownCloud Android repository](https://github.com/owncloud/android), or in [public ownCloud forum](https://central.owncloud.org/).

### Capturing App Debug Logs

You will find the Logs in the Settings view of the Android app. If the Logs option is not visible, tap five times on the build number (bottom of the Settings view), and Logs will be an option inside the "More" section.

When you have reproduced the issue you want to address, the "Send History" button will automatically add the logs to a new mail message. Please follow the next steps to send us your logs:

1. Delete History
2. Perform the steps to reproduce the error
3. Go back to the settings and select Logs
4. Send History

If your issue is an app crash, also the Logcat log can be helpful. To get it:

1. Delete History
2. Perform the steps to reproduce the error
3. Go back to the settings and select Logs
4. In the three-dot-button (top right corner), select Logcat
5. Send History


Deleting the history before getting logs is a good practice that will help the team to debug the problem, avoiding innecesary noise.


### ownCloud server Log File

The ownCloud server also maintains an ownCloud specific log file. This log file
must be enabled through the ownCloud Administration page. On that page, you can
adjust the log level. We recommend that when setting the log file level that
you set it to a verbose level like `Debug` or `Info`.
  
You can view the server log file using the web interface or you can open it
directly from the file system in the ownCloud server data directory.

You can find more information about ownCloud server logging at
https://doc.owncloud.com/server/10.0/admin_manual/configuration/server/logging_configuration.html.

### Webserver Log Files

It can be helpful to view your webserver's error log file to isolate any
ownCloud-related problems. For Apache on Linux, the error logs are typically
located in the `/var/log/apache2` directory. Some helpful files include the
following:

- `error_log` -- Maintains errors associated with PHP code. 
- `access_log` -- Typically records all requests handled by the server; very
  useful as a debugging tool because the log line contains information specific
  to each request and its result.
  
The ownCloud Android app sends the `X-REQUEST-ID` header with every request. You'll find the `X-REQUEST-ID` in the `owncloud.log`, and you can configure your webserver to add the `X-REQUEST-ID` to the logs. [Here](https://doc.owncloud.com/server/latest/admin_manual/configuration/server/request_tracing.html) you can find more information. 
  
You can find more information about Apache logging at this [link] (http://httpd.apache.org/docs/current/logs.html).

## Tools

### mitmproxy

mitmproxy is an interactive man-in-the-middle proxy for HTTP and HTTPS with a console interface. At ownCloud we use it a lot to investigate every detail of HTTP requests and responses:  
https://mitmproxy.org/

