@file:OptIn(ExperimentalMaterial3Api::class)

package com.aro.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aro.expensetracker.data.DEFAULT_CATEGORIES
import com.aro.expensetracker.ui.AppViewModel
import com.aro.expensetracker.ui.AuthState
import com.aro.expensetracker.ui.BarChart
import com.aro.expensetracker.ui.PieChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    RootScreen(viewModel)
                }
            }
        }
    }
}

enum class Screen { DASHBOARD, ADD_EXPENSE, ADD_INCOME, CATEGORIES, BUDGETS, REPORTS, PROFILE }

@Composable
fun RootScreen(viewModel: AppViewModel) {
    val authState by viewModel.authState.collectAsState()
    when (authState) {
        AuthState.LoggedOut -> AuthScreen(viewModel)
        AuthState.LoggedIn -> MainApp(viewModel)
    }
}

// ================= AUTH =================

@Composable
fun AuthScreen(viewModel: AppViewModel) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.authError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Expense Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(if (isLogin) "Log in to continue" else "Create an account", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        if (!isLogin) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        if (error != null) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (isLogin) viewModel.login(email.trim(), password)
                else viewModel.register(email.trim(), password, name.trim())
            },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Please wait..." else if (isLogin) "Log in" else "Register")
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { isLogin = !isLogin }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isLogin) "No account? Register" else "Have an account? Log in")
        }
    }
}

// ================= MAIN APP SHELL =================

