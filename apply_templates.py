import re

app_file = "app/src/main/java/com/example/ui/DominoApp.kt"
with open(app_file, "r", encoding="utf-8") as f:
    content = f.read()

# Let's manually replace a few key formatted strings
reps = {
    '"${players.size} jugadores registrados"': 'stringResource(id = R.string.jugadores_registrados_format, players.size)',
    '"$limit pts"': 'stringResource(id = R.string.puntos_format, limit)',
    '"• Ronda 1: +$bonusRound1 pts"': 'stringResource(id = R.string.ronda_1_bonus_format, bonusRound1)',
    '"• Ronda 2: +$bonusRound2 pts"': 'stringResource(id = R.string.ronda_2_bonus_format, bonusRound2)',
    '"• Ronda 3: +$bonusRound3 pts"': 'stringResource(id = R.string.ronda_3_bonus_format, bonusRound3)',
    '"• Ronda 4: +$bonusRound4 pts"': 'stringResource(id = R.string.ronda_4_bonus_format, bonusRound4)',
    '"• Capicú: +$bonusCapicu pts"': 'stringResource(id = R.string.capicu_bonus_format, bonusCapicu)',
    '"• Chuchazo: +$bonusChuchazo pts"': 'stringResource(id = R.string.chuchazo_bonus_format, bonusChuchazo)',
    '"🏆 $winningTeamName 🏆"': 'stringResource(id = R.string.winning_team_format, winningTeamName)',
    '"Marcador Final: ${game.team1Score} - ${game.team2Score}"': 'stringResource(id = R.string.marcador_final_format, game.team1Score, game.team2Score)',
    '"M${round.roundNumber}"': 'stringResource(id = R.string.m_round_format, round.roundNumber)',
    '"${round.points} pts"': 'stringResource(id = R.string.puntos_format, round.points)',
    '"${round.basePoints} base | ${round.bonusPoints} bono"': 'stringResource(id = R.string.base_bono_format, round.basePoints, round.bonusPoints)',
    '"Capicú (+${game.bonusCapicu} pts)"': 'stringResource(id = R.string.capicu_plus_format, game.bonusCapicu)',
    '"Chuchazo (+${game.bonusChuchazo} pts)"': 'stringResource(id = R.string.chuchazo_plus_format, game.bonusChuchazo)',
    '"📊 Desglose de Puntos para $winnerTeamName"': 'stringResource(id = R.string.desglose_puntos_format, winnerTeamName)',
    '"$pointsScored pts"': 'stringResource(id = R.string.puntos_format, pointsScored)',
    '"Bonificación de Ronda $nextRoundNum:"': 'stringResource(id = R.string.bonificacion_ronda_format, nextRoundNum)',
    '"+$roundBonusAmount pts"': 'stringResource(id = R.string.plus_puntos_format, roundBonusAmount)',
    '"+$capicuBonusAmount pts"': 'stringResource(id = R.string.plus_puntos_format, capicuBonusAmount)',
    '"+$chuchazoBonusAmount pts"': 'stringResource(id = R.string.plus_puntos_format, chuchazoBonusAmount)',
    '"$totalManoScore pts"': 'stringResource(id = R.string.puntos_format, totalManoScore)',
}

for k, v in reps.items():
    content = content.replace(k, v)

with open(app_file, "w", encoding="utf-8") as f:
    f.write(content)

