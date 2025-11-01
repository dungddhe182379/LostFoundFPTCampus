package com.fptcampus.lostfoundfptcampus.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
    // Permission request codes
    public static final int REQUEST_LOCATION_PERMISSION = 100;
    public static final int REQUEST_CAMERA_PERMISSION = 101;
    public static final int REQUEST_STORAGE_PERMISSION = 102;

    // Check if location permission is granted
    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Request location permission
    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
            new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            },
            REQUEST_LOCATION_PERMISSION);
    }

    // Check if camera permission is granted
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    // Request camera permission
    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
            new String[]{Manifest.permission.CAMERA},
            REQUEST_CAMERA_PERMISSION);
    }

    // Check if storage permission is granted
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // Request storage permission
    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                },
                REQUEST_STORAGE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(activity,
                new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                REQUEST_STORAGE_PERMISSION);
        }
    }

    // Check if all permissions are granted
    public static boolean hasAllPermissions(Context context) {
        return hasLocationPermission(context) && 
               hasCameraPermission(context) && 
               hasStoragePermission(context);
    }
}
