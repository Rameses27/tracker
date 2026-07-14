package com.aro.expensetracker.data

import androidx.room.Entity

/**
 * One row per category = the monthly budget the user set for it.
 * category is the primary key so there's only ever one budget per category.
 */
@Entity(tableName = "budgets", primaryKeys = ["category"])
data class CategoryBudget(
    val category: String,
    val monthlyLimit: Double
)
