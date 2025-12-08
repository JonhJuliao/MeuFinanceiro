package com.meufinanceiro.backend.repository

import com.meufinanceiro.backend.dao.CategoriaDao
import com.meufinanceiro.backend.model.Categoria

class CategoriaRepository(
    private val dao: CategoriaDao
) {

    suspend fun salvar(categoria: Categoria): Long {
        return if (categoria.id == 0L) {
            dao.inserir(categoria)
        } else {
            dao.atualizar(categoria)
            categoria.id
        }
    }

    suspend fun listarTodas(): List<Categoria> = dao.listarTodas()

    suspend fun buscarPorId(id: Long): Categoria? = dao.buscarPorId(id)

    suspend fun deletar(categoria: Categoria) {
        dao.deletar(categoria)
    }
}