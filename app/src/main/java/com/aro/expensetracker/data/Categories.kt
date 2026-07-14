package com.aro.expensetracker.data

// Default categories every user starts with. They can add their own on top of these
// (custom categories are stored in Firestore at users/{uid}/categories).
val DEFAULT_CATEGORIES = listOf(
    "Food", "Transport", "Bills", "Shopping", "Entertainment", "Health", "Education", "Other"
)

