package com.learn.learnworkmanager

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.work.*
import coil.compose.rememberImagePainter
import com.learn.learnworkmanager.ui.theme.LearnWorkManagerTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("UnrememberedMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val downloadImageRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        val colorFilterRequest = OneTimeWorkRequestBuilder<ColorFilterWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        val workManager = WorkManager.getInstance(applicationContext)

        setContent {
            LearnWorkManagerTheme(false) {
                //Will get the info of all our worker as LiveData.
                val workInfos = workManager.getWorkInfosForUniqueWorkLiveData("download")
                    .observeAsState()
                    .value

                val downloadInfo = remember(key1 = workInfos) {
                    workInfos?.find { it.id == downloadImageRequest.id }
                }

                val colorFilterInfo = remember(key1 = workInfos) {
                    workInfos?.find { it.id == colorFilterRequest.id }
                }

                val imageUri by derivedStateOf {
                    val downloadUri = downloadInfo?.outputData?.getString(WorkerKeys.IMAGE_URI)?.toUri()
                    val filterUri = colorFilterInfo?.outputData?.getString(WorkerKeys.FILTER_URI)?.toUri()

                    filterUri ?: downloadUri
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    imageUri?.let { uri ->
                        Image(
                            painter = rememberImagePainter(data = uri), contentDescription = "",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Button(
                        onClick = {
                            workManager.beginUniqueWork("download", ExistingWorkPolicy.KEEP, downloadImageRequest)
                                .then(colorFilterRequest)
                                .enqueue()
                        },
                        enabled = downloadInfo?.state != WorkInfo.State.RUNNING
                    ) {
                        Text(text = "Start Download")
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    when (downloadInfo?.state) {
                        WorkInfo.State.RUNNING -> Text(text = "Downloading...")
                        WorkInfo.State.ENQUEUED -> Text(text = "Download Enqueued")
                        WorkInfo.State.SUCCEEDED -> Text(text = "Download Succeeded")
                        WorkInfo.State.FAILED -> Text(text = "Download Failed")
                        WorkInfo.State.BLOCKED -> Text(text = "Download Blocked")
                        WorkInfo.State.CANCELLED -> Text(text = "Download Cancelled")
                        else -> {}
                    }

                    when (colorFilterInfo?.state) {
                        WorkInfo.State.RUNNING -> Text(text = "Applying Filter...")
                        WorkInfo.State.ENQUEUED -> Text(text = "Filter Enqueued")
                        WorkInfo.State.SUCCEEDED -> Text(text = "Filter Succeeded")
                        WorkInfo.State.FAILED -> Text(text = "Filter Failed")
                        WorkInfo.State.BLOCKED -> Text(text = "Filter Blocked")
                        WorkInfo.State.CANCELLED -> Text(text = "Filter Cancelled")
                        else -> {}
                    }

                }
            }
        }
    }
}
