package com.example.appointmed.features.admin.api
import com.example.appointmed.features.admin.models.AdminAppointmentApiResponse
import com.example.appointmed.features.admin.models.AdminPatientApiResponse
import com.example.appointmed.features.admin.models.DoctorCreateRequest
import com.example.appointmed.features.admin.models.DoctorResponse
import com.example.appointmed.features.admin.models.DoctorStatusUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AdminApiService {

    @POST("api/admin/doctors")
    suspend fun createDoctor(@Body request: DoctorCreateRequest): Response<DoctorResponse>

    @GET("api/admin/doctors")
    suspend fun listDoctors(): Response<List<DoctorResponse>>

    @PUT("api/admin/doctors/{id}/status")
    suspend fun updateDoctorStatus(
        @Path("id") id: Long,
        @Body request: DoctorStatusUpdateRequest
    ): Response<DoctorResponse>

    @GET("api/admin/patients")
    suspend fun listPatients(): Response<List<AdminPatientApiResponse>>

    @GET("api/admin/patients/{id}/appointments")
    suspend fun patientHistory(@Path("id") id: Long): Response<List<AdminAppointmentApiResponse>>

    @GET("api/admin/appointments")
    suspend fun listAppointments(): Response<List<AdminAppointmentApiResponse>>

    @PUT("api/admin/appointments/{id}/cancel")
    suspend fun overrideCancel(@Path("id") id: Long): Response<AdminAppointmentApiResponse>
}
