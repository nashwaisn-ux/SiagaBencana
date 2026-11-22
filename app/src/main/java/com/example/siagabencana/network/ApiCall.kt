package com.example.siagabencana.network

import com.example.siagabencana.model.ApiResponse
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T?, val message: String) : ApiResult<T>()
    data class Failure(val message: String, val isNetwork: Boolean = false, val code: Int = 400) : ApiResult<Nothing>()
}

suspend fun <T> apiCall(block: suspend () -> Response<ApiResponse<T>>): ApiResult<T> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success) {
                ApiResult.Success(body.data, body.message)
            } else {
                val msg = body?.message ?: "Gagal memproses permintaan"
                ApiResult.Failure(msg, false, response.code())
            }
        } else {
            val errorJson = response.errorBody()?.string()
            val parsedMsg = parseErrorMessage(errorJson, "Gagal terhubung ke server (${response.code()})")
            ApiResult.Failure(parsedMsg, false, response.code())
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        val isNet = e is IOException
        val msg = if (isNet) "Tidak dapat terhubung ke server" else "Terjadi kesalahan: ${e.localizedMessage}"
        ApiResult.Failure(msg, isNet, 0)
    }
}

private fun parseErrorMessage(errorJson: String?, defaultMsg: String): String {
    return try {
        val json = JSONObject(errorJson ?: "")
        val msg = json.optString("message", defaultMsg)
        val errorsObj = json.optJSONObject("errors")
        if (errorsObj != null) {
            val keys = errorsObj.keys()
            val sb = StringBuilder(msg)
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = errorsObj.optJSONArray(key)
                if (arr != null && arr.length() > 0) {
                    sb.append("\n• ").append(arr.getString(0))
                }
            }
            sb.toString()
        } else {
            msg
        }
    } catch (_: Exception) {
        defaultMsg
    }
}
