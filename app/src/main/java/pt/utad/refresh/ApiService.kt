package pt.utad.refresh

import okhttp3.MultipartBody
import pt.utad.refresh.ui.perfil.PerfilViewModel
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("Account/changeDisplayName")
    suspend fun changeDisplayName(@Body request: ChangeDisplayNameRequest): Response<Unit>

    @Multipart
    @POST("Account/changePhoto")
    suspend fun changePhoto(@Part photo: MultipartBody.Part): Response<Unit>

    @POST("Account/removePhoto")
    suspend fun removePhoto(): Response<Unit>

    @POST("Account/me/ingredient/{ingredientId}")
    suspend fun addOrUpdateIngredient(
        @Path("ingredientId") ingredientId: Int,
        @Body request: UpdateIngredientRequest
    ): Response<Unit>

    @DELETE("Account/me/ingredient/{ingredientId}")
    suspend fun deleteIngredient(
        @Path("ingredientId") ingredientId: Int
    ): Response<Unit>

    @GET("Account/me/ingredient/{ingredientId}")
    suspend fun getIngredient(
        @Path("ingredientId") ingredientId: Int
    ): Response<IngredientResponse>

    @GET("Account/me/ingredients")
    suspend fun getIngredients(): Response<List<IngredientResponse>>

    @GET("Recipe/{id}")
    suspend fun getRecipe(
        @Path("id") id: Int
    ): Response<RecipeResponse>

    @GET("Recipe/search")
    suspend fun searchRecipes(
        @Query("query") query: String?
    ): Response<List<RecipeResponse>>

    @GET("Recipe/ingredients")
    suspend fun getRecipeIngredients(): Response<List<IngredientResponse>>

    @GET("Recipe/search-ingredients")
    suspend fun searchIngredients(
        @Query("query") query: String?
    ): Response<List<IngredientResponse>>

    // In ApiService.kt
    @GET("Product/barcode/{barcode}")
    suspend fun getProductByBarcode(@Path("barcode") barcode: String): Response<ProductWithIngredientsDto>

    @GET("Recipe/list")
    suspend fun getRecipeList(): Response<List<RecipeInListDto>>

    @GET("Recipe/find")
    suspend fun findRecipes(
        @Query("query") ingredients: String
    ): Response<List<RecipeInListDto>>

}

data class RecipeInListDto (
    val id: Int,
    val name: String,
    val imageUrl: String?,
)

data class ProductWithIngredientsDto(
    val barcode: String,
    val productName: String,
    val ingredients: List<IngredientDto>
)

data class IngredientDto(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val quantity: Int,
    val isFavorite: Boolean,
    val expirationDate: String
)