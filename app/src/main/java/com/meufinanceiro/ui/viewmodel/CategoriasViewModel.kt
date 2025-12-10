package com.meufinanceiro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meufinanceiro.backend.model.Categoria
import com.meufinanceiro.backend.repository.CategoriaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoriasViewModel(
    private val repository: CategoriaRepository
) : ViewModel() {

    // Lista que a tela vai observar (Começa vazia)
    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias.asStateFlow()

    init {
        // Assim que o ViewModel nasce, ele carrega a lista do banco
        carregarCategorias()
    }

    private fun carregarCategorias() {
        viewModelScope.launch {
            _categorias.value = repository.listarTodas()
        }
    }

    fun adicionarCategoria(nome: String) {
        viewModelScope.launch {
            val novaCategoria = Categoria(nome = nome)
            repository.salvar(novaCategoria)
            // Recarrega a lista para aparecer na tela imediatamente
            carregarCategorias()
        }
    }

    fun deletarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            repository.deletar(categoria)
            carregarCategorias()
        }
    }
}

class CategoriasViewModelFactory(private val repository: CategoriaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriasViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}