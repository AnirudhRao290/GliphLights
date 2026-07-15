package com.example.gliphlights.models

sealed class SdkResult<out T> {
    data class Success<T>(val data: T) : SdkResult<T>()
    data class Error(val exception: Throwable, val message: String) : SdkResult<Nothing>()
}

fun <T> SdkResult<T>.getOrNull(): T? = when (this) {
    is SdkResult.Success -> data
    is SdkResult.Error -> null
}

fun <T> SdkResult<T>.getOrElse(default: T): T = when (this) {
    is SdkResult.Success -> data
    is SdkResult.Error -> default
}

fun <T, R> SdkResult<T>.map(transform: (T) -> R): SdkResult<R> = when (this) {
    is SdkResult.Success -> SdkResult.Success(transform(data))
    is SdkResult.Error -> SdkResult.Error(exception, message)
}
