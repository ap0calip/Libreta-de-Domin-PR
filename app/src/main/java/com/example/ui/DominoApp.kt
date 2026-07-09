@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.ui.theme.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.activity.compose.BackHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

// Safe Screen classes for reliable stack navigation
sealed interface Screen {
    object Dashboard : Screen
    object GameSetup : Screen
    object ActiveGame : Screen
    data class AddRound(val gameId: Int, val preselectedTeam: Int? = null) : Screen
    object PlayerStats : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DominoApp(viewModel: DominoViewModel) {
    // Custom Navigation Stack
    val navigationStack = remember { mutableStateListOf<Screen>(Screen.Dashboard) }
    val currentScreen = navigationStack.lastOrNull() ?: Screen.Dashboard

    val navigateTo: (Screen) -> Unit = { screen ->
        navigationStack.add(screen)
    }

    val navigateBack: () -> Unit = {
        if (navigationStack.size > 1) {
            navigationStack.removeAt(navigationStack.size - 1)
        }
    }

    BackHandler(enabled = navigationStack.size > 1) {
        navigateBack()
    }

    // Collecting States
    val players by viewModel.allPlayers.collectAsStateWithLifecycle()
    val games by viewModel.allGames.collectAsStateWithLifecycle()
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()
    val activeGames by viewModel.activeGames.collectAsStateWithLifecycle()
    val activeGameRounds by viewModel.activeGameRounds.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            is Screen.Dashboard -> "Libreta de Dominó Boricua"
                            is Screen.GameSetup -> "Nueva Partida"
                            is Screen.ActiveGame -> "Marcador en Vivo"
                            is Screen.AddRound -> "Anotar Ronda"
                            is Screen.PlayerStats -> "Estadísticas y Líderes"
                        },
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                navigationIcon = {
                    if (currentScreen != Screen.Dashboard) {
                        IconButton(onClick = { navigateBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás"
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Logo",
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    if (currentScreen == Screen.Dashboard) {
                        IconButton(onClick = { navigateTo(Screen.PlayerStats) }) {
                            Icon(imageVector = Icons.Default.Leaderboard, contentDescription = "Líderes")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width / 3 } + fadeOut()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    is Screen.Dashboard -> {
                        DashboardScreen(
                            players = players,
                            games = games,
                            activeGames = activeGames,
                            onStartNewGame = { navigateTo(Screen.GameSetup) },
                            onResumeGame = { id ->
                                viewModel.resumeGame(id)
                                navigateTo(Screen.ActiveGame)
                            },
                            onViewStats = { navigateTo(Screen.PlayerStats) },
                            onDeleteGame = { id -> viewModel.deleteGame(id) },
                            viewModel = viewModel
                        )
                    }
                    is Screen.GameSetup -> {
                        GameSetupScreen(
                            players = players,
                            onStartGame = { mode, maxPts, t1, t2, p1, p2, p3, p4, r1, r2, r3, r4, cap, chuch, useB ->
                                viewModel.startNewGame(
                                    mode, maxPts, t1, t2, p1, p2, p3, p4,
                                    bonusRound1 = r1,
                                    bonusRound2 = r2,
                                    bonusRound3 = r3,
                                    bonusRound4 = r4,
                                    bonusCapicu = cap,
                                    bonusChuchazo = chuch,
                                    useBonuses = useB
                                )
                                navigationStack.removeAt(navigationStack.size - 1) // Remove setup from stack
                                navigateTo(Screen.ActiveGame)
                            },
                            viewModel = viewModel
                        )
                    }
                    is Screen.ActiveGame -> {
                        ActiveGameScreen(
                            game = activeGame,
                            rounds = activeGameRounds,
                            onAddRound = { id, preselectedTeam -> navigateTo(Screen.AddRound(id, preselectedTeam)) },
                            onUndoRound = { viewModel.undoLastRound() },
                            onCancelGame = {
                                viewModel.cancelActiveGame()
                                navigateBack()
                            },
                            onFinishGame = {
                                navigationStack.clear()
                                navigationStack.add(Screen.Dashboard)
                            },
                            viewModel = viewModel
                        )
                    }
                    is Screen.AddRound -> {
                        AddRoundScreen(
                            gameId = targetScreen.gameId,
                            game = activeGame,
                            preselectedTeam = targetScreen.preselectedTeam,
                            onSaveRound = { winner, countType, pts, notes, isCap, isChuch ->
                                viewModel.addRound(winner, countType, pts, notes, isCap, isChuch)
                                navigateBack()
                            },
                            viewModel = viewModel
                        )
                    }
                    is Screen.PlayerStats -> {
                        PlayerStatsScreen(
                            players = players,
                            onCreatePlayer = { name -> viewModel.createPlayer(name) },
                            onDeletePlayer = { player -> viewModel.deletePlayer(player) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(
    players: List<Player>,
    games: List<Game>,
    activeGames: List<Game>,
    onStartNewGame: () -> Unit,
    onResumeGame: (Int) -> Unit,
    onViewStats: () -> Unit,
    onDeleteGame: (Int) -> Unit,
    viewModel: DominoViewModel
) {
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var gameToDeleteId by remember { mutableStateOf<Int?>(null) }
    var showAllMatches by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bandera_pr),
                    contentDescription = "Bandera de Puerto Rico",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            }
        }

        // Active Games Reminder
        if (activeGames.isNotEmpty()) {
            item {
                Text(
                    text = if (activeGames.size > 1) "PARTIDAS EN CURSO (${activeGames.size})" else "PARTIDA EN CURSO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }
            items(activeGames) { gameItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .clickable { onResumeGame(gameItem.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Activo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${gameItem.gameMode}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = if (gameItem.maxPoints == 500 && gameItem.useBonuses) "Meta: 500 pts (Bono)" else "Meta: ${gameItem.maxPoints} pts",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Matchup
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gameItem.team1Name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(text = "${gameItem.team1Score}", fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                            }
                            Text(text = "VS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gameItem.team2Name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(text = "${gameItem.team2Score}", fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${viewModel.getPlayerName(gameItem.player1Id)}" +
                                        (if (gameItem.player2Id != null) " y ${viewModel.getPlayerName(gameItem.player2Id)}" else ""),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Text(
                                text = "${viewModel.getPlayerName(gameItem.player3Id)}" +
                                        (if (gameItem.player4Id != null) " y ${viewModel.getPlayerName(gameItem.player4Id)}" else ""),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Main Actions Row
        item {
            // New game button
            Button(
                onClick = onStartNewGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Nueva Partida", fontWeight = FontWeight.Bold)
            }
        }

        // Leaderboard teaser
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewStats() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Ver Ránking y Estadísticas", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "${players.size} jugadores registrados",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ver",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Historical games section
        item {
            Text(
                text = "Historial de Partidos",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val completedGames = games.filter { it.status == "COMPLETED" || it.status == "CANCELED" }
        if (completedGames.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay partidos finalizados aún.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            val gamesToDisplay = if (showAllMatches) completedGames else completedGames.take(3)
            items(gamesToDisplay) { game ->
                var isExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (game.status == "CANCELED") Color.LightGray.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Modo: ${if (game.gameMode == "PAREJAS") "2v2 Parejas" else "1v1 Individual"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            val sdf = SimpleDateFormat("dd/MMM/yy hh:mm a", Locale.getDefault())
                            Text(
                                text = sdf.format(Date(game.createdTimestamp)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Team 1 details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = game.team1Name,
                                    fontWeight = if (game.winnerTeamIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (game.winnerTeamIndex == 1) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = "Ganador",
                                            modifier = Modifier.size(12.dp),
                                            tint = Color(0xFFEAB308)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(text = "Ganó", fontSize = 11.sp, color = Color(0xFFEAB308))
                                    }
                                }
                            }

                            // Score display
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${game.team1Score}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = if (game.winnerTeamIndex == 1) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                                Text(
                                    text = " - ",
                                    maxLines = 1,
                                    softWrap = false,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "${game.team2Score}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = if (game.winnerTeamIndex == 2) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }

                            // Team 2 details
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = game.team2Name,
                                    fontWeight = if (game.winnerTeamIndex == 2) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (game.winnerTeamIndex == 2) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = "Ganador",
                                            modifier = Modifier.size(12.dp),
                                            tint = Color(0xFFEAB308)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(text = "Ganó", fontSize = 11.sp, color = Color(0xFFEAB308))
                                    }
                                }
                            }
                        }

                        // Expanded View with player names and deletion
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Jugadores:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "• ${viewModel.getPlayerName(game.player1Id)}",
                                        fontSize = 13.sp
                                    )
                                    if (game.player2Id != null) {
                                        Text(
                                            text = "• ${viewModel.getPlayerName(game.player2Id)}",
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "• ${viewModel.getPlayerName(game.player3Id)}",
                                        fontSize = 13.sp
                                    )
                                    if (game.player4Id != null) {
                                        Text(
                                            text = "• ${viewModel.getPlayerName(game.player4Id)}",
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Chiva Indicator
                            val isChiva = (game.team1Score == 0 && game.team2Score > 0 && game.status == "COMPLETED") ||
                                    (game.team2Score == 0 && game.team1Score > 0 && game.status == "COMPLETED")
                            if (isChiva) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🐐 ¡Uffff, terrible Chiva (partido a cero)! 🐐",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF78350F)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { gameToDeleteId = game.id },
                                    colors = ButtonDefaults.textButtonColors(contentColor = BoricuaRed)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Eliminar de Historial", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            if (completedGames.size > 3) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { showAllMatches = !showAllMatches }
                        ) {
                            Icon(
                                imageVector = if (showAllMatches) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showAllMatches) "Mostrar menos" else "Mostrar más",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Card below Historial de Partidas with generated logo_forcomputer
        item {
            Spacer(modifier = Modifier.height(8.dp))
            val uriHandler = LocalUriHandler.current
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clickable {
                            try {
                                uriHandler.openUri("https://ap0calip.github.io/")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_forcomputer),
                            contentDescription = "Logo forcomputer",
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("Haz clic aquí para ver la información del desarrollador ")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("ap0calip.")
                                }
                            },
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // New Player Quick Add Dialog
    if (showAddPlayerDialog) {
        var playerName by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showAddPlayerDialog = false
                errorMessage = ""
            },
            title = { Text("Registrar Nuevo Jugador", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Escribe el nombre del jugador para ingresarlo al sistema:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = {
                            playerName = it
                            if (it.isNotBlank()) errorMessage = ""
                        },
                        label = { Text("Nombre del jugador") },
                        isError = errorMessage.isNotBlank(),
                        supportingText = {
                            if (errorMessage.isNotBlank()) Text(text = errorMessage, color = BoricuaRed)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playerName.isBlank()) {
                            errorMessage = "El nombre no puede estar vacío."
                        } else {
                            viewModel.createPlayer(playerName)
                            showAddPlayerDialog = false
                            playerName = ""
                            errorMessage = ""
                        }
                    }
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddPlayerDialog = false
                    errorMessage = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Confirmation Dialog for Deleting a Game
    if (gameToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { gameToDeleteId = null },
            title = {
                Text(
                    text = "Eliminar Partida",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar esta partida del historial? Esta acción no se puede deshacer.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        gameToDeleteId?.let { id ->
                            onDeleteGame(id)
                        }
                        gameToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BoricuaRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { gameToDeleteId = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ==========================================
// 2. GAME SETUP SCREEN
// ==========================================
@Composable
fun GameSetupScreen(
    players: List<Player>,
    onStartGame: (String, Int, String, String, String, String?, String, String?, Int, Int, Int, Int, Int, Int, Boolean) -> Unit,
    viewModel: DominoViewModel
) {
    val games by viewModel.allGames.collectAsStateWithLifecycle()

    var isCouplesMode by remember { mutableStateOf(true) } // default is parejas 2v2
    var targetScore by remember { mutableStateOf(200) } // default classic PR is 200

    // 500-point rules and bonuses state
    var useBonuses by remember { mutableStateOf(true) }
    var bonusRound1 by remember { mutableStateOf(100) }
    var bonusRound2 by remember { mutableStateOf(75) }
    var bonusRound3 by remember { mutableStateOf(50) }
    var bonusRound4 by remember { mutableStateOf(25) }
    var bonusCapicu by remember { mutableStateOf(100) }
    var bonusChuchazo by remember { mutableStateOf(100) }
    var showBonusDialog by remember { mutableStateOf(false) }

    var team1Name by remember { mutableStateOf("Ell@s") }
    var team2Name by remember { mutableStateOf("Nosotr@s") }

    // Participant names (support autocomplete/dropdown)
    var p1Name by remember { mutableStateOf("") }
    var p2Name by remember { mutableStateOf("") } // partner for team 1
    var p3Name by remember { mutableStateOf("") } // opponent 1
    var p4Name by remember { mutableStateOf("") } // partner for team 2

    var selectedHistoryIndex by remember { mutableStateOf(-1) }

    val historicalProfiles = remember(games, players) {
        games.mapNotNull { game ->
            val p1 = players.find { it.id == game.player1Id }?.name ?: ""
            val p2 = game.player2Id?.let { id -> players.find { it.id == id }?.name } ?: ""
            val p3 = players.find { it.id == game.player3Id }?.name ?: ""
            val p4 = game.player4Id?.let { id -> players.find { it.id == id }?.name } ?: ""
            
            if (p1.isNotBlank() && p3.isNotBlank()) {
                HistoricalProfile(
                    team1 = game.team1Name,
                    team2 = game.team2Name,
                    p1 = p1,
                    p2 = p2,
                    p3 = p3,
                    p4 = p4,
                    gameMode = game.gameMode
                )
            } else {
                null
            }
        }.distinct()
    }

    LaunchedEffect(games, players) {
        val lastGame = games.firstOrNull()
        if (lastGame != null && p1Name.isBlank() && p3Name.isBlank()) {
            val p1 = players.find { it.id == lastGame.player1Id }
            val p2 = lastGame.player2Id?.let { id -> players.find { it.id == id } }
            val p3 = players.find { it.id == lastGame.player3Id }
            val p4 = lastGame.player4Id?.let { id -> players.find { it.id == id } }

            p1Name = p1?.name ?: ""
            p2Name = p2?.name ?: ""
            p3Name = p3?.name ?: ""
            p4Name = p4?.name ?: ""
            isCouplesMode = lastGame.gameMode == "PAREJAS"
            targetScore = lastGame.maxPoints
            team1Name = lastGame.team1Name
            team2Name = lastGame.team2Name
            useBonuses = lastGame.useBonuses
            bonusRound1 = lastGame.bonusRound1
            bonusRound2 = lastGame.bonusRound2
            bonusRound3 = lastGame.bonusRound3
            bonusRound4 = lastGame.bonusRound4
            bonusCapicu = lastGame.bonusCapicu
            bonusChuchazo = lastGame.bonusChuchazo
        }
    }

    var validationError by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // Dialog for custom 500-point rules editing
    if (showBonusDialog) {
        var tempRound1 by remember { mutableStateOf(bonusRound1.toString()) }
        var tempRound2 by remember { mutableStateOf(bonusRound2.toString()) }
        var tempRound3 by remember { mutableStateOf(bonusRound3.toString()) }
        var tempRound4 by remember { mutableStateOf(bonusRound4.toString()) }
        var tempCapicu by remember { mutableStateOf(bonusCapicu.toString()) }
        var tempChuchazo by remember { mutableStateOf(bonusChuchazo.toString()) }

        AlertDialog(
            onDismissRequest = { showBonusDialog = false },
            title = {
                Text(
                    text = "Ajustes de Bonificación (500 pts)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Configura los puntos extra que se otorgarán bajo estas reglas de bonificación.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(text = "Puntos por Ronda Ganada", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tempRound1,
                            onValueChange = { tempRound1 = it },
                            label = { Text("1ra Ronda") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempRound2,
                            onValueChange = { tempRound2 = it },
                            label = { Text("2da Ronda") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tempRound3,
                            onValueChange = { tempRound3 = it },
                            label = { Text("3ra Ronda") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempRound4,
                            onValueChange = { tempRound4 = it },
                            label = { Text("4ta Ronda") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Puntos por Jugadas Especiales", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = tempCapicu,
                        onValueChange = { tempCapicu = it },
                        label = { Text("Ganar con Capicú") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tempChuchazo,
                        onValueChange = { tempChuchazo = it },
                        label = { Text("Ganar con Chuchazo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    TextButton(
                        onClick = {
                            tempRound1 = "100"
                            tempRound2 = "75"
                            tempRound3 = "50"
                            tempRound4 = "25"
                            tempCapicu = "100"
                            tempChuchazo = "100"
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("Restablecer Valores por Defecto", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        bonusRound1 = tempRound1.toIntOrNull() ?: 100
                        bonusRound2 = tempRound2.toIntOrNull() ?: 75
                        bonusRound3 = tempRound3.toIntOrNull() ?: 50
                        bonusRound4 = tempRound4.toIntOrNull() ?: 25
                        bonusCapicu = tempCapicu.toIntOrNull() ?: 100
                        bonusChuchazo = tempChuchazo.toIntOrNull() ?: 100
                        showBonusDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBonusDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuración del Partido",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Game Mode Toggle
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Modo de Juego", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    val couplesColor = if (isCouplesMode) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    else ButtonDefaults.outlinedButtonColors()
                    Button(
                        onClick = { isCouplesMode = true },
                        colors = couplesColor,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Parejas (2v2)", fontSize = 12.sp)
                    }

                    val indColor = if (!isCouplesMode) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    else ButtonDefaults.outlinedButtonColors()
                    Button(
                        onClick = { isCouplesMode = false },
                        colors = indColor,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Individual (1v1)", fontSize = 12.sp)
                    }
                }
            }
        }

        // Score limit Selection
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Puntuación de Victoria (Meta)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "Selecciona jugar partida rápida clásica (200 pts) o con bonificación (500 pts).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val limits = listOf(200, 500)
                    limits.forEach { limit ->
                        val isSel = targetScore == limit
                        FilterChip(
                            selected = isSel,
                            onClick = { targetScore = limit },
                            label = { Text("$limit pts", modifier = Modifier.padding(horizontal = 8.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Customizable Rule panel for 500 points
        if (targetScore == 500) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🏆 Bonificaciones Activas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Se otorgan puntos extra automáticos",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useBonuses,
                            onCheckedChange = { useBonuses = it },
                            modifier = Modifier.testTag("use_bonuses_switch")
                        )
                    }

                    if (useBonuses) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Valores de Bonificación Actuales:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "• Ronda 1: +$bonusRound1 pts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "• Ronda 2: +$bonusRound2 pts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "• Ronda 3: +$bonusRound3 pts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "• Ronda 4: +$bonusRound4 pts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "• Capicú: +$bonusCapicu pts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "• Chuchazo: +$bonusChuchazo pts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { showBonusDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Configurar Bonificaciones", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Player Profiles Input
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Integrantes de los Equipos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Ingresa nombres o carga de historial.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (historicalProfiles.isNotEmpty()) {
                                    val nextIndex = if (selectedHistoryIndex == -1) {
                                        0
                                    } else {
                                        (selectedHistoryIndex + 1) % historicalProfiles.size
                                    }
                                    selectedHistoryIndex = nextIndex
                                    val profile = historicalProfiles[nextIndex]
                                    team1Name = profile.team1
                                    team2Name = profile.team2
                                    p1Name = profile.p1
                                    p2Name = profile.p2
                                    p3Name = profile.p3
                                    p4Name = profile.p4
                                    isCouplesMode = (profile.gameMode == "PAREJAS")
                                }
                            },
                            enabled = historicalProfiles.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Anterior del historial",
                                tint = if (historicalProfiles.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                team1Name = "Ell@s"
                                team2Name = "Nosotr@s"
                                p1Name = ""
                                p2Name = ""
                                p3Name = ""
                                p4Name = ""
                                selectedHistoryIndex = -1
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restablecer nombres predefinidos",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (historicalProfiles.isNotEmpty()) {
                                    val nextIndex = if (selectedHistoryIndex == -1 || selectedHistoryIndex == 0) {
                                        historicalProfiles.size - 1
                                    } else {
                                        selectedHistoryIndex - 1
                                    }
                                    selectedHistoryIndex = nextIndex
                                    val profile = historicalProfiles[nextIndex]
                                    team1Name = profile.team1
                                    team2Name = profile.team2
                                    p1Name = profile.p1
                                    p2Name = profile.p2
                                    p3Name = profile.p3
                                    p4Name = profile.p4
                                    isCouplesMode = (profile.gameMode == "PAREJAS")
                                }
                            },
                            enabled = historicalProfiles.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Siguiente del historial",
                                tint = if (historicalProfiles.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (selectedHistoryIndex != -1 && historicalProfiles.isNotEmpty()) {
                    Text(
                        text = "📂 Cargado: Juego Histórico #${selectedHistoryIndex + 1} (${if (historicalProfiles[selectedHistoryIndex].gameMode == "PAREJAS") "Parejas" else "Individual"})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // --- TEAM 1 ---
                OutlinedTextField(
                    value = team1Name,
                    onValueChange = { team1Name = it },
                    label = { Text("Equipo 1 (Nombre)") },
                    placeholder = { Text("Ell@s") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PlayerDropdownStateInput(
                    label = "Jugador 1",
                    value = p1Name,
                    onValueChange = { p1Name = it },
                    existingPlayers = players
                )

                if (isCouplesMode) {
                    PlayerDropdownStateInput(
                        label = "Pareja Jugador 1",
                        value = p2Name,
                        onValueChange = { p2Name = it },
                        existingPlayers = players
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- TEAM 2 ---
                OutlinedTextField(
                    value = team2Name,
                    onValueChange = { team2Name = it },
                    label = { Text("Equipo 2 (Nombre)") },
                    placeholder = { Text("Nosotr@s") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PlayerDropdownStateInput(
                    label = if (isCouplesMode) "Jugador 3" else "Jugador 2",
                    value = p3Name,
                    onValueChange = { p3Name = it },
                    existingPlayers = players
                )

                if (isCouplesMode) {
                    PlayerDropdownStateInput(
                        label = "Pareja Jugador 3",
                        value = p4Name,
                        onValueChange = { p4Name = it },
                        existingPlayers = players
                    )
                }
            }
        }

        if (validationError.isNotBlank()) {
            Text(
                text = validationError,
                color = BoricuaRed,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Start Game Button
        Button(
            onClick = {
                val finalP1Name = p1Name.trim().ifBlank { "Jugador 1" }
                val finalP2Name = if (isCouplesMode) p2Name.trim().ifBlank { "Jugador 2" } else ""
                val finalP3Name = if (isCouplesMode) p3Name.trim().ifBlank { "Jugador 3" } else p3Name.trim().ifBlank { "Jugador 2" }
                val finalP4Name = if (isCouplesMode) p4Name.trim().ifBlank { "Jugador 4" } else ""

                val nameList = listOfNotNull(
                    finalP1Name.lowercase(),
                    finalP2Name.takeIf { isCouplesMode }?.lowercase(),
                    finalP3Name.lowercase(),
                    finalP4Name.takeIf { isCouplesMode }?.lowercase()
                )

                if (nameList.size != nameList.distinct().size) {
                    validationError = "Los nombres de los jugadores deben ser únicos."
                } else {
                    validationError = ""
                    onStartGame(
                        if (isCouplesMode) "PAREJAS" else "INDIVIDUAL",
                        targetScore,
                        team1Name.trim().ifBlank { "Ell@s" },
                        team2Name.trim().ifBlank { "Nosotr@s" },
                        finalP1Name,
                        finalP2Name.takeIf { isCouplesMode },
                        finalP3Name,
                        finalP4Name.takeIf { isCouplesMode },
                        bonusRound1,
                        bonusRound2,
                        bonusRound3,
                        bonusRound4,
                        bonusCapicu,
                        bonusChuchazo,
                        targetScore == 500 && useBonuses
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.SportsEsports, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "¡Iniciar Partido!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PlayerDropdownStateInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    existingPlayers: List<Player>
) {
    var isExpanded by remember { mutableStateOf(false) }

    val filteredList = if (value.isBlank()) existingPlayers
    else existingPlayers.filter { it.name.lowercase().contains(value.lowercase()) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                isExpanded = true
            },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Sugerencias")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = isExpanded && filteredList.isNotEmpty(),
            onDismissRequest = { isExpanded = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            filteredList.take(5).forEach { player ->
                DropdownMenuItem(
                    text = { Text(player.name) },
                    onClick = {
                        onValueChange(player.name)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

// ==========================================
// 3. ACTIVE GAME SCREEN
// ==========================================
@Composable
fun ActiveGameScreen(
    game: Game?,
    rounds: List<GameRound>,
    onAddRound: (Int, Int?) -> Unit,
    onUndoRound: () -> Unit,
    onCancelGame: () -> Unit,
    onFinishGame: () -> Unit,
    viewModel: DominoViewModel
) {
    if (game == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isFinished = game.status == "COMPLETED"
    var showCancelDialog by remember { mutableStateOf(false) }
    var showUndoDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Grand Celebration if Completed
        if (isFinished) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = NoteYellowContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(Color(0xFFFDE047), RoundedCornerShape(35.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Campeón",
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFFCA8A04)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "¡FIN DEL PARTIDO!",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = Color(0xFF78350F)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val winningTeamName = if (game.winnerTeamIndex == 1) game.team1Name else game.team2Name
                        Text(
                            text = "🏆 $winningTeamName 🏆",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val winningPlayers = if (game.winnerTeamIndex == 1) {
                            "${viewModel.getPlayerName(game.player1Id)}" + (if (game.player2Id != null) " y ${viewModel.getPlayerName(game.player2Id)}" else "")
                        } else {
                            "${viewModel.getPlayerName(game.player3Id)}" + (if (game.player4Id != null) " y ${viewModel.getPlayerName(game.player4Id)}" else "")
                        }
                        Text(
                            text = winningPlayers,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF78350F)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Marcador Final: ${game.team1Score} - ${game.team2Score}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )

                        // Check for Chiva
                        val targetGotChiva = (game.winnerTeamIndex == 1 && game.team2Score == 0) ||
                                (game.winnerTeamIndex == 2 && game.team1Score == 0)

                        if (targetGotChiva) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF991B1B))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🐐 ¡UNA CHIVA LEGENDARIA! (200 - 0) 🐐",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onFinishGame,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volver al Dashboard")
                        }
                    }
                }
            }
        }

        // Live Scoreboard
        item {
            val team1Color = when {
                game.team1Score > game.team2Score -> BoricuaBlue
                game.team1Score < game.team2Score -> BoricuaRed
                else -> MaterialTheme.colorScheme.primary
            }
            val team2Color = when {
                game.team2Score > game.team1Score -> BoricuaBlue
                game.team2Score < game.team1Score -> BoricuaRed
                else -> BoricuaRed
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MARCADOR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Team 1
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (!isFinished) {
                                        Modifier.clickable { onAddRound(game.id, 1) }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = game.team1Name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = team1Color
                            )
                            Text(
                                text = "${game.team1Score}",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false,
                                color = team1Color
                            )
                            Text(
                                text = "${viewModel.getPlayerName(game.player1Id)}" +
                                        (if (game.player2Id != null) " y ${viewModel.getPlayerName(game.player2Id)}" else ""),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Divider VS
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.4f)) {
                            Text(
                                text = "vs",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(60.dp)
                                    .background(Color.LightGray)
                            )
                        }

                        // Team 2
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (!isFinished) {
                                        Modifier.clickable { onAddRound(game.id, 2) }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = game.team2Name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = team2Color
                            )
                            Text(
                                text = "${game.team2Score}",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false,
                                color = team2Color
                            )
                            Text(
                                text = "${viewModel.getPlayerName(game.player3Id)}" +
                                        (if (game.player4Id != null) " y ${viewModel.getPlayerName(game.player4Id)}" else ""),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (game.maxPoints == 500 && game.useBonuses) "Meta: 500 pts (Bono)" else "Se juega a: ${game.maxPoints} puntos",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Mid-game action buttons
        if (!isFinished) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Score Round Button
                    Button(
                        onClick = { onAddRound(game.id, null) },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DominoMint)
                    ) {
                        Icon(imageVector = Icons.Default.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Anotar Ronda", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }

                    // Undo Button
                    if (rounds.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showUndoDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(imageVector = Icons.Default.Undo, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Deshacer", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Round history section
        item {
            Text(
                text = "Registro de Rondas / Manos",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (rounds.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Aún no hay rondas anotadas en esta partida. ¡Empieza tirando la chiva!",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(rounds.reversed()) { round ->
                val isTeam1 = round.winnerTeamIndex == 1
                val roundWinnerName = if (isTeam1) game.team1Name else game.team2Name
                
                // Calculate dynamic colors according to the current scoreboard
                val team1Color = when {
                    game.team1Score > game.team2Score -> BoricuaBlue
                    game.team1Score < game.team2Score -> BoricuaRed
                    else -> MaterialTheme.colorScheme.primary
                }
                val team2Color = when {
                    game.team2Score > game.team1Score -> BoricuaBlue
                    game.team2Score < game.team1Score -> BoricuaRed
                    else -> BoricuaRed
                }
                val winColor = if (isTeam1) team1Color else team2Color

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isTeam1) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Card(modifier = Modifier.fillMaxWidth(0.92f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isTeam1) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(winColor.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "M${round.roundNumber}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = winColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Column(
                                horizontalAlignment = if (isTeam1) Alignment.Start else Alignment.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = roundWinnerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = winColor,
                                    textAlign = if (isTeam1) TextAlign.Start else TextAlign.End
                                )
                                Text(
                                    text = "${round.points} pts",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = winColor,
                                    textAlign = if (isTeam1) TextAlign.Start else TextAlign.End
                                )

                                val detailsList = mutableListOf<String>()
                                if (round.winType != "DOMINACION" && !round.winType.equals("Dominación", ignoreCase = true)) {
                                    detailsList.add(round.winType.capitalized())
                                }
                                if (round.isCapicu) detailsList.add("Capicú")
                                if (round.isChuchazo) detailsList.add("Chuchazo")

                                val extraNotes = round.notes?.trim()
                                if (!extraNotes.isNullOrBlank() && !extraNotes.equals("Dominación", ignoreCase = true) && !extraNotes.equals("DOMINACION", ignoreCase = true)) {
                                    detailsList.add(extraNotes)
                                }

                                if (detailsList.isNotEmpty()) {
                                    Text(
                                        text = detailsList.joinToString(", "),
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        textAlign = if (isTeam1) TextAlign.Start else TextAlign.End
                                    )
                                }
                                if (round.bonusPoints > 0) {
                                    Text(
                                        text = "${round.basePoints} base | ${round.bonusPoints} bono",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = if (isTeam1) TextAlign.Start else TextAlign.End
                                    )
                                }
                            }

                            if (!isTeam1) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(winColor.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "M${round.roundNumber}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = winColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cancel / Exit Button
        if (!isFinished) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BoricuaRed),
                    border = BorderStroke(1.5.dp, BoricuaRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancelar y salir de la partida")
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("¿Cancelar partida actual?") },
            text = {
                Text(
                    "Esta acción cancelará la sesión activa del juego y guardará como cancelada en el historial. No se actualizarán récords finales."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BoricuaRed)
                ) {
                    Text("Sí, Cancelar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, Seguir Jugando")
                }
            }
        )
    }

    if (showUndoDialog) {
        AlertDialog(
            onDismissRequest = { showUndoDialog = false },
            title = { Text("¿Deshacer última ronda?") },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar la última ronda anotada? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUndoDialog = false
                        onUndoRound()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BoricuaRed)
                ) {
                    Text("Sí, Deshacer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUndoDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ==========================================
// 4. ADD ROUND SCREEN (WITH CALCULATOR)
// ==========================================
val NoteYellowContainer = Color(0xFFFEF08A)

@Composable
fun AddRoundScreen(
    gameId: Int,
    game: Game?,
    preselectedTeam: Int? = null,
    onSaveRound: (Int, String, Int, String, Boolean, Boolean) -> Unit,
    viewModel: DominoViewModel
) {
    if (game == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var selectedWinnerIndex by remember { mutableStateOf(preselectedTeam ?: 1) } // Default Team 1
    var pointsInputText by remember { mutableStateOf("") }
    val pointsScored = pointsInputText.toIntOrNull() ?: 0

    var isCapicu by remember { mutableStateOf(false) }
    var isChuchazo by remember { mutableStateOf(false) }

    val rounds by viewModel.activeGameRounds.collectAsStateWithLifecycle()
    val nextRoundNum = rounds.size + 1

    var validationError by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
    }

    val handleSaveRound = {
        if (pointsScored <= 0) {
            validationError = "Por favor ingresa un puntaje válido para la mano."
        } else if (pointsScored > 180) {
            validationError = "El puntaje es inusitadamente alto para una sola mano (máx 168)."
        } else {
            validationError = ""
            onSaveRound(selectedWinnerIndex, "DOMINACION", pointsScored, "Dominación", isCapicu, isChuchazo)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Guardar Resultados de Mano",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // 1. Who Won?
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "¿Quién ganó la mano?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    val team1Color = when {
                        game.team1Score > game.team2Score -> BoricuaBlue
                        game.team1Score < game.team2Score -> BoricuaRed
                        else -> MaterialTheme.colorScheme.primary
                    }
                    val team2Color = when {
                        game.team2Score > game.team1Score -> BoricuaBlue
                        game.team2Score < game.team1Score -> BoricuaRed
                        else -> BoricuaRed
                    }

                    val t1Color = if (selectedWinnerIndex == 1) ButtonDefaults.buttonColors(containerColor = team1Color)
                    else ButtonDefaults.outlinedButtonColors()
                    Button(
                      onClick = { selectedWinnerIndex = 1 },
                      colors = t1Color,
                      modifier = Modifier
                          .weight(1f)
                          .padding(end = 6.dp),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(game.team1Name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    val t2Color = if (selectedWinnerIndex == 2) ButtonDefaults.buttonColors(containerColor = team2Color)
                    else ButtonDefaults.outlinedButtonColors()
                    Button(
                      onClick = { selectedWinnerIndex = 2 },
                      colors = t2Color,
                      modifier = Modifier
                          .weight(1f)
                          .padding(start = 6.dp),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(game.team2Name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // 2. Special Bonuses (Now above Score input)
        if (game.useBonuses) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Bonificaciones Especiales", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCapicu,
                            onCheckedChange = { isCapicu = it },
                            modifier = Modifier.testTag("capicu_checkbox")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = "Capicú (+${game.bonusCapicu} pts)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "Ganar por ambos lados abiertos", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChuchazo,
                            onCheckedChange = { isChuchazo = it },
                            modifier = Modifier.testTag("chuchazo_checkbox")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = "Chuchazo (+${game.bonusChuchazo} pts)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "Ganar cerrando con el doble seis", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // 3. Score Input Field (Puntos Regulares) - Now below Bonuses
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Puntos Regulares", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pointsInputText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            pointsInputText = newValue
                        }
                    },
                    label = { Text("Puntos anotados en la mano") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleSaveRound() }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("points_input_field")
                )
            }
        }

        // 4. Score Breakdown
        val winnerTeamName = if (selectedWinnerIndex == 1) game.team1Name else game.team2Name
        var roundBonusAmount = 0
        if (game.useBonuses) {
            when (nextRoundNum) {
                1 -> roundBonusAmount = game.bonusRound1
                2 -> roundBonusAmount = game.bonusRound2
                3 -> roundBonusAmount = game.bonusRound3
                4 -> roundBonusAmount = game.bonusRound4
            }
        }
        val capicuBonusAmount = if (game.useBonuses && isCapicu) game.bonusCapicu else 0
        val chuchazoBonusAmount = if (game.useBonuses && isChuchazo) game.bonusChuchazo else 0
        val totalManoScore = pointsScored + roundBonusAmount + capicuBonusAmount + chuchazoBonusAmount

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📊 Desglose de Puntos para $winnerTeamName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Puntos base (mano):", fontSize = 12.sp)
                    Text(text = "$pointsScored pts", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                if (game.useBonuses) {
                    if (roundBonusAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Bonificación de Ronda $nextRoundNum:", fontSize = 12.sp, color = BoricuaBlue)
                            Text(text = "+$roundBonusAmount pts", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BoricuaBlue)
                        }
                    }
                    if (capicuBonusAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Bonificación por Capicú:", fontSize = 12.sp, color = BoricuaBlue)
                            Text(text = "+$capicuBonusAmount pts", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BoricuaBlue)
                        }
                    }
                    if (chuchazoBonusAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Bonificación por Chuchazo:", fontSize = 12.sp, color = BoricuaBlue)
                            Text(text = "+$chuchazoBonusAmount pts", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BoricuaBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Total para esta mano:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "$totalManoScore pts",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (validationError.isNotBlank()) {
            Text(
                text = validationError,
                color = BoricuaRed,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Action Button: Save Round
        Button(
            onClick = { handleSaveRound() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Guardar Mano", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 5. PLAYER STATISTICS / LEADERBOARD SCREEN
// ==========================================
@Composable
fun PlayerStatsScreen(
    players: List<Player>,
    onCreatePlayer: (String) -> Unit,
    onDeletePlayer: (Player) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sortByGamesWon by remember { mutableStateOf(true) } // true: gamesWon, false: winRatio

    var showAddPlayerDialog by remember { mutableStateOf(false) }

    val filteredPlayers = remember(players, query, sortByGamesWon) {
        val filtered = if (query.isBlank()) players
        else players.filter { it.name.lowercase().contains(query.lowercase()) }

        if (sortByGamesWon) {
            filtered.sortedByDescending { it.gamesWon }
        } else {
            filtered.sortedByDescending {
                if (it.gamesPlayed == 0) 0f else (it.gamesWon.toFloat() / it.gamesPlayed.toFloat())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Líderes de la Traba 🇵🇷",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Button(onClick = { showAddPlayerDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo")
            }
        }

        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar jugador...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Sorting Switcher
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = sortByGamesWon,
                    onClick = { sortByGamesWon = true },
                    label = { Text("Partidos Ganados") }
                )
                FilterChip(
                    selected = !sortByGamesWon,
                    onClick = { sortByGamesWon = false },
                    label = { Text("% de Victoria") }
                )
            }
        }

        // Leaderboard List
        if (filteredPlayers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ningún jugador coincide.",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(filteredPlayers) { index, player ->
                    var showDetailDialog by remember { mutableStateOf(false) }

                    // Decorative rank badge color
                    val badgeColor = when (index) {
                        0 -> Color(0xFFEAB308) // Gold
                        1 -> Color(0xFF94A3B8) // Silver
                        2 -> Color(0xFFD97706) // Bronze
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val badgeTextColor = when (index) {
                        0, 1, 2 -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDetailDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank Badge
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(badgeColor, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = badgeTextColor
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = player.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val ratio = if (player.gamesPlayed == 0) 0f else (player.gamesWon.toFloat() / player.gamesPlayed.toFloat() * 100)
                                Text(
                                    text = "Racha: ${player.gamesWon}V - ${player.gamesPlayed - player.gamesWon}D  |  ${String.format("%.1f", ratio)}% Vic",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            // Chivas given statistic
                            if (player.chivasGiven > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "🐐 ${player.chivasGiven} Chivas",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = Color(0xFF78350F)
                                    )
                                }
                            }
                        }
                    }

                    // Detailed Player Stats popup Dialog
                    if (showDetailDialog) {
                        AlertDialog(
                            onDismissRequest = { showDetailDialog = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            title = { Text(player.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "ESTADÍSTICAS DEL JUGADOR",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Divider()

                                    StatRow(label = "Partidos Jugados:", value = "${player.gamesPlayed}")
                                    StatRow(label = "Partidos Ganados:", value = "${player.gamesWon}")
                                    val winRatio = if (player.gamesPlayed == 0) 0f else (player.gamesWon.toFloat() / player.gamesPlayed.toFloat() * 100)
                                    StatRow(label = "Efectividad Victoria:", value = "${String.format("%.1f", winRatio)}%")

                                    Divider()

                                    StatRow(label = "Manoes / Rondas Jugadas:", value = "${player.roundsPlayed}")
                                    StatRow(label = "Manos Ganadas:", value = "${player.roundsWon}")
                                    val roundRatio = if (player.roundsPlayed == 0) 0f else (player.roundsWon.toFloat() / player.roundsPlayed.toFloat() * 100)
                                    StatRow(label = "Rendimiento Manos:", value = "${String.format("%.1f", roundRatio)}%")

                                    Divider()

                                    StatRow(label = "Puntos Traídos a Mesa:", value = "${player.totalPointsScored}")
                                    StatRow(label = "Chivas Propinadas (Dadas):", value = "🐐 ${player.chivasGiven}")
                                    StatRow(label = "Chivas Recibidas:", value = "🐐 ${player.chivasReceived}")
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showDetailDialog = false }) {
                                    Text("Regresar")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        onDeletePlayer(player)
                                        showDetailDialog = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = BoricuaRed)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remover de Club", fontSize = 12.sp)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddPlayerDialog) {
        var playerName by remember { mutableStateOf("") }
        var errorState by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showAddPlayerDialog = false
                errorState = ""
            },
            title = { Text("Registrar Nuevo Jugador") },
            text = {
                OutlinedTextField(
                    value = playerName,
                    onValueChange = {
                        playerName = it
                        if (it.isNotBlank()) errorState = ""
                    },
                    label = { Text("Nombre Completo") },
                    isError = errorState.isNotBlank(),
                    supportingText = { if (errorState.isNotBlank()) Text(text = errorState, color = BoricuaRed) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playerName.isBlank()) {
                            errorState = "No puede quedar vacío"
                        } else {
                            onCreatePlayer(playerName)
                            showAddPlayerDialog = false
                            playerName = ""
                            errorState = ""
                        }
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddPlayerDialog = false
                    errorState = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// String extension for titles capitalization
fun String.capitalized(): String {
    return this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

data class HistoricalProfile(
    val team1: String,
    val team2: String,
    val p1: String,
    val p2: String,
    val p3: String,
    val p4: String,
    val gameMode: String
)
