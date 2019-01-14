# Building the Docs

The Android documentation is not built directly; instead, it is built when [the administration, developer, and user manuals](https://github.com/owncloud/docs/) are made.

However, if you would like to build a local copy of the Android documentation, to preview changes that you are making, as you are making them, you can use the following command:

```
antora \
    --redirect-facility static \
    --stacktrace \
    site.local.yml
```

**Note** this command requires Antora’s command-line tools to be installed.
To learn more about how to install them, please refer to [that documentation in the docs repository](https://github.com/owncloud/docs/blob/master/docs/install-antora.md).

## Previewing the Generated Docs

Assuming that there are no build errors, the next thing to do is to view the result in your browser.
In case you have already installed a web server, you need to configure a virtual host (or similar) which points to the directory `public`, located in the root directory of the repository.
This directory contains the generated documentation.
Alternatively, use the [NPM Serve tool](https://www.npmjs.com/package/serve) or [PHP's built-in webserver](https://secure.php.net/manual/en/features.commandline.webserver.php).

The following example starts *NPM's Serve* running in the background, using the `public` directory as its directory root, and listening on `http://localhost:5000` (if available):

```
serve public &
```
