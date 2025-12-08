package com.meufinanceiro.backend.service

import com.meufinanceiro.backend.model.Categoria
import com.meufinanceiro.backend.repository.CategoriaRepository

class CategoriaService(
    private val repository: CategoriaRepository
) {

    suspend fun criarCategoria(nome: String): Long {
        val nomeLimpo = nome.trim()
        require(nomeLimpo.isNotEmpty()) { "Nome da categoria não pode ser vazio" }

        val categoria = Categoria(nome = nomeLimpo)
        return repository.salvar(categoria)
    }

    suspend fun listarCategorias(): List<Categoria> =
        repository.listarTodas()

    suspend fun removerCategoria(id: Long) {
        val categoria = repository.buscarPorId(id)
            ?: return // ou lançar exceção, se preferir
        repository.deletar(categoria)
    }
}