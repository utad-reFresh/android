package pt.utad.refresh.ui.receitas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.utad.refresh.ApiClient
import pt.utad.refresh.RecipeInListDto

class ReceitasViewModel : ViewModel() {
    private val _receitas = MutableLiveData<List<RecipeInListDto>>()
    val receitas: LiveData<List<RecipeInListDto>> = _receitas

    private var favoriteIds: Set<Int> = emptySet()

    init {
        fetchReceitas()
    }

    private suspend fun fetchFavoriteIds() {
        val favResponse = ApiClient.apiService.getFavoriteRecipes()
        if (favResponse.isSuccessful) {
            favoriteIds = favResponse.body()?.map { it.id }?.toSet() ?: emptySet()

        } else {
            favoriteIds = emptySet()
        }
    }

    fun fetchReceitas() {
        viewModelScope.launch {
            fetchFavoriteIds()
            val response = ApiClient.apiService.getRecipeList()
            if (response.isSuccessful) {
                val list = response.body() ?: emptyList()
                _receitas.value = list.sortedByDescending { favoriteIds.contains(it.id) }
            } else {
                _receitas.value = emptyList()
            }
        }
    }


    fun setReceitas(list: List<pt.utad.refresh.RecipeInListDto>) {
        _receitas.value = list.sortedByDescending { favoriteIds.contains(it.id) }
    }

}
