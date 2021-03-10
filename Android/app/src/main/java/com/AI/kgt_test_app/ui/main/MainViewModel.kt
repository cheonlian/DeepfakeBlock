package com.AI.kgt_test_app.ui.main

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.AI.kgt_test_app.R
import com.google.gson.GsonBuilder
import okhttp3.*
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
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
        private val SERVER_URL = "http://211.198.5.202:1111/"

        val fileName = "VISION_" + SimpleDateFormat("yyMMdd_HHmm").format(Date())+".png"
    }


    fun saveBitmap(bitmap: Bitmap?) : Uri? {
        if (bitmap == null){
            Log.e(TAG + "_SAVE", "No Image")
            return null
        }
        try {
            val relativePath = Environment.DIRECTORY_PICTURES + File.separator + myApplication.getString(
                    R.string.app_name)
            val mimeType = "image/*"

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
                Log.e(TAG + "_SAVE", "Failed to create new  MediaStore record.")
                return null
            }

            val outputStream = resolver.openOutputStream(uri)

            if(outputStream == null) {
                Log.e(TAG + "_SAVE", "Failed to get output stream.")
            }

            val saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            if(!saved) {
                Log.e(TAG + "_SAVE", "Fail to save photo to gallery")
            }

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            return uri
        } catch (e: Exception) {
            Log.e(TAG + "_SAVE", "error : $e")
            return null
        }
    }


    fun sendImage(Image_Path: String): Bitmap?{
//        val Image = File(Image_Path)
//
//        var requestBody: RequestBody = RequestBody.create(MediaType.parse("image/*"), Image)
//        var body = MultipartBody.Part.createFormData("input_image", fileName, requestBody)

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(SERVER_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

//        Log.d(TAG, "Body: ${Image.canonicalFile}")
//        val server = retrofit.create(Reqapi::class.java)
//        var output:Bitmap? = null
//        server.getImage(body).enqueue(object : Callback<ResponseBody> {
//            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
//                Log.d(TAG + "_SEND", "Success Connect")
//                if (response?.isSuccessful) {
//                    Log.d(TAG + "_SEND", "Success: ${response.body()}")
//                    var responseBody = response.body()!!.byteStream()
//                    output = BitmapFactory.decodeStream(responseBody)
//                } else {
//                    Log.e(TAG + "_SEND", "Fail 2: ${response.body()}")
//                }
//            }
//
//            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                Log.e(TAG + "_SEND", "Fail 1: ${t.message}")
//            }
//        })
//        return output

        val client = OkHttpClient().newBuilder()
                .build()
        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("input_image", "/C:/Users/besto/KGT_AT/PROJECT/data/adv_image.jpg",
                        RequestBody.create(MediaType.parse("application/octet-stream"),
                                File(Image_Path)))
                .build()

        val request: Request = Request.Builder()
                .url("http://211.198.5.202:1111/post/")
                .method("POST", body)
                .build()
        var output:Bitmap? = null
        client.newCall(request).enqueue(object: okhttp3.Callback{
            override fun onResponse(call: Call, response: okhttp3.Response) {
                Log.d(TAG + "_SEND", "Success Connect")
                if (response?.isSuccessful) {
                    Log.d(TAG + "_SEND", "Success: ${response.body()}")
                    var responseBody = response.body()!!.byteStream()
                    output = BitmapFactory.decodeStream(responseBody)
                } else {
                    Log.e(TAG + "_SEND", "Fail 2: ${response.body()!!.byteStream()}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG + "_SEND", "Fail 1: ${e.message}")
            }
        })


        return output
    }
}