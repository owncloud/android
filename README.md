# ownCloud Android Library 

### Introduction
Using ownCloud Android library it will be the easiest way to communicate with ownCloud servers.
Add this library in your project and integrate your application with ownCloud seamlessly.

### Install Library
#### 2.1. Information how to get the library

Get this code and compile it. In the repository it is not only the library project but also the example project “sample_client”; thanks to it you will learn how to use the library.

#### 2.2. Add library to your project
There are different ways of adding this library to your code, then it is described one of them

__Step 1.__ Compile the ownCloud Android Library
__Step 2.__ Define a dependency within your project. For that, access to Properties > Android > Library and click on add and select the ownCloud Android library

###  Branching strategy

The repository holds two main branches with an infinite lifetime:

- stable
- master 

Branch __origin/stable__ is considered the main branch where the source code of HEAD always reflects a production-ready state.

Branch __origin/master__ is considered the main branch where the source code of HEAD always reflects a state with the latest delivered development changes for the next release.

When the source code in the master branch reaches a stable point and is ready to be released, all of the changes should be merged back into stable somehow and then tagged with a release number. 

Other branches, some supporting branches are used to aid parallel development between team members, ease tracking of features, prepare for production releases and to assist in quickly fixing live production problems. Unlike the main branches, these branches always have a limited life time, since they will be removed eventually.

The different types of branches we may use are:

- Branch __perNewFeature__    
- Branch  __releaseBranches__

Both of them branch off from master and must merge back into master branch through a Pull Request in Github. Once the PR is approved and merged, the US branch may be deleted.


###  License

ownCloud Android Library is available under MIT license. See the file LICENSE.md with the full license text. 

#### Third party libraries

ownCloud Android Library uses OkHttp version 4.6.0, licensed under Apache License and version 2.0. Besides, it uses Dav4Android, licensed under Mozilla Public License, v. 2.0
   

### Compatibility

ownCloud Android Library is valid for Android systems from version Android 5 (android:minSdkVersion="21" android:targetSdkVersion="29").

ownCloud Android library supports ownCloud server from version 4.5.
