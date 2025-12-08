package com.meufinanceiro.backend.service

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meufinanceiro.backend.db.AppDatabase
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResumoFinanceiroServiceTest {

    private lateinit var db: AppDatabase
    private lateinit var categoriaService: CategoriaService
    private lateinit var transacaoService: TransacaoService
    private lateinit var resumoService: ResumoFinanceiroService

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
        val transacaoDao = db.transacaoDao()

        val categoriaRepository = CategoriaRepository(categoriaDao)
        val transacaoRepository = TransacaoRepository(transacaoDao)

        categoriaService = CategoriaService(categoriaRepository)
        transacaoService = TransacaoService(transacaoRepository, categoriaRepository)
        resumoService = ResumoFinanceiroService(transacaoRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun calcularResumo_deveRetornarZerosQuandoNaoHaTransacoes() = runBlocking {
        val resumo = resumoService.calcularResumo()

        assertEquals(0.0, resumo.totalReceitas, 0.001)
        assertEquals(0.0, resumo.totalDespesas, 0.001)
        assertEquals(0.0, resumo.saldo, 0.001)
    }

    @Test
    fun calcularResumo_deveSomarReceitasEDespesasCorretamente() = runBlocking {
        val catSalario = categoriaService.criarCategoria("Salário")
        val catMercado = categoriaService.criarCategoria("Mercado")
        val catLazer = categoriaService.criarCategoria("Lazer")

        transacaoService.registrarTransacao(
            tipo = TipoTransacao.RECEITA,
            valor = 2000.0,
            dataMillis = System.currentTimeMillis(),
            categoriaId = catSalario,
            descricao = "Salário"
        )

        transacaoService.registrarTransacao(
            tipo = TipoTransacao.DESPESA,
            valor = 300.0,
            dataMillis = System.currentTimeMillis(),
            categoriaId = catMercado,
            descricao = "Compras do mês"
        )

        transacaoService.registrarTransacao(
            tipo = TipoTransacao.DESPESA,
            valor = 200.0,
            dataMillis = System.currentTimeMillis(),
            categoriaId = catLazer,
            descricao = "Cinema e pizza"
        )

        val resumo = resumoService.calcularResumo()

        assertEquals(2000.0, resumo.totalReceitas, 0.001)
        assertEquals(500.0, resumo.totalDespesas, 0.001)
        assertEquals(1500.0, resumo.saldo, 0.001)
    }
}
