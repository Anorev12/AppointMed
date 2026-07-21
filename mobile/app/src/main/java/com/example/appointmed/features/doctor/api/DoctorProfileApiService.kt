package com.example.appointmed.features.doctor.api
import com.example.appointmed.features.doctor.models.DoctorProfileResponse
import com.example.appointmed.features.doctor.models.PasswordChangeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/** Doctor's own account actions — separate from DoctorAppointmentApiService, which is patient-appointment-facing. */
interface DoctorProfileApiService {
    @GET("api/doctor/profile")
    suspend fun getProfile(): Response<DoctorProfileResponse>

    @PUT("api/doctor/profile/password")
    suspend fun changePassword(@Body request: PasswordChangeRequest): Response<Unit>
}