package com.AI.kgt_test_app.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

/* # Reqapi #
   http://SERVER_URL/post로 이미지를 전송후 리턴값(attack 이미지)을 받아옴
 */
interface Reqapi{
    @Multipart
    @POST("post/")
    fun getImage(@Part Image: MultipartBody.Part): Call<ResponseBody>
}

/* # CrobReqapi #
   http://SERVER_URL/crob로 이미지와 x, y, w, h를 전송후 리턴값(attack 이미지)을 받아옴
 */
interface CrobReqapi{
    @Multipart
    @POST("crop/")
    fun getImage(@Part Image: MultipartBody.Part, @Part("x") x: Float, @Part("y") y: Float, @Part("w") w: Float, @Part("h") h: Float): Call<ResponseBody>
}

/* # ReqPreviewapi #
   http://SERVER_URL/preview로 noise값(int)을 전송후 리턴값(preview 이미지)을 받아옴
 */
interface ReqPreviewapi{
    @POST("preview/")
    fun getImage(@Query("noise") noise:Int): Call<ResponseBody>
}
