package pt.utad.refresh

data class RecipeResponse(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val summary: String,
    val steps: List<RecipeStepDto>,
    val ingredients: List<IngredientDto>,
    val ingredientDets: List<IngredientDetsDto>,
    val equipment: List<EquipmentDto>,
    val sourceUrl: String?,
    val spoonacularSourceUrl: String,
    val vegetarian: Boolean?,
    val vegan: Boolean?,
    val glutenFree: Boolean?,
    val dairyFree: Boolean?,
    val veryHealthy: Boolean?,
    val cheap: Boolean?,
    val veryPopular: Boolean?,
    val sustainable: Boolean?,
    val lowFodmap: Boolean?,
    val preparationMinutes: Int?,
    val cookingMinutes: Int?,
    val readyInMinutes: Int?,
    val servings: Int?,
    val healthScore: Double?,
    val aggregateLikes: Int?,
    val weightWatcherSmartPoints: Int?,
    val creditsText: String?,
    val sourceName: String?,
    val cuisines: List<String>?,
    val dishTypes: List<String>?,
    val diets: List<String>?,
    val occasions: List<String>?,
    val spoonacularScore: Double?,
    val lastUpdated: String
)

data class IngredientDetsDto(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val amountMetric: Double,
    val unitShortMetric: String,
    val unitLongMetric: String,
    val amountImperial: Double,
    val unitShortImperial: String,
    val unitLongImperial: String
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