package com.owncloud.android.services.observer;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.owncloud.android.files.services.RetryUploadJobService;
import com.owncloud.android.utils.Extras;

import java.io.File;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SyncCameraFolderJobService extends JobService {

    private static final String TAG = RetryUploadJobService.class.getName();

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        //Get local folder images
        String localCameraPath = jobParameters.getExtras().getString(Extras.EXTRA_LOCAL_CAMERA_PATH);

        File cameraFolderFiles[] = new File[0];

        if (localCameraPath != null) {
            File cameraFolder = new File(localCameraPath);
            cameraFolderFiles = cameraFolder.listFiles();
        }

        jobFinished(jobParameters, false);  // done here, real job was delegated to another castle
        return true;    // TODO or false? what is the real effect, Google!?!?!?!?
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

}