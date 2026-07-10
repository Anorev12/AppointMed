package com.example.appointmed.features.doctor.api

import com.example.appointmed.features.doctor.models.DoctorAppointmentApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface DoctorAppointmentApiService {

    @GET("api/doctor/appointments")
    suspend fun getAppointments(): Response<List<DoctorAppointmentApiResponse>>

    @PUT("api/doctor/appointments/{id}/cancel")
    suspend fun cancelAppointment(@Path("id") id: Long): Response<DoctorAppointmentApiResponse>

    @PUT("api/doctor/appointments/{id}/complete")
    suspend fun completeAppointment(@Path("id") id: Long): Response<DoctorAppointmentApiResponse>
}