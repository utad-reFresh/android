package pt.utad.refresh

data class RegisterRequest(
    val displayName: String,
    val userName: String,
    val email: String,
    val password: String
)