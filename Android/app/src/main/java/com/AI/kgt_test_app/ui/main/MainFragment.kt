package com.AI.kgt_test_app.ui.main

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
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.AI.kgt_test_app.R
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
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

        var currentPhotoPath: String? = null

        private lateinit var viewModel: MainViewModel

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


        camera.setOnClickListener {
            dispatchTakePictureIntent()
        }

        gallery.setOnClickListener {
            openGalleryForImage()
        }

        trans.setOnClickListener {
            if (imageView.drawable == null){
                Log.e(TAG, imageView.toString())
                Toast.makeText(this.activity!!.applicationContext, "No Image", Toast.LENGTH_SHORT)
                    .show()
            }
            else {
                Log.d(TAG + "_TRANS", "Photo path: $currentPhotoPath")
                if (viewModel.saveBitmap(viewModel.sendImage(currentPhotoPath!!)) == null){
                    Toast.makeText(this.activity!!.applicationContext, "Save Fail...", Toast.LENGTH_SHORT)
                        .show()
                }else {
                    Toast.makeText(this.activity!!.applicationContext, "Saved", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && file_path.isFile) {
            val bitmap = getImage()
            imageView.setImageBitmap(bitmap)
        }else if (requestCode == REQUEST_GALLERY_TAKE && resultCode == RESULT_OK){
            currentPhotoPath = data?.data!!.path
            imageView.setImageURI(data?.data) // handle chosen image
        }
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
        val storageDir: File = this.context!!.getExternalFilesDir(Environment.DIRECTORY_DCIM)!!
        Log.d(TAG + "_CREATETEMP", "Storage Path: ${storageDir.absolutePath.toString()}")
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
                    Log.d(TAG + "_DISPATCH", "Photo uri: ${photoURI.toString()}")
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