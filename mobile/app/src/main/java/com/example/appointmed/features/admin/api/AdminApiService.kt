package com.example.appointmed.features.admin.api
import com.example.appointmed.features.admin.models.AdminAppointmentApiResponse
import com.example.appointmed.features.admin.models.AdminCreateRequest
import com.example.appointmed.features.admin.models.AdminPatientApiResponse
import com.example.appointmed.features.admin.models.AdminSimpleResponse
import com.example.appointmed.features.admin.models.DoctorCreateRequest
import com.example.appointmed.features.admin.models.DoctorResponse
import com.example.appointmed.features.admin.models.DoctorStatusUpdateRequest
import com.example.appointmed.features.admin.models.PasswordChangeRequest
import com.example.appointmed.features.admin.models.PatientCreateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApiService {

    @POST("api/admin/doctors")
    suspend fun createDoctor(@Body request: DoctorCreateRequest): Response<DoctorResponse>

    @GET("api/admin/doctors")
    suspend fun listDoctors(@Query("search") search: String? = null): Response<List<DoctorResponse>>

    @PUT("api/admin/doctors/{id}/status")
    suspend fun updateDoctorStatus(
        @Path("id") id: Long,
        @Body request: DoctorStatusUpdateRequest
    ): Response<DoctorResponse>

    @DELETE("api/admin/doctors/{id}")
    suspend fun deleteDoctor(@Path("id") id: Long): Response<String>

    @GET("api/admin/patients")
    suspend fun listPatients(@Query("search") search: String? = null): Response<List<AdminPatientApiResponse>>

    @POST("api/admin/patients")
    suspend fun createPatient(@Body request: PatientCreateRequest): Response<AdminPatientApiResponse>

    @DELETE("api/admin/patients/{id}")
    suspend fun deletePatient(@Path("id") id: Long): Response<String>

    @GET("api/admin/patients/{id}/appointments")
    suspend fun patientHistory(@Path("id") id: Long): Response<List<AdminAppointmentApiResponse>>

    @GET("api/admin/appointments")
    suspend fun listAppointments(): Response<List<AdminAppointmentApiResponse>>

    @PUT("api/admin/appointments/{id}/cancel")
    suspend fun overrideCancel(@Path("id") id: Long): Response<AdminAppointmentApiResponse>

    // ---- Admin management — no delete endpoint exists here on purpose. ----

    @GET("api/admin/admins")
    suspend fun listAdmins(): Response<List<AdminSimpleResponse>>

    @POST("api/admin/admins")
    suspend fun createAdmin(@Body request: AdminCreateRequest): Response<AdminSimpleResponse>

    @PUT("api/admin/password")
    suspend fun changeOwnPassword(@Body request: PasswordChangeRequest): Response<String>
}
