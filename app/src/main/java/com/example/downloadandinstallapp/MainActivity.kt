package com.example.downloadandinstallapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.*


class MainActivity : AppCompatActivity() {

    private val LOG_TAG = "MainActivity"
    private val EXTRA_KEY_DATA = "data"
    private var directoryPath: String? = null
    private var filePath: String? = null
    private var file: File? = null
    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dirPath = getExternalFilesDir(null)!!.path
        directoryPath = dirPath + File.separator + "downloads"
        filePath = directoryPath + File.separator + "app.apk"

        file = File(directoryPath)
        if (!file!!.exists()) {
            file!!.mkdirs()
        }

        uri = Uri.parse("file://$filePath")

        file = File(filePath)
        if (file!!.exists()) {
            file!!.delete()
        }


        val intent = Intent(this, BackgroundService::class.java)
        val newThread = Thread {
            try {
                intent.putExtra(
                    EXTRA_KEY_DATA, downloadAndSaveFile(
                        "X.X.X.X",
                        XX,
                        "FTPUser",
                        "XXXXXXXXXXXXXXXXXXX",
                        "app.apk", file!!
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        newThread.start()
        startService(intent)
    }

    @Throws(IOException::class)
    private fun downloadAndSaveFile(
        server: String, portNumber: Int,
        user: String, password: String, filename: String, localFile: File
    ): Boolean? {
        var ftp: FTPClient? = null
        return try {
            Looper.prepare()
            Handler(Looper.getMainLooper()).postDelayed({
                textView.text = "Connecting"
                progressBar.visibility = View.VISIBLE
                textView.visibility = View.VISIBLE
            }, 0)
            ftp = FTPClient()
            ftp.connect(server, portNumber)


            Handler(Looper.getMainLooper()).postDelayed({
                textView.text = "Logged in"
            }, 1000)
            ftp.login(user, password)

            Handler(Looper.getMainLooper()).postDelayed({
                textView.text = "Downloading"
            }, 3000)
            ftp.setFileType(FTP.BINARY_FILE_TYPE)

            ftp.enterLocalPassiveMode()
            var outputStream: OutputStream? = null
            var success = false
            try {
                outputStream = BufferedOutputStream(FileOutputStream(file!!.path))
                success = ftp.retrieveFile(filename, outputStream)

                Log.d(LOG_TAG, "success: $success")

                Toast.makeText(this, "installing", Toast.LENGTH_SHORT).show()
                installAPK()
                finishAffinity()
                Process.killProcess(Process.myPid())
            } finally {
                outputStream?.close()
            }
            success
        } finally {
            if (ftp != null) {
                Toast.makeText(this, "Downloading Failed", Toast.LENGTH_SHORT).show()
                finish()
                ftp.logout()
                ftp.disconnect()
            }
        }
    }

    private fun installAPK() {
        val file = File(filePath)
        if (file.exists()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(
                uriFromFile(this, File(filePath)),
                "application/vnd.android.package-archive"
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                this.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                Log.e("TAG", "Error in opening the file!")
            }
        } else {
            Toast.makeText(this, "installing", Toast.LENGTH_LONG).show()
        }
    }

    private fun uriFromFile(context: Context?, file: File?): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context?.let {
                FileProvider.getUriForFile(
                    it, BuildConfig.APPLICATION_ID + ".provider",
                    file!!
                )
            }
        } else {
            Uri.fromFile(file)
        }
    }
}
