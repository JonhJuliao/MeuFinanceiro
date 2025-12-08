package com.meufinanceiro.backend.service

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meufinanceiro.backend.db.AppDatabase
import com.meufinanceiro.backend.repository.CategoriaRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoriaServiceTest {

    private lateinit var db: AppDatabase
    private lateinit var categoriaService: CategoriaService

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        val categoriaDao = db.categoriaDao()
        val categoriaRepository = CategoriaRepository(categoriaDao)

        categoriaService = CategoriaService(categoriaRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun criarCategoria_deveSalvarQuandoNomeValido() = runBlocking {
        val id = categoriaService.criarCategoria("Mercado")

        val categorias = categoriaService.listarCategorias()

        assertEquals(1, categorias.size)
        assertEquals(id, categorias[0].id)
        assertEquals("Mercado", categorias[0].nome)
    }

    @Test
    fun criarCategoria_deveFalharQuandoNomeVazio() = runBlocking {
        try {
            categoriaService.criarCategoria("   ")
            fail("Era esperado IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // OK
        }
    }

    @Test
    fun removerCategoria_deveRemoverQuandoExiste() = runBlocking {
        val id = categoriaService.criarCategoria("Lazer")

        categoriaService.removerCategoria(id)

        val categorias = categoriaService.listarCategorias()
        assertTrue(categorias.isEmpty())
    }

    @Test
    fun removerCategoria_naoDeveLancarQuandoIdNaoExiste() = runBlocking {
        categoriaService.removerCategoria(999L)
        // se não lançou, está ok
    }
}
