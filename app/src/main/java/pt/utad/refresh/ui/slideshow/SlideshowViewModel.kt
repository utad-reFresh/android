package pt.utad.refresh.ui.slideshow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pt.utad.refresh.ApiService
import pt.utad.refresh.UpdateProfileRequest
import pt.utad.refresh.ChangePasswordRequest

class SlideshowViewModel(private val apiService: ApiService) : ViewModel() {
    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    data class UserProfile(
        var displayName: String = "",
        var photoUrl: String = "",
        var email: String = ""
    )

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

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        try {
            val response = apiService.changePassword(ChangePasswordRequest(currentPassword, newPassword))
            if (!response.isSuccessful) {
                _error.value = "Erro ao alterar senha"
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }
}