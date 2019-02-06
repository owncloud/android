# Makefile for the documentation

SHELL ?= bash

BUILD_DIR ?= public
FONTS_DIR ?= fonts
STYLES_DIR ?= resources/themes

STYLE ?= owncloud
REVDATE ?= "$(shell date +'%B %d, %Y')"

ifndef VERSION
	ifneq ($(DRONE_TAG),)
		VERSION ?= $(subst v,,$(DRONE_TAG))
		OUTPUT_VERSION ?= $(VERSION)/
	else
		ifneq ($(DRONE_BRANCH),)
			VERSION ?= $(subst /,,$(DRONE_BRANCH))
			OUTPUT_VERSION ?= $(VERSION)/
		else
			VERSION ?= latest
			OUTPUT_VERSION ?=
		endif
	endif
endif

#
# Remove build artifacts from output dir.
#
.PHONY: clean
clean:
	@echo "Cleaning up any artifacts from output dir"
	@-rm -rf $(BUILD_DIR)
	@echo

#
# Generate PDF version of the manual.
#
.PHONY: pdf
pdf:
	@echo "Building PDF version of the manual"
	asciidoctor-pdf \
		-a pdf-stylesdir=$(STYLES_DIR)/ \
		-a pdf-style=$(STYLE) \
		-a pdf-fontsdir=$(FONTS_DIR) \
		-a examplesdir=modules/ROOT/examples \
		-a imagesdir=modules/ROOT/assets/images \
		-a revnumber=$(VERSION) \
		-a revdate=$(REVDATE) \
		--base-dir $(CURDIR) \
		--out-file android/$(OUTPUT_VERSION)ownCloud_Android_App_Manual.pdf \
		--destination-dir $(BUILD_DIR) \
		books/ownCloud_Android_App_Manual.adoc
