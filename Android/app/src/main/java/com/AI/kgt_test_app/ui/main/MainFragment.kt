package com.AI.kgt_test_app.ui.main

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.AI.kgt_test_app.R
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()

        val REQUEST_IMAGE_CAPTURE = 1
        val REQUEST_GALLERY_TAKE = 2

        private val TAG = "MAIN_FRAGMENT"

        lateinit var packageManager:PackageManager

        lateinit var imageView:ImageView
        lateinit var camera:Button
        lateinit var gallery:Button
        lateinit var trans:Button

        lateinit var file_path:File
        lateinit var store_path:File
        lateinit var file_Uri:Uri

        var currentPhotoPath: String? = null
        var xy: List<Int> = listOf()

        private lateinit var viewModel: MainViewModel

        @SuppressLint("SimpleDateFormat")
        val fileName = "VISION_" + SimpleDateFormat("yyMMdd_HHmm").format(Date())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(viewModelStore, MainViewModelFactory(this.activity!!.application)).get(
            MainViewModel::class.java)

        imageView = view!!.findViewById(R.id.imageView)
        camera = view!!.findViewById(R.id.camera_btn)
        gallery = view!!.findViewById(R.id.gallery_btn)
        trans = view!!.findViewById(R.id.trans)
        packageManager = this.context!!.packageManager

        store_path = this.context!!.getExternalFilesDir(Environment.DIRECTORY_DCIM)!!


        camera.setOnClickListener {
            dispatchTakePictureIntent()
        }

        gallery.setOnClickListener {
            openGalleryForImage()
        }

        trans.setOnClickListener {
            if (imageView.drawable == null) {
                Log.e(TAG, imageView.toString())
                Toast.makeText(
                    this.activity!!.applicationContext,
                    "No Image",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                Log.d(TAG + "_Crop Click", "Start")
                CropImage.activity(file_Uri).start(context!!, this)
                Log.d(TAG + "_Crop Click", "End")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && file_path.isFile) {
            val bitmap = getImage()
            imageView.setImageBitmap(bitmap)
        }else if (requestCode == REQUEST_GALLERY_TAKE && resultCode == RESULT_OK){
            val bitmap = data?.data
            file_Uri = bitmap!!
            currentPhotoPath = bitmap!!.path
            imageView.setImageURI(bitmap) // handle chosen image
        }else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            Log.d(TAG + "_Crop Activity Result", "Start")
            val result = CropImage.getActivityResult(data)
            Log.d(TAG + "_Crop Activity Result", result.toString())
            if (resultCode == RESULT_OK) {
                xy = listOf(result.cropRect.left, result.cropRect.top, result.cropRect.right, result.cropRect.bottom) // 크롭 좌표
                val realxy = listOf(result.wholeImageRect.left, result.wholeImageRect.top, result.wholeImageRect.right, result.wholeImageRect.bottom) // 사진 원래 좌표
                val str_xy = xy.map { it.toString() }
                val d = imageView.drawable.toBitmap()
                Log.d(TAG + "_TRANS", "Photo path: ${currentPhotoPath}")

                if (viewModel.crop_sendImage(d, store_path, str_xy)) {
                    Toast.makeText(
                        this.activity!!.applicationContext,
                        "Saving... Wait Next Message",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    viewModel.showSaveToast.observe(this, {
                        it.getContentIfNotHandled()?.let {
                            Toast.makeText(
                                this.activity!!.applicationContext,
                                "Save Success!! :)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    Toast.makeText(
                        this.activity!!.applicationContext,
                        "Save Fail...",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
            Log.d(TAG + "_Crop Activity Result", "End")
        }else if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
            Log.e(TAG, "CROP ERROR!!")
    }

    // 권한요청 결과
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG + "_PERMMISION", "Permission: " + permissions[0] + "was " + grantResults[0])
        }else{
            Log.d(TAG + "_PERMMISION","Denies")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val storageDir: File = store_path
        Log.d(TAG + "_CREATETEMP", "Storage Path: ${storageDir.absolutePath}")
        return File.createTempFile(
            fileName, /* prefix */
            ".png", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    // 카메라 열기
    private fun dispatchTakePictureIntent() {
        file_path = createImageFile()
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this.context!!,
                        "com.AI.kgt_test.fileprovider",
                        file_path
                    )
                    Log.d(TAG + "_DISPATCH", "Photo uri: ${photoURI}")
                    file_Uri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    // 갤러리 열기
    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY_TAKE)
    }


    private fun getImage(): Bitmap {
        val options:BitmapFactory.Options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        try {
            val in_Stream:InputStream = FileInputStream(file_path)
            BitmapFactory.decodeStream(in_Stream, null, options)
            in_Stream.close()
        } catch ( e:Exception ){
            e.printStackTrace()
        }

        val im_Options = BitmapFactory.Options()

        return BitmapFactory.decodeFile(file_path.absolutePath, im_Options)
    }
}