@Composable
fun MainApp(viewModel: AppViewModel) {
    var screen by remember { mutableStateOf(Screen.DASHBOARD) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == Screen.DASHBOARD, onClick = { screen = Screen.DASHBOARD },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") }, label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = screen == Screen.REPORTS, onClick = { screen = Screen.REPORTS },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") }, label = { Text("Reports") }
                )
                NavigationBarItem(
                    selected = screen == Screen.BUDGETS, onClick = { screen = Screen.BUDGETS },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Budgets") }, label = { Text("Budgets") }
                )
                NavigationBarItem(
                    selected = screen == Screen.CATEGORIES, onClick = { screen = Screen.CATEGORIES },
                    icon = { Icon(Icons.Default.Label, contentDescription = "Categories") }, label = { Text("Categories") }
                )
                NavigationBarItem(
                    selected = screen == Screen.PROFILE, onClick = { screen = Screen.PROFILE },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") }, label = { Text("Profile") }
                )
            }
        },
        floatingActionButton = {
            if (screen == Screen.DASHBOARD) {
                Column {
                    SmallFloatingActionButton(onClick = { screen = Screen.ADD_INCOME }) {
                        Icon(Icons.Default.Add, contentDescription = "Add income")
                    }
                    Spacer(Modifier.height(8.dp))
                    FloatingActionButton(onClick = { screen = Screen.ADD_EXPENSE }) {
                        Icon(Icons.Default.Remove, contentDescription = "Add expense")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (screen) {
                Screen.DASHBOARD -> DashboardScreen(viewModel)
                Screen.ADD_EXPENSE -> AddExpenseScreen(viewModel, onDone = { screen = Screen.DASHBOARD })
                Screen.ADD_INCOME -> AddIncomeScreen(viewModel, onDone = { screen = Screen.DASHBOARD })
                Screen.CATEGORIES -> CategoriesScreen(viewModel)
                Screen.BUDGETS -> BudgetsScreen(viewModel)
                Screen.REPORTS -> ReportsScreen(viewModel)
                Screen.PROFILE -> ProfileScreen(viewModel)
            }
        }
    }
}

// ================= DASHBOARD =================

@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val expenses by viewModel.expensesThisMonth.collectAsState()
    val incomeList by viewModel.incomeThisMonth.collectAsState()
    val overBudget by viewModel.overBudgetCategories.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    val totalExpense = expenses.sumOf { it.amount }
    val totalIncome = incomeList.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Income", totalIncome, Modifier.weight(1f))
            SummaryCard("Expense", totalExpense, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Balance this month", style = MaterialTheme.typography.titleMedium)
                Text(
                    "₦%.2f".format(balance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                if (overBudget.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Over budget: ${overBudget.joinToString(", ")}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Recent activity", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            val combined = (expenses.map { Triple(it.dateMillis, "- ₦%.2f".format(it.amount), it.category) } +
                incomeList.map { Triple(it.dateMillis, "+ ₦%.2f".format(it.amount), it.source) })
                .sortedByDescending { it.first }
            items(combined) { (millis, amountText, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(label, fontWeight = FontWeight.Medium)
                        Text(dateFormat.format(Date(millis)), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        amountText,
                        color = if (amountText.startsWith("+")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(label: String, value: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("₦%.0f".format(value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

// ================= ADD EXPENSE =================

@Composable
fun AddExpenseScreen(viewModel: AppViewModel, onDone: () -> Unit) {
    val customCategories by viewModel.customCategories.collectAsState()
    val allCategories = remember(customCategories) { DEFAULT_CATEGORIES + customCategories }

    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(allCategories.first()) }
    var note by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Add Expense", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = category, onValueChange = {}, readOnly = true, label = { Text("Category") },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allCategories.forEach { c ->
                    DropdownMenuItem(text = { Text(c) }, onClick = { category = c; expanded = false })
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    viewModel.addExpense(amount, category, note)
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save expense") }
    }
}

// ================= ADD INCOME =================

@Composable
fun AddIncomeScreen(viewModel: AppViewModel, onDone: () -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Add Income", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source (e.g. Salary)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount != null && amount > 0 && source.isNotBlank()) {
                    viewModel.addIncome(amount, source, note)
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save income") }
    }
}

// ================= CATEGORIES =================

@Composable
fun CategoriesScreen(viewModel: AppViewModel) {
    val customCategories by viewModel.customCategories.collectAsState()
    var newCategory by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Categories", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newCategory, onValueChange = { newCategory = it },
                label = { Text("New category name") }, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newCategory.isNotBlank()) { viewModel.addCategory(newCategory.trim()); newCategory = "" }
            }) { Text("Add") }
        }
        Spacer(Modifier.height(16.dp))

        Text("Default categories", style = MaterialTheme.typography.titleMedium)
        DEFAULT_CATEGORIES.forEach { Text("• $it", modifier = Modifier.padding(vertical = 2.dp)) }

        Spacer(Modifier.height(16.dp))
        Text("Your categories", style = MaterialTheme.typography.titleMedium)
        if (customCategories.isEmpty()) {
            Text("None yet — add one above.", style = MaterialTheme.typography.bodySmall)
        } else {
            customCategories.forEach { Text("• $it", modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}

// ================= BUDGETS =================

@Composable
fun BudgetsScreen(viewModel: AppViewModel) {
    val customCategories by viewModel.customCategories.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val categoryTotals by viewModel.categoryTotalsThisMonth.collectAsState()
    val allCategories = remember(customCategories) { DEFAULT_CATEGORIES + customCategories }
    val budgetMap = remember(budgets) { budgets.associate { it.category to it.monthlyLimit } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Monthly Budgets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        allCategories.forEach { category ->
            var text by remember(category, budgetMap[category]) { mutableStateOf(budgetMap[category]?.toString() ?: "") }
            val spent = categoryTotals[category] ?: 0.0
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(category, fontWeight = FontWeight.Bold)
                    Text("Spent this month: ₦%.2f".format(spent), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = text, onValueChange = { text = it },
                            label = { Text("Monthly limit") }, modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { text.toDoubleOrNull()?.let { if (it > 0) viewModel.setBudget(category, it) } }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

// ================= REPORTS =================

@Composable
fun ReportsScreen(viewModel: AppViewModel) {
    val categoryTotals by viewModel.categoryTotalsThisMonth.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val monthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    // Last 6 months trend, computed from all-time expenses.
    val monthlyTrend = remember(expenses) {
        val cal = java.util.Calendar.getInstance()
        val buckets = LinkedHashMap<String, Double>()
        for (i in 5 downTo 0) {
            val c = cal.clone() as java.util.Calendar
            c.add(java.util.Calendar.MONTH, -i)
            buckets[monthFormat.format(c.time)] = 0.0
        }
        expenses.forEach { e ->
            val label = monthFormat.format(Date(e.dateMillis))
            if (buckets.containsKey(label)) buckets[label] = (buckets[label] ?: 0.0) + e.amount
        }
        buckets
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Reports & Analytics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Text("Spending by category (this month)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        PieChart(data = categoryTotals, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
        Text("Monthly trend (last 6 months)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        BarChart(data = monthlyTrend, modifier = Modifier.fillMaxWidth())
    }
}

// ================= PROFILE =================

@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Signed in as:", style = MaterialTheme.typography.bodyMedium)
        Text(com.aro.expensetracker.data.FirebaseRepository.currentUserEmail ?: "", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { viewModel.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Log out")
        }
    }
}
