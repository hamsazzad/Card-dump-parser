package com.example.carddumpparser

/**
 * Model representing a parsed credit card. Each card contains a
 * generated card holder name, the primary account number, expiry and CVV.
 */
data class Card(
    val name: String,
    val cardNumber: String,
    val expiry: String,
    val cvv: String
)