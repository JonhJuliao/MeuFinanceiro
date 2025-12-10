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
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.ui.viewmodel.RegistrarViewModel
import com.meufinanceiro.ui.viewmodel.RegistrarViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarScreen(navController: NavController) {
    val context = LocalContext.current

    // 1. CONFIGURAÇÃO DO BANCO E VIEWMODEL (Cérebro da tela)
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db").build()
    }
    val repository = remember { TransacaoRepository(db.transacaoDao()) }
    val viewModel: RegistrarViewModel = viewModel(
        factory = RegistrarViewModelFactory(repository)
    )

    // 2. ESTADOS DO FORMULÁRIO
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var tipo by remember { mutableStateOf(TipoTela.DESPESA) }

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // DATA (Padrão = Hoje)
    val calendar = remember { Calendar.getInstance() }
    var dateMillis by remember { mutableStateOf(calendar.timeInMillis) }
    var isSaving by remember { mutableStateOf(false) } // Para travar botão ao salvar

    // Categorias (Depois mudaremos isso para vir do banco também, parte 2 do Colega B)
    val sampleCategories = listOf("Alimentação", "Transporte", "Salário", "Lazer", "Contas", "Outros")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Transação") },
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
            // TIPO (Receita / Despesa)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = tipo == TipoTela.RECEITA,
                    onClick = { tipo = TipoTela.RECEITA },
                    label = { Text("Receita") },
                    leadingIcon = {
                        if (tipo == TipoTela.RECEITA) Icon(Icons.Default.Check, null)
                    }
                )
                FilterChip(
                    selected = tipo == TipoTela.DESPESA,
                    onClick = { tipo = TipoTela.DESPESA },
                    label = { Text("Despesa") },
                    leadingIcon = {
                        if (tipo == TipoTela.DESPESA) Icon(Icons.Default.Check, null)
                    }
                )
            }

            // VALOR
            OutlinedTextField(
                value = amountText,
                onValueChange = { new ->
                    val filtered = new.filter { it.isDigit() || it == '.' || it == ',' }
                    amountText = filtered
                },
                label = { Text("Valor (ex: 123.45)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // DATA
            val dateLabel = remember(dateMillis) { sdf.format(Date(dateMillis)) }
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

            OutlinedTextField(
                value = dateLabel,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().clickable { datePickerOnClick() },
                label = { Text("Data") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
            )

            // CATEGORIA
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedCategory ?: "Escolha uma categoria",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text("Categoria") },
                    leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    sampleCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                expanded = false
                            }
                        )
                    }
                }
            }

            // DESCRIÇÃO
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // BOTÃO SALVAR (AGORA FUNCIONA!)
            Button(
                enabled = !isSaving,
                onClick = {
                    val parsed = amountText.replace(",", ".").toDoubleOrNull()
                    when {
                        parsed == null -> Toast.makeText(context, "Valor inválido", Toast.LENGTH_SHORT).show()
                        parsed <= 0.0 -> Toast.makeText(context, "Valor deve ser maior que 0", Toast.LENGTH_SHORT).show()
                        selectedCategory == null -> Toast.makeText(context, "Escolha uma categoria", Toast.LENGTH_SHORT).show()
                        else -> {
                            isSaving = true

                            // CHAMA O VIEWMODEL PARA SALVAR NO BANCO
                            viewModel.salvarTransacao(
                                tipoTela = tipo,
                                valor = parsed,
                                dataMillis = dateMillis,
                                categoria = selectedCategory!!,
                                descricao = description,
                                onSuccess = {
                                    Toast.makeText(context, "Salvo com sucesso!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack() // Volta pra Home
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Salvar", fontSize = 16.sp)
                }
            }
        }
    }
}

enum class TipoTela { RECEITA, DESPESA }