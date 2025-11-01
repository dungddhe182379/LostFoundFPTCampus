package com.fptcampus.lostfoundfptcampus.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.fptcampus.lostfoundfptcampus.model.api.AuthApi;
import com.fptcampus.lostfoundfptcampus.model.api.ItemApi;
import com.fptcampus.lostfoundfptcampus.model.api.NotificationApi;
import com.fptcampus.lostfoundfptcampus.model.api.UserApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Production URL (deployed to Tomcat root with HTTPS)
    private static final String BASE_URL = "https://vietsuky.com/";
    
    // For local testing, uncomment:
    // private static final String BASE_URL = "http://10.0.2.2:8080/";

    private static Retrofit retrofit = null;
    private static Context appContext;

    // Initialize with application context
    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    // Get Retrofit instance
    public static Retrofit getClient() {
        if (retrofit == null) {
            // Gson configuration
            Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .setLenient()
                .create();

            // OkHttp client with interceptors
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor())
                .addInterceptor(getLoggingInterceptor())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

            // Retrofit builder
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        }
        return retrofit;
    }

    // Logging interceptor for debugging
    private static HttpLoggingInterceptor getLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }

    // Auth interceptor to add JWT token to requests
    private static class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            
            // Skip auth header for login/register endpoints
            String path = originalRequest.url().encodedPath();
            if (path.contains("/auth/login") || path.contains("/auth/register")) {
                return chain.proceed(originalRequest);
            }

            // Get token from SharedPreferences
            String token = getToken();
            
            // Add Authorization header if token exists
            if (token != null && !token.isEmpty()) {
                Request newRequest = originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
                return chain.proceed(newRequest);
            }

            return chain.proceed(originalRequest);
        }

        private String getToken() {
            if (appContext == null) return null;
            SharedPreferences prefs = appContext.getSharedPreferences("LostFoundPrefs", Context.MODE_PRIVATE);
            return prefs.getString("jwt_token", null);
        }
    }

    // API service instances
    public static AuthApi getAuthApi() {
        return getClient().create(AuthApi.class);
    }

    public static UserApi getUserApi() {
        return getClient().create(UserApi.class);
    }

    public static ItemApi getItemApi() {
        return getClient().create(ItemApi.class);
    }

    public static NotificationApi getNotificationApi() {
        return getClient().create(NotificationApi.class);
    }

    // Reset client (useful for logout)
    public static void reset() {
        retrofit = null;
    }
}
