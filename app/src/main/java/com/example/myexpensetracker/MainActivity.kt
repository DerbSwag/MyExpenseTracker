// ═══════════════════════════════════════════════════════════════
//  MainActivity.kt  —  Expense Tracker v4
//  + Confirm delete recurring, transaction count, sorting,
//    pie chart, savings goal, multi-currency, biometric lock
// ═══════════════════════════════════════════════════════════════

package com.example.myexpensetracker

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.myexpensetracker.ui.theme.MyExpenseTrackerTheme
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

// ── DataStore ──────────────────────────────────────────────────
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
val BIOMETRIC_KEY = booleanPreferencesKey("biometric_lock")
val CURRENCY_KEY  = stringPreferencesKey("currency")

// ═══════════════════════════════════════════════════════════════
//  CONSTANTS
// ═══════════════════════════════════════════════════════════════

data class Category(val id: String, val label: String, val emoji: String, val color: Color)

val CATEGORIES = listOf(
    Category("food",          "อาหาร",       "🍜", Color(0xFFFF6B6B)),
    Category("transport",     "เดินทาง",      "🚗", Color(0xFF4ECDC4)),
    Category("shopping",      "ช้อปปิ้ง",     "🛍️",Color(0xFF45B7D1)),
    Category("health",        "สุขภาพ",       "💊", Color(0xFF96CEB4)),
    Category("entertainment", "บันเทิง",      "🎮", Color(0xFFFFEAA7)),
    Category("utility",       "สาธารณูปโภค", "💡", Color(0xFFDDA0DD)),
    Category("salary",        "เงินเดือน",   "💼", Color(0xFF98FB98)),
    Category("other",         "อื่นๆ",        "📦", Color(0xFFF0E68C)),
)

data class CurrencyInfo(val code: String, val symbol: String, val label: String)
val CURRENCIES = listOf(
    CurrencyInfo("THB", "฿", "บาท"),
    CurrencyInfo("USD", "$", "ดอลลาร์"),
    CurrencyInfo("EUR", "€", "ยูโร"),
    CurrencyInfo("JPY", "¥", "เยน"),
    CurrencyInfo("GBP", "£", "ปอนด์"),
    CurrencyInfo("KRW", "₩", "วอน"),
)

val MONTH_TH = listOf("ม.ค.","ก.พ.","มี.ค.","เม.ย.","พ.ค.","มิ.ย.","ก.ค.","ส.ค.","ก.ย.","ต.ค.","พ.ย.","ธ.ค.")
val FREQ_LABEL = mapOf("monthly" to "รายเดือน", "weekly" to "รายสัปดาห์")
val SORT_OPTIONS = listOf("date_desc" to "วันที่ ใหม่→เก่า", "date_asc" to "วันที่ เก่า→ใหม่",
    "amount_desc" to "จำนวน มาก→น้อย", "amount_asc" to "จำนวน น้อย→มาก", "category" to "หมวดหมู่")

fun catById(id: String) = CATEGORIES.find { it.id == id } ?: Category("other", id, "📦", Color(0xFFF0E68C))

fun formatMoney(n: Double, currency: CurrencyInfo): String {
    val nf = NumberFormat.getNumberInstance(Locale("th","TH")).apply { maximumFractionDigits = 0 }
    return "${currency.symbol}${nf.format(n)}"
}

// ═══════════════════════════════════════════════════════════════
//  DATA MODELS
// ═══════════════════════════════════════════════════════════════

data class Transaction(
    val fid: String = "", val type: String = "expense", val category: String = "other",
    val amount: Double = 0.0, val note: String = "", val date: String = "",
    val createdAt: Long = 0L, val isRecurring: Boolean = false, val receipt: String? = null,
)

data class RecurringItem(
    val id: String = "", val type: String = "expense", val category: String = "other",
    val amount: Double = 0.0, val name: String = "", val freq: String = "monthly", val day: Int = 1,
)

data class SavingsGoal(val name: String = "", val target: Double = 0.0, val current: Double = 0.0)

// ═══════════════════════════════════════════════════════════════
//  VIEWMODEL
// ═══════════════════════════════════════════════════════════════

class ExpenseViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions
    private val _budgets = MutableStateFlow<Map<String, Double>>(emptyMap())
    val budgets: StateFlow<Map<String, Double>> = _budgets
    private val _recurring = MutableStateFlow<List<RecurringItem>>(emptyList())
    val recurring: StateFlow<List<RecurringItem>> = _recurring
    private val _savingsGoal = MutableStateFlow(SavingsGoal())
    val savingsGoal: StateFlow<SavingsGoal> = _savingsGoal
    val shownAlerts = mutableSetOf<String>()
    private val uid: String? get() = auth.currentUser?.uid
    private fun txCol() = db.collection("users").document(uid ?: "anon").collection("transactions")
    private fun settingsDoc() = db.collection("users").document(uid ?: "anon").collection("settings")

    fun startListening() { if (uid == null) return; listenTransactions(); listenBudgets(); listenRecurring(); listenSavings() }
    fun clearData() { _transactions.value = emptyList(); _budgets.value = emptyMap(); _recurring.value = emptyList(); _savingsGoal.value = SavingsGoal(); shownAlerts.clear() }

    private fun listenTransactions() {
        txCol().orderBy("createdAt", Query.Direction.DESCENDING).addSnapshotListener { snap, err ->
            if (err != null) { Log.e("FS", err.message ?: ""); return@addSnapshotListener }
            _transactions.value = snap?.documents?.map { d ->
                Transaction(d.id, d.getString("type") ?: "expense", d.getString("category") ?: "other",
                    d.getDouble("amount") ?: 0.0, d.getString("note") ?: "", d.getString("date") ?: "",
                    d.getLong("createdAt") ?: 0L, d.getBoolean("isRecurring") ?: false, d.getString("receipt"))
            } ?: emptyList()
        }
    }
    private fun listenBudgets() {
        settingsDoc().document("budgets").addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) _budgets.value = snap.data?.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 } ?: emptyMap()
        }
    }
    @Suppress("UNCHECKED_CAST")
    private fun listenRecurring() {
        settingsDoc().document("recurring").addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) {
                val list = snap.get("list") as? List<Map<String, Any>> ?: emptyList()
                _recurring.value = list.map { m -> RecurringItem(m["id"] as? String ?: "", m["type"] as? String ?: "expense",
                    m["category"] as? String ?: "other", (m["amount"] as? Number)?.toDouble() ?: 0.0,
                    m["name"] as? String ?: "", m["freq"] as? String ?: "monthly", (m["day"] as? Number)?.toInt() ?: 1) }
            }
        }
    }
    private fun listenSavings() {
        settingsDoc().document("savings").addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) _savingsGoal.value = SavingsGoal(
                snap.getString("name") ?: "", snap.getDouble("target") ?: 0.0, snap.getDouble("current") ?: 0.0)
        }
    }

    fun save(type: String, catId: String, amount: Double, note: String, receipt: String? = null) {
        txCol().add(hashMapOf("type" to type, "category" to catId, "amount" to amount, "note" to note,
            "date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            "createdAt" to System.currentTimeMillis(), "isRecurring" to false, "receipt" to receipt))
    }
    fun update(fid: String, type: String, catId: String, amount: Double, note: String, date: String, receipt: String?) {
        txCol().document(fid).update(mapOf("type" to type, "category" to catId, "amount" to amount,
            "note" to note, "date" to date, "receipt" to receipt, "updatedAt" to System.currentTimeMillis()))
    }
    fun delete(fid: String) = txCol().document(fid).delete()
    fun saveBudget(catId: String, amount: Double) { settingsDoc().document("budgets").set(_budgets.value.toMutableMap().also { it[catId] = amount }) }
    fun saveRecurring(item: RecurringItem) {
        val list = _recurring.value.toMutableList(); val idx = list.indexOfFirst { it.id == item.id }
        if (idx >= 0) list[idx] = item else list.add(item)
        settingsDoc().document("recurring").set(mapOf("list" to list.map { mapOf("id" to it.id, "type" to it.type, "category" to it.category, "amount" to it.amount, "name" to it.name, "freq" to it.freq, "day" to it.day) }))
    }
    fun deleteRecurring(id: String) {
        val list = _recurring.value.filter { it.id != id }
        settingsDoc().document("recurring").set(mapOf("list" to list.map { mapOf("id" to it.id, "type" to it.type, "category" to it.category, "amount" to it.amount, "name" to it.name, "freq" to it.freq, "day" to it.day) }))
    }
    fun applyRecurring(item: RecurringItem) {
        txCol().add(hashMapOf("type" to item.type, "category" to item.category, "amount" to item.amount, "note" to item.name,
            "date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()), "createdAt" to System.currentTimeMillis(), "isRecurring" to true, "receipt" to null))
    }
    fun saveSavingsGoal(name: String, target: Double, current: Double) { settingsDoc().document("savings").set(mapOf("name" to name, "target" to target, "current" to current)) }
    fun addToSavings(amount: Double) { val g = _savingsGoal.value; saveSavingsGoal(g.name, g.target, g.current + amount) }

    fun getMonthTx(year: Int, month: Int) = _transactions.value.filter { it.date.startsWith("%04d-%02d".format(year, month + 1)) }
    fun monthIncome(y: Int, m: Int) = getMonthTx(y, m).filter { it.type == "income" }.sumOf { it.amount }
    fun monthExpense(y: Int, m: Int) = getMonthTx(y, m).filter { it.type == "expense" }.sumOf { it.amount }
}

// ═══════════════════════════════════════════════════════════════
//  FCM + NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(msg: RemoteMessage) {
        pushNotification(applicationContext, msg.notification?.title ?: msg.data["title"] ?: "แจ้งเตือน", msg.notification?.body ?: msg.data["body"] ?: "")
    }
}
fun pushNotification(ctx: Context, title: String, body: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannel("budget_alerts", "แจ้งเตือนงบประมาณ", NotificationManager.IMPORTANCE_HIGH).also { ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(it) }
    val n = NotificationCompat.Builder(ctx, "budget_alerts").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
    if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) NotificationManagerCompat.from(ctx).notify(System.currentTimeMillis().toInt(), n)
}
fun checkBudgetNotifications(ctx: Context, transactions: List<Transaction>, budgets: Map<String, Double>, shownAlerts: MutableSet<String>, cur: CurrencyInfo) {
    val prefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    val moExp = transactions.filter { it.date.startsWith(prefix) && it.type == "expense" }
    CATEGORIES.forEach { cat ->
        val bdg = budgets[cat.id]?.takeIf { it > 0 } ?: return@forEach
        val spent = moExp.filter { it.category == cat.id }.sumOf { it.amount }
        val pct = spent / bdg * 100; val k100 = "over_${cat.id}_$prefix"; val k80 = "warn_${cat.id}_$prefix"
        when {
            pct >= 100 && k100 !in shownAlerts -> { shownAlerts += k100; pushNotification(ctx, "🚨 เกินงบ! ${cat.emoji} ${cat.label}", "${formatMoney(spent, cur)} / ${formatMoney(bdg, cur)} — เกิน ${formatMoney(spent - bdg, cur)}") }
            pct in 80.0..99.99 && k80 !in shownAlerts -> { shownAlerts += k80; pushNotification(ctx, "⚠️ ใกล้เกินงบ ${cat.emoji} ${cat.label}", "ใช้ไปแล้ว ${pct.toInt()}% — เหลือ ${formatMoney(bdg - spent, cur)}") }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  MAIN ACTIVITY (FragmentActivity for Biometric)
// ═══════════════════════════════════════════════════════════════
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.CAMERA), 1001)
        setContent {
            val scope = rememberCoroutineScope()
            val isDark by applicationContext.dataStore.data.map { it[DARK_MODE_KEY] ?: false }.collectAsState(initial = false)
            val biometricEnabled by applicationContext.dataStore.data.map { it[BIOMETRIC_KEY] ?: false }.collectAsState(initial = false)
            val currencyCode by applicationContext.dataStore.data.map { it[CURRENCY_KEY] ?: "THB" }.collectAsState(initial = "THB")
            val currency = CURRENCIES.find { it.code == currencyCode } ?: CURRENCIES[0]
            MyExpenseTrackerTheme(darkTheme = isDark) {
                AppRoot(isDark = isDark,
                    onToggle = { scope.launch { applicationContext.dataStore.edit { p -> p[DARK_MODE_KEY] = !isDark } } },
                    biometricEnabled = biometricEnabled,
                    onBiometricToggle = { scope.launch { applicationContext.dataStore.edit { p -> p[BIOMETRIC_KEY] = !biometricEnabled } } },
                    currency = currency,
                    onCurrencyChange = { c -> scope.launch { applicationContext.dataStore.edit { p -> p[CURRENCY_KEY] = c.code } } },
                    activity = this)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  APP ROOT — Auth + Biometric lock
// ═══════════════════════════════════════════════════════════════
@Composable
fun AppRoot(isDark: Boolean, onToggle: () -> Unit, biometricEnabled: Boolean, onBiometricToggle: () -> Unit, currency: CurrencyInfo, onCurrencyChange: (CurrencyInfo) -> Unit, activity: FragmentActivity) {
    val auth = Firebase.auth
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var biometricPassed by remember { mutableStateOf(!biometricEnabled) }

    // Biometric prompt
    if (currentUser != null && biometricEnabled && !biometricPassed) {
        LaunchedEffect(Unit) {
            val executor: Executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { biometricPassed = true }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) activity.finish() }
            })
            prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("ปลดล็อค MyExpenseTracker").setSubtitle("ใช้ลายนิ้วมือหรือ PIN เพื่อเข้าแอป").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL).build())
        }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("🔒", fontSize = 48.sp); Spacer(Modifier.height(16.dp)); Text("กรุณาปลดล็อค", fontSize = 18.sp, color = MaterialTheme.colorScheme.outline) }
        }
        return
    }

    if (currentUser == null) { AuthScreen { currentUser = auth.currentUser; biometricPassed = true } }
    else ExpenseTrackerApp(isDark = isDark, onToggle = onToggle, onLogout = { auth.signOut(); currentUser = null },
        biometricEnabled = biometricEnabled, onBiometricToggle = onBiometricToggle, currency = currency, onCurrencyChange = onCurrencyChange)
}

