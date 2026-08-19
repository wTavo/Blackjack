package com.example.blackjack.game

import com.example.blackjack.R

fun Card.resourceId(): Int = when (suit) {
    Suit.SPADES -> when (rank) {
        Rank.ACE -> R.drawable.as_picas
        Rank.TWO -> R.drawable.two_picas
        Rank.THREE -> R.drawable.three_picas
        Rank.FOUR -> R.drawable.four_picas
        Rank.FIVE -> R.drawable.five_picas
        Rank.SIX -> R.drawable.six_picas
        Rank.SEVEN -> R.drawable.seven_picas
        Rank.EIGHT -> R.drawable.eight_picas
        Rank.NINE -> R.drawable.nine_picas
        Rank.TEN -> R.drawable.ten_picas
        Rank.JACK -> R.drawable.j_picas
        Rank.QUEEN -> R.drawable.q_picas
        Rank.KING -> R.drawable.k_picas
    }
    Suit.CLUBS -> when (rank) {
        Rank.ACE -> R.drawable.as_trebol
        Rank.TWO -> R.drawable.two_trebol
        Rank.THREE -> R.drawable.three_trebol
        Rank.FOUR -> R.drawable.four_trebol
        Rank.FIVE -> R.drawable.five_trebol
        Rank.SIX -> R.drawable.six_trebol
        Rank.SEVEN -> R.drawable.seven_trebol
        Rank.EIGHT -> R.drawable.eight_trebol
        Rank.NINE -> R.drawable.nine_trebol
        Rank.TEN -> R.drawable.ten_trebol
        Rank.JACK -> R.drawable.j_trebol
        Rank.QUEEN -> R.drawable.q_trebol
        Rank.KING -> R.drawable.k_trebol
    }
    Suit.DIAMONDS -> when (rank) {
        Rank.ACE -> R.drawable.as_rombo
        Rank.TWO -> R.drawable.two_rombo
        Rank.THREE -> R.drawable.three_rombo
        Rank.FOUR -> R.drawable.four_rombo
        Rank.FIVE -> R.drawable.five_rombo
        Rank.SIX -> R.drawable.six_rombo
        Rank.SEVEN -> R.drawable.seven_rombo
        Rank.EIGHT -> R.drawable.eight_rombo
        Rank.NINE -> R.drawable.nine_rombo
        Rank.TEN -> R.drawable.ten_rombo
        Rank.JACK -> R.drawable.j_rombo
        Rank.QUEEN -> R.drawable.q_rombo
        Rank.KING -> R.drawable.k_rombo
    }
    Suit.HEARTS -> when (rank) {
        Rank.ACE -> R.drawable.as_corazones
        Rank.TWO -> R.drawable.two_corazones
        Rank.THREE -> R.drawable.three_corazones
        Rank.FOUR -> R.drawable.four_corazones
        Rank.FIVE -> R.drawable.five_corazones
        Rank.SIX -> R.drawable.six_corazones
        Rank.SEVEN -> R.drawable.seven_corazones
        Rank.EIGHT -> R.drawable.eight_corazones
        Rank.NINE -> R.drawable.nine_corazones
        Rank.TEN -> R.drawable.ten_corazones
        Rank.JACK -> R.drawable.j_corazones
        Rank.QUEEN -> R.drawable.q_corazones
        Rank.KING -> R.drawable.k_corazones
    }
}

fun resourceIdToCard(resourceId: Int): Card =
    Suit.entries
        .flatMap { suit -> Rank.entries.map { rank -> Card(rank, suit) } }
        .first { it.resourceId() == resourceId }
