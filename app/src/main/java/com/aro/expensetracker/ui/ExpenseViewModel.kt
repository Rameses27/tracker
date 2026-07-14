package com.aro.expensetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aro.expensetracker.data.AppDatabase
import com.aro.expensetracker.data.CategoryBudget
import com.aro.expensetracker.data.CategoryTotal
import com.aro.expensetracker.data.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * The ViewModel survives screen rotations and is where all the "business logic"
 * lives, so the Composable screens only need to display state and forward
 * user actions here.
 */
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).expenseDao()

    // Start/end of the current calendar month, used to scope "this month's" data.
    private fun monthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis - 1
        return start to end
    }

    val expensesThisMonth: StateFlow<List<Expense>> = monthRange().let { (start, end) ->
        dao.getBetween(start, end)
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryTotalsThisMonth: StateFlow<List<CategoryTotal>> = monthRange().let { (start, end) ->
        dao.getCategoryTotals(start, end)
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<CategoryBudget>> =
        dao.getBudgets().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    // Categories whose spend has crossed their budget this month.
    val overBudgetCategories: StateFlow<List<String>> =
        combine(categoryTotalsThisMonth, budgets) { totals, budgetList ->
            val budgetMap = budgetList.associate { it.category to it.monthlyLimit }
            totals.filter { t -> (budgetMap[t.category] ?: Double.MAX_VALUE) < t.total }.map { it.category }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(amount: Double, category: String, note: String, dateMillis: Long) {
        viewModelScope.launch {
            dao.insert(Expense(amount = amount, category = category, note = note, dateMillis = dateMillis))
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { dao.delete(expense) }
    }

    fun setBudget(category: String, limit: Double) {
        viewModelScope.launch { dao.setBudget(CategoryBudget(category, limit)) }
    }
}
