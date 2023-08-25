package com.owncloud.android.ui.helpers

import android.accounts.Account
import android.content.ContentResolver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisRemake
import com.owncloud.android.ui.helpers.UriUploader.UriUploaderResultCode
import com.owncloud.android.utils.FileStorageUtils
import timber.log.Timber
import java.io.InputStream
import java.util.concurrent.CountDownLatch

class UriUploaderRe constructor(
    private val urisToUpload:ArrayList<Uri>,
    private val account:Account,
    private val spaceId:String,
    private val uploadPath:String,
    private val showWaitingDialog:Boolean, handlerThreadLooper:Looper){

    private val copyAndUploadContentUriTask by lazy {
        CopyAndUploadContentUrisRemake(account = account,spaceId =spaceId, uploadPath = uploadPath) }


    private val ioThreadHandler = Handler(handlerThreadLooper)

    fun uploadUris(getDisplayNameForUri:(Uri)->String,
        showLoadingDialog:()->Unit,
        getInputStreamFromUri:(Uri)->InputStream?
        ):UriUploaderResultCode{
        var uriUploaderResultCode= UriUploaderResultCode.OK
        val countdownLatch = CountDownLatch(1)
        ioThreadHandler.post {
            val validUris=urisToUpload.filter {
                ContentResolver.SCHEME_CONTENT.equals(it)
            }
            uriUploaderResultCode=try {
                if (validUris.isEmpty()){
                    UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD
                }else{
                    if (showWaitingDialog){
                        //activity.showLoadingDialog(R.string.wait_for_tmp_copy_from_private_storage)
                        showLoadingDialog()
                    }
                    val fullTemporaryPaths=validUris.map {uri-> getFullTemporaryPath(getDisplayNameForUri(uri)) }
                    // activity.contentResolver.openInputStream(uri)

                    val currentFullTemporaryPathInputStreams=  validUris.mapNotNull {
                        getInputStreamFromUri(it)
                    }

                    if (currentFullTemporaryPathInputStreams.size == fullTemporaryPaths.size){
                        copyAndUploadContentUriTask.uploadFile(fullTemporaryPaths,
                            currentFullTemporaryPathInputStreams)
                        UriUploaderResultCode.COPY_THEN_UPLOAD
                    }else{
                        // not equal
                        UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD
                    }
                }

            }catch (securityException:SecurityException){
                Timber.e(securityException, "Permissions fail")
                UriUploaderResultCode.ERROR_READ_PERMISSION_NOT_GRANTED
            }catch (exception:Exception){
                Timber.e(exception,"Unknown error occurred")
                UriUploaderResultCode.ERROR_UNKNOWN
            }
            countdownLatch.countDown()
        }
        Timber.d("LOg upload: $uriUploaderResultCode")
        try {
            countdownLatch.await()
        }catch (_:Exception){}
       return  uriUploaderResultCode
    }
    //UriUtils.getDisplayNameForUri(uri,activity)}
    private fun getFullTemporaryPath(displayNameForUri:String):String{
        val currentRemotePath="$uploadPath${displayNameForUri}"
       return FileStorageUtils.getTemporalPath(account.name,
            spaceId) + currentRemotePath
    }
}