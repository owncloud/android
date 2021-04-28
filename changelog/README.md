# Changelog

We are using [calens](https://github.com/restic/calens) to properly generate a
changelog before we are tagging a new release. 

## Create Changelog items
Create a file according to the [template](TEMPLATE.md) for each 
feature, fix, change...  in the [unreleased](./unreleased) folder. The file should be named after
the # of the merged PR it is describing. The following change types are possible: `Bugfix, Change, Enhancement, Security`.

## Automated Changelog build and commit
- After each merge to master, the CHANGELOG.md file is automatically updated and the new version will be committed to master while skipping CI.

## Create a new Release
- Copy the files from the [unreleased](./unreleased) folder into a folder matching the schema `0.3.0_2020-01-10`


