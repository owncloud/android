package com.owncloud.android.domain.camerauploads.usecases

import com.owncloud.android.domain.BaseUseCaseWithResult
import com.owncloud.android.domain.camerauploads.CameraUploadsRepository
import com.owncloud.android.domain.camerauploads.model.CameraUploadsConfiguration

class GetCameraUploadsConfigurationUseCase(
    private val cameraUploadsRepository: CameraUploadsRepository
) : BaseUseCaseWithResult<CameraUploadsConfiguration?, Unit>() {

    override fun run(params: Unit): CameraUploadsConfiguration? =
        cameraUploadsRepository.getCameraUploadsConfiguration()
}
