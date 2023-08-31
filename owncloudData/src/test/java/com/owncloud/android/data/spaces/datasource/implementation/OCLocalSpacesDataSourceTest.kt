package com.owncloud.android.data.spaces.datasource.implementation


import com.owncloud.android.data.OwncloudDatabase
import com.owncloud.android.data.spaces.datasources.implementation.OCLocalSpacesDataSource
import com.owncloud.android.data.spaces.db.SpacesDao
import com.owncloud.android.testutil.OC_SPACE_PERSONAL
import com.owncloud.android.testutil.OC_SPACE_PROJECT_WITH_IMAGE
import com.owncloud.android.testutil.OC_SPACE_SPECIAL_IMAGE
import com.owncloud.android.testutil.OC_SPACE_SPECIAL_README
import io.mockk.every
import io.mockk.mockkClass
import org.junit.Before

class OCLocalSpacesDataSourceTest {

    private lateinit var ocLocalSpacesDataSource: OCLocalSpacesDataSource
    private val spacesDao = mockkClass(SpacesDao::class)

    val listSpace = listOf(OC_SPACE_PERSONAL, OC_SPACE_PROJECT_WITH_IMAGE)
    val listSpaceSpecial = listOf(OC_SPACE_SPECIAL_IMAGE, OC_SPACE_SPECIAL_README)

    @Before
    fun init() {
        val db = mockkClass(OwncloudDatabase::class)

        every { db.spacesDao() } returns spacesDao

        ocLocalSpacesDataSource =  OCLocalSpacesDataSource(spacesDao)
    }
}