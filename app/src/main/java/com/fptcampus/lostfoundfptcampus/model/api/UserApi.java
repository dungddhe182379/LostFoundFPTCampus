package com.fptcampus.lostfoundfptcampus.model.api;

import com.fptcampus.lostfoundfptcampus.model.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface UserApi {
    @GET("api/lostfound/user/profile")
    Call<ApiResponse<User>> getProfile(@Header("Authorization") String token);

    @PUT("api/lostfound/user/profile")
    Call<ApiResponse<User>> updateProfile(
        @Header("Authorization") String token,
        @Body User user
    );

    @GET("api/lostfound/user/{userId}")
    Call<ApiResponse<User>> getUserById(
        @Header("Authorization") String token,
        @Path("userId") long userId
    );
}
