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
import com.AI.kgt_test_app.api.*
import com.google.gson.GsonBuilder
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
    /*  # companion object #
        ## variable ##
        SERVER_URL = api 서버 주소

        fileName = Target File 이름
     */
    companion object{
        private val TAG = "MAIN_VIEWMODEL"
        private val SERVER_URL = "http://18.221.61.105:8000/"

        val fileName = "VISION_" + SimpleDateFormat("yyMMdd_HHmm").format(Date())
    }

    /*  # saveBitmap #
        bitmap을 받아 갤러리에 저장하는 함수

        ## variable ##
        relativePath = 파일을 저장할 갤러리 Path
        mimeType = 파일 타입
        values = 파일에 들어갈 정보들
     */
    fun Save_Bitmap(bitmap: Bitmap?) : Uri? {
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

    /*  # send_Image #
        Image와 저장 경로를 받아 API에 전송 후 결과를 save_Bitmap의 인자로 호출
        성공 여부를 return

        ## variable ##
        isSuccess = 성공 여부

        ImageFile = 보낼 이미지를 임시 저장할 파일
        out = 이미지를 만들기 위한 OutputStream
        bitmap = 보낼 Image 파일

        requestBody = 보낼 Image를 담은 requestbody
        body = body
        gson = gson을 사용하기 위한 빌더
        client = 응답 시간 조절을 위한 client
        retrofit = api주소와 연결된 retrofit
        server = api주소로 요청할 api를 불러옴

        ## function ##
        server.getImage(body)
        api.kt -> Reqapi -> getImage
     */
    fun Send_Image(Image: Bitmap, store_path: File): Boolean {
        Log.d(TAG + "_SEND", "Send Image Start")
        var output: Bitmap?
        var isSuccess = false

        val ImageFile = File.createTempFile(fileName, ".png", store_path)
        val out: OutputStream = FileOutputStream(ImageFile)
        Image.compress(Bitmap.CompressFormat.PNG, 100, out)

        Log.d(TAG + "_SEND", "File path: ${ImageFile.path}")
        val bitmap = File(ImageFile.absolutePath)

        var requestBody: RequestBody = RequestBody.create(MediaType.parse("image/png"), bitmap)
        var body = MultipartBody.Part.createFormData("input_image", fileName, requestBody)

        val gson = GsonBuilder()
                .setLenient()
                .create()

        val client:OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
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
                    Save_Bitmap(output)
                    bitmap_Save_Message()
                    isSuccess = true
                } else {
                    Log.e(TAG + "_SEND", "Fail 2: ${response.body()}")
                    bitmap_Save_Message()
                    isSuccess = false
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG + "_SEND", "Fail 1: ${t.message}")
                bitmap_Save_Message()
                isSuccess = false
            }
        })

        Log.d(TAG + "_SEND", "Send Image End")
        return isSuccess
    }

    /*  # crop_send_Image #
        Image와 저장 경로, 크롭범위를 받아 API에 전송 후 결과를 save_Bitmap의 인자로 호출
        성공 여부를 return

        ## variable ##
        isSuccess = 성공 여부

        ImageFile = 보낼 이미지를 임시 저장할 파일
        out = 이미지를 만들기 위한 OutputStream
        bitmap = 보낼 Image 파일

        requestBody = 보낼 Image를 담은 requestbody
        body = body
        gson = gson을 사용하기 위한 빌더
        client = 응답 시간 조절을 위한 client
        retrofit = api주소와 연결된 retrofit
        server = api주소로 요청할 api를 불러옴

        ## function ##
        server.getImage(body, x, y, w, h)
        - api.kt -> CrobReqapi -> getImage
     */
    fun Crop_Send_Image(Image: Bitmap, store_path:File, xy:List<Float>, noise: Int): Boolean {
        Log.d(TAG + "_SEND", "Send Image Start")
        var output: Bitmap?
        var isSuccess = true

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
                .writeTimeout(2, TimeUnit.MINUTES)
                .build()

        val retrofit = Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        val server = retrofit.create(CrobReqapi::class.java)

        Log.d(TAG + "_SEND", "$body, ${xy[0]}, ${xy[1]}, ${xy[2]}, ${xy[3]}, ${noise}")
        server.getImage(body, xy[0], xy[1], xy[2], xy[3], noise).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d(TAG + "_SEND", "Success Connect")
                if (response?.isSuccessful) {
                    Log.d(TAG + "_SEND", "Success: ${response.body()}")
                    var responseBody = response.body()!!.byteStream()
                    output = BitmapFactory.decodeStream(responseBody)
                    Log.d(TAG + "_SEND", "Send Image End")
                    Save_Bitmap(output)
                    MainFragment.temp.setImageBitmap(output)
                    bitmap_Save_Message()
                } else {
                    Log.e(TAG + "_SEND", "Fail 2: ${response.body()}")
                    bitmap_Save_Message()
                    isSuccess = false
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG + "_SEND", "Fail 1: ${t.message}")
                bitmap_Save_Message()
                isSuccess = false
            }
        })

        Log.d(TAG + "_SEND", "Send Image End${isSuccess}")
        return isSuccess
    }


    /* # req_preview
        noise 정도를 입력 받아 preview 요청후 preview return

        ## variable ##
        preview: 이미지 preview

        requestBody = 보낼 Image를 담은 requestbody
        body = body
        gson = gson을 사용하기 위한 빌더
        client = 응답 시간 조절을 위한 client
        retrofit = api주소와 연결된 retrofit
        server = api주소로 요청할 api를 불러옴


        ## function ##
        server.getImage(body, x, y, w, h)
        - api.kt -> ReqPreviewapi -> getImage
     */
    fun Req_BoxPreview(Image:Bitmap, store_path:File, isOri:Boolean): Boolean?{
        var preview: Bitmap? = null
        var isSuccess = false

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
                .writeTimeout(2, TimeUnit.MINUTES)
                .build()

        val retrofit = Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

        val server = retrofit.create(ReqPreviewapi::class.java)

        server.getImage(body).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response?.isSuccessful) {
                    Log.d(TAG + "_SEND", "Success: ${response.body()}")
                    var responseBody = response.body()!!.byteStream()
                    preview = BitmapFactory.decodeStream(responseBody)
                    if (isOri) MainFragment.preViewnt.setImageBitmap(preview)
                    else MainFragment.preViewt.setImageBitmap(preview)
                    isSuccess = true
                } else {
                    Log.e(TAG + "_SEND", "Fail 2: ${response.body()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG + "_SEND", "Fail 1: ${t.message}")
            }
        })

        return isSuccess
    }


    /*  # Connect Fragment and ViewModel#
        Viewmodel에서 Fragment로 Event 호출

        ## variable ##
        _showSaveMSG = Event
        showSaveToast = Event

        ## function ##
        bitmap_Save_Message
        - 이벤트 발생 신호 전송
     */
    private val _showSaveMSG = MutableLiveData<Event<Boolean>>()

    val Show_Save_Toast: LiveData<Event<Boolean>> = _showSaveMSG

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