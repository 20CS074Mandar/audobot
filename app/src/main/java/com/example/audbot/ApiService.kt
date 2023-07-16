package com.example.audbot

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @GET("/audiophp/index.php")
    fun getSampleData(): Call<SampleData>

    @Multipart
    @POST("/audiophp/audioupload.php")
    fun uploadAudioFile(@Part audioFile: MultipartBody.Part): Call<ResponseBody>

}