package com.aro.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * A DAO (Data Access Object) is just a list of database operations.
 * Room reads the annotations (@Query, @Insert, ...) and writes the
 * real SQL/Java code for us at compile time — we never write SQL by hand
 * except for the @Query strings below.
 */
@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    // Flow = this list automatically refreshes the UI whenever the data changes.
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE dateMillis BETWEEN :start AND :end ORDER BY dateMillis DESC")
    fun getBetween(start: Long, end: Long): Flow<List<Expense>>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE dateMillis BETWEEN :start AND :end GROUP BY category")
    fun getCategoryTotals(start: Long, end: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM budgets")
    fun getBudgets(): Flow<List<CategoryBudget>>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: CategoryBudget)
}
