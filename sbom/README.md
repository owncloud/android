# Software Bill of Materials (SBOM)

---

## What is an SBOM?

An **SBOM (Software Bill of Materials)** is a detailed inventory of all software components, libraries, and dependencies included in a project. It provides transparency into its composition.


## Why CycloneDX?

[CycloneDX](https://cyclonedx.org/) is a leading open standard for SBOMs that supports rich metadata and is widely supported by tools and ecosystems. It is especially suited for modern DevSecOps workflows and integrates well with build tools.


## How to Generate an SBOM

The ownCloud Android app is ready to generate it. The dependency *CycloneDX* is included in the build.gradle file, so, the SBOM file is an easy one via gradlew:

```
./gradlew cyclonedxBom
```

that command will generate the sbom file in json and xml formats.

## Preview

Since XML and JSON are not easily human-readable formats, the `xsltproc` tool can be used to transform the SBOM into a more user-friendly and visually readable HTML document, with a XSLT template. 

XSLT (Extensible Stylesheet Language Transformations) is a language for transforming XML documents into other formats, such as HTML, plain text, or other XML structures. It uses a set of rules defined in an XSLT stylesheet to match and manipulate elements in the source XML, allowing the data to be presented in a more readable or usable format.

Now...

```
xsltproc cyclonedx-xml-to-html.xslt bom.xml > bom.html
```

And the `bom.html` file is ready to go.



