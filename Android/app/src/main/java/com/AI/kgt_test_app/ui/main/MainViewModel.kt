package com.AI.kgt_test_app.ui.main

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.AI.kgt_test_app.R
import com.AI.kgt_test_app.api.CrobReqapi
import com.AI.kgt_test_app.api.Reqapi
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import okhttp3.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainViewModelFactory(private val myApplication: Application) : ViewModelProvider.Factory{
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java).newInstance(myApplication)
    }
}

class MainViewModel(private val myApplication: Application) : ViewModel() {


    companion object{
        private val TAG = "MAIN_VIEWMODEL"
        private val SERVER_URL = "http://211.198.5.202:1111/"

        val fileName = "VISION_" + SimpleDateFormat("yyMMdd_HHmm").format(Date())
    }


    fun saveBitmap(bitmap: Bitmap?) : Uri? {
        Log.d(TAG + "_SAVE", "Save Image Start")
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

            val saved = bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            if(!saved) {
                Log.e(TAG + "_SAVE", "Fail to save photo to gallery")
            }

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            Log.d(TAG + "_SAVE", "Save Image End")
            return uri
        } catch (e: Exception) {
            Log.e(TAG + "_SAVE", "error : $e")
            Log.d(TAG + "_SAVE", "Save Image End")
            return null
        }
    }

    fun sendImage(Image: Bitmap, store_path:File): Boolean {
        Log.d(TAG + "_SEND", "Send Image Start")
        val scope = CoroutineScope(Dispatchers.IO)
        var output: Bitmap?
        scope.launch {
            val Image_File = File.createTempFile(fileName, ".png", store_path)
            val out: OutputStream = FileOutputStream(Image_File)
            Image.compress(Bitmap.CompressFormat.PNG, 100, out)

            Log.d(TAG + "_SEND", "File path: ${Image_File.path}")
            val bitmap = File(Image_File.absolutePath)

            var requestBody: RequestBody = RequestBody.create(MediaType.parse("image/png"), bitmap)
            var body = MultipartBody.Part.createFormData("input_image", fileName, requestBody)

            val gson = GsonBuilder()
                    .setLenient()
                    .create()

            val client:OkHttpClient = OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.MINUTES)
                    .readTimeout(2, TimeUnit.MINUTES)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

            val retrofit = Retrofit.Builder()
                    .baseUrl(SERVER_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()

            val server = retrofit.create(Reqapi::class.java)

            server.getImage(body).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d(TAG + "_SEND", "Success Connect")
                    if (response?.isSuccessful) {
                        Log.d(TAG + "_SEND", "Success: ${response.body()}")
                        var responseBody = response.body()!!.byteStream()
                        output = BitmapFactory.decodeStream(responseBody)
                        Log.d(TAG + "_SEND", "Send Image End")
                        saveBitmap(output)
                        bitmap_Save_Message()
                    } else {
                        Log.e(TAG + "_SEND", "Fail 2: ${response.body()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e(TAG + "_SEND", "Fail 1: ${t.message}")
                }
            })
        }

        Log.d(TAG + "_SEND", "Send Image End")
        return true
    }


    fun crop_sendImage(Image: Bitmap, store_path:File, xy:List<String>): Boolean {
        Log.d(TAG + "_SEND", "Send Image Start")
        var output: Bitmap?

        val Image_File = File.createTempFile(fileName, ".png", store_path)
        val out: OutputStream = FileOutputStream(Image_File)
        Image.compress(Bitmap.CompressFormat.PNG, 100, out)

        Log.d(TAG + "_SEND", "File path: ${Image_File.path}")
        val bitmap = File(Image_File.absolutePath)

        var requestBody: RequestBody = RequestBody.create(MediaType.parse("image/png"), bitmap)
        var body = MultipartBody.Part.createFormData("input_image", fileName, requestBody)

        val gson = GsonBuilder()
                .setLenient()
                .create()

        val client:OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

        val retrofit = Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        val server = retrofit.create(CrobReqapi::class.java)

        server.getImage(body, xy[0], xy[1], xy[2], xy[3]).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d(TAG + "_SEND", "Success Connect")
                if (response?.isSuccessful) {
                    Log.d(TAG + "_SEND", "Success: ${response.body()}")
                    var responseBody = response.body()!!.byteStream()
                    output = BitmapFactory.decodeStream(responseBody)
                    Log.d(TAG + "_SEND", "Send Image End")
                    saveBitmap(output)
                    bitmap_Save_Message()
                } else {
                    Log.e(TAG + "_SEND", "Fail 2: ${response.body()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG + "_SEND", "Fail 1: ${t.message}")
            }
        })

        Log.d(TAG + "_SEND", "Send Image End")
        return true
    }


    private val _showSaveMSG = MutableLiveData<Event<Boolean>>()

    val showSaveToast: LiveData<Event<Boolean>> = _showSaveMSG

    fun bitmap_Save_Message() {
        _showSaveMSG.value = Event(true)
    }
}


open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) { // 이벤트가 이미 처리 되었다면
            null // null을 반환하고,
        } else { // 그렇지 않다면
            hasBeenHandled = true // 이벤트가 처리되었다고 표시한 후에
            content // 값을 반환합니다.
        }
    }
}