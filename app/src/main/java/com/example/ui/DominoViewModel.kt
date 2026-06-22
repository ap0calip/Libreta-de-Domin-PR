package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DominoViewModel(private val repository: DominoRepository) : ViewModel() {

    // --- Core Data Flows ---
    val allPlayers: StateFlow<List<Player>> = repository.allPlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGames: StateFlow<List<Game>> = repository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGames: StateFlow<List<Game>> = repository.activeGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Game Session State ---
    private val _activeGameId = MutableStateFlow<Int?>(null)
    val activeGameId: StateFlow<Int?> = _activeGameId.asStateFlow()

    val activeGame: StateFlow<Game?> = combine(activeGames, _activeGameId) { activeList, id ->
        // Direct match from active games, or query specific if needed
        if (id != null) {
            repository.getGameById(id)
        } else {
            activeList.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeGameRounds: StateFlow<List<GameRound>> = _activeGameId
        .flatMapLatest { id ->
            if (id != null) repository.getRoundsForGame(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Initialize active game check on startup
    init {
        viewModelScope.launch {
            val dbActive = repository.activeGames.firstOrNull()?.firstOrNull()
            if (dbActive != null) {
                _activeGameId.value = dbActive.id
            }
        }
    }

    // --- On-the-fly Player creation ---
    fun createPlayer(name: String, onComplete: (Player) -> Unit = {}) {
        viewModelScope.launch {
            val p = repository.getOrCreatePlayer(name)
            onComplete(p)
        }
    }

    // --- Game Lifecycle Actions ---
    fun startNewGame(
        gameMode: String,
        maxPoints: Int,
        team1Name: String,
        team2Name: String,
        p1Name: String,
        p2Name: String?,
        p3Name: String,
        p4Name: String?,
        bonusRound1: Int = 100,
        bonusRound2: Int = 75,
        bonusRound3: Int = 50,
        bonusRound4: Int = 25,
        bonusCapicu: Int = 100,
        bonusChuchazo: Int = 100,
        useBonuses: Boolean = false
    ) {
        viewModelScope.launch {
            val p1 = repository.getOrCreatePlayer(p1Name)
            val p2 = p2Name?.let { if (it.isNotBlank()) repository.getOrCreatePlayer(it) else null }
            val p3 = repository.getOrCreatePlayer(p3Name)
            val p4 = p4Name?.let { if (it.isNotBlank()) repository.getOrCreatePlayer(it) else null }

            val gid = repository.startNewGame(
                gameMode = gameMode,
                maxPoints = maxPoints,
                team1Name = team1Name.ifBlank { if (gameMode == "PAREJAS") "Ell@s" else p1.name },
                team2Name = team2Name.ifBlank { if (gameMode == "PAREJAS") "Nosotr@s" else p3.name },
                player1Id = p1.id,
                player2Id = p2?.id,
                player3Id = p3.id,
                player4Id = p4?.id,
                bonusRound1 = bonusRound1,
                bonusRound2 = bonusRound2,
                bonusRound3 = bonusRound3,
                bonusRound4 = bonusRound4,
                bonusCapicu = bonusCapicu,
                bonusChuchazo = bonusChuchazo,
                useBonuses = useBonuses
            )
            _activeGameId.value = gid.toInt()
        }
    }

    fun resumeGame(gameId: Int) {
        _activeGameId.value = gameId
    }

    fun addRound(
        winnerTeamIndex: Int,
        winType: String,
        points: Int,
        notes: String? = "",
        isCapicu: Boolean = false,
        isChuchazo: Boolean = false
    ) {
        val gameId = _activeGameId.value ?: return
        viewModelScope.launch {
            repository.addRoundToGame(gameId, winnerTeamIndex, winType, points, notes, isCapicu, isChuchazo)
        }
    }

    fun undoLastRound() {
        val gameId = _activeGameId.value ?: return
        viewModelScope.launch {
            repository.deleteLastRoundOfGame(gameId)
        }
    }

    fun cancelActiveGame() {
        val gameId = _activeGameId.value ?: return
        viewModelScope.launch {
            repository.cancelActiveGame(gameId)
            _activeGameId.value = null
        }
    }

    fun deleteGame(gameId: Int) {
        viewModelScope.launch {
            repository.deleteGameAndRounds(gameId)
            if (_activeGameId.value == gameId) {
                _activeGameId.value = null
            }
        }
    }

    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            repository.deletePlayer(player)
        }
    }

    // --- On-the-fly calculations for players name lookup in active game ---
    fun getPlayerName(id: Int?): String {
        if (id == null) return ""
        return allPlayers.value.find { it.id == id }?.name ?: "Jugador $id"
    }
}

class DominoViewModelFactory(private val repository: DominoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DominoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DominoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
