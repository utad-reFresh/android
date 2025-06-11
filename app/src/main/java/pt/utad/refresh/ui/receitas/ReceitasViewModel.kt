package pt.utad.refresh.ui.receitas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReceitasViewModel : ViewModel() {
    private val _receitas = MutableLiveData<List<String>>().apply {
        value = (1..16).mapIndexed { _, i ->
            "Receita #$i"
        }
    }

    val receitas: LiveData<List<String>> = _receitas
}