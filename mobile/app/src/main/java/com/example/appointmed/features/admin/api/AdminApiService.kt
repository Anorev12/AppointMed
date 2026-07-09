package com.example.appointmed.features.admin.api
import com.example.appointmed.features.admin.models.DoctorCreateRequest
import com.example.appointmed.features.admin.models.DoctorResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AdminApiService {

    @POST("api/admin/doctors")
    suspend fun createDoctor(@Body request: DoctorCreateRequest): Response<DoctorResponse>
}