package com.AI.kgt_test_app.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface Reqapi{
    @POST("post/")
    fun getImage(@Body Image: RequestBody): Call<ResponseBody>
}