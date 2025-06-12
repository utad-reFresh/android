package pt.utad.refresh.ui.perfil

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import pt.utad.refresh.ApiService
import pt.utad.refresh.ChangeDisplayNameRequest
import pt.utad.refresh.UpdateProfileRequest
import pt.utad.refresh.ChangePasswordRequest
import pt.utad.refresh.LoginRequest
import pt.utad.refresh.SessionManager

class PerfilViewModel(private val apiService: ApiService) : ViewModel() {
    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile

    private val _passwordChanged = MutableLiveData<Boolean>()
    val passwordChanged: LiveData<Boolean> = _passwordChanged

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    data class UserProfile(
        var displayName: String = "",
        var photoUrl: String = "",
        var email: String = ""
    )

    suspend fun getProfile() {
        try {
            val response = apiService.getProfile()
            if (response.isSuccessful) {
                response.body()?.let {
                    _userProfile.value = it
                }
            } else {
                _error.value = "Failed to load profile"
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    suspend fun updateProfile(displayName: String, photoUrl: String) {
        try {
            val response = apiService.updateProfile(UpdateProfileRequest(displayName, photoUrl))
            if (response.isSuccessful) {
                _userProfile.value = _userProfile.value?.copy(
                    displayName = displayName,
                    photoUrl = photoUrl
                )
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    suspend fun changePasswordAndReAuth(context: Context?, email: String, currentPassword: String, newPassword: String) {
        try {
            val changeResponse = apiService.changePassword(ChangePasswordRequest(currentPassword, newPassword))
            if (changeResponse.isSuccessful) {
                // Re-authenticate with new password
                val loginResponse = apiService.login(LoginRequest(email, newPassword))
                if (loginResponse.isSuccessful) {
                    val newToken = loginResponse.body()?.token
                    if (newToken != null && context != null) {
                        SessionManager(context).saveAuthToken(newToken)
                        _passwordChanged.postValue(true)
                    }

                } else {
                    _error.value = "Password changed, but failed to re-authenticate"
                }
            } else {
                _error.value = "Failed to change password"
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    suspend fun changeDisplayName(displayName: String) {
        try {
            val response = apiService.changeDisplayName(ChangeDisplayNameRequest(displayName))
            if (response.isSuccessful) {
                _userProfile.value = _userProfile.value?.copy(displayName = displayName)
            } else {
                _error.value = "Failed to change display name"
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    suspend fun changePhoto(context: Context, photoUri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(photoUri)
            val bytes = inputStream?.readBytes() ?: throw Exception("Failed to read image")
            val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), bytes)
            val body = MultipartBody.Part.createFormData("photo", "photo.jpg", requestFile)
            val response = apiService.changePhoto(body)
            if (response.isSuccessful) {
                // Atualize o perfil para buscar a nova foto
                getProfile()
            } else {
                _error.value = "Failed to change photo"
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    suspend fun removePhoto() {
        try {
            val response = apiService.removePhoto()
            if (response.isSuccessful) {
                _userProfile.value = _userProfile.value?.copy(photoUrl = "")
            } else {
                _error.value = "Failed to remove photo"
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }
}