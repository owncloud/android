package com.owncloud.android.testutil

import com.owncloud.android.data.appregistry.db.AppRegistryEntity
import com.owncloud.android.domain.appregistry.model.AppRegistry
import com.owncloud.android.domain.appregistry.model.AppRegistryMimeType
import com.owncloud.android.lib.resources.appregistry.responses.AppRegistryMimeTypeResponse
import com.owncloud.android.lib.resources.appregistry.responses.AppRegistryResponse

val OC_APP_REGISTRY_MIMETYPE = AppRegistryMimeType(
    mimeType = "DIR",
    ext = "appRegistryMimeTypes.ext",
    appProviders = emptyList(),
    name = "appRegistryMimeTypes.name",
    icon = "appRegistryMimeTypes.icon",
    description = "appRegistryMimeTypes.description",
    allowCreation = true,
    defaultApplication = "appRegistryMimeTypes.defaultApplication",
)

val OC_APP_REGISTRY_ENTITY = AppRegistryEntity(
    accountName = OC_ACCOUNT_NAME,
    mimeType = "DIR",
    ext = "appRegistryMimeTypes.ext",
    appProviders = "[]",
    name = "appRegistryMimeTypes.name",
    icon = "appRegistryMimeTypes.icon",
    description = "appRegistryMimeTypes.description",
    allowCreation = true,
    defaultApplication = "appRegistryMimeTypes.defaultApplication",
)

val OC_APP_REGISTRY = AppRegistry(
    accountName = OC_ACCOUNT_NAME,
    mimetypes = listOf(OC_APP_REGISTRY_MIMETYPE)
)

val OC_APP_REGISTRY_RESPONSE = AppRegistryResponse(
    value = listOf(
        AppRegistryMimeTypeResponse(
            mimeType = "DIR",
            ext = "appRegistryMimeTypes.ext",
            appProviders = emptyList(),
            name = "appRegistryMimeTypes.name",
            icon = "appRegistryMimeTypes.icon",
            description = "appRegistryMimeTypes.description",
            allowCreation = true,
            defaultApplication = "appRegistryMimeTypes.defaultApplication",
        )
    )
)