// ═══════════════════════════════════════════════════════════════
//  AUTH SCREEN
// ═══════════════════════════════════════════════════════════════
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val auth = Firebase.auth; val ctx = LocalContext.current
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }; var loading by remember { mutableStateOf(false) }; var showPass by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxWidth().padding(32.dp).align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("💰", fontSize = 56.sp); Text("MyExpenseTracker", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(if (isLogin) "เข้าสู่ระบบ" else "สมัครสมาชิก", fontSize = 16.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it.trim() }, label = { Text("อีเมล") }, leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("รหัสผ่าน") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = { IconButton({ showPass = !showPass }) { Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Button(onClick = {
                if (email.isBlank() || password.length < 6) { Toast.makeText(ctx, "กรุณากรอกอีเมลและรหัสผ่าน (6 ตัวขึ้นไป)", Toast.LENGTH_SHORT).show(); return@Button }
                loading = true
                val task = if (isLogin) auth.signInWithEmailAndPassword(email, password) else auth.createUserWithEmailAndPassword(email, password)
                task.addOnSuccessListener { loading = false; Toast.makeText(ctx, if (isLogin) "เข้าสู่ระบบสำเร็จ ✓" else "สมัครสำเร็จ ✓", Toast.LENGTH_SHORT).show(); onAuthSuccess() }
                    .addOnFailureListener { e -> loading = false; val msg = when {
                        e.message?.contains("badly formatted") == true -> "รูปแบบอีเมลไม่ถูกต้อง"
                        e.message?.contains("already in use") == true -> "อีเมลนี้ถูกใช้แล้ว"
                        e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "อีเมลหรือรหัสผ่านไม่ถูกต้อง"
                        else -> e.message ?: "เกิดข้อผิดพลาด"
                    }; Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show() }
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))) {
                if (loading) CircularProgressIndicator(Modifier.size(24.dp), Color.White, strokeWidth = 2.dp)
                else Text(if (isLogin) "เข้าสู่ระบบ" else "สมัครสมาชิก", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            TextButton({ isLogin = !isLogin }) { Text(if (isLogin) "ยังไม่มีบัญชี? สมัครสมาชิก" else "มีบัญชีแล้ว? เข้าสู่ระบบ", color = MaterialTheme.colorScheme.primary) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  ROOT COMPOSABLE
// ═══════════════════════════════════════════════════════════════
@Composable
fun ExpenseTrackerApp(vm: ExpenseViewModel = viewModel(), isDark: Boolean, onToggle: () -> Unit, onLogout: () -> Unit,
                      biometricEnabled: Boolean, onBiometricToggle: () -> Unit, currency: CurrencyInfo, onCurrencyChange: (CurrencyInfo) -> Unit) {
    val transactions by vm.transactions.collectAsState(); val budgets by vm.budgets.collectAsState()
    val ctx = LocalContext.current; val nav = rememberNavController()
    LaunchedEffect(Unit) { vm.startListening() }
    LaunchedEffect(transactions, budgets) { if (transactions.isNotEmpty() && budgets.isNotEmpty()) checkBudgetNotifications(ctx, transactions, budgets, vm.shownAlerts, currency) }
    Scaffold(bottomBar = { BottomNav(nav) }) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { TransactionListScreen(vm, nav, isDark, onToggle, onLogout, biometricEnabled, onBiometricToggle, currency, onCurrencyChange) }
            composable("add") { AddEditScreen(vm, nav, currency = currency) }
            composable("edit/{fid}") { back -> val fid = back.arguments?.getString("fid") ?: return@composable; val tx = transactions.find { it.fid == fid }; if (tx != null) AddEditScreen(vm, nav, tx, currency) }
            composable("summary") { SummaryScreen(vm, currency) }
            composable("budget") { BudgetScreen(vm, currency) }
            composable("recur") { RecurringScreen(vm, ctx, currency) }
        }
    }
}

@Composable
fun BottomNav(nav: NavController) {
    val cur = nav.currentBackStackEntryAsState().value?.destination?.route
    NavigationBar {
        NavigationBarItem(selected = cur == "home", onClick = { nav.navigate("home") { launchSingleTop = true } }, icon = { Icon(Icons.Default.List, null) }, label = { Text("รายการ") })
        NavigationBarItem(selected = cur == "add", onClick = { nav.navigate("add") { launchSingleTop = true } }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("บันทึก") })
        NavigationBarItem(selected = cur == "summary", onClick = { nav.navigate("summary") { launchSingleTop = true } }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("สรุป") })
        NavigationBarItem(selected = cur == "budget", onClick = { nav.navigate("budget") { launchSingleTop = true } }, icon = { Icon(Icons.Default.AccountBalance, null) }, label = { Text("งบ") })
        NavigationBarItem(selected = cur == "recur", onClick = { nav.navigate("recur") { launchSingleTop = true } }, icon = { Icon(Icons.Default.Refresh, null) }, label = { Text("ประจำ") })
    }
}

// ═══════════════════════════════════════════════════════════════
//  SCREEN 1 — TRANSACTION LIST + Sorting + Count + Settings
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(vm: ExpenseViewModel, nav: NavController, isDark: Boolean, onToggle: () -> Unit, onLogout: () -> Unit,
                          biometricEnabled: Boolean, onBiometricToggle: () -> Unit, currency: CurrencyInfo, onCurrencyChange: (CurrencyInfo) -> Unit) {
    val transactions by vm.transactions.collectAsState(); val savingsGoal by vm.savingsGoal.collectAsState()
    val cal = Calendar.getInstance(); val ctx = LocalContext.current
    var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }; var month by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    var search by remember { mutableStateOf("") }; var filterCat by remember { mutableStateOf<String?>(null) }
    var showFilter by remember { mutableStateOf(false) }; var receiptPreview by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }; var sortBy by remember { mutableStateOf("date_desc") }
    var showSettings by remember { mutableStateOf(false) }; var showSavingsDialog by remember { mutableStateOf(false) }
    var savingsName by remember { mutableStateOf(savingsGoal.name) }; var savingsTarget by remember { mutableStateOf(if (savingsGoal.target > 0) savingsGoal.target.toLong().toString() else "") }
    var savingsAdd by remember { mutableStateOf("") }

    val monthTx = remember(transactions, year, month) { vm.getMonthTx(year, month) }
    val sorted = remember(monthTx, search, filterCat, sortBy) {
        monthTx.filter { tx ->
            val ms = search.isBlank() || tx.note.contains(search, true) || catById(tx.category).label.contains(search, true) || tx.amount.toString().contains(search)
            val mc = filterCat == null || tx.category == filterCat; ms && mc
        }.let { list -> when (sortBy) {
            "date_asc" -> list.sortedBy { it.date }; "amount_desc" -> list.sortedByDescending { it.amount }
            "amount_asc" -> list.sortedBy { it.amount }; "category" -> list.sortedBy { it.category }; else -> list
        } }
    }
    val income = monthTx.filter { it.type == "income" }.sumOf { it.amount }
    val expense = monthTx.filter { it.type == "expense" }.sumOf { it.amount }

    if (showLogoutDialog) AlertDialog(onDismissRequest = { showLogoutDialog = false }, title = { Text("ออกจากระบบ?") }, text = { Text("คุณต้องการออกจากระบบใช่ไหม?") },
        confirmButton = { TextButton({ vm.clearData(); onLogout(); showLogoutDialog = false }) { Text("ออกจากระบบ", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ showLogoutDialog = false }) { Text("ยกเลิก") } })

    // Settings dialog (biometric + currency)
    if (showSettings) Dialog(onDismissRequest = { showSettings = false }) {
        Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("⚙️ ตั้งค่า", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🔒 ล็อคด้วยลายนิ้วมือ/PIN"); Switch(checked = biometricEnabled, onCheckedChange = { onBiometricToggle() })
            }
            Text("💱 สกุลเงิน", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            CURRENCIES.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { c -> FilterChip(selected = currency.code == c.code, onClick = { onCurrencyChange(c) }, label = { Text("${c.symbol} ${c.label}", fontSize = 11.sp) }) }
            } }
            TextButton({ showSettings = false }, Modifier.align(Alignment.End)) { Text("ปิด") }
        } }
    }

    // Savings goal dialog
    if (showSavingsDialog) {
        LaunchedEffect(savingsGoal) { savingsName = savingsGoal.name; savingsTarget = if (savingsGoal.target > 0) savingsGoal.target.toLong().toString() else "" }
        AlertDialog(onDismissRequest = { showSavingsDialog = false }, title = { Text("🎯 เป้าหมายออม") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(savingsName, { savingsName = it }, label = { Text("ชื่อเป้าหมาย") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(savingsTarget, { savingsTarget = it }, label = { Text("เป้าหมาย (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (savingsGoal.target > 0) {
                    OutlinedTextField(savingsAdd, { savingsAdd = it }, label = { Text("เพิ่มเงินออม (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            } },
            confirmButton = { TextButton({
                val t = savingsTarget.toDoubleOrNull() ?: 0.0; val a = savingsAdd.toDoubleOrNull() ?: 0.0
                if (a > 0) vm.addToSavings(a) else vm.saveSavingsGoal(savingsName, t, savingsGoal.current)
                savingsAdd = ""; showSavingsDialog = false
            }) { Text("บันทึก") } },
            dismissButton = { TextButton({ showSavingsDialog = false }) { Text("ยกเลิก") } })
    }

    if (receiptPreview != null) Dialog(onDismissRequest = { receiptPreview = null }) {
        val bytes = Base64.decode(receiptPreview!!, Base64.DEFAULT); val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        Card(shape = RoundedCornerShape(16.dp)) { Box { Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.FillWidth)
            IconButton({ receiptPreview = null }, Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.Close, null, tint = Color.White) } } }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("💰 รายรับ-รายจ่าย", fontWeight = FontWeight.Bold) }, actions = {
        IconButton({ showFilter = !showFilter }) { Icon(Icons.Default.FilterList, null, tint = if (filterCat != null) MaterialTheme.colorScheme.primary else LocalContentColor.current) }
        IconButton({ exportCSV(ctx, monthTx, currency) }) { Icon(Icons.Default.Download, null) }
        IconButton(onToggle) { Icon(if (isDark) Icons.Default.WbSunny else Icons.Default.DarkMode, null) }
        IconButton({ showSettings = true }) { Icon(Icons.Default.Settings, null) }
        IconButton({ showLogoutDialog = true }) { Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error) }
    }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { val email = Firebase.auth.currentUser?.email ?: ""; if (email.isNotBlank()) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline); Spacer(Modifier.width(4.dp)); Text(email, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline) } }
            item { MonthNav(year, month, { if (month == 0) { month = 11; year-- } else month-- }, { if (month == 11) { month = 0; year++ } else month++ }) }
            // Balance card + transaction count
            item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ยอดคงเหลือ ${MONTH_TH[month]} ${year + 543}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.65f))
                        Text("${monthTx.size} รายการ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.65f))
                    }
                    Text(formatMoney(income - expense, currency), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = if (income >= expense) Color(0xFF2E7D32) else Color(0xFFC62828))
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        AmountLabel("รายรับ", "+${formatMoney(income, currency)}", Color(0xFF2E7D32))
                        AmountLabel("รายจ่าย", "-${formatMoney(expense, currency)}", Color(0xFFC62828))
                    }
                }
            } }
            // Savings goal card
            if (savingsGoal.target > 0) item {
                val pct = (savingsGoal.current / savingsGoal.target).coerceAtMost(1.0).toFloat()
                Card(Modifier.fillMaxWidth().clickable { showSavingsDialog = true }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("🎯 ${savingsGoal.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("${(pct * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(6.dp)); LinearProgressIndicator(progress = { pct }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)), color = Color(0xFF4CAF50))
                        Spacer(Modifier.height(4.dp)); Text("${formatMoney(savingsGoal.current, currency)} / ${formatMoney(savingsGoal.target, currency)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(.7f))
                    }
                }
            } else item { OutlinedButton({ showSavingsDialog = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("🎯 ตั้งเป้าหมายออม") } }
            // Search + Sort
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(search, { search = it }, placeholder = { Text("🔍 ค้นหา...") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), singleLine = true,
                    trailingIcon = if (search.isNotBlank()) {{ IconButton({ search = "" }) { Icon(Icons.Default.Clear, null) } }} else null)
                var sortExpanded by remember { mutableStateOf(false) }
                Box { IconButton({ sortExpanded = true }) { Icon(Icons.Default.Sort, null) }
                    DropdownMenu(sortExpanded, { sortExpanded = false }) { SORT_OPTIONS.forEach { (key, label) -> DropdownMenuItem(text = { Row { if (sortBy == key) Icon(Icons.Default.Check, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(label, fontSize = 13.sp) } }, onClick = { sortBy = key; sortExpanded = false }) } }
                }
            } }
            if (showFilter) item { Column {
                Text("กรองหมวดหมู่", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { FilterChip(selected = filterCat == null, onClick = { filterCat = null }, label = { Text("ทั้งหมด", fontSize = 11.sp) }) }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { CATEGORIES.take(4).forEach { cat -> FilterChip(selected = filterCat == cat.id, onClick = { filterCat = if (filterCat == cat.id) null else cat.id }, label = { Text("${cat.emoji} ${cat.label}", fontSize = 11.sp) }) } }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { CATEGORIES.drop(4).forEach { cat -> FilterChip(selected = filterCat == cat.id, onClick = { filterCat = if (filterCat == cat.id) null else cat.id }, label = { Text("${cat.emoji} ${cat.label}", fontSize = 11.sp) }) } }
            } }
            if (sorted.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text(if (monthTx.isEmpty()) "ยังไม่มีรายการเดือนนี้" else "ไม่พบรายการที่ค้นหา", color = MaterialTheme.colorScheme.outline) } }
            else items(sorted, key = { it.fid }) { tx -> TxItem(tx, currency, { nav.navigate("edit/${tx.fid}") }, { vm.delete(tx.fid) }, { if (tx.receipt != null) receiptPreview = tx.receipt }) }
        }
    }
}

