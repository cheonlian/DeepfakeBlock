package com.AI.kgt_test_app.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

/* # Reqapi #
   http://SERVER_URL/post로 이미지를 전송후 리턴값을 받아옴
 */
interface Reqapi{
    @Multipart
    @POST("post/")
    fun getImage(@Part Image: MultipartBody.Part): Call<ResponseBody>
}

/* # CrobReqapi #
   http://SERVER_URL/crob로 이미지와 x, y, w, h를 전송후 리턴값을 받아옴
 */
interface CrobReqapi{
    @Multipart
    @POST("crob/")
    fun getImage(@Part Image: MultipartBody.Part, @Part("x") x: String, @Part("y") y: String, @Part("w") w: String, @Part("h") h: String): Call<ResponseBody>
}