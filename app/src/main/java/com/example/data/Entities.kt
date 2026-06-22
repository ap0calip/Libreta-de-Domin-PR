package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val roundsPlayed: Int = 0,
    val roundsWon: Int = 0,
    val totalPointsScored: Int = 0,
    val chivasGiven: Int = 0,
    val chivasReceived: Int = 0,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gameMode: String, // "PAREJAS" (2v2) or "INDIVIDUAL" (1v1)
    val maxPoints: Int = 200, // target score (e.g. 100, 150, 200, 300)
    val team1Name: String,
    val team2Name: String,
    val player1Id: Int,
    val player2Id: Int? = null, // partner of P1 in 2v2
    val player3Id: Int,         // opponent 1
    val player4Id: Int? = null, // opponent 2
    val team1Score: Int = 0,
    val team2Score: Int = 0,
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED"
    val winnerTeamIndex: Int? = null, // 1 or 2
    val createdTimestamp: Long = System.currentTimeMillis(),
    val completedTimestamp: Long? = null,
    
    // 500-point rules and bonuses (Boricua mode)
    val bonusRound1: Int = 100,
    val bonusRound2: Int = 75,
    val bonusRound3: Int = 50,
    val bonusRound4: Int = 25,
    val bonusCapicu: Int = 100,
    val bonusChuchazo: Int = 100,
    val useBonuses: Boolean = false
)

@Entity(tableName = "game_rounds")
data class GameRound(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gameId: Int,
    val roundNumber: Int,
    val winnerTeamIndex: Int, // 1 or 2
    val winType: String, // "DOMINACION" or "TRANQUE"
    val points: Int, // points won in this hand (total including bonuses)
    val openerId: Int? = null, // who started the round
    val notes: String? = "",
    val timestamp: Long = System.currentTimeMillis(),
    
    // Bonus-specific details
    val isCapicu: Boolean = false,
    val isChuchazo: Boolean = false,
    val basePoints: Int = 0,
    val bonusPoints: Int = 0
)
