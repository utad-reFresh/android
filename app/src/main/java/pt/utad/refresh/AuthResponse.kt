package pt.utad.refresh

data class AuthResponse(
    val token: String,
    val user: User
)