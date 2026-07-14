package com.aro.expensetracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Single place that talks to Firebase. Every screen goes through here instead of
 * calling FirebaseAuth/Firestore directly — keeps the UI code simple, and if we
 * ever change backend, only this file changes.
 *
 * Firestore layout (matches the system architecture diagram):
 * users/{uid}                     -> profile fields (name, email)
 * users/{uid}/expenses/{id}       -> Expense docs
 * users/{uid}/income/{id}         -> Income docs
 * users/{uid}/budgets/{category}  -> Budget docs (category name is the doc id)
 * users/{uid}/categories/{id}     -> custom category names the user added
 */
object FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // ---------- Authentication ----------

    val currentUserId: String? get() = auth.currentUser?.uid
    val currentUserEmail: String? get() = auth.currentUser?.email

    suspend fun register(email: String, password: String, name: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: return
        db.collection("users").document(uid)
            .set(mapOf("name" to name, "email" to email))
            .await()
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    fun logout() {
        auth.signOut()
    }

    // ---------- Expenses ----------

    private fun expensesRef(uid: String) = db.collection("users").document(uid).collection("expenses")

    suspend fun addExpense(expense: Expense) {
        val uid = currentUserId ?: return
        expensesRef(uid).add(
            mapOf(
                "amount" to expense.amount,
                "category" to expense.category,
                "note" to expense.note,
                "dateMillis" to expense.dateMillis
            )
        ).await()
    }

    suspend fun deleteExpense(id: String) {
        val uid = currentUserId ?: return
        expensesRef(uid).document(id).delete().await()
    }

    // Live-updating list: emits a new list automatically whenever data changes in Firestore.
    fun observeExpenses(): Flow<List<Expense>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        val registration = expensesRef(uid).addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { doc ->
                Expense(
                    id = doc.id,
                    amount = doc.getDouble("amount") ?: 0.0,
                    category = doc.getString("category") ?: "",
                    note = doc.getString("note") ?: "",
                    dateMillis = doc.getLong("dateMillis") ?: 0L
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    // ---------- Income ----------

    private fun incomeRef(uid: String) = db.collection("users").document(uid).collection("income")

    suspend fun addIncome(income: Income) {
        val uid = currentUserId ?: return
        incomeRef(uid).add(
            mapOf(
                "amount" to income.amount,
                "source" to income.source,
                "note" to income.note,
                "dateMillis" to income.dateMillis
            )
        ).await()
    }

    suspend fun deleteIncome(id: String) {
        val uid = currentUserId ?: return
        incomeRef(uid).document(id).delete().await()
    }

    fun observeIncome(): Flow<List<Income>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        val registration = incomeRef(uid).addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { doc ->
                Income(
                    id = doc.id,
                    amount = doc.getDouble("amount") ?: 0.0,
                    source = doc.getString("source") ?: "",
                    note = doc.getString("note") ?: "",
                    dateMillis = doc.getLong("dateMillis") ?: 0L
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    // ---------- Budgets ----------

    private fun budgetsRef(uid: String) = db.collection("users").document(uid).collection("budgets")

    suspend fun setBudget(category: String, limit: Double) {
        val uid = currentUserId ?: return
        budgetsRef(uid).document(category).set(mapOf("category" to category, "monthlyLimit" to limit)).await()
    }

    fun observeBudgets(): Flow<List<Budget>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        val registration = budgetsRef(uid).addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { doc ->
                Budget(
                    category = doc.getString("category") ?: doc.id,
                    monthlyLimit = doc.getDouble("monthlyLimit") ?: 0.0
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    // ---------- Custom categories ----------

    private fun categoriesRef(uid: String) = db.collection("users").document(uid).collection("categories")

    suspend fun addCategory(name: String) {
        val uid = currentUserId ?: return
        categoriesRef(uid).document(name).set(mapOf("name" to name)).await()
    }

    fun observeCustomCategories(): Flow<List<String>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        val registration = categoriesRef(uid).addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.getString("name") } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }
}
