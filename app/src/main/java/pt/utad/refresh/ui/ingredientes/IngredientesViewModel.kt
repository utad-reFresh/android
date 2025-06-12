package pt.utad.refresh.ui.ingredientes

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import pt.utad.refresh.ApiService
import pt.utad.refresh.IngredientDto

class IngredientesViewModel(private val apiService: ApiService) : ViewModel() {

    private val _ingredients = MutableLiveData<List<IngredientDto>>()
    val ingredients: LiveData<List<IngredientDto>> = _ingredients

    class IngredientesViewModelFactory(
        private val apiService: ApiService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(IngredientesViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return IngredientesViewModel(apiService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    init {
        fetchIngredients()
    }

    public fun fetchIngredients() {
        viewModelScope.launch {
            val response = apiService.getIngredients()
            if (response.isSuccessful) {
                val ingredientResponses = response.body() ?: emptyList()
                val ingredientDtos = ingredientResponses.map { response ->
                    IngredientDto(
                        id = response.id,
                        name = response.name,
                        imageUrl = response.imageUrl,
                        quantity = response.quantity,
                        isFavorite = response.isFavorite,
                        expirationDate = response.expirationDate ?: ""
                    )
                }
                _ingredients.value = ingredientDtos
            } else {
                _ingredients.value = emptyList()
            }
        }
    }
}