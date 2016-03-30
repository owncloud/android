package com.owncloud.android.test.ui.testSuites;

import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;

import com.owncloud.android.test.ui.groups.FlexibleCategories;
import com.owncloud.android.test.ui.groups.InProgressCategory;
import com.owncloud.android.test.ui.groups.FlexibleCategories.TestClassPrefix;
import com.owncloud.android.test.ui.groups.FlexibleCategories.TestClassSuffix;
import com.owncloud.android.test.ui.groups.FlexibleCategories.TestScanPackage;
import com.owncloud.android.test.ui.groups.LoginTestCategory;


@RunWith(FlexibleCategories.class)
@IncludeCategory(LoginTestCategory.class)
@TestScanPackage("com.owncloud.android.test.ui.testSuites")
@TestClassPrefix("")
@TestClassSuffix("TestSuite")
public class RunInProgressTest {

}
