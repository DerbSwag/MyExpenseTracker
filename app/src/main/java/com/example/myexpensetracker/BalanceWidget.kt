package com.example.myexpensetracker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class BalanceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance)
            val uid = Firebase.auth.currentUser?.uid
            if (uid == null) {
                views.setTextViewText(R.id.widget_balance, "กรุณาเข้าสู่ระบบ")
                views.setTextViewText(R.id.widget_month, "")
                views.setTextViewText(R.id.widget_income, "")
                views.setTextViewText(R.id.widget_expense, "")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            val prefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val cal = Calendar.getInstance()
            val monthIdx = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            val monthNames = listOf("ม.ค.","ก.พ.","มี.ค.","เม.ย.","พ.ค.","มิ.ย.","ก.ค.","ส.ค.","ก.ย.","ต.ค.","พ.ย.","ธ.ค.")
            views.setTextViewText(R.id.widget_month, "${monthNames[monthIdx]} ${year + 543}")

            Firebase.firestore.collection("users").document(uid)
                .collection("wallets").document("default")
                .collection("transactions")
                .whereGreaterThanOrEqualTo("date", "$prefix-01")
                .whereLessThan("date", "$prefix-32")
                .get()
                .addOnSuccessListener { snap ->
                    val nf = NumberFormat.getNumberInstance(Locale("th", "TH")).apply { maximumFractionDigits = 0 }
                    var income = 0.0; var expense = 0.0
                    snap.documents.forEach { d ->
                        val amount = d.getDouble("amount") ?: 0.0
                        if (d.getString("type") == "income") income += amount else expense += amount
                    }
                    views.setTextViewText(R.id.widget_balance, "฿${nf.format(income - expense)}")
                    views.setTextViewText(R.id.widget_income, "+฿${nf.format(income)}")
                    views.setTextViewText(R.id.widget_expense, "-฿${nf.format(expense)}")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
        }
    }
}
