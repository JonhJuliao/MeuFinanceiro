package com.meufinanceiro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.room.Room
import com.meufinanceiro.backend.db.AppDatabase
import com.meufinanceiro.backend.model.TipoTransacao
import com.meufinanceiro.backend.model.TransacaoComCategoria
import com.meufinanceiro.backend.repository.TransacaoRepository
import com.meufinanceiro.navigation.Screen
import com.meufinanceiro.ui.extensions.categoriaNome
import com.meufinanceiro.ui.extensions.toCurrency
import com.meufinanceiro.ui.viewmodel.HomeViewModel
import com.meufinanceiro.ui.viewmodel.HomeViewModelFactory
import com.meufinanceiro.ui.theme.GradientCyberpunk
import com.meufinanceiro.ui.theme.GradientLightMode
import com.meufinanceiro.ui.theme.AcessibilidadeApp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val db = remember { Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db").build() }
    val repository = remember { TransacaoRepository(db.transacaoDao()) }

    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(repository, context)
    )

    val saldo by viewModel.saldoTotal.collectAsState()
    val nomeUsuario by viewModel.nomeUsuario.collectAsState()
    val ultimasTransacoes by viewModel.ultimasTransacoes.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.carregarDados()
    }

    var showBalance by remember { mutableStateOf(true) }
    // Renomeei para ficar mais claro: agora é Configuração de Perfil
    var showProfileSettingsDialog by remember { mutableStateOf(false) }
    var mostrarDialogoPremium by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    // --- DIALOG DE CONFIGURAÇÕES DO PERFIL (Nome + Acessibilidade) ---
    if (showProfileSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showProfileSettingsDialog = false },
            title = { Text("Perfil e Configurações") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Editar Nome
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Seu nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // Switch Acessibilidade (AGORA NO LUGAR CERTO)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo Daltônico",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ajusta cores para melhor visibilidade (Azul/Laranja)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = AcessibilidadeApp.isModoDaltonicoAtivo,
                            onCheckedChange = { AcessibilidadeApp.isModoDaltonicoAtivo = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tempName.isNotBlank()) {
                        viewModel.atualizarNome(tempName)
                    }
                    // A acessibilidade atualiza sozinha pelo Switch, então só fechamos
                    showProfileSettingsDialog = false
                }) { Text("Concluir") }
            },
            dismissButton = {
                TextButton(onClick = { showProfileSettingsDialog = false }) { Text("Fechar") }
            }
        )
    }

    if (mostrarDialogoPremium) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPremium = false },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(32.dp)) },
            title = { Text(text = "Funcionalidade Premium", textAlign = TextAlign.Center) },
            text = {
                Text(
                    "Em breve você poderá conectar sua conta do Nubank, Itaú e outros bancos diretamente aqui!\n\nEstamos trabalhando na integração segura via Open Finance.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoPremium = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Avise-me quando lançar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPremium = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    Scaffold(
        containerColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFFAFAFA),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Registrar.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Nova Transação")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // 1. HERO CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (isDark) GradientCyberpunk else GradientLightMode
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // LINHA 1: Nome e Avatar (AGORA CLICÁVEL PARA SETTINGS)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempName = nomeUsuario
                                showProfileSettingsDialog = true // Abre o novo dialog
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Olá,", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                            Text(
                                text = nomeUsuario,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "Perfil e Configurações",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // LINHA 2: Saldo
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Seu saldo total", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { showBalance = !showBalance }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (showBalance) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = "Esconder saldo",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (showBalance) saldo.toCurrency() else "R$ •••••",
                            color = Color.White,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp)
                        )
                    }
                }
            }

            // ... O RESTO DO ARQUIVO CONTINUA IGUAL (Banner Premium, Ações, Lista) ...
            Spacer(modifier = Modifier.height(24.dp))

            // 1.5 BANNER PREMIUM
            Card(
                onClick = { mostrarDialogoPremium = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sincronização Bancária",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Conecte Nubank, Itaú e mais...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300)
                    )
                }
            }

            // 2. AÇÕES RÁPIDAS
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Ações Rápidas",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionButton(Icons.Rounded.History, "Ver\nHistórico") { navController.navigate(Screen.Historico.route) }
                ActionButton(Icons.Rounded.PieChart, "Resumo\nMensal") { navController.navigate(Screen.Resumo.route) }
                ActionButton(Icons.Rounded.Settings, "Categorias") { navController.navigate(Screen.Categorias.route) }
            }

            // 3. ÚLTIMAS MOVIMENTAÇÕES
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Últimas Movimentações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                TextButton(onClick = { navController.navigate(Screen.Historico.route) }) {
                    Text("Ver todas")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (ultimasTransacoes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ListAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nenhuma movimentação ainda",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    for (item in ultimasTransacoes) {
                        MiniTransacaoCard(transacao = item)
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun MiniTransacaoCard(transacao: TransacaoComCategoria) {
    val isReceita = transacao.transacao.tipo == TipoTransacao.RECEITA
    val icon = if (isReceita) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward
    val color = if (isReceita) AcessibilidadeApp.corReceita else AcessibilidadeApp.corDespesa
    val isDark = isSystemInDarkTheme()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else Color(0xFFE0E0E0)
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = color.copy(alpha = 0.1f), modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = transacao.categoriaNome, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = transacao.transacao.descricao?.takeIf { it.isNotBlank() } ?: "Sem descrição",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Text(
                text = transacao.transacao.valor.toCurrency(),
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 14.sp
            )
        }
    }
}