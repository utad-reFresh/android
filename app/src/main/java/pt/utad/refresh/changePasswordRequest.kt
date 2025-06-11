package pt.utad.refresh

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)