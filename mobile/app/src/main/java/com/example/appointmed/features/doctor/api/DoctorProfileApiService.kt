package com.example.appointmed.features.doctor.api
import com.example.appointmed.features.doctor.models.PasswordChangeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT

/** Doctor's own account actions — separate from DoctorAppointmentApiService, which is patient-appointment-facing. */
interface DoctorProfileApiService {
    @PUT("api/doctor/profile/password")
    suspend fun changePassword(@Body request: PasswordChangeRequest): Response<String>
}
