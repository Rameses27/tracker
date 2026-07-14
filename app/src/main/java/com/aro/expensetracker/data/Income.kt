package com.aro.expensetracker.data

/**
 * One income record. Stored in Firestore at: users/{uid}/income/{id}
 */
data class Income(
    val id: String = "",
    val amount: Double = 0.0,
    val source: String = "",
    val note: String = "",
    val dateMillis: Long = 0L
)
