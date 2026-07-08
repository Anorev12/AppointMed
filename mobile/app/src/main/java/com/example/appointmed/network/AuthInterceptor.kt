package com.example.appointmed.network

import android.content.Context
import com.example.appointmed.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Automatically attaches "Authorization: Bearer <token>" to every
 * outgoing request, if a token has been saved (i.e. the user is logged in).
 *
 * Requests made before login (e.g. the login/register calls themselves)
 * will simply have no token yet, so no header is added — the backend
 * doesn't require auth for those endpoints anyway.
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = TokenManager(context).getToken()

        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}