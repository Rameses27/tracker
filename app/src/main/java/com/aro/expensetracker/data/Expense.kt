package com.aro.expensetracker.data

/**
 * One expense record. Stored in Firestore at: users/{uid}/expenses/{id}
 * "id" is filled in after Firestore assigns a document ID; empty until then.
 */
data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val note: String = "",
    val dateMillis: Long = 0L
)
