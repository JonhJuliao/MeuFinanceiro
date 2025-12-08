package com.meufinanceiro.backend.service

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meufinanceiro.backend.db.AppDatabase
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransacaoServiceTest {

    private lateinit var db: AppDatabase
    private lateinit var transacaoService: TransacaoService
    private lateinit var categoriaService: CategoriaService

    @Before
    fun setup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        val categoriaRepo = CategoriaRepository(db.categoriaDao())
        val transacaoRepo = TransacaoRepository(db.transacaoDao())

        categoriaService = CategoriaService(categoriaRepo)
        transacaoService = TransacaoService(transacaoRepo, categoriaRepo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun registrarTransacao_deveSalvarQuandoValida() = runBlocking {
        val categoriaId = categoriaService.criarCategoria("Salário")

        val id = transacaoService.registrarTransacao(
            tipo = TipoTransacao.RECEITA,
            valor = 1000.0,
            dataMillis = System.currentTimeMillis(),
            categoriaId = categoriaId,
            descricao = "Pagamento"
        )

        val todas = transacaoService.listarTransacoes()
        assertEquals(1, todas.size)
        assertEquals(id, todas[0].id)
        assertEquals(1000.0, todas[0].valor, 0.0)
    }

    @Test
    fun registrarTransacao_deveFalharQuandoValorNaoPositivo() = runBlocking {
        val categoriaId = categoriaService.criarCategoria("Teste")

        try {
            transacaoService.registrarTransacao(
                tipo = TipoTransacao.DESPESA,
                valor = -10.0,
                dataMillis = System.currentTimeMillis(),
                categoriaId = categoriaId,
                descricao = null
            )
            fail("Era esperado IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // OK
        }
    }

    @Test
    fun registrarTransacao_deveFalharQuandoCategoriaInexistente() = runBlocking {
        try {
            transacaoService.registrarTransacao(
                tipo = TipoTransacao.DESPESA,
                valor = 50.0,
                dataMillis = System.currentTimeMillis(),
                categoriaId = 999L, // inexistente
                descricao = null
            )
            fail("Era esperado IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // OK
        }
    }

    @Test
    fun excluirTransacao_deveExcluirQuandoExiste() = runBlocking {
        val categoriaId = categoriaService.criarCategoria("Transporte")

        val id = transacaoService.registrarTransacao(
            tipo = TipoTransacao.DESPESA,
            valor = 30.0,
            dataMillis = System.currentTimeMillis(),
            categoriaId = categoriaId,
            descricao = "Uber"
        )

        transacaoService.excluirTransacao(id)

        val lista = transacaoService.listarTransacoes()
        assertTrue(lista.isEmpty())
    }

    @Test
    fun excluirTransacao_naoDeveLancarQuandoInexistente() = runBlocking {
        transacaoService.excluirTransacao(999L)
        // Se não lançar exceção -> OK
    }
}
