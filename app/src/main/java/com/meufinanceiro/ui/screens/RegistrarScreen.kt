package com.meufinanceiro.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.Room
import com.meufinanceiro.backend.db.AppDatabase
import com.meufinanceiro.backend.model.Categoria
import com.meufinanceiro.backend.model.MetodoPagamento
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.repository.CategoriaRepository
import com.meufinanceiro.backend.repository.TransacaoRepository
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
    val colors = MaterialTheme.colorScheme // TEMA ATIVO

    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    val viewModel: RegistrarViewModel = viewModel(
        factory = RegistrarViewModelFactory(
            TransacaoRepository(db.transacaoDao()),
            CategoriaRepository(db.categoriaDao())
        )
    )

    // VARIÁVEIS DE ESTADO
    var rawAmountString by remember { mutableStateOf("") }
    var amountTextFieldValue by remember { mutableStateOf(TextFieldValue("R$ 0,00")) }
    var description by remember { mutableStateOf("") }

    var selectedCategory by remember { mutableStateOf<Categoria?>(null) }
    var tipo by remember { mutableStateOf(TipoTela.DESPESA) }
    var isSaving by remember { mutableStateOf(false) }

    var metodoPagamento by remember { mutableStateOf(MetodoPagamento.DINHEIRO) }
    var numeroParcelas by remember { mutableIntStateOf(1) }

    // NOVO: Controle de fatura fechada
    var isFaturaFechada by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }
    val listaCategorias by viewModel.categorias.collectAsState()

    val corAtiva = if (tipo == TipoTela.RECEITA) colors.primary else colors.error

    // INTELIGÊNCIA DE CATEGORIA
    LaunchedEffect(selectedCategory) {
        selectedCategory?.let { cat ->
            val nome = cat.nome.trim().lowercase()
            val novoMetodo = when {
                nome.contains("uber") || nome.contains("ifood") ||
                        nome.contains("amazon") || nome.contains("netflix") ||
                        nome.contains("assinatura") -> MetodoPagamento.CREDITO

                nome.contains("mercado") || nome.contains("farmacia") ||
                        nome.contains("internet") -> MetodoPagamento.DEBITO

                nome.contains("padaria") || nome.contains("onibus") -> MetodoPagamento.DINHEIRO

                else -> null
            }
            if (novoMetodo != null) metodoPagamento = novoMetodo
        }
    }

    // CARREGAR DADOS (EDIÇÃO)
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
                metodoPagamento = try { MetodoPagamento.valueOf(transacao.metodoPagamento) } catch (e: Exception) { MetodoPagamento.DINHEIRO }
                numeroParcelas = transacao.totalParcelas
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (transacaoId > 0L) "Editar" else "Nova Transação", fontWeight = FontWeight.SemiBold, color = colors.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancelar", tint = colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ABAS
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (tipo == TipoTela.DESPESA) colors.error.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { tipo = TipoTela.DESPESA },
                    contentAlignment = Alignment.Center
                ) { Text("Despesa", color = if (tipo == TipoTela.DESPESA) colors.error else colors.onSurfaceVariant, fontWeight = FontWeight.Bold) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (tipo == TipoTela.RECEITA) colors.primary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { tipo = TipoTela.RECEITA },
                    contentAlignment = Alignment.Center
                ) { Text("Receita", color = if (tipo == TipoTela.RECEITA) colors.primary else colors.onSurfaceVariant, fontWeight = FontWeight.Bold) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // VALOR
            Text("Valor da transação", color = colors.onSurfaceVariant, fontSize = 14.sp)
            TextField(
                value = amountTextFieldValue,
                onValueChange = { novoValor ->
                    val apenasNumeros = novoValor.text.filter { it.isDigit() }
                    if (apenasNumeros.length <= 12) {
                        rawAmountString = apenasNumeros
                        val formatado = formatarMoedaVisual(rawAmountString)
                        amountTextFieldValue = TextFieldValue(text = formatado, selection = TextRange(formatado.length))
                    }
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = corAtiva
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // CARD FORMULÁRIO
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

                    // PAGAMENTO
                    Column {
                        Text("Pagamento", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(MetodoPagamento.values().size) { index ->
                                val metodo = MetodoPagamento.values()[index]
                                val isSelected = metodoPagamento == metodo
                                val label = metodo.name.lowercase().replaceFirstChar { it.uppercase() }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { metodoPagamento = metodo },
                                    label = { Text(label) },
                                    leadingIcon = { if (isSelected) Icon(Icons.Rounded.Check, null) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = corAtiva.copy(alpha = 0.2f),
                                        selectedLabelColor = corAtiva,
                                        containerColor = colors.background,
                                        labelColor = colors.onSurface
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(borderColor = if(isSelected) corAtiva else colors.outline, enabled = true, selected = isSelected)
                                )
                            }
                        }

                        // ÁREA DO CRÉDITO: PARCELAS E VENCIMENTO
                        AnimatedVisibility(visible = metodoPagamento == MetodoPagamento.CREDITO) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                // Parcelas
                                var parcelasStr by remember { mutableStateOf("1") }
                                OutlinedTextField(
                                    value = parcelasStr,
                                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) { parcelasStr = it; numeroParcelas = it.toIntOrNull() ?: 1 } },
                                    label = { Text("Parcelas") },
                                    trailingIcon = { Text("x", modifier = Modifier.padding(end = 12.dp), color = colors.onSurfaceVariant) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = corAtiva,
                                        unfocusedBorderColor = colors.outline,
                                        focusedTextColor = colors.onSurface,
                                        unfocusedTextColor = colors.onSurface
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // NOVO: Toggle de Fatura Fechada
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.background, RoundedCornerShape(12.dp))
                                        .clickable { isFaturaFechada = !isFaturaFechada }
                                        .padding(12.dp)
                                ) {
                                    Checkbox(
                                        checked = isFaturaFechada,
                                        onCheckedChange = { isFaturaFechada = it },
                                        colors = CheckboxDefaults.colors(checkedColor = corAtiva)
                                    )
                                    Column {
                                        Text("Fatura já fechou?", fontWeight = FontWeight.Bold, color = colors.onSurface)
                                        Text("Pagar no mês que vem", fontSize = 12.sp, color = colors.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))

                    // DATA
                    val dataFormatada = remember(datePickerState.selectedDateMillis, isFaturaFechada, metodoPagamento) {
                        val millisOriginal = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = millisOriginal

                        // Simula visualmente a data de pagamento
                        if (metodoPagamento == MetodoPagamento.CREDITO && isFaturaFechada) {
                            cal.add(Calendar.MONTH, 1)
                        }

                        val formatter = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("pt", "BR"))
                        formatter.format(cal.time)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CalendarToday, null, tint = colors.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Data da Compra", color = colors.onSurfaceVariant)
                        }
                        // Mostra data recalculada se for crédito fechado
                        Text(dataFormatada, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    }

                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))

                    // CATEGORIA
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Category, null, tint = colors.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Categoria", color = colors.onSurfaceVariant)
                        }
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedCategory?.nome ?: "Selecionar", fontWeight = FontWeight.SemiBold, color = if(selectedCategory == null) corAtiva else colors.onSurface)
                                Icon(Icons.Rounded.ChevronRight, null, tint = colors.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(colors.surface)) {
                                if (listaCategorias.isEmpty()) {
                                    DropdownMenuItem(text = { Text("Nenhuma categoria", color = colors.onSurface) }, onClick = { expanded = false })
                                } else {
                                    listaCategorias.forEach { cat ->
                                        DropdownMenuItem(text = { Text(cat.nome, color = colors.onSurface) }, onClick = { selectedCategory = cat; expanded = false })
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))

                    // DESCRIÇÃO
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Edit, null, tint = colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        TextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Descrição (Opcional)", color = colors.onSurfaceVariant.copy(alpha = 0.5f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = colors.onSurface,
                                unfocusedTextColor = colors.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // BOTÃO SALVAR
            Button(
                enabled = !isSaving,
                onClick = {
                    val valorFinal = if (rawAmountString.isNotEmpty()) rawAmountString.toDouble() / 100 else 0.0
                    if (valorFinal <= 0.0 || selectedCategory == null) {
                        Toast.makeText(context, "Insira valor e categoria", Toast.LENGTH_SHORT).show()
                    } else {
                        isSaving = true

                        // LÓGICA DE DATA INTELIGENTE
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()

                        // Se for crédito e fatura fechada, joga para o próximo mês
                        if (metodoPagamento == MetodoPagamento.CREDITO && isFaturaFechada) {
                            cal.add(Calendar.MONTH, 1)
                        }

                        val dataParaSalvar = cal.timeInMillis
                        val parcelasParaSalvar = if (metodoPagamento == MetodoPagamento.CREDITO) numeroParcelas else 1

                        viewModel.salvarTransacao(
                            tipoTela = tipo,
                            valor = valorFinal,
                            dataMillis = dataParaSalvar,
                            categoriaId = selectedCategory!!.id,
                            descricao = description,
                            metodoPagamento = metodoPagamento.name,
                            totalParcelas = parcelasParaSalvar,
                            onSuccess = {
                                Toast.makeText(context, "Salvo com sucesso", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { isSaving = false }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = corAtiva, disabledContainerColor = corAtiva.copy(alpha = 0.5f)),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                if (isSaving) CircularProgressIndicator(color = colors.onPrimary, modifier = Modifier.size(24.dp))
                else Text("Salvar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.onPrimary)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun formatarMoedaVisual(centavosStr: String): String {
    if (centavosStr.isEmpty()) return "R$ 0,00"
    val valor = centavosStr.toLongOrNull() ?: 0L
    val nf = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return nf.format(valor / 100.0)
}

enum class TipoTela { RECEITA, DESPESA }