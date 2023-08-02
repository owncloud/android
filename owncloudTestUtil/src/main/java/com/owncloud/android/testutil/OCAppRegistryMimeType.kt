package com.owncloud.android.testutil

import com.owncloud.android.domain.appregistry.model.AppRegistryMimeType

val OC_APP_REGISTRY_MIMETYPE = AppRegistryMimeType(
    mimeType = "DIR",
    ext = "appRegistryMimeTypes.ext",
    appProviders = emptyList(),
    name = "appRegistryMimeTypes.name",
    icon = "appRegistryMimeTypes.icon",
    description= "appRegistryMimeTypes.description",
    allowCreation=true,
    defaultApplication= "appRegistryMimeTypes.defaultApplication")
