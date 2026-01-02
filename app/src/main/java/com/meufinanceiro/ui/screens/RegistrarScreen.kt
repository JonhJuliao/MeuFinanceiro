package com.meufinanceiro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
import com.meufinanceiro.ui.theme.AcessibilidadeApp
import com.meufinanceiro.ui.viewmodel.RegistrarViewModel
import com.meufinanceiro.ui.viewmodel.RegistrarViewModelFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarScreen(
    navController: NavController,
    transacaoId: Long = 0L
) {
    val context = LocalContext.current
    val db = remember { Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db").build() }

    val viewModel: RegistrarViewModel = viewModel(
        factory = RegistrarViewModelFactory(
            TransacaoRepository(db.transacaoDao()),
            CategoriaRepository(db.categoriaDao())
        )
    )

    // Estados
    var rawAmountString by remember { mutableStateOf("") }
    var amountTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Categoria?>(null) }
    var tipo by remember { mutableStateOf(TipoTela.DESPESA) }
    var isSaving by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }

    // Cores Dinâmicas
    val corReceita = AcessibilidadeApp.corReceita
    val corDespesa = AcessibilidadeApp.corDespesa
    val corAtiva = if (tipo == TipoTela.RECEITA) corReceita else corDespesa

    // Carregar Edição
    LaunchedEffect(transacaoId) {
        if (transacaoId > 0) {
            viewModel.carregarDadosParaEdicao(transacaoId) { transacao, categoria ->
                val valorCentavos = (transacao.valor * 100).toLong().toString()
                rawAmountString = valorCentavos
                amountTextFieldValue = TextFieldValue(formatarMoedaVisual(valorCentavos))
                description = transacao.descricao ?: ""
                datePickerState.selectedDateMillis = transacao.dataMillis
                selectedCategory = categoria
                tipo = if (transacao.tipo == TipoTransacao.RECEITA) TipoTela.RECEITA else TipoTela.DESPESA
            }
        }
    }

    val listaCategorias by viewModel.categorias.collectAsState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transacaoId > 0L) "Editar" else "Nova Transação", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                }
                // REMOVI O SWITCH DAQUI. TELA LIMPA AGORA.
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 1. TIPO
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = tipo == TipoTela.RECEITA,
                    onClick = { tipo = TipoTela.RECEITA },
                    label = { Text("Receita") },
                    leadingIcon = { if (tipo == TipoTela.RECEITA) Icon(Icons.Rounded.ArrowUpward, null) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = corReceita,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                FilterChip(
                    selected = tipo == TipoTela.DESPESA,
                    onClick = { tipo = TipoTela.DESPESA },
                    label = { Text("Despesa") },
                    leadingIcon = { if (tipo == TipoTela.DESPESA) Icon(Icons.Rounded.ArrowDownward, null) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = corDespesa,
                        selectedLabelColor = MaterialTheme.colorScheme.onError,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onError
                    )
                )
            }

            // 2. VALOR
            OutlinedTextField(
                value = amountTextFieldValue,
                onValueChange = { novoValor ->
                    val apenasNumeros = novoValor.text.filter { it.isDigit() }
                    if (apenasNumeros.length <= 12) {
                        rawAmountString = apenasNumeros
                        val formatado = formatarMoedaVisual(rawAmountString)
                        amountTextFieldValue = TextFieldValue(text = formatado, selection = TextRange(formatado.length))
                    }
                },
                label = { Text("Valor") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Rounded.AttachMoney, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = corAtiva
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = corAtiva,
                    focusedLabelColor = corAtiva
                )
            )

            // 3. DATA
            val dataFormatada = remember(datePickerState.selectedDateMillis) {
                val millis = datePickerState.selectedDateMillis
                if (millis != null) {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.timeInMillis = millis
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(cal.time)
                } else {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dataFormatada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data") },
                    leadingIcon = { Icon(Icons.Rounded.CalendarToday, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Box(Modifier.matchParentSize().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showDatePicker = true })
            }

            // 4. CATEGORIA
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedCategory?.nome ?: "Selecione uma categoria",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text("Categoria") },
                    leadingIcon = { Icon(Icons.Rounded.Category, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    if (listaCategorias.isEmpty()) {
                        DropdownMenuItem(text = { Text("Sem categorias") }, onClick = { expanded = false })
                    } else {
                        listaCategorias.forEach { categoria ->
                            DropdownMenuItem(text = { Text(categoria.nome) }, onClick = { selectedCategory = categoria; expanded = false })
                        }
                    }
                }
            }

            // 5. DESCRIÇÃO
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição (Opcional)") },
                leadingIcon = { Icon(Icons.Rounded.Description, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 6. BOTÃO SALVAR
            Button(
                enabled = !isSaving,
                onClick = {
                    val valorFinal = if (rawAmountString.isNotEmpty()) rawAmountString.toDouble() / 100 else 0.0
                    if (valorFinal <= 0.0 || selectedCategory == null) {
                        Toast.makeText(context, "Preencha valor e categoria", Toast.LENGTH_SHORT).show()
                    } else {
                        isSaving = true
                        viewModel.salvarTransacao(
                            tipoTela = tipo,
                            valor = valorFinal,
                            dataMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis(),
                            categoriaId = selectedCategory!!.id,
                            descricao = description,
                            onSuccess = { Toast.makeText(context, "Salvo!", Toast.LENGTH_SHORT).show(); navController.popBackStack() },
                            onError = { isSaving = false }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = corAtiva)
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text(if (transacaoId > 0L) "Atualizar" else "Salvar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun formatarMoedaVisual(centavosStr: String): String {
    if (centavosStr.isEmpty()) return ""
    val valor = centavosStr.toLongOrNull() ?: 0L
    val nf = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return nf.format(valor / 100.0)
}

enum class TipoTela { RECEITA, DESPESA }