@Composable
fun TxItem(tx: Transaction, currency: CurrencyInfo, onEdit: () -> Unit, onDelete: () -> Unit, onReceipt: () -> Unit) {
    val cat = catById(tx.category); var showMenu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(12.dp), Arrangement.spacedBy(11.dp), Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(cat.color.copy(.22f)), Alignment.Center) { Text(cat.emoji, fontSize = 21.sp) }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(cat.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (tx.isRecurring) { Spacer(Modifier.width(4.dp)); Text("🔄", fontSize = 10.sp) }
                    if (tx.receipt != null) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Receipt, null, Modifier.size(13.dp).clickable { onReceipt() }, tint = MaterialTheme.colorScheme.primary) } }
                Text(buildString { if (tx.note.isNotBlank()) append("${tx.note} · "); append(tx.date.takeLast(5).replace("-", "/")) }, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${if (tx.type == "income") "+" else "-"}${formatMoney(tx.amount, currency)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (tx.type == "income") Color(0xFF2E7D32) else Color(0xFFC62828))
                Row { Icon(Icons.Default.Edit, null, Modifier.size(18.dp).clickable { onEdit() }, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp).clickable { showMenu = true }, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (showMenu) AlertDialog(onDismissRequest = { showMenu = false }, title = { Text("ลบรายการ?") }, text = { Text("${cat.emoji} ${cat.label} — ${formatMoney(tx.amount, currency)}") },
        confirmButton = { TextButton({ onDelete(); showMenu = false }) { Text("ลบ", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ showMenu = false }) { Text("ยกเลิก") } })
}

// ═══════════════════════════════════════════════════════════════
//  SCREEN 2 — ADD / EDIT
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(vm: ExpenseViewModel, nav: NavController, editTx: Transaction? = null, currency: CurrencyInfo) {
    val ctx = LocalContext.current; val isEdit = editTx != null
    var amount by remember { mutableStateOf(editTx?.amount?.toString() ?: "") }; var note by remember { mutableStateOf(editTx?.note ?: "") }
    var type by remember { mutableStateOf(editTx?.type ?: "expense") }; var selectedCat by remember { mutableStateOf(catById(editTx?.category ?: "food")) }
    var expanded by remember { mutableStateOf(false) }; var date by remember { mutableStateOf(editTx?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var receiptB64 by remember { mutableStateOf(editTx?.receipt) }; var tempBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) { val out = ByteArrayOutputStream(); val s = 400f / bmp.width; val sc = Bitmap.createScaledBitmap(bmp, 400, (bmp.height * s).toInt(), true); sc.compress(Bitmap.CompressFormat.JPEG, 70, out); receiptB64 = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT); tempBitmap = sc }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(if (isEdit) "แก้ไขรายการ" else "บันทึกรายการ", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf("expense" to "💸 รายจ่าย", "income" to "💵 รายรับ").forEach { (t, label) -> val active = type == t
                Button({ type = t }, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = when { active && t == "expense" -> Color(0xFFC62828); active -> Color(0xFF2E7D32); else -> MaterialTheme.colorScheme.surfaceVariant }, contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)) { Text(label, fontWeight = FontWeight.Bold) } } } }
            item { OutlinedTextField(amount, { amount = it }, label = { Text("จำนวนเงิน (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)) }
            item { ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
                OutlinedTextField("${selectedCat.emoji} ${selectedCat.label}", {}, readOnly = true, label = { Text("หมวดหมู่") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded, { expanded = false }) { CATEGORIES.forEach { cat -> DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(cat.color.copy(.2f)), Alignment.Center) { Text(cat.emoji, fontSize = 15.sp) }; Text(cat.label, fontSize = 14.sp) } }, onClick = { selectedCat = cat; expanded = false }) } }
            } }
            item { OutlinedTextField(note, { note = it }, label = { Text("หมายเหตุ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)) }
            item { val c = Calendar.getInstance(); try { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)?.let { c.time = it } } catch (_: Exception) {}
                OutlinedTextField(date, {}, readOnly = true, label = { Text("วันที่") }, modifier = Modifier.fillMaxWidth().clickable { DatePickerDialog(ctx, { _, y, m, d -> date = "%04d-%02d-%02d".format(y, m + 1, d) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show() },
                    singleLine = true, shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.CalendarMonth, null) }, enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant)) }
            item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("ใบเสร็จ", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                if (receiptB64 != null) { val bytes = Base64.decode(receiptB64!!, Base64.DEFAULT); val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    Box { Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop); IconButton({ receiptB64 = null }, Modifier.align(Alignment.TopEnd).background(Color.Black.copy(.5f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color.White) } } }
                OutlinedButton({ cameraLauncher.launch(null) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (receiptB64 != null) "ถ่ายใหม่" else "ถ่ายรูปใบเสร็จ") }
            } }
            item { Button({ val amt = amount.toDoubleOrNull(); if (amt == null || amt <= 0) { Toast.makeText(ctx, "กรุณากรอกจำนวนเงิน", Toast.LENGTH_SHORT).show(); return@Button }
                if (isEdit) vm.update(editTx!!.fid, type, selectedCat.id, amt, note, date, receiptB64) else vm.save(type, selectedCat.id, amt, note, receiptB64)
                Toast.makeText(ctx, if (isEdit) "แก้ไขสำเร็จ ✓" else "บันทึกสำเร็จ! 🎉", Toast.LENGTH_SHORT).show(); nav.navigate("home") { launchSingleTop = true }
            }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))) { Text(if (isEdit) "บันทึกการแก้ไข ✓" else "บันทึก ✓", fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SCREEN 3 — SUMMARY + BAR CHART + PIE CHART
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(vm: ExpenseViewModel, currency: CurrencyInfo) {
    val transactions by vm.transactions.collectAsState(); val budgets by vm.budgets.collectAsState()
    val cal = Calendar.getInstance(); var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }; var month by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    val monthTx = remember(transactions, year, month) { vm.getMonthTx(year, month) }
    val totalExp = monthTx.filter { it.type == "expense" }.sumOf { it.amount }
    val chartData = remember(transactions, year, month) { (5 downTo 0).map { i -> var m = month - i; var y = year; if (m < 0) { m += 12; y-- }; Triple(MONTH_TH[m], vm.monthIncome(y, m).toFloat(), vm.monthExpense(y, m).toFloat()) } }
    val catSummary = CATEGORIES.map { cat -> Triple(cat, monthTx.filter { it.type == "expense" && it.category == cat.id }.sumOf { it.amount }, budgets[cat.id] ?: 0.0) }.filter { it.second > 0 }.sortedByDescending { it.second }

    Scaffold(topBar = { TopAppBar(title = { Text("📊 สรุปยอด", fontWeight = FontWeight.Bold) }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { MonthNav(year, month, { if (month == 0) { month = 11; year-- } else month-- }, { if (month == 11) { month = 0; year++ } else month++ }) }
            // Bar chart
            item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(12.dp)) {
                Text("รายรับ-รายจ่าย 6 เดือน", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 8.dp))
                AndroidView(factory = { ctx -> BarChart(ctx).apply { description.isEnabled = false; legend.isEnabled = true; setDrawGridBackground(false); setTouchEnabled(false); xAxis.apply { position = XAxis.XAxisPosition.BOTTOM; setDrawGridLines(false); granularity = 1f; textSize = 11f }; axisLeft.apply { setDrawGridLines(true); textSize = 10f }; axisRight.isEnabled = false } },
                    update = { chart -> val labels = chartData.map { it.first }; val dsInc = BarDataSet(chartData.mapIndexed { i, d -> BarEntry(i * 2f, d.second) }, "รายรับ").apply { color = Color(0xFF4CAF50).toArgb(); valueTextSize = 0f }
                        val dsExp = BarDataSet(chartData.mapIndexed { i, d -> BarEntry(i * 2f + 1f, d.third) }, "รายจ่าย").apply { color = Color(0xFFF44336).toArgb(); valueTextSize = 0f }
                        chart.data = BarData(dsInc, dsExp).apply { barWidth = 0.4f }; chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels.flatMap { listOf(it, "") }); chart.invalidate() }, modifier = Modifier.fillMaxWidth().height(200.dp))
            } } }
            // Pie chart
            if (catSummary.isNotEmpty()) item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(12.dp)) {
                Text("สัดส่วนรายจ่าย", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 8.dp))
                AndroidView(factory = { ctx -> PieChart(ctx).apply { description.isEnabled = false; isDrawHoleEnabled = true; holeRadius = 45f; transparentCircleRadius = 50f; setUsePercentValues(true); legend.isEnabled = true; setEntryLabelTextSize(10f) } },
                    update = { chart -> val entries = catSummary.map { (cat, spent, _) -> PieEntry(spent.toFloat(), cat.emoji) }
                        val ds = PieDataSet(entries, "").apply { colors = catSummary.map { it.first.color.toArgb() }; valueTextSize = 11f; valueFormatter = PercentFormatter(chart); sliceSpace = 2f }
                        chart.data = PieData(ds); chart.invalidate() }, modifier = Modifier.fillMaxWidth().height(220.dp))
            } } }
            item { Text("หมวดหมู่รายจ่าย", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline) }
            if (catSummary.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) { Text("ยังไม่มีรายจ่าย", color = MaterialTheme.colorScheme.outline) } }
            else items(catSummary) { (cat, spent, bdg) -> val isOver = bdg > 0 && spent > bdg; val bdgPct = if (bdg > 0) (spent / bdg).coerceAtMost(1.0).toFloat() else 0f
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (isOver) MaterialTheme.colorScheme.errorContainer.copy(.3f) else MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(13.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(cat.color.copy(.2f)), Alignment.Center) { Text(cat.emoji, fontSize = 18.sp) }
                        Column(Modifier.weight(1f)) { Text(cat.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); if (bdg > 0) Text("${formatMoney(spent, currency)} / ${formatMoney(bdg, currency)}", fontSize = 11.sp, color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline) }
                        Column(horizontalAlignment = Alignment.End) { Text(formatMoney(spent, currency), fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("${if (totalExp > 0) (spent / totalExp * 100).toInt() else 0}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline) }
                    }; if (bdg > 0) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { bdgPct }, Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)), color = if (isOver) MaterialTheme.colorScheme.error else cat.color, trackColor = MaterialTheme.colorScheme.outline.copy(.15f))
                        if (isOver) Text("🚨 เกินงบ ${formatMoney(spent - bdg, currency)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) } }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SCREEN 4 — BUDGET
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(vm: ExpenseViewModel, currency: CurrencyInfo) {
    val budgets by vm.budgets.collectAsState(); val transactions by vm.transactions.collectAsState()
    val cal = Calendar.getInstance(); var month by remember { mutableStateOf(cal.get(Calendar.MONTH)) }; var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
    val monthTx = remember(transactions, year, month) { vm.getMonthTx(year, month) }
    var editCat by remember { mutableStateOf<Category?>(null) }; var bdgInput by remember { mutableStateOf("") }
    if (editCat != null) AlertDialog(onDismissRequest = { editCat = null }, title = { Text("ตั้งงบประมาณ ${editCat!!.emoji} ${editCat!!.label}") },
        text = { OutlinedTextField(bdgInput, { bdgInput = it }, label = { Text("งบประมาณ (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton({ vm.saveBudget(editCat!!.id, bdgInput.toDoubleOrNull() ?: 0.0); editCat = null }) { Text("บันทึก") } },
        dismissButton = { TextButton({ editCat = null }) { Text("ยกเลิก") } })
    Scaffold(topBar = { TopAppBar(title = { Text("💰 งบประมาณ", fontWeight = FontWeight.Bold) }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MonthNav(year, month, { if (month == 0) { month = 11; year-- } else month-- }, { if (month == 11) { month = 0; year++ } else month++ }) }
            item { Text("${MONTH_TH[month]} ${year + 543} — แตะเพื่อตั้งงบ", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline) }
            items(CATEGORIES.filter { it.id != "salary" }) { cat -> val bdg = budgets[cat.id] ?: 0.0; val spent = monthTx.filter { it.type == "expense" && it.category == cat.id }.sumOf { it.amount }
                val pct = if (bdg > 0) (spent / bdg).coerceAtMost(1.0).toFloat() else 0f; val isOver = bdg > 0 && spent > bdg
                Card(Modifier.fillMaxWidth().clickable { editCat = cat; bdgInput = if (bdg > 0) bdg.toLong().toString() else "" }, shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isOver) MaterialTheme.colorScheme.errorContainer.copy(.25f) else MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(cat.color.copy(.2f)), Alignment.Center) { Text(cat.emoji, fontSize = 18.sp) }
                        Column(Modifier.weight(1f)) { Text(cat.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(if (bdg > 0) "${formatMoney(spent, currency)} / ${formatMoney(bdg, currency)}" else "ยังไม่ได้ตั้งงบ", fontSize = 11.sp, color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline) }
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                    }; if (bdg > 0) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { pct }, Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)), color = if (isOver) MaterialTheme.colorScheme.error else cat.color, trackColor = MaterialTheme.colorScheme.outline.copy(.12f)) } }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SCREEN 5 — RECURRING (+ confirm delete)
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(vm: ExpenseViewModel, ctx: Context, currency: CurrencyInfo) {
    val recurList by vm.recurring.collectAsState(); var showForm by remember { mutableStateOf(false) }; var editItem by remember { mutableStateOf<RecurringItem?>(null) }
    var rAmt by remember { mutableStateOf("") }; var rName by remember { mutableStateOf("") }; var rType by remember { mutableStateOf("expense") }
    var rCat by remember { mutableStateOf(CATEGORIES[0]) }; var rFreq by remember { mutableStateOf("monthly") }; var rDay by remember { mutableStateOf("1") }; var rExpanded by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf<RecurringItem?>(null) }
    fun resetForm() { rAmt = ""; rName = ""; rType = "expense"; rCat = CATEGORIES[0]; rFreq = "monthly"; rDay = "1"; rExpanded = false; editItem = null }

    // Delete confirmation dialog
    if (deleteConfirm != null) { val item = deleteConfirm!!; val cat = catById(item.category)
        AlertDialog(onDismissRequest = { deleteConfirm = null }, title = { Text("ลบรายการประจำ?") },
            text = { Text("${cat.emoji} ${item.name} — ${formatMoney(item.amount, currency)}") },
            confirmButton = { TextButton({ vm.deleteRecurring(item.id); deleteConfirm = null }) { Text("ลบ", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ deleteConfirm = null }) { Text("ยกเลิก") } })
    }

    if (showForm) AlertDialog(onDismissRequest = { showForm = false; resetForm() }, title = { Text(if (editItem != null) "แก้ไขรายการประจำ" else "เพิ่มรายการประจำ") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("expense" to "รายจ่าย", "income" to "รายรับ").forEach { (t, l) -> FilterChip(selected = rType == t, onClick = { rType = t }, label = { Text(l, fontSize = 12.sp) }) } }
            OutlinedTextField(rAmt, { rAmt = it }, label = { Text("จำนวนเงิน") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(rName, { rName = it }, label = { Text("ชื่อ เช่น ค่าเช่า") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(rExpanded, { rExpanded = !rExpanded }) { OutlinedTextField("${rCat.emoji} ${rCat.label}", {}, readOnly = true, label = { Text("หมวดหมู่") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(rExpanded, { rExpanded = false }) { CATEGORIES.forEach { cat -> DropdownMenuItem(text = { Text("${cat.emoji} ${cat.label}") }, onClick = { rCat = cat; rExpanded = false }) } } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("monthly" to "รายเดือน", "weekly" to "รายสัปดาห์").forEach { (f, l) -> FilterChip(selected = rFreq == f, onClick = { rFreq = f }, label = { Text(l, fontSize = 12.sp) }) } }
            OutlinedTextField(rDay, { rDay = it }, label = { Text(if (rFreq == "monthly") "วันที่ (1–31)" else "วันในสัปดาห์ (1=จ.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        } },
        confirmButton = { TextButton({ val amt = rAmt.toDoubleOrNull() ?: return@TextButton; vm.saveRecurring(RecurringItem(editItem?.id ?: "rc_${System.currentTimeMillis()}", rType, rCat.id, amt, rName, rFreq, rDay.toIntOrNull() ?: 1)); showForm = false; resetForm() }) { Text("บันทึก") } },
        dismissButton = { TextButton({ showForm = false; resetForm() }) { Text("ยกเลิก") } })

    Scaffold(topBar = { TopAppBar(title = { Text("🔄 รายการประจำ", fontWeight = FontWeight.Bold) }, actions = { IconButton({ showForm = true }) { Icon(Icons.Default.Add, null) } }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("รายการที่เกิดซ้ำ กด 'บันทึกวันนี้' เพื่อเพิ่มเข้าประวัติ", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline) }
            if (recurList.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("ยังไม่มีรายการประจำ", color = MaterialTheme.colorScheme.outline) } }
            else items(recurList, key = { it.id }) { item -> val cat = catById(item.category)
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(13.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(cat.color.copy(.2f)), Alignment.Center) { Text(cat.emoji, fontSize = 19.sp) }
                        Column(Modifier.weight(1f)) { Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text("${cat.label} · ${FREQ_LABEL[item.freq]} · วันที่ ${item.day}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline) }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${if (item.type == "income") "+" else "-"}${formatMoney(item.amount, currency)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (item.type == "income") Color(0xFF2E7D32) else Color(0xFFC62828))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton({ vm.applyRecurring(item); Toast.makeText(ctx, "บันทึกแล้ว ✓", Toast.LENGTH_SHORT).show() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), modifier = Modifier.height(28.dp)) { Text("บันทึกวันนี้", fontSize = 10.sp) }
                                IconButton({ deleteConfirm = item }, Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  CSV EXPORT
// ═══════════════════════════════════════════════════════════════
fun exportCSV(ctx: Context, transactions: List<Transaction>, currency: CurrencyInfo) {
    val sb = StringBuilder(); sb.appendLine("วันที่,ประเภท,หมวดหมู่,หมายเหตุ,จำนวนเงิน (${currency.symbol}),ประจำ")
    transactions.forEach { t -> sb.appendLine("${t.date},${if (t.type == "income") "รายรับ" else "รายจ่าย"},${catById(t.category).label.replace(",","")},${t.note.replace(",", " ")},${t.amount},${if (t.isRecurring) "ใช่" else "ไม่"}") }
    try { val file = File(ctx.getExternalFilesDir(null), "expense_${System.currentTimeMillis()}.csv"); file.writeText("\uFEFF$sb")
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Export CSV"))
    } catch (e: Exception) { Toast.makeText(ctx, "ไม่สามารถ export ได้: ${e.message}", Toast.LENGTH_SHORT).show() }
}

// ═══════════════════════════════════════════════════════════════
//  SHARED COMPOSABLES
// ═══════════════════════════════════════════════════════════════
@Composable
fun MonthNav(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onPrev) { Text("‹", fontSize = 24.sp) }; Text("${MONTH_TH[month]} ${year + 543}", fontSize = 16.sp, fontWeight = FontWeight.Bold); IconButton(onNext) { Text("›", fontSize = 24.sp) }
    }
}
@Composable
fun AmountLabel(label: String, value: String, color: Color) {
    Column { Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.6f)); Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}
