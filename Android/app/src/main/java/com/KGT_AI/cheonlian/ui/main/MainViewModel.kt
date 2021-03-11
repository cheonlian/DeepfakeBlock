package com.KGT_AI.cheonlian.ui.main

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.KGT_AI.cheonlian.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainViewModelFactory(private val myApplication: Application) : ViewModelProvider.Factory{
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java).newInstance(myApplication)
    }
}

class MainViewModel(private val myApplication: Application) : ViewModel() {
    companion object{
        private val TAG = "MAIN_VIEWMODEL"
    }
    fun saveBitmap(bitmap: Bitmap) : Uri? {
        try {
            val relativePath = Environment.DIRECTORY_PICTURES + File.separator + myApplication.getString(R.string.app_name)
            val mimeType = "image/*"
            val fileName = "VISION_" + SimpleDateFormat("yyMMdd_HHmm").format(Date())+".jpg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.IS_PENDING, 1)
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            }
            Log.d(TAG + "_SAVE", fileName)
            Log.d(TAG + "_SAVE", relativePath)

            val resolver = myApplication.contentResolver ?: return null

            val collection = MediaStore.Images.Media
                    .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values)

            if(uri == null) {
                Log.e(TAG + "_SAVE","Failed to create new  MediaStore record.")
                return null
            }

            val outputStream = resolver.openOutputStream(uri)

            if(outputStream == null) {
                Log.e(TAG + "_SAVE", "Failed to get output stream.")
            }

            val saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            if(!saved) {
                Log.e(TAG + "_SAVE","Fail to save photo to gallery")
            }

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            return uri
        } catch (e:Exception) {
            Log.e(TAG + "_SAVE", "error : $e")
            return null
        }
    }
}