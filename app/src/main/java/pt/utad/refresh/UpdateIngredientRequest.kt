package pt.utad.refresh

data class UpdateIngredientRequest(
    val quantity: Int,
    val isFavorite: Boolean,
    val expirationDate: String? = null
)