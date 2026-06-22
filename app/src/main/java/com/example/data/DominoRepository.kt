package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class DominoRepository(private val dominoDao: DominoDao) {

    val allPlayers: Flow<List<Player>> = dominoDao.getAllPlayers()
    val allGames: Flow<List<Game>> = dominoDao.getAllGames()
    val activeGames: Flow<List<Game>> = dominoDao.getActiveGamesFlow()

    suspend fun getPlayerById(id: Int): Player? = dominoDao.getPlayerById(id)

    suspend fun getOrCreatePlayer(name: String): Player {
        val trimmed = name.trim()
        val existing = dominoDao.getPlayerByName(trimmed)
        if (existing != null) {
            return existing
        }
        val newPlayer = Player(name = trimmed)
        val id = dominoDao.insertPlayer(newPlayer)
        return newPlayer.copy(id = id.toInt())
    }

    suspend fun insertPlayer(player: Player): Long = dominoDao.insertPlayer(player)
    suspend fun deletePlayer(player: Player) = dominoDao.deletePlayer(player)

    suspend fun getGameById(id: Int): Game? = dominoDao.getGameById(id)

    suspend fun startNewGame(
        gameMode: String,
        maxPoints: Int,
        team1Name: String,
        team2Name: String,
        player1Id: Int,
        player2Id: Int?,
        player3Id: Int,
        player4Id: Int?,
        bonusRound1: Int = 100,
        bonusRound2: Int = 75,
        bonusRound3: Int = 50,
        bonusRound4: Int = 25,
        bonusCapicu: Int = 100,
        bonusChuchazo: Int = 100,
        useBonuses: Boolean = false
    ): Long {
        // First cancel any previous ACTIVE games, to keep it clean (only one active game at a time)
        val active = dominoDao.getActiveGame()
        if (active != null) {
            dominoDao.updateGame(active.copy(status = "CANCELED", completedTimestamp = System.currentTimeMillis()))
        }

        val game = Game(
            gameMode = gameMode,
            maxPoints = maxPoints,
            team1Name = team1Name,
            team2Name = team2Name,
            player1Id = player1Id,
            player2Id = player2Id,
            player3Id = player3Id,
            player4Id = player4Id,
            team1Score = 0,
            team2Score = 0,
            status = "ACTIVE",
            bonusRound1 = bonusRound1,
            bonusRound2 = bonusRound2,
            bonusRound3 = bonusRound3,
            bonusRound4 = bonusRound4,
            bonusCapicu = bonusCapicu,
            bonusChuchazo = bonusChuchazo,
            useBonuses = useBonuses
        )
        return dominoDao.insertGame(game)
    }

    suspend fun getRoundsForGameSync(gameId: Int) = dominoDao.getRoundsForGameSync(gameId)
    fun getRoundsForGame(gameId: Int): Flow<List<GameRound>> = dominoDao.getRoundsForGame(gameId)

    suspend fun addRoundToGame(
        gameId: Int,
        winnerTeamIndex: Int,
        winType: String,
        points: Int,
        notes: String? = "",
        isCapicu: Boolean = false,
        isChuchazo: Boolean = false
    ): Game? {
        val game = dominoDao.getGameById(gameId) ?: return null
        if (game.status != "ACTIVE") return game

        // Create the new round
        val existingRounds = dominoDao.getRoundsForGameSync(gameId)
        val nextRoundNum = existingRounds.size + 1
        
        var roundBonusNum = 0
        var capicuBonusNum = 0
        var chuchazoBonusNum = 0
        
        if (game.useBonuses) {
            roundBonusNum = when (nextRoundNum) {
                1 -> game.bonusRound1
                2 -> game.bonusRound2
                3 -> game.bonusRound3
                4 -> game.bonusRound4
                else -> 0
            }
            if (isCapicu) {
                capicuBonusNum = game.bonusCapicu
            }
            if (isChuchazo) {
                chuchazoBonusNum = game.bonusChuchazo
            }
        }
        
        val totalBonus = roundBonusNum + capicuBonusNum + chuchazoBonusNum
        val totalRoundPoints = points + totalBonus

        val newRound = GameRound(
            gameId = gameId,
            roundNumber = nextRoundNum,
            winnerTeamIndex = winnerTeamIndex,
            winType = winType,
            points = totalRoundPoints,
            notes = notes,
            isCapicu = isCapicu,
            isChuchazo = isChuchazo,
            basePoints = points,
            bonusPoints = totalBonus
        )
        dominoDao.insertRound(newRound)

        // Update scores
        val newTeam1Score = if (winnerTeamIndex == 1) game.team1Score + totalRoundPoints else game.team1Score
        val newTeam2Score = if (winnerTeamIndex == 2) game.team2Score + totalRoundPoints else game.team2Score

        // Check if finished
        val isFinished = newTeam1Score >= game.maxPoints || newTeam2Score >= game.maxPoints
        val finalStatus = if (isFinished) "COMPLETED" else "ACTIVE"
        val winnerTeam = if (isFinished) {
            if (newTeam1Score >= game.maxPoints) 1 else 2
        } else {
            null
        }

        val updatedGame = game.copy(
            team1Score = newTeam1Score,
            team2Score = newTeam2Score,
            status = finalStatus,
            winnerTeamIndex = winnerTeam,
            completedTimestamp = if (isFinished) System.currentTimeMillis() else null
        )

        dominoDao.updateGame(updatedGame)

        // Update individual round statistics for all players in this game
        updatePlayersRoundStats(game, winnerTeamIndex, totalRoundPoints)

        // If game is completed, update match statistics too
        if (isFinished && winnerTeam != null) {
            updatePlayersGameEndStats(updatedGame, winnerTeam)
        }

        return updatedGame
    }

    suspend fun deleteLastRoundOfGame(gameId: Int): Game? {
        val game = dominoDao.getGameById(gameId) ?: return null
        val existingRounds = dominoDao.getRoundsForGameSync(gameId)
        if (existingRounds.isEmpty()) return game

        val lastRound = existingRounds.last()
        dominoDao.deleteRound(lastRound)

        // Recalculate game scores from remaining rounds
        val remainingRounds = dominoDao.getRoundsForGameSync(gameId)
        var newTeam1Score = 0
        var newTeam2Score = 0
        remainingRounds.forEach { r ->
            if (r.winnerTeamIndex == 1) newTeam1Score += r.points
            else newTeam2Score += r.points
        }

        // Revert any stats if the game was marked "COMPLETED"; though simplest is to only allow delete last round during ACTIVE sessions
        // We will reset game status to "ACTIVE" and recalculate
        val updatedGame = game.copy(
            team1Score = newTeam1Score,
            team2Score = newTeam2Score,
            status = "ACTIVE",
            winnerTeamIndex = null,
            completedTimestamp = null
        )
        dominoDao.updateGame(updatedGame)

        // To keep player stats perfectly correct, we'd need to subtract,
        // but for simplicity in scoreboards, we subtract round stats:
        revertRoundStats(game, lastRound.winnerTeamIndex, lastRound.points)

        return updatedGame
    }

    private suspend fun updatePlayersRoundStats(game: Game, winnerTeamIndex: Int, points: Int) {
        val playerIds = listOfNotNull(game.player1Id, game.player2Id, game.player3Id, game.player4Id)
        for (pid in playerIds) {
            val p = dominoDao.getPlayerById(pid) ?: continue
            val isWinnerTeam = when (winnerTeamIndex) {
                1 -> pid == game.player1Id || pid == game.player2Id
                2 -> pid == game.player3Id || pid == game.player4Id
                else -> false
            }

            val updatedPlayer = p.copy(
                roundsPlayed = p.roundsPlayed + 1,
                roundsWon = if (isWinnerTeam) p.roundsWon + 1 else p.roundsWon,
                totalPointsScored = if (isWinnerTeam) p.totalPointsScored + points else p.totalPointsScored
            )
            dominoDao.updatePlayer(updatedPlayer)
        }
    }

    private suspend fun revertRoundStats(game: Game, winnerTeamIndex: Int, points: Int) {
        val playerIds = listOfNotNull(game.player1Id, game.player2Id, game.player3Id, game.player4Id)
        for (pid in playerIds) {
            val p = dominoDao.getPlayerById(pid) ?: continue
            val isWinnerTeam = when (winnerTeamIndex) {
                1 -> pid == game.player1Id || (game.player2Id != null && pid == game.player2Id)
                2 -> pid == game.player3Id || (game.player4Id != null && pid == game.player4Id)
                else -> false
            }

            val updatedPlayer = p.copy(
                roundsPlayed = maxOf(0, p.roundsPlayed - 1),
                roundsWon = if (isWinnerTeam) maxOf(0, p.roundsWon - 1) else p.roundsWon,
                totalPointsScored = if (isWinnerTeam) maxOf(0, p.totalPointsScored - points) else p.totalPointsScored
            )
            dominoDao.updatePlayer(updatedPlayer)
        }
    }

    private suspend fun updatePlayersGameEndStats(game: Game, winnerTeamIndex: Int) {
        val team1Ids = listOfNotNull(game.player1Id, game.player2Id)
        val team2Ids = listOfNotNull(game.player3Id, game.player4Id)

        val team1Won = winnerTeamIndex == 1
        val team2Won = winnerTeamIndex == 2

        val team1Score = game.team1Score
        val team2Score = game.team2Score

        // Check for "chivas"
        // A "chiva" occurs when opponents score exactly 0 points in a full match!
        val team1GotChiva = team2Won && team1Score == 0
        val team2GotChiva = team1Won && team2Score == 0

        // Team 1 players
        for (pid in team1Ids) {
            val p = dominoDao.getPlayerById(pid) ?: continue
            val updatedPlayer = p.copy(
                gamesPlayed = p.gamesPlayed + 1,
                gamesWon = if (team1Won) p.gamesWon + 1 else p.gamesWon,
                chivasGiven = if (team2GotChiva) p.chivasGiven + 1 else p.chivasGiven,
                chivasReceived = if (team1GotChiva) p.chivasReceived + 1 else p.chivasReceived
            )
            dominoDao.updatePlayer(updatedPlayer)
        }

        // Team 2 players
        for (pid in team2Ids) {
            val p = dominoDao.getPlayerById(pid) ?: continue
            val updatedPlayer = p.copy(
                gamesPlayed = p.gamesPlayed + 1,
                gamesWon = if (team2Won) p.gamesWon + 1 else p.gamesWon,
                chivasGiven = if (team1GotChiva) p.chivasGiven + 1 else p.chivasGiven,
                chivasReceived = if (team2GotChiva) p.chivasReceived + 1 else p.chivasReceived
            )
            dominoDao.updatePlayer(updatedPlayer)
        }
    }

    suspend fun cancelActiveGame(gameId: Int) {
        val game = dominoDao.getGameById(gameId) ?: return
        if (game.status == "ACTIVE") {
            dominoDao.updateGame(game.copy(status = "CANCELED", completedTimestamp = System.currentTimeMillis()))
        }
    }

    suspend fun deleteGameAndRounds(gameId: Int) {
        val game = dominoDao.getGameById(gameId) ?: return
        // If the game was COMPLETED, we'd ideally revert matches stats from players,
        // but simple complete deletion handles removing from history. Let's do a simple delete.
        dominoDao.deleteRoundsForGame(gameId)
        dominoDao.deleteGame(game)
    }
}
