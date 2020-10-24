package si.virag.scopeddownloadtest

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import si.virag.scopeddownloadtest.databinding.ActivityMainBinding

/** This is a small proof-of-concept activity which downloads a PDF file and allows it to be shared. */
class MainActivity : AppCompatActivity() {
    private val DOWNLOAD_URL: String =
        "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"

    lateinit var binding: ActivityMainBinding
    lateinit var downloadManager: DownloadManager
    lateinit var receiver: DownloadSuccessfulReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.downloadButton.setOnClickListener { startDownload() }
        setContentView(binding.root)

        downloadManager = getSystemService(DownloadManager::class.java)
    }

    override fun onStart() {
        super.onStart()

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        receiver = DownloadSuccessfulReceiver()
        registerReceiver(receiver, filter)
        checkRequestWriteExternalStoragePermission()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receiver)
    }

    private fun checkRequestWriteExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return
        }
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
    }

    /** Triggers a download to external storage. */
    private fun startDownload() {
        val request = DownloadManager.Request(Uri.parse(DOWNLOAD_URL))
            .setDescription("This is a file")
            .setTitle("This is the title")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setMimeType("application/pdf")
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "downloaded_file.pdf"
            )
        var downloadId = downloadManager.enqueue(request)
        Log.d("Download", "Download started for ID $downloadId")
    }

    /** Opens share dialog to reshare downloaded file. */
    private fun downloadCompleted(downloadId: Long) {
        Log.d("Download", "Download complete for id $downloadId")
        // It's **IMPORTANT** what we use the Uri returned to us by the DownloadManager on
        // Q+. We have permission to access this Uri, but not the files themselves!
        // Note that this permission likely will not outlive this Activity.
        val downloadedContentUri = downloadManager.getUriForDownloadedFile(downloadId)
        Log.d("Download", "Downloaded content URI is $downloadedContentUri")
        val share = Intent(Intent.ACTION_SEND)
        share.putExtra(Intent.EXTRA_STREAM, downloadedContentUri)
        share.type = downloadManager.getMimeTypeForDownloadedFile(downloadId)
        // It is **IMPORTANT** that we set this flag which will transitively give permission
        // to read our Uri to the application that will receive this intent.
        share.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(Intent.createChooser(share, "Share PDF"))
    }

    /** Receives notification when download is complete. */
    inner class DownloadSuccessfulReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i("Download", "Downloaded: $intent")
            val downloadId = intent?.extras?.getLong(DownloadManager.EXTRA_DOWNLOAD_ID)
            downloadCompleted(downloadId!!)
        }
    }
}