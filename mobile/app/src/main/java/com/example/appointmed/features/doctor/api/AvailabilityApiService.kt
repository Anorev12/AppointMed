package com.example.appointmed.features.doctor.api
import com.example.appointmed.features.doctor.models.AvailabilityResponse
import com.example.appointmed.features.doctor.models.UnavailableDateRequest
import com.example.appointmed.features.doctor.models.UnavailableDatesResponse
import com.example.appointmed.features.doctor.models.UpdateScheduleRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface AvailabilityApiService {

    @GET("api/doctor/availability")
    suspend fun getAvailability(): Response<AvailabilityResponse>

    @PUT("api/doctor/availability")
    suspend fun updateSchedule(@Body request: UpdateScheduleRequest): Response<AvailabilityResponse>

    @retrofit2.http.POST("api/doctor/availability/unavailable-dates")
    suspend fun addUnavailableDate(@Body request: UnavailableDateRequest): Response<UnavailableDatesResponse>

    @DELETE("api/doctor/availability/unavailable-dates/{date}")
    suspend fun removeUnavailableDate(@Path("date") date: String): Response<UnavailableDatesResponse>
}