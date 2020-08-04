package com.owncloud.android.domain.validator

import org.junit.Assert
import org.junit.Test

class FileNameValidatorTest {

    private val validator = FileNameValidator()

    @Test
    fun validateNameOk(){
        Assert.assertTrue(validator.validate("Photos"))
    }

    @Test
    fun validateNameWithBackSlash(){
        Assert.assertFalse(validator.validate("/Photos"))
    }

    @Test
    fun validateNameWithForwardSlash(){
        Assert.assertFalse(validator.validate("\\Photos"))
    }

    @Test
    fun validateNameWithBothSlashes(){
        Assert.assertFalse(validator.validate("\\Photos/"))
    }

}
