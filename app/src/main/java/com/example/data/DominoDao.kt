package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DominoDao {

    // --- Players ---
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAllPlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE id = :id LIMIT 1")
    suspend fun getPlayerById(id: Int): Player?

    @Query("SELECT * FROM players WHERE name = :name LIMIT 1")
    suspend fun getPlayerByName(name: String): Player?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player): Long

    @Update
    suspend fun updatePlayer(player: Player)

    @Delete
    suspend fun deletePlayer(player: Player)

    // --- Games ---
    @Query("SELECT * FROM games ORDER BY createdTimestamp DESC")
    fun getAllGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveGame(): Game?

    @Query("SELECT * FROM games WHERE status = 'ACTIVE' ORDER BY createdTimestamp DESC")
    fun getActiveGamesFlow(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: Int): Game?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game): Long

    @Update
    suspend fun updateGame(game: Game)

    @Delete
    suspend fun deleteGame(game: Game)

    // --- Rounds ---
    @Query("SELECT * FROM game_rounds WHERE gameId = :gameId ORDER BY roundNumber ASC")
    fun getRoundsForGame(gameId: Int): Flow<List<GameRound>>

    @Query("SELECT * FROM game_rounds WHERE gameId = :gameId ORDER BY roundNumber ASC")
    suspend fun getRoundsForGameSync(gameId: Int): List<GameRound>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: GameRound): Long

    @Delete
    suspend fun deleteRound(round: GameRound)

    @Query("DELETE FROM game_rounds WHERE gameId = :gameId")
    suspend fun deleteRoundsForGame(gameId: Int)
}
