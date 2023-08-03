package com.owncloud.android.data.authentication.datasources.implementation

import com.owncloud.android.data.ClientManager
import com.owncloud.android.testutil.OC_USER_INFO
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OCRemoteAuthenticationDataSourceTest {
    private lateinit var ocRemoteAuthenticationDataSource: OCRemoteAuthenticationDataSource
    private val clientManager: ClientManager = mockk(relaxed = true)

    @Before
    fun init() {
        ocRemoteAuthenticationDataSource = OCRemoteAuthenticationDataSource(clientManager)
    }


    @Test
    fun `loginOAuth successful login`() {
        val serverPath = "https://ocis.owncloud.com"
        val username = ""
        val password = "eyJhbGciOiJQUzI1NiIsImtpZCI6InByaXZhdGUta2V5IiwidHlwIjoiSldUIn0.eyJhdWQiOiJlNHJBc05VU0lVczBsRjRuYnY5Rm1DZVVrVGxWOUdkZ1RMREgxYjV1aWU3c3liOTBTekVWcmJON0hJcG1XSmVEIiwiZXhwIjoxNjkxMDU0MDU3LCJpYXQiOjE2OTEwNTM3NTcsImlzcyI6Imh0dHBzOi8vb2Npcy5vd25jbG91ZC5jb20iLCJqdGkiOiJoMXdmNnZDb25hM19IQ3VxbG1zR1lLNFJhNFpFdUFDaSIsImxnLmkiOnsiZG4iOiJLYXRoZXJpbmUgSm9obnNvbiIsImlkIjoib3duQ2xvdWRVVUlEPTUzNGJiMDM4LTZmOWQtNDA5My05NDZmLTEzM2JlNjFmYTRlNyIsInVuIjoia2F0aGVyaW5lIn0sImxnLnAiOiJpZGVudGlmaWVyLWxkYXAiLCJsZy50IjoiMSIsInNjcCI6Im9wZW5pZCBvZmZsaW5lX2FjY2VzcyBlbWFpbCBwcm9maWxlIiwic3ViIjoiZXB2RHBQaDdrbk5fTHlJNkBtTEs1c1NxVWFlX1YwM21TT2tGQktTZG1LNkRMekxyWnFTRElaSFl0am9GaXRCTnE1dGkzbTdfWE1MSHpIVWxqVzVMeGd3In0.KZAwwItDXAbDDcoOzvNz56gmydoguRnyKNEHOyTOJmmN7l-P1D6P9ci2nU0KJjzmzmlvgLyZ_S763s4vzHUTVdlDJ5Ui4h4yFRVNlK0e87waLmTwWjC894sgt4UZ7VTlMVQ5oTprtrKu-r2592qFVTsVjLWmeM1dbm9f0b-4bFERhz-DufCWScNyT70e3GBIk7xA6YtiLT4IznnqFdtt0KUgK6jIKki9Jz63J3OPzWbC9yBtQ6DGyJC38l__HhHP0l624a-PUChVxyBpBtStpZ0AVzHNPAt_RiYCKi-ebse3CendV5-moqZ6u9w3gwk9DlcVX7Lq_YT-Y38jGnXkKgU6Od7iBcqJG9cpCslTEXgLqKA495MPlSA7bWtWMAY9YgZR3tYK8l3NJKhmr1ZKUL1W56TcoyX7ccwypGvxYdMif0GQQw66SY-cXHYNRIZuwFIYyexTk2jXois0MsUbJMr2u43BVoSXPUyeFFhXNYUaf5nzr6WjjMBw1bl6W3wIN28utGwKXDzOTMns5XIUfeOufqzj7GXl18OAqBKaSerywmVCp5ZeZ4l1f-KFiCuylwL7YjsLLzZ2eQLCbnfQt3_4QUMPs6CLnLWUXIDB08YbP0BsoRBd7pPqvm0UUlM2ZniM26Kd6HO2BOz3pKAjbv4oB89pFCpWu6EBG48ICG4"

        val userInfo = OC_USER_INFO
        val expectedResult = Pair(userInfo, null)

        val result = ocRemoteAuthenticationDataSource.loginOAuth(serverPath, username, password)

        // Verificación de los resultados
        assertEquals(expectedResult, result)
    }
}