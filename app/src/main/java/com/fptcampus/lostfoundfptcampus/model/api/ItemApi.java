package com.fptcampus.lostfoundfptcampus.model.api;

import com.fptcampus.lostfoundfptcampus.model.LostItem;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ItemApi {
    @GET("api/lostfound/items")
    Call<ApiResponse<List<LostItem>>> getAllItems(@Header("Authorization") String token);

    @GET("api/lostfound/items")
    Call<ApiResponse<List<LostItem>>> getItemsByUserId(
        @Header("Authorization") String token,
        @Query("userId") long userId
    );

    @GET("api/lostfound/items")
    Call<ApiResponse<List<LostItem>>> getItemsByCategory(
        @Header("Authorization") String token,
        @Query("category") String category
    );

    @GET("api/lostfound/items/{itemId}")
    Call<ApiResponse<LostItem>> getItemById(
        @Header("Authorization") String token,
        @Path("itemId") long itemId
    );

    @GET("api/lostfound/items/status/{status}")
    Call<ApiResponse<List<LostItem>>> getItemsByStatus(
        @Header("Authorization") String token,
        @Path("status") String status
    );

    @GET("api/lostfound/items/search")
    Call<ApiResponse<List<LostItem>>> searchItems(
        @Header("Authorization") String token,
        @Query("q") String keyword
    );

    @POST("api/lostfound/items")
    Call<ApiResponse<LostItem>> createItem(
        @Header("Authorization") String token,
        @Body LostItem item
    );

    @PUT("api/lostfound/items/{itemId}")
    Call<ApiResponse<LostItem>> updateItem(
        @Header("Authorization") String token,
        @Path("itemId") long itemId,
        @Body LostItem item
    );

    @DELETE("api/lostfound/items/{itemId}")
    Call<ApiResponse<Void>> deleteItem(
        @Header("Authorization") String token,
        @Path("itemId") long itemId
    );
}
