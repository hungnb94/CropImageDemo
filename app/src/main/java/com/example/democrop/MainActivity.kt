package com.example.democrop

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(), CoroutineScope {

    companion object {
        private const val TAG = "DemoCameraCrop"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 1203
        private const val REQUEST_IMAGE_CAPTURE = 204
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private var jobCrop: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main


    private lateinit var drawing: CropView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        var btn = findViewById<Button>(R.id.take_image)
        var btnCrop = findViewById<Button>(R.id.crop_image)
        drawing = findViewById(R.id.view_drawing)
      var  r : ImageView = findViewById(R.id.view)



        btn.setOnClickListener {
            requestPermissions()
        }

        btnCrop.setOnClickListener {
            var bm = drawing.getImage()
            Log.e("AMBE1203","bitmap: "+ bm.height)
            drawing.visibility = View.GONE
            r.visibility = View.VISIBLE
            val d = BitmapDrawable(resources,bm)
            r.setImageDrawable(d)
   //         showDialog("Image crop")
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDialog(title: String) {
        val dialog = Dialog(this@MainActivity)
        val view = layoutInflater.inflate(R.layout.layout_result, null)
        dialog.setCancelable(false)
        dialog.setContentView(view)


        val back = view.findViewById<Button>(R.id.btn_back)
        val img = view.findViewById<ImageView>(R.id.img_result)
        Log.e("AMBE1203", drawing.getImage().toString())
        img.setImageBitmap(drawing.getImage())
        back.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

    }

    private fun requestPermissions() {
        if (allPermissionsGranted()) {
            dispatchTakePictureIntent()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri? = FileProvider.getUriForFile(
                        this,
                        "com.example.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (resultCode == RESULT_OK && requestCode == REQUEST_IMAGE_CAPTURE) {
            drawing.clear()
            var bitmap: Bitmap? = null
            bitmap = decodeSampledBitmapFromPath(currentPhotoPath, true)
            drawing.setBitmap(bitmap!!)
            drawing.setImageBitmap(bitmap)


        }
    }

    private fun decodeSampledBitmapFromPath(
        path: String?,
        isFullScreen: Boolean
    ): Bitmap? {
        var bmp: Bitmap? = null
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        if (isFullScreen) options.inSampleSize = calculateInSampleSize(
            options,
            getScreenWidth(),
            getScreenHeight()
        ) else options.inSampleSize = calculateInSampleSize(options, 200, 200)

        options.inJustDecodeBounds = false
        bmp = BitmapFactory.decodeFile(path, options)
        return bmp
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int, reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            inSampleSize = if (width > height) {
                (height.toFloat() / reqHeight.toFloat()).roundToInt()
            } else {
                (width.toFloat() / reqWidth.toFloat()).roundToInt()
            }
        }
        return inSampleSize
    }

    @SuppressLint("NewApi")
    fun getScreenHeight(): Int {
        val display =
            (this.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay
        var height = 0
        height = if (Build.VERSION.SDK_INT >= 13) {
            val size = Point()
            display.getSize(size)
            size.y
        } else {
            display.height
        }
        return height
    }

    @SuppressLint("NewApi")
    fun getScreenWidth(): Int {
        val display =
            (this.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay
        var width = 0
        width = if (Build.VERSION.SDK_INT >= 13) {
            val size = Point()
            display.getSize(size)
            size.x
        } else {
            display.width
        }
        return width
    }


    override fun onDestroy() {
        super.onDestroy()
        jobCrop.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

}