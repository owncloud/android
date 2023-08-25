package com.owncloud.android.ui.asynctasks

import android.accounts.Account
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.usecases.transfers.uploads.UploadFileFromSystemUseCase
import com.owncloud.android.usecases.transfers.uploads.UploadFilesFromSystemUseCase
import com.owncloud.android.utils.UriUtils
import com.owncloud.android.utils.UriUtilsKt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

class CopyAndUploadContentUrisRemake constructor(
    private val account: Account,
    private val spaceId:String,
    private val uploadPath:String,
) {

    private val uploadFilesFromSystemUseCase: UploadFilesFromSystemUseCase by inject(UploadFileFromSystemUseCase::class.java)
    fun uploadFile(temporaryFilePaths:List<String>,
        temporaryFilePathInputStreams:List<InputStream>):ResultCode{
        val filesToUpload= arrayListOf<String>()
        return try {
            for (it in temporaryFilePaths.zip(temporaryFilePathInputStreams)) {
                val (filePathInString,filePathInputStream) = it
                val createdTempFilePath=createTempFileFromContentUrisInputStream(filePathInputStream,
                    filePathInString)
                if (createdTempFilePath==null){
                    ResultCode.FILE_NOT_FOUND
                }else{
                    filesToUpload.add(createdTempFilePath)
                }
            }
            val uploadParams=UploadFilesFromSystemUseCase.Params(account.name,
                filesToUpload,
                uploadPath,spaceId)
            uploadFilesFromSystemUseCase.execute(uploadParams)
            filesToUpload.clear()
            ResultCode.OK
        }catch (exception:Exception){
          Timber.e(exception,"Exception while copying files from given input streams")
          ResultCode.LOCAL_STORAGE_NOT_COPIED
        }
    }
    private fun createTempFileFromContentUrisInputStream(
        inputStream: InputStream,
        temporaryFilePathInString:String):String?{
        val temporaryFilePath=  File(temporaryFilePathInString)
        val cacheFileTemporaryDir= createTempDirectory(temporaryFilePath.parentFile?.absolutePath)
        val cacheFileTemporaryFile=kotlin.io.path.createTempFile(directory =cacheFileTemporaryDir,
            prefix = temporaryFilePath.absolutePath)
        return try {
            cacheFileTemporaryFile.bufferedWriter().use {writer->
                val buffer = CharArray(4096)
                inputStream.bufferedReader().use {reader->
                    val count = reader.read(buffer)
                    while (count>0){
                        writer.write(count)
                    }
                }
            }
            return cacheFileTemporaryFile.pathString
        }catch (ex:IOException){
            Timber.e("Error While Creating Temp File $ex")
            null
        }
    }
}