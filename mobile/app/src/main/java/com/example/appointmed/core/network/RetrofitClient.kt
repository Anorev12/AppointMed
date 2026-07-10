package com.example.appointmed.core.network
import android.content.Context
import com.example.appointmed.features.doctor.api.AvailabilityApiService
import com.example.appointmed.features.auth.api.AuthApiService
import com.example.appointmed.features.admin.api.AdminApiService
import com.example.appointmed.features.patient.api.PatientApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


/**
 * Single shared Retrofit instance for the whole app.
 * Base URL points to the emulator's alias for the host machine's
 * localhost, matching the Spring Boot dev server running on port 8080.
 */
object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    private var retrofit: Retrofit? = null

    fun getInstance(context: Context): Retrofit {
        if (retrofit == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(context.applicationContext))
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    fun getAuthApi(context: Context): AuthApiService {
        return getInstance(context).create(AuthApiService::class.java)
    }
    fun getAvailabilityApi(context: Context): AvailabilityApiService {
        return getInstance(context).create(AvailabilityApiService::class.java)
    }
    fun getAdminApi(context: Context): AdminApiService {
        return getInstance(context).create(AdminApiService::class.java)
    }
    fun getPatientApi(context: Context): PatientApiService {
        return getInstance(context).create(PatientApiService::class.java)
    }
}