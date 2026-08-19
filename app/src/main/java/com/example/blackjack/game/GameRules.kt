package com.example.blackjack.game

enum class GameResult {
    PLAYER_WIN,
    DEALER_WIN,
    DRAW
}

object GameRules {
    fun resolve(player: HandScore, dealer: HandScore): GameResult {
        if (player.isBust) return GameResult.DEALER_WIN
        if (dealer.isBust) return GameResult.PLAYER_WIN

        if (player.isBlackjack && dealer.isBlackjack) return GameResult.DRAW
        if (player.isBlackjack) return GameResult.PLAYER_WIN
        if (dealer.isBlackjack) return GameResult.DEALER_WIN

        return when {
            player.best > dealer.best -> GameResult.PLAYER_WIN
            player.best < dealer.best -> GameResult.DEALER_WIN
            else -> GameResult.DRAW
        }
    }
}
