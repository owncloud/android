'use strict'
/* Copyright (c) 2018 OpenDevise, Inc.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * Scans converted pages and navigation files for broken xrefs; if any broken
 * xrefs are detected, outputs a report and exits with a failure code.
 *
 * Usage (from root of playbook repository):
 *
 *  $ NODE_PATH=netlify/node_modules antora --pull --generator=./generator/xref-validator.js antora-playbook.yml
 */
const aggregateContent = require('@antora/content-aggregator')
const buildPlaybook = require('@antora/playbook-builder')
const classifyContent = require('@antora/content-classifier')
const { convertDocument } = require('@antora/document-converter')
const { resolveConfig: resolveAsciiDocConfig } = require('@antora/asciidoc-loader')

const BROKEN_XREF_RX = /<a href="#">([^>]+\.adoc)(#[^<]+)?<\/a>/g

module.exports = async (args, env) => {
  const playbook = buildPlaybook(args, env)
  const contentCatalog = await aggregateContent(playbook).then((aggregate) => classifyContent(playbook, aggregate))
  const asciidocConfig = resolveAsciiDocConfig(playbook)
  const docsWithBrokenXrefs = new Map()
  const unsilenceStderr = silenceStderr()
  contentCatalog
    .getFiles()
    .filter((file) => (file.src.family === 'page' && file.out) || file.src.family === 'nav')
    .forEach((doc) => {
      convertDocument(doc, contentCatalog, asciidocConfig)
      if (doc.contents.includes('href="#"')) {
        const brokenXrefs = new Set()
        const contents = doc.contents.toString()
        let match
        while ((match = BROKEN_XREF_RX.exec(contents))) {
          const [, pageSpec, hash ] = match
          // Q: should we report the while xref or just the target?
          brokenXrefs.add(pageSpec)
        }
        if (brokenXrefs.size) docsWithBrokenXrefs.set(doc, [ ...brokenXrefs ])
      }
    })
  unsilenceStderr()
  if (docsWithBrokenXrefs.size) {
    const byOrigin = Array.from(docsWithBrokenXrefs).reduce((accum, [page, xrefs]) => {
      let origin
      const originData = page.src.origin
      let startPath = ''
      if (originData.worktree) {
        origin = [
          `worktree: ${originData.editUrlPattern.slice(7, originData.editUrlPattern.length - 3)}`,
          `component: ${page.src.component}`,
          `version: ${page.src.version}`,
        ].join(' | ')
      } else {
        if (originData.startPath) startPath = `${originData.startPath}/`
        origin = [
          `repo: ${originData.url.split(':').pop().replace(/\.git$/, '')}`,
          `branch: ${originData.branch}`,
          `component: ${page.src.component}`,
          `version: ${page.src.version}`,
        ].join(' | ')
      }
      if (!(origin in accum)) accum[origin] = []
      accum[origin].push({ path: `${startPath}${page.path}`, xrefs })
      return accum
    }, {})
    console.error('Invalid Xrefs Detected:')
    console.error()
    Object.keys(byOrigin).sort().forEach((origin) => {
      console.error(origin)
      byOrigin[origin].sort((a, b) => a.path.localeCompare(b.path)).forEach(({ path, xrefs }) => {
        //console.error(`  path: ${path}`)
        //xrefs.forEach((xref) => console.error(`    ${xref}`))
        xrefs.forEach((xref) => console.error(`  path: ${path} | xref: ${xref}`))
      })
      console.error()
    })
    console.error('antora: xref validation failed! See previous report for details.')
    process.exitCode = 1
  }
}

function silenceStderr () {
  const stderrWriter = process.stderr.write
  process.stderr.write = () => {}
  return () => { process.stderr.write = stderrWriter }
}

