package com.aro.expensetracker.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes the given expenses out as a .csv file (openable in Excel/Google Sheets)
 * and returns a content:// Uri that can be shared or opened via an Intent.
 * This satisfies the "export reports" objective from the project proposal
 * without needing any paid service.
 */
object CsvExporter {

    fun export(context: Context, expenses: List<Expense>): Uri {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "expense_report_${System.currentTimeMillis()}.csv")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        file.bufferedWriter().use { writer ->
            writer.write("Date,Category,Amount,Note\n")
            expenses.forEach { e ->
                val date = dateFormat.format(Date(e.dateMillis))
                val note = e.note.replace(",", ";")
                writer.write("$date,${e.category},${e.amount},$note\n")
            }
        }

        return FileProvider.getUriForFile(context, "com.aro.expensetracker.fileprovider", file)
    }
}
