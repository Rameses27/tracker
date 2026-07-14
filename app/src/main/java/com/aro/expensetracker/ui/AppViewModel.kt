package com.aro.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aro.expensetracker.data.Budget
import com.aro.expensetracker.data.Expense
import com.aro.expensetracker.data.FirebaseRepository
import com.aro.expensetracker.data.Income
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class AuthState {
    object LoggedOut : AuthState()
    object LoggedIn : AuthState()
}

class AppViewModel : ViewModel() {

    private val repo = FirebaseRepository

    private val _authState = MutableStateFlow(
        if (repo.currentUserId != null) AuthState.LoggedIn else AuthState.LoggedOut
    )
    val authState: StateFlow<AuthState> = _authState

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val expenses: StateFlow<List<Expense>> =
        repo.observeExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val income: StateFlow<List<Income>> =
        repo.observeIncome().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> =
        repo.observeBudgets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategories: StateFlow<List<String>> =
        repo.observeCustomCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- derived, "this calendar month" scoped values ----

    private fun isThisMonth(millis: Long): Boolean {
        val cal = Calendar.getInstance()
        val now = Calendar.getInstance()
        cal.timeInMillis = millis
        return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }

    val expensesThisMonth: StateFlow<List<Expense>> =
        expenses.map { list -> list.filter { isThisMonth(it.dateMillis) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeThisMonth: StateFlow<List<Income>> =
        income.map { list -> list.filter { isThisMonth(it.dateMillis) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryTotalsThisMonth: StateFlow<Map<String, Double>> =
        expensesThisMonth.map { list ->
            list.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val overBudgetCategories: StateFlow<List<String>> =
        categoryTotalsThisMonth.combine(budgets) { totals, budgetList ->
            val budgetMap = budgetList.associate { it.category to it.monthlyLimit }
            totals.filter { (cat, total) -> (budgetMap[cat] ?: Double.MAX_VALUE) < total }.keys.toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Auth actions ----

    fun register(email: String, password: String, name: String) {
        _isLoading.value = true
        _authError.value = null
        viewModelScope.launch {
            try {
                repo.register(email, password, name)
                _authState.value = AuthState.LoggedIn
            } catch (e: Exception) {
                _authError.value = e.message ?: "Registration failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        _isLoading.value = true
        _authError.value = null
        viewModelScope.launch {
            try {
                repo.login(email, password)
                _authState.value = AuthState.LoggedIn
            } catch (e: Exception) {
                _authError.value = e.message ?: "Login failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        repo.logout()
        _authState.value = AuthState.LoggedOut
    }

    // ---- Data actions ----

    fun addExpense(amount: Double, category: String, note: String) {
        viewModelScope.launch {
            repo.addExpense(Expense(amount = amount, category = category, note = note, dateMillis = System.currentTimeMillis()))
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch { repo.deleteExpense(id) }
    }

    fun addIncome(amount: Double, source: String, note: String) {
        viewModelScope.launch {
            repo.addIncome(Income(amount = amount, source = source, note = note, dateMillis = System.currentTimeMillis()))
        }
    }

    fun deleteIncome(id: String) {
        viewModelScope.launch { repo.deleteIncome(id) }
    }

    fun setBudget(category: String, limit: Double) {
        viewModelScope.launch { repo.setBudget(category, limit) }
    }

    fun addCategory(name: String) {
        viewModelScope.launch { repo.addCategory(name) }
    }
}
