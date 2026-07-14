package com.aro.expensetracker.data

/**
 * A monthly budget limit set for one category.
 * Stored in Firestore at: users/{uid}/budgets/{category}  (category is used as the doc ID)
 */
data class Budget(
    val category: String = "",
    val monthlyLimit: Double = 0.0
)
