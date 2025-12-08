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
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackendServicesInstrumentedTest {

    private lateinit var db: AppDatabase
    private lateinit var categoriaService: CategoriaService
    private lateinit var transacaoService: TransacaoService
    private lateinit var resumoService: ResumoFinanceiroService

    @Before
    fun setup() {
        // Contexto de teste
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Banco em memória só para teste
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries() // OK em teste
            .build()

        // DAOs reais
        val categoriaDao = db.categoriaDao()
        val transacaoDao = db.transacaoDao()

        // Repositórios reais
        val categoriaRepository = CategoriaRepository(categoriaDao)
        val transacaoRepository = TransacaoRepository(transacaoDao)

        // Services reais (os que você colou)
        categoriaService = CategoriaService(categoriaRepository)
        transacaoService = TransacaoService(transacaoRepository, categoriaRepository)
        resumoService = ResumoFinanceiroService(transacaoRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun fluxoCompleto_deveCalcularResumoCorretamente() = runBlocking {
        // 1. Criar categorias
        val idSalario = categoriaService.criarCategoria("Salário")
        val idMercado = categoriaService.criarCategoria("Mercado")

        // 2. Registrar uma receita
        transacaoService.registrarTransacao(
            tipo = TipoTransacao.RECEITA,
            valor = 2000.0,
            dataMillis = System.currentTimeMillis(),
            categoriaId = idSalario,
            descricao = "Salário do mês"
        )

        // 3. Registrar uma despesa
        transacaoService.registrarTransacao(
            tipo = TipoTransacao.DESPESA,
            valor = 500.0,
            dataMillis = System.currentTimeMillis(),
            categoriaId = idMercado,
            descricao = "Compras do mês"
        )

        // 4. Verificar se as transações estão lá
        val transacoes = transacaoService.listarTransacoes()
        Assert.assertEquals(2, transacoes.size)

        // 5. Verificar resumo financeiro
        val resumo = resumoService.calcularResumo()

        Assert.assertEquals(2000.0, resumo.totalReceitas, 0.001)
        Assert.assertEquals(500.0, resumo.totalDespesas, 0.001)
        Assert.assertEquals(1500.0, resumo.saldo, 0.001)

        // 6. Verificar se listagem com categoria está funcionando
        val transacoesComCategoria = transacaoService.listarTransacoesComCategoria()
        Assert.assertEquals(2, transacoesComCategoria.size)
        Assert.assertTrue(transacoesComCategoria.any { it.categoria.nome == "Salário" })
        Assert.assertTrue(transacoesComCategoria.any { it.categoria.nome == "Mercado" })
    }
}