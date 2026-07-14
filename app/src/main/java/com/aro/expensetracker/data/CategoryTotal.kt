package com.aro.expensetracker.data

/**
 * Result of "sum up all expenses per category" — used to build the
 * spending-by-category summary and to compare against budgets.
 */
data class CategoryTotal(
    val category: String,
    val total: Double
)
