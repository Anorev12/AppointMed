package com.example.appointmed.features.patient.api

import com.example.appointmed.features.patient.models.AppointmentApiResponse
import com.example.appointmed.features.patient.models.AppointmentBookRequest
import com.example.appointmed.features.patient.models.AppointmentRescheduleRequest
import com.example.appointmed.features.patient.models.DoctorApiResponse
import com.example.appointmed.features.patient.models.PatientProfileResponse
import com.example.appointmed.features.patient.models.PatientProfileUpdateRequest
import com.example.appointmed.features.patient.models.TimeSlot
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Query

interface PatientApiService {

    @GET("api/doctors")
    suspend fun getDoctors(): Response<List<DoctorApiResponse>>

    @GET("api/doctors/{id}/slots")
    suspend fun getSlots(@Path("id") doctorId: Long, @Query("date") date: String): Response<List<TimeSlot>>

    /** FR-012: status/keyword/from/to are all optional filters for a searchable appointment history. */
    @GET("api/patient/appointments")
    suspend fun getAppointments(
        @Query("status") status: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<AppointmentApiResponse>>

    @POST("api/patient/appointments")
    suspend fun bookAppointment(@Body request: AppointmentBookRequest): Response<AppointmentApiResponse>

    @PUT("api/patient/appointments/{id}/cancel")
    suspend fun cancelAppointment(@Path("id") id: Long): Response<AppointmentApiResponse>

    /** FR-011: move a confirmed appointment to a new date/time with the same doctor. */
    @PUT("api/patient/appointments/{id}/reschedule")
    suspend fun rescheduleAppointment(
        @Path("id") id: Long,
        @Body request: AppointmentRescheduleRequest
    ): Response<AppointmentApiResponse>

    @GET("api/patient/profile")
    suspend fun getProfile(): Response<PatientProfileResponse>

    @PUT("api/patient/profile")
    suspend fun updateProfile(@Body request: PatientProfileUpdateRequest): Response<PatientProfileResponse>
}