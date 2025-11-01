package com.fptcampus.lostfoundfptcampus.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.fptcampus.lostfoundfptcampus.R;

/**
 * Helper class to display user-friendly error dialogs
 * Following best practices from lostfound_project_summary.md
 */
public class ErrorDialogHelper {

    public static void showError(Context context, String title, String message) {
        showError(context, title, message, null);
    }

    public static void showError(Context context, String title, String message, Runnable onDismiss) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .setCancelable(false)
                .show();
    }

    public static void showSuccess(Context context, String title, String message, Runnable onDismiss) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .setCancelable(false)
                .show();
    }

    public static void showNetworkError(Context context) {
        showError(context, 
                "Lỗi kết nối", 
                "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng và thử lại.");
    }

    public static void showAuthError(Context context) {
        showError(context, 
                "Lỗi xác thực", 
                "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    }

    public static void showValidationError(Context context, String field) {
        showError(context, 
                "Thông tin không hợp lệ", 
                "Vui lòng kiểm tra lại " + field + " và thử lại.");
    }
}
