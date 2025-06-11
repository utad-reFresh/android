package pt.utad.refresh

import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.Response

interface ApiService {
    @POST("Account/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("Account/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}