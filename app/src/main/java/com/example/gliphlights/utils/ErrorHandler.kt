package com.example.gliphlights.utils

import android.content.Context
import com.example.gliphlights.R

object ErrorHandler {

    fun getSdkUnavailableMessage(context: Context): String {
        return context.getString(R.string.error_sdk_unavailable)
    }

    fun getUnsupportedDeviceMessage(context: Context): String {
        return context.getString(R.string.error_unsupported_device)
    }

    fun getPermissionDeniedMessage(context: Context): String {
        return context.getString(R.string.error_permission_denied)
    }

    fun getRuntimeErrorMessage(context: Context, exception: Throwable): String {
        return when {
            exception.message?.contains("SDK") == true ->
                "Glyph SDK error: ${exception.message}"
            exception.message?.contains("session") == true ->
                "Session error: ${exception.message}"
            exception.message?.contains("register") == true ->
                "Registration error: ${exception.message}"
            else ->
                context.getString(R.string.error_runtime)
        }
    }

    fun isDeviceSupported(): Boolean {
        // TODO: Use actual Common.is*() methods
        // return Common.is24111() || Common.is20111() || Common.is22111() ||
        //        Common.is23111() || Common.is23113() || Common.is25111()
        return true // Default to true for development
    }

    fun getDeviceModel(): String {
        // TODO: Use actual Common.is*() methods
        // when {
        //     Common.is24111() -> return "Nothing Phone (3a) Pro"
        //     Common.is20111() -> return "Nothing Phone (1)"
        //     Common.is22111() -> return "Nothing Phone (2)"
        //     Common.is23111() -> return "Nothing Phone (2a)"
        //     Common.is23113() -> return "Nothing Phone (2a) Plus"
        //     Common.is25111() -> return "Nothing Phone (4a)"
        // }
        return "Nothing Phone (3a) Pro"
    }
}
