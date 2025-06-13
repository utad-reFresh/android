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

    init {
        fetchReceitas()
    }

    fun fetchReceitas() {
        viewModelScope.launch {
            val response = ApiClient.apiService.getRecipeList()
            if (response.isSuccessful) {
                _receitas.value = response.body() ?: emptyList()
            } else {
                _receitas.value = emptyList()
            }
        }
    }


    fun setReceitas(list: List<pt.utad.refresh.RecipeInListDto>) {
        _receitas.value = list
    }

}
