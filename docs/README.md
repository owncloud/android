# Building the Docs

The Android documentation is not built directly; instead, it is built together with the [core documentation](https://github.com/owncloud/docs/). However, if you would like to build a local copy of the Android documentation, to preview changes that you are making, you can use the following command within the `docs/` directory:

```
yarn install
yarn antora
```

**Note** these commands require NodeJS and Yarn to be installed. To learn more about how to install them, please refer to [that documentation in the docs repository](https://github.com/owncloud/docs/blob/master/docs/getting-started.md).

## Previewing the Generated Docs

Assuming that there are no build errors, the next thing to do is to view the result in your browser. In case you have already installed a web server, you need to configure a virtual host (or similar) which points to the directory `public/`, located in the `docs/` directory of this repository. This directory contains the generated documentation. Alternatively, use the simple server bundled with the current package.json, just execute the following command to serve the documentation at [http://localhost:8080/android/](http://localhost:8080/android/):

```
yarn serve
```
