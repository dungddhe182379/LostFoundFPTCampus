package com.fptcampus.lostfoundfptcampus.model.api;

import com.fptcampus.lostfoundfptcampus.model.History;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface HistoryApi {
    @GET("api/lostfound/histories")
    Call<ApiResponse<List<History>>> getAllHistories(@Header("Authorization") String token);

    @GET("api/lostfound/histories/{historyId}")
    Call<ApiResponse<History>> getHistoryById(
        @Header("Authorization") String token,
        @Path("historyId") long historyId
    );

    @POST("api/lostfound/histories")
    Call<ApiResponse<History>> createHistory(
        @Header("Authorization") String token,
        @Body History history
    );
}
