package pt.utad.refresh

import pt.utad.refresh.ui.perfil.PerfilViewModel
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @POST("Account/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("Account/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("Account/changePassword")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    @POST("Account/changeUserData")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<Unit>

    @GET("Account/me")
    suspend fun getProfile(): Response<PerfilViewModel.UserProfile>
}