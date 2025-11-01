package com.fptcampus.lostfoundfptcampus.model.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("api/lostfound/auth/register")
    Call<ApiResponse<LoginResponse>> register(@Body RegisterRequest request);

    @POST("api/lostfound/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);
}
