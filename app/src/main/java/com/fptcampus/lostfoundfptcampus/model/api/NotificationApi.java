package com.fptcampus.lostfoundfptcampus.model.api;

import com.fptcampus.lostfoundfptcampus.model.Notification;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface NotificationApi {
    @GET("api/lostfound/notifications")
    Call<ApiResponse<List<Notification>>> getAllNotifications(@Header("Authorization") String token);

    @GET("api/lostfound/notifications/unread")
    Call<ApiResponse<List<Notification>>> getUnreadNotifications(@Header("Authorization") String token);

    @GET("api/lostfound/notifications/count")
    Call<ApiResponse<NotificationCountResponse>> getUnreadCount(@Header("Authorization") String token);

    @POST("api/lostfound/notifications")
    Call<ApiResponse<Notification>> createNotification(
        @Header("Authorization") String token,
        @Body Notification notification
    );

    @PUT("api/lostfound/notifications/{notificationId}/read")
    Call<ApiResponse<Void>> markAsRead(
        @Header("Authorization") String token,
        @Path("notificationId") long notificationId
    );

    @PUT("api/lostfound/notifications/read-all")
    Call<ApiResponse<Void>> markAllAsRead(@Header("Authorization") String token);

    @DELETE("api/lostfound/notifications/{notificationId}")
    Call<ApiResponse<Void>> deleteNotification(
        @Header("Authorization") String token,
        @Path("notificationId") long notificationId
    );
}
