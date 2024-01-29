package com.example.sample

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import com.example.sample.ConstantHelpers.CHOOSE_FILE
import com.example.sample.ConstantHelpers.CREATE_FILE_REQUEST_CODE
import com.example.sample.ConstantHelpers.OPEN_FILE_REQUEST_CODE
import com.example.sample.ConstantHelpers.OPEN_FOLDER_REQUEST_CODE
import com.example.sample.ConstantHelpers.READ_EXTERNAL_STORAGE_PERMISSION
import com.example.sample.ConstantHelpers.downloadUrl
import com.example.sample.databinding.ActivityMainBinding
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    lateinit var mContentViewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContentViewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mContentViewBinding.root)
        clickListeners()
    }


    private fun clickListeners() {
        mContentViewBinding.createFileBt.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (permissionToReadWrite) {
                    createFile()
                } else {
                    permissionForReadWrite()
                }
            } else {
                createFile()
            }
        }

        mContentViewBinding.openFileBt.setOnClickListener {
            openFile()
        }

        mContentViewBinding.openFolderBt.setOnClickListener {
            openFolder()
        }

        mContentViewBinding.downloadImageDownloadBt.setOnClickListener {
            downloadImage()
        }
        mContentViewBinding.downloadImageAppFolderBt.setOnClickListener {
            downloadImageToAppFolder()
        }
    }

    private fun createFile() {

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TITLE, "Test.txt")
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE)
    }

    private fun writeFileContent(uri: Uri?) {
        try {
            val file = uri?.let { this.contentResolver.openFileDescriptor(it, "w") }

            file?.let {
                val fileOutputStream = FileOutputStream(
                    it.fileDescriptor
                )
                val textContent = "This is the dummy text."

                fileOutputStream.write(textContent.toByteArray())

                fileOutputStream.close()
                it.close()
            }

        } catch (e: FileNotFoundException) {
//print logs
        } catch (e: IOException) {
//print logs
        }

    }

    private fun downloadImage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (permissionToReadWrite) {
                downloadImageToDownloadFolder()
            } else {
                permissionForReadWrite()
            }

        } else {
            downloadImageToDownloadFolder()
        }
    }

    private fun downloadImageToAppFolder() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (permissionToReadWrite) {
                downloadToAppFolder()
            } else {
                permissionForReadWrite()
            }

        } else {
            downloadToAppFolder()
        }
    }

    //Downloading file to Internal Folder
    private fun downloadToAppFolder() {
        try {
            val file = File(
                this.getExternalFilesDir(
                    null
                ), "test2.png"
            )

            if (!file.exists())
                file.createNewFile()

            var fileOutputStream: FileOutputStream? = null

            fileOutputStream = FileOutputStream(file)
            val bitmap = (ContextCompat.getDrawable(this, R.drawable.test) as BitmapDrawable).bitmap

            bitmap?.compress(Bitmap.CompressFormat.PNG, 80, fileOutputStream)
            Toast.makeText(
                applicationContext,
                getString(R.string.download_successful) + file.absolutePath,
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var permissionToReadWrite: Boolean = false
        get() {
            val permissionGrantedResult: Int = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            return permissionGrantedResult == PackageManager.PERMISSION_GRANTED
        }

    //Request Permission For Read Storage
    private fun permissionForReadWrite() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ), READ_EXTERNAL_STORAGE_PERMISSION
        )
    }

    private fun downloadImageToDownloadFolder() {
        val mgr = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = Uri.parse(downloadUrl)
        val request = DownloadManager.Request(
            downloadUri
        )
        request.setAllowedNetworkTypes(
            DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
        )
            .setAllowedOverRoaming(false).setTitle("Image Sample")
            .setDescription("Testing")
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "test1.jpg"
            )

        Toast.makeText(
            applicationContext,
            "Downloaded successfully to ${downloadUri?.path}",
            Toast.LENGTH_LONG
        ).show()

        mgr.enqueue(request)

    }

    private fun openFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
        startActivityForResult(intent, OPEN_FOLDER_REQUEST_CODE)
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//if you want to open PDF file
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
//Adding Read URI permission
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_FILE_REQUEST_CODE) {
                data?.data?.also { documentUri ->
//Permission needed if you want to retain access even after reboot
                    contentResolver.takePersistableUriPermission(
                        documentUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    Toast.makeText(this, documentUri.path.toString(), Toast.LENGTH_LONG).show()
                }
            } else if (requestCode == OPEN_FOLDER_REQUEST_CODE) {
                val directoryUri = data?.data ?: return

//Taking permission to retain access
                contentResolver.takePersistableUriPermission(
                    directoryUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
//Now you have access to the folder, you can easily view the content or do whatever you want.
                val documentsTree = DocumentFile.fromTreeUri(application, directoryUri) ?: return
                val childDocuments = documentsTree.listFiles().asList()
                Toast.makeText(
                    this,
                    "Total Items Under this folder =" + childDocuments.size.toString(),
                    Toast.LENGTH_LONG
                ).show()

            } else if (requestCode == CHOOSE_FILE)
            else if (requestCode == CREATE_FILE_REQUEST_CODE) {
                if (data != null) {
                    writeFileContent(data.data)
                }

            }
        }
    }
}