# Makefile for the documentation

SHELL = bash

FONTS_DIR ?= fonts
STYLES_DIR ?= resources/themes

STYLE ?= owncloud
REVDATE ?= "$(shell date +'%B %d, %Y')"

ifndef VERSION
	ifneq ($(DRONE_TAG),)
		VERSION ?= $(subst v,,$(DRONE_TAG))
	else
		ifneq ($(DRONE_BRANCH),)
			VERSION ?= $(subst /,,$(DRONE_BRANCH))
		else
			VERSION ?= master
		endif
	endif
endif

ifndef OUTPUT
	ifneq ($(VERSION),master)
		OUTPUT ?= build/android/$(VERSION)
	else
		OUTPUT ?= build/android
	endif
endif

.PHONY: help
help: ## Print a basic help about the targets
	@IFS=$$'\n' ; \
	help_lines=(`fgrep -h "##" $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' | sed -e 's/##/:/'`); \
	printf "%-30s %s\n" "target" "help" ; \
	printf "%-30s %s\n" "------" "----" ; \
	for help_line in $${help_lines[@]}; do \
		IFS=$$':' ; \
		help_split=($$help_line) ; \
		help_command=`echo $${help_split[0]} | sed -e 's/^ *//' -e 's/ *$$//'` ; \
		help_info=`echo $${help_split[2]} | sed -e 's/^ *//' -e 's/ *$$//'` ; \
		printf '\033[36m'; \
		printf "%-30s %s" $$help_command ; \
		printf '\033[0m'; \
		printf "%s\n" $$help_info; \
	done

.PHONY: setup
setup: ## Install Antora's command tools locally
	yarn install

.PHONY: clean
clean: ## Remove build artifacts from output dir
	-rm -rf build/

.PHONY: pdf
pdf: ## Generate PDF version of the manual
	asciidoctor-pdf \
		-a pdf-stylesdir=$(STYLES_DIR)/ \
		-a pdf-style=$(STYLE) \
		-a pdf-fontsdir=$(FONTS_DIR) \
		-a examplesdir=modules/ROOT/examples \
		-a imagesdir=modules/ROOT/assets/images \
		-a revnumber=$(VERSION) \
		-a revdate=$(REVDATE) \
		--base-dir $(CURDIR) \
		--out-file $(OUTPUT)/ownCloud_Android_App_Manual.pdf \
		books/ownCloud_Android_App_Manual.adoc
