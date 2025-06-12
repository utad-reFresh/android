package pt.utad.refresh

data class RecipeResponse(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val summary: String?,
    val steps: List<RecipeStepDto>,
    val ingredients: List<IngredientDto>,
    val equipment: List<EquipmentDto>
)

data class RecipeStepDto(
    val number: Int,
    val step: String
)


data class EquipmentDto(
    val id: Int,
    val name: String,
    val imageUrl: String?
)