package com.meufinanceiro.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.Room
import com.meufinanceiro.backend.db.AppDatabase
import com.meufinanceiro.backend.model.Categoria
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.ui.viewmodel.RegistrarViewModel
import com.meufinanceiro.ui.viewmodel.RegistrarViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarScreen(
    navController: NavController,
    // Parâmetro opcional: Se vier 0L, é uma NOVA transação.
    // Se vier um número (ex: 5L), estamos EDITANDO a transação 5.
    transacaoId: Long = 0L
) {
    val context = LocalContext.current

    // 1. CONFIGURAÇÃO DO BANCO
    val db = remember { Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db").build() }

    // Cria o ViewModel injetando DOIS repositórios (Transação para salvar, Categoria para listar)
    val viewModel: RegistrarViewModel = viewModel(
        factory = RegistrarViewModelFactory(
            TransacaoRepository(db.transacaoDao()),
            CategoriaRepository(db.categoriaDao())
        )
    )

    // 2. ESTADOS DO FORMULÁRIO (Controlam o que aparece na tela)
    var amountText by remember { mutableStateOf("") }        // Texto do valor
    var description by remember { mutableStateOf("") }       // Texto da descrição
    var selectedCategory by remember { mutableStateOf<Categoria?>(null) } // Objeto Categoria selecionado
    var tipo by remember { mutableStateOf(TipoTela.DESPESA) } // Receita ou Despesa

    // Configuração de Data
    val calendar = remember { Calendar.getInstance() }
    var dateMillis by remember { mutableStateOf(calendar.timeInMillis) }

    // Estado para bloquear o botão enquanto salva (evita clique duplo)
    var isSaving by remember { mutableStateOf(false) }

    // 3. MODO EDIÇÃO (A Mágica acontece aqui)
    // O LaunchedEffect roda apenas uma vez quando a tela abre.
    // Se transacaoId > 0, buscamos os dados no banco e preenchemos os campos.
    LaunchedEffect(transacaoId) {
        if (transacaoId > 0) {
            viewModel.carregarDadosParaEdicao(transacaoId) { transacao, categoria ->
                // Preenche o formulário com os dados antigos
                amountText = transacao.valor.toString().replace(".", ",")
                description = transacao.descricao ?: ""
                dateMillis = transacao.dataMillis
                selectedCategory = categoria
                tipo = if (transacao.tipo == TipoTransacao.RECEITA) TipoTela.RECEITA else TipoTela.DESPESA
            }
        }
    }

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Observa a lista de categorias do banco para preencher o Dropdown
    val listaCategorias by viewModel.categorias.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                // Muda o título dependendo se é Novo ou Edição
                title = { Text(if (transacaoId > 0L) "Editar Transação" else "Nova Transação") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- SELETOR DE TIPO (Chips) ---
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = tipo == TipoTela.RECEITA,
                    onClick = { tipo = TipoTela.RECEITA },
                    label = { Text("Receita") },
                    leadingIcon = { if (tipo == TipoTela.RECEITA) Icon(Icons.Default.Check, null) }
                )
                FilterChip(
                    selected = tipo == TipoTela.DESPESA,
                    onClick = { tipo = TipoTela.DESPESA },
                    label = { Text("Despesa") },
                    leadingIcon = { if (tipo == TipoTela.DESPESA) Icon(Icons.Default.Check, null) }
                )
            }

            // --- CAMPO VALOR ---
            OutlinedTextField(
                value = amountText,
                onValueChange = { new ->
                    // Filtra apenas números, ponto e vírgula
                    amountText = new.filter { it.isDigit() || it == '.' || it == ',' }
                },
                label = { Text("Valor") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.ShoppingCart, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // --- CAMPO DATA (Com Truque do Box) ---
            val dateLabel = remember(dateMillis) { sdf.format(Date(dateMillis)) }

            // Função que abre o calendário nativo do Android
            val datePickerOnClick = {
                val c = Calendar.getInstance().apply { timeInMillis = dateMillis }
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val nc = Calendar.getInstance()
                        nc.set(year, month, dayOfMonth)
                        dateMillis = nc.timeInMillis
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            // Box para sobrepor o clique
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = {},
                    readOnly = true, // O usuário não pode digitar a data, só selecionar
                    label = { Text("Data") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Caixa invisível que captura o clique e abre o calendário
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { datePickerOnClick() }
                )
            }

            // --- SELETOR DE CATEGORIA (Dropdown) ---
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    readOnly = true,
                    // Mostra o nome da categoria ou um texto padrão
                    value = selectedCategory?.nome ?: "Escolha uma categoria",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text("Categoria") },
                    leadingIcon = { Icon(Icons.Default.List, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    // Itera sobre as categorias do banco para criar as opções
                    listaCategorias.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria.nome) },
                            onClick = {
                                selectedCategory = categoria // Salva o objeto selecionado
                                expanded = false
                            }
                        )
                    }
                }
            }

            // --- CAMPO DESCRIÇÃO (Opcional) ---
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- BOTÃO SALVAR ---
            Button(
                enabled = !isSaving, // Desabilita se já estiver salvando
                onClick = {
                    // Validação simples
                    val parsed = amountText.replace(",", ".").toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0 || selectedCategory == null) {
                        Toast.makeText(context, "Preencha valor e categoria", Toast.LENGTH_SHORT).show()
                    } else {
                        isSaving = true
                        // Chama o ViewModel passando todos os dados
                        viewModel.salvarTransacao(
                            tipoTela = tipo,
                            valor = parsed,
                            dataMillis = dateMillis,
                            categoriaId = selectedCategory!!.id,
                            descricao = description,
                            onSuccess = {
                                Toast.makeText(context, "Salvo com sucesso!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack() // Volta para a tela anterior
                            },
                            onError = { isSaving = false }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                else Text(if (transacaoId > 0L) "Atualizar" else "Salvar", fontSize = 16.sp)
            }
        }
    }
}

// Enum simples para controlar a UI de Receita/Despesa
enum class TipoTela { RECEITA, DESPESA }