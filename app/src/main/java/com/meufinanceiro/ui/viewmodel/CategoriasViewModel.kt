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

// O ViewModel é o "cérebro" da tela. Ele sobrevive a mudanças de configuração (girar tela).
// Recebe o Repository no construtor para poder falar com o Banco de Dados.
class CategoriasViewModel(
    private val repository: CategoriaRepository
) : ViewModel() {

    // --- PADRÃO DE "BACKING PROPERTY" (Encapsulamento) ---
    // 1. _categorias (Privado): É mutável. Só o ViewModel pode alterar essa lista.
    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())

    // 2. categorias (Público): É imutável (StateFlow). A tela só pode LER isso, nunca alterar diretamente.
    // Isso protege os dados e evita bugs na interface.
    val categorias: StateFlow<List<Categoria>> = _categorias.asStateFlow()

    // Bloco init: Executado automaticamente assim que a classe é criada.
    init {
        carregarCategorias()
    }

    // Busca a lista atualizada no banco e joga dentro do StateFlow (_categorias)
    private fun carregarCategorias() {
        // viewModelScope.launch: Inicia uma Coroutine (thread secundária).
        // Operações de banco não podem rodar na thread principal (UI), senão o app trava.
        viewModelScope.launch {
            _categorias.value = repository.listarTodas()
        }
    }

    // Adiciona uma nova categoria e atualiza a lista imediatamente
    fun adicionarCategoria(nome: String) {
        viewModelScope.launch {
            val novaCategoria = Categoria(nome = nome)
            repository.salvar(novaCategoria)

            // Dica: Após salvar, recarregamos a lista para a tela atualizar sozinha.
            carregarCategorias()
        }
    }

    // Remove uma categoria e atualiza a lista
    fun deletarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            repository.deletar(categoria)

            // Recarrega para o item sumir da tela instantaneamente
            carregarCategorias()
        }
    }
}

// --- FACTORY (Fábrica de ViewModel) ---
// O Android não sabe criar ViewModels que têm parâmetros no construtor (como o 'repository').
// Essa classe ensina o Android a construir o nosso ViewModel injetando o repository.
class CategoriasViewModelFactory(private val repository: CategoriaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriasViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}