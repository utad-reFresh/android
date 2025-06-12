package pt.utad.refresh

data class IngredientResponse(
    val id: Int,
    val name: String,
    val quantity: Int,
    val isFavorite: Boolean,
    val expirationDate: String?,
    val imageUrl: String,
)