// ═══════════════════════════════════════════════════════════════
//  MainActivity.kt  —  Expense Tracker v5
//  + Voice input, Multi-wallet, Exchange rate, Trip budget
// ═══════════════════════════════════════════════════════════════
package com.example.myexpensetracker

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Base64
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
val CURRENCY_KEY = stringPreferencesKey("currency")
val WALLET_KEY = stringPreferencesKey("active_wallet")
val ONBOARDED_KEY = booleanPreferencesKey("onboarded")

// ═══════════════════════════════════════════════════════════════
//  CONSTANTS
// ═══════════════════════════════════════════════════════════════
data class Category(val id: String, val label: String, val emoji: String, val color: Color)
val CATEGORIES = listOf(
    Category("food","อาหาร","🍜",Color(0xFFFF6B6B)), Category("transport","เดินทาง","🚗",Color(0xFF4ECDC4)),
    Category("shopping","ช้อปปิ้ง","🛍️",Color(0xFF45B7D1)), Category("health","สุขภาพ","💊",Color(0xFF96CEB4)),
    Category("entertainment","บันเทิง","🎮",Color(0xFFFFEAA7)), Category("utility","สาธารณูปโภค","💡",Color(0xFFDDA0DD)),
    Category("salary","เงินเดือน","💼",Color(0xFF98FB98)), Category("other","อื่นๆ","📦",Color(0xFFF0E68C)),
)
data class CurrencyInfo(val code: String, val symbol: String, val label: String)
val CURRENCIES = listOf(CurrencyInfo("THB","฿","บาท"),CurrencyInfo("USD","$","ดอลลาร์"),CurrencyInfo("EUR","€","ยูโร"),
    CurrencyInfo("JPY","¥","เยน"),CurrencyInfo("GBP","£","ปอนด์"),CurrencyInfo("KRW","₩","วอน"),CurrencyInfo("CNY","¥","หยวน"))
val MONTH_TH = listOf("ม.ค.","ก.พ.","มี.ค.","เม.ย.","พ.ค.","มิ.ย.","ก.ค.","ส.ค.","ก.ย.","ต.ค.","พ.ย.","ธ.ค.")
val FREQ_LABEL = mapOf("monthly" to "รายเดือน", "weekly" to "รายสัปดาห์")
val SORT_OPTIONS = listOf("date_desc" to "วันที่ ใหม่→เก่า","date_asc" to "วันที่ เก่า→ใหม่","amount_desc" to "จำนวน มาก→น้อย","amount_asc" to "จำนวน น้อย→มาก","category" to "หมวดหมู่")
fun catById(id: String) = CATEGORIES.find { it.id == id } ?: Category("other",id,"📦",Color(0xFFF0E68C))
fun formatMoney(n: Double, c: CurrencyInfo): String { val nf = NumberFormat.getNumberInstance(Locale("th","TH")).apply { maximumFractionDigits = 0 }; return "${c.symbol}${nf.format(n)}" }

// ═══════════════════════════════════════════════════════════════
//  DATA MODELS
// ═══════════════════════════════════════════════════════════════
data class Transaction(val fid: String="", val type: String="expense", val category: String="other", val amount: Double=0.0, val note: String="", val date: String="", val createdAt: Long=0L, val isRecurring: Boolean=false, val receipt: String?=null)
data class RecurringItem(val id: String="", val type: String="expense", val category: String="other", val amount: Double=0.0, val name: String="", val freq: String="monthly", val day: Int=1)
data class SavingsGoal(val name: String="", val target: Double=0.0, val current: Double=0.0)
data class Wallet(val id: String="", val name: String="", val emoji: String="💼", val currency: String="THB", val exchangeRate: Double=1.0, val budget: Double=0.0)

// ═══════════════════════════════════════════════════════════════
//  VIEWMODEL
// ═══════════════════════════════════════════════════════════════
class ExpenseViewModel : ViewModel() {
    private val db = Firebase.firestore; private val auth = Firebase.auth
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList()); val transactions: StateFlow<List<Transaction>> = _transactions
    private val _budgets = MutableStateFlow<Map<String, Double>>(emptyMap()); val budgets: StateFlow<Map<String, Double>> = _budgets
    private val _recurring = MutableStateFlow<List<RecurringItem>>(emptyList()); val recurring: StateFlow<List<RecurringItem>> = _recurring
    private val _savingsGoal = MutableStateFlow(SavingsGoal()); val savingsGoal: StateFlow<SavingsGoal> = _savingsGoal
    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList()); val wallets: StateFlow<List<Wallet>> = _wallets
    private val _isLoading = MutableStateFlow(true); val isLoading: StateFlow<Boolean> = _isLoading
    val shownAlerts = mutableSetOf<String>()
    private val uid: String? get() = auth.currentUser?.uid
    private var _activeWallet = MutableStateFlow("default"); val activeWallet: StateFlow<String> = _activeWallet
    fun setActiveWallet(id: String) { _activeWallet.value = id; startListening() }

    private fun userDoc() = db.collection("users").document(uid ?: "anon")
    private fun txCol() = userDoc().collection("wallets").document(_activeWallet.value).collection("transactions")
    private fun settingsCol() = userDoc().collection("wallets").document(_activeWallet.value).collection("settings")
    private fun walletsCol() = userDoc().collection("wallet_list")

    fun startListening() { if (uid == null) return; listenTransactions(); listenBudgets(); listenRecurring(); listenSavings(); listenWallets() }
    fun clearData() { _transactions.value = emptyList(); _budgets.value = emptyMap(); _recurring.value = emptyList(); _savingsGoal.value = SavingsGoal(); _wallets.value = emptyList(); shownAlerts.clear() }

    private fun listenTransactions() { _isLoading.value = true; txCol().orderBy("createdAt", Query.Direction.DESCENDING).addSnapshotListener { snap, err ->
        _isLoading.value = false
        if (err != null) { Log.e("FS", err.message ?: ""); return@addSnapshotListener }
        _transactions.value = snap?.documents?.map { d -> Transaction(d.id, d.getString("type")?:"expense", d.getString("category")?:"other", d.getDouble("amount")?:0.0, d.getString("note")?:"", d.getString("date")?:"", d.getLong("createdAt")?:0L, d.getBoolean("isRecurring")?:false, d.getString("receipt")) } ?: emptyList()
    } }
    private fun listenBudgets() { settingsCol().document("budgets").addSnapshotListener { snap, _ -> if (snap != null && snap.exists()) _budgets.value = snap.data?.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 } ?: emptyMap() } }
    @Suppress("UNCHECKED_CAST")
    private fun listenRecurring() { settingsCol().document("recurring").addSnapshotListener { snap, _ -> if (snap != null && snap.exists()) { val list = snap.get("list") as? List<Map<String, Any>> ?: emptyList()
        _recurring.value = list.map { m -> RecurringItem(m["id"] as? String ?: "", m["type"] as? String ?: "expense", m["category"] as? String ?: "other", (m["amount"] as? Number)?.toDouble() ?: 0.0, m["name"] as? String ?: "", m["freq"] as? String ?: "monthly", (m["day"] as? Number)?.toInt() ?: 1) } } } }
    private fun listenSavings() { settingsCol().document("savings").addSnapshotListener { snap, _ -> if (snap != null && snap.exists()) _savingsGoal.value = SavingsGoal(snap.getString("name")?:"", snap.getDouble("target")?:0.0, snap.getDouble("current")?:0.0) } }
    private fun listenWallets() { walletsCol().addSnapshotListener { snap, _ -> _wallets.value = snap?.documents?.map { d -> Wallet(d.id, d.getString("name")?:"", d.getString("emoji")?:"💼", d.getString("currency")?:"THB", d.getDouble("exchangeRate")?:1.0, d.getDouble("budget")?:0.0) } ?: emptyList()
        if (_wallets.value.isEmpty()) saveWallet(Wallet("default","กระเป๋าหลัก","💼","THB",1.0,0.0))
    } }

    fun save(type: String, catId: String, amount: Double, note: String, receipt: String?=null) { txCol().add(hashMapOf("type" to type,"category" to catId,"amount" to amount,"note" to note,"date" to SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(Date()),"createdAt" to System.currentTimeMillis(),"isRecurring" to false,"receipt" to receipt)) }
    fun update(fid: String, type: String, catId: String, amount: Double, note: String, date: String, receipt: String?) { txCol().document(fid).update(mapOf("type" to type,"category" to catId,"amount" to amount,"note" to note,"date" to date,"receipt" to receipt,"updatedAt" to System.currentTimeMillis())) }
    fun delete(fid: String) = txCol().document(fid).delete()
    fun saveBudget(catId: String, amount: Double) { settingsCol().document("budgets").set(_budgets.value.toMutableMap().also { it[catId] = amount }) }
    fun saveRecurring(item: RecurringItem) { val list = _recurring.value.toMutableList(); val idx = list.indexOfFirst { it.id == item.id }; if (idx >= 0) list[idx] = item else list.add(item)
        settingsCol().document("recurring").set(mapOf("list" to list.map { mapOf("id" to it.id,"type" to it.type,"category" to it.category,"amount" to it.amount,"name" to it.name,"freq" to it.freq,"day" to it.day) })) }
    fun deleteRecurring(id: String) { val list = _recurring.value.filter { it.id != id }; settingsCol().document("recurring").set(mapOf("list" to list.map { mapOf("id" to it.id,"type" to it.type,"category" to it.category,"amount" to it.amount,"name" to it.name,"freq" to it.freq,"day" to it.day) })) }
    fun applyRecurring(item: RecurringItem) { txCol().add(hashMapOf("type" to item.type,"category" to item.category,"amount" to item.amount,"note" to item.name,"date" to SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(Date()),"createdAt" to System.currentTimeMillis(),"isRecurring" to true,"receipt" to null)) }
    fun saveSavingsGoal(name: String, target: Double, current: Double) { settingsCol().document("savings").set(mapOf("name" to name,"target" to target,"current" to current)) }
    fun addToSavings(amount: Double) { val g = _savingsGoal.value; saveSavingsGoal(g.name, g.target, g.current + amount) }
    fun saveWallet(w: Wallet) { walletsCol().document(w.id).set(mapOf("name" to w.name,"emoji" to w.emoji,"currency" to w.currency,"exchangeRate" to w.exchangeRate,"budget" to w.budget)) }
    fun deleteWallet(id: String) { if (id == "default") return; walletsCol().document(id).delete(); if (_activeWallet.value == id) setActiveWallet("default") }
    fun getMonthTx(year: Int, month: Int) = _transactions.value.filter { it.date.startsWith("%04d-%02d".format(year, month + 1)) }
    fun monthIncome(y: Int, m: Int) = getMonthTx(y, m).filter { it.type == "income" }.sumOf { it.amount }
    fun monthExpense(y: Int, m: Int) = getMonthTx(y, m).filter { it.type == "expense" }.sumOf { it.amount }
}

// ═══════════════════════════════════════════════════════════════
//  VOICE INPUT HELPER — แปลงเสียงเป็นรายการ
// ═══════════════════════════════════════════════════════════════
fun parseVoiceInput(text: String): Triple<String, Double, String>? {
    // ตัวอย่าง: "กินข้าว 50 บาท" → (food, 50, กินข้าว) / "เงินเดือน 30000" → (salary, 30000, เงินเดือน)
    val numRegex = Regex("(\\d+\\.?\\d*)")
    val match = numRegex.find(text) ?: return null
    val amount = match.value.toDoubleOrNull() ?: return null
    val note = text.replace(numRegex, "").replace("บาท","").trim()
    val catKeywords = mapOf("กิน" to "food","ข้าว" to "food","อาหาร" to "food","กาแฟ" to "food","ชา" to "food",
        "รถ" to "transport","แท็กซี่" to "transport","น้ำมัน" to "transport","เดินทาง" to "transport","แกร็บ" to "transport",
        "ซื้อ" to "shopping","ช้อป" to "shopping","เสื้อ" to "shopping",
        "หมอ" to "health","ยา" to "health","โรงพยาบาล" to "health",
        "หนัง" to "entertainment","เกม" to "entertainment","เที่ยว" to "entertainment",
        "ค่าไฟ" to "utility","ค่าน้ำ" to "utility","ค่าเน็ต" to "utility","ค่าโทร" to "utility",
        "เงินเดือน" to "salary","โบนัส" to "salary","รายได้" to "salary")
    val lower = note.lowercase()
    val catId = catKeywords.entries.firstOrNull { lower.contains(it.key) }?.value ?: "other"
    val type = if (catId == "salary" || lower.contains("รายได้") || lower.contains("โบนัส")) "income" else "expense"
    return Triple(catId, amount, note.ifBlank { catById(catId).label })
}

// ═══════════════════════════════════════════════════════════════
//  FCM + NOTIFICATIONS
// ═══════════════════════════════════════════════════════════════
class MyFirebaseMessagingService : FirebaseMessagingService() { override fun onMessageReceived(msg: RemoteMessage) { pushNotification(applicationContext, msg.notification?.title ?: msg.data["title"] ?: "แจ้งเตือน", msg.notification?.body ?: msg.data["body"] ?: "") } }
fun pushNotification(ctx: Context, title: String, body: String) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannel("budget_alerts","แจ้งเตือนงบประมาณ",NotificationManager.IMPORTANCE_HIGH).also { ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(it) }
    val n = NotificationCompat.Builder(ctx,"budget_alerts").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
    if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) NotificationManagerCompat.from(ctx).notify(System.currentTimeMillis().toInt(), n) }
fun checkBudgetNotifications(ctx: Context, tx: List<Transaction>, budgets: Map<String, Double>, shown: MutableSet<String>, cur: CurrencyInfo) {
    val prefix = SimpleDateFormat("yyyy-MM",Locale.getDefault()).format(Date()); val moExp = tx.filter { it.date.startsWith(prefix) && it.type == "expense" }
    CATEGORIES.forEach { cat -> val bdg = budgets[cat.id]?.takeIf { it > 0 } ?: return@forEach; val spent = moExp.filter { it.category == cat.id }.sumOf { it.amount }; val pct = spent / bdg * 100
        val k100 = "over_${cat.id}_$prefix"; val k80 = "warn_${cat.id}_$prefix"
        when { pct >= 100 && k100 !in shown -> { shown += k100; pushNotification(ctx,"🚨 เกินงบ! ${cat.emoji} ${cat.label}","${formatMoney(spent,cur)} / ${formatMoney(bdg,cur)}") }
            pct in 80.0..99.99 && k80 !in shown -> { shown += k80; pushNotification(ctx,"⚠️ ใกล้เกินงบ ${cat.emoji} ${cat.label}","ใช้ไปแล้ว ${pct.toInt()}%") } } } }

// ═══════════════════════════════════════════════════════════════
//  MAIN ACTIVITY
// ═══════════════════════════════════════════════════════════════
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 1001)
        setContent {
            val scope = rememberCoroutineScope()
            // Avoid calling Flow operators directly in composition — remember the transformed flows
            val isDarkFlow = remember { applicationContext.dataStore.data.map { it[DARK_MODE_KEY] ?: false } }
            val bioEnabledFlow = remember { applicationContext.dataStore.data.map { it[BIOMETRIC_KEY] ?: false } }
            val curCodeFlow = remember { applicationContext.dataStore.data.map { it[CURRENCY_KEY] ?: "THB" } }
            val walletIdFlow = remember { applicationContext.dataStore.data.map { it[WALLET_KEY] ?: "default" } }
            val onboardedFlow = remember { applicationContext.dataStore.data.map { it[ONBOARDED_KEY] ?: false } }

            val isDark by isDarkFlow.collectAsState(initial = false)
            val bioEnabled by bioEnabledFlow.collectAsState(initial = false)
            val curCode by curCodeFlow.collectAsState(initial = "THB")
            val walletId by walletIdFlow.collectAsState(initial = "default")
            val onboarded by onboardedFlow.collectAsState(initial = false)

            val currency = CURRENCIES.find { it.code == curCode } ?: CURRENCIES[0]

            MyExpenseTrackerTheme(darkTheme = isDark) {
                AppRoot(isDark,
                    { scope.launch { applicationContext.dataStore.edit { it[DARK_MODE_KEY] = !isDark } } },
                    bioEnabled,
                    { scope.launch { applicationContext.dataStore.edit { it[BIOMETRIC_KEY] = !bioEnabled } } },
                    currency,
                    { c -> scope.launch { applicationContext.dataStore.edit { it[CURRENCY_KEY] = c.code } } },
                    walletId,
                    { w -> scope.launch { applicationContext.dataStore.edit { it[WALLET_KEY] = w } } },
                    onboarded,
                    { scope.launch { applicationContext.dataStore.edit { it[ONBOARDED_KEY] = true } } },
                    this)
            }
        }
    }
}

@Composable
fun AppRoot(isDark: Boolean, onToggle: () -> Unit, bioEnabled: Boolean, onBioToggle: () -> Unit, currency: CurrencyInfo, onCurChange: (CurrencyInfo) -> Unit, walletId: String, onWalletChange: (String) -> Unit, onboarded: Boolean, onOnboarded: () -> Unit, activity: FragmentActivity) {
    val auth = Firebase.auth; val vm: ExpenseViewModel = viewModel(); var currentUser by remember { mutableStateOf(auth.currentUser) }; var bioPassed by remember { mutableStateOf(!bioEnabled) }

    // Onboarding screen
    if (!onboarded) { OnboardingScreen(onOnboarded); return }

    if (currentUser != null && bioEnabled && !bioPassed) {
        LaunchedEffect(Unit) { val executor: Executor = ContextCompat.getMainExecutor(activity)
            BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { bioPassed = true }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) activity.finish() }
            }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("ปลดล็อค MyExpenseTracker").setSubtitle("ใช้ลายนิ้วมือหรือ PIN").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL).build()) }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("🔒", fontSize = 48.sp); Spacer(Modifier.height(16.dp)); Text("กรุณาปลดล็อค", fontSize = 18.sp, color = MaterialTheme.colorScheme.outline) } }; return }
    if (currentUser == null) AuthScreen { currentUser = auth.currentUser; bioPassed = true }
    else ExpenseTrackerApp(vm, isDark, onToggle, { auth.signOut(); currentUser = null }, bioEnabled, onBioToggle, currency, onCurChange, walletId, onWalletChange)
}

// ═══════════════════════════════════════════════════════════════
//  AUTH SCREEN
// ═══════════════════════════════════════════════════════════════
@Composable
fun AuthScreen(onSuccess: () -> Unit) { val auth = Firebase.auth; val ctx = LocalContext.current; var email by remember { mutableStateOf("") }; var pw by remember { mutableStateOf("") }; var isLogin by remember { mutableStateOf(true) }; var loading by remember { mutableStateOf(false) }; var showPw by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { Column(Modifier.fillMaxWidth().padding(32.dp).align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("💰", fontSize = 56.sp); Text("MyExpenseTracker", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(if (isLogin) "เข้าสู่ระบบ" else "สมัครสมาชิก", fontSize = 16.sp, color = MaterialTheme.colorScheme.outline); Spacer(Modifier.height(8.dp))
        OutlinedTextField(email, { email = it.trim() }, label = { Text("อีเมล") }, leadingIcon = { Icon(Icons.Default.Email, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(pw, { pw = it }, label = { Text("รหัสผ่าน") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, trailingIcon = { IconButton({ showPw = !showPw }) { Icon(if (showPw) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Button({ if (email.isBlank() || pw.length < 6) { Toast.makeText(ctx,"กรุณากรอกอีเมลและรหัสผ่าน (6 ตัวขึ้นไป)",Toast.LENGTH_SHORT).show(); return@Button }; loading = true
            (if (isLogin) auth.signInWithEmailAndPassword(email, pw) else auth.createUserWithEmailAndPassword(email, pw))
                .addOnSuccessListener { loading = false; Toast.makeText(ctx, if (isLogin) "เข้าสู่ระบบสำเร็จ ✓" else "สมัครสำเร็จ ✓", Toast.LENGTH_SHORT).show(); onSuccess() }
                .addOnFailureListener { e -> loading = false; Toast.makeText(ctx, when { e.message?.contains("badly formatted")==true -> "รูปแบบอีเมลไม่ถูกต้อง"; e.message?.contains("already in use")==true -> "อีเมลนี้ถูกใช้แล้ว"; e.message?.contains("INVALID_LOGIN")==true -> "อีเมลหรือรหัสผ่านไม่ถูกต้อง"; else -> e.message ?: "เกิดข้อผิดพลาด" }, Toast.LENGTH_LONG).show() }
        }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))) {
            if (loading) CircularProgressIndicator(Modifier.size(24.dp), Color.White, strokeWidth = 2.dp) else Text(if (isLogin) "เข้าสู่ระบบ" else "สมัครสมาชิก", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        TextButton({ isLogin = !isLogin }) { Text(if (isLogin) "ยังไม่มีบัญชี? สมัครสมาชิก" else "มีบัญชีแล้ว? เข้าสู่ระบบ", color = MaterialTheme.colorScheme.primary) }
    } }
}

// ═══════════════════════════════════════════════════════════════
//  ROOT + NAV
// ═══════════════════════════════════════════════════════════════
@Composable
fun ExpenseTrackerApp(vm: ExpenseViewModel = viewModel(), isDark: Boolean, onToggle: () -> Unit, onLogout: () -> Unit, bioEnabled: Boolean, onBioToggle: () -> Unit, currency: CurrencyInfo, onCurChange: (CurrencyInfo) -> Unit, walletId: String, onWalletChange: (String) -> Unit) {
    val tx by vm.transactions.collectAsState(); val budgets by vm.budgets.collectAsState(); val wallets by vm.wallets.collectAsState()
    val loading by vm.isLoading.collectAsState()
    val ctx = LocalContext.current; val nav = rememberNavController()
    val activeWallet = wallets.find { it.id == walletId } ?: Wallet("default","กระเป๋าหลัก","💼","THB",1.0,0.0)
    val walletCurrency = CURRENCIES.find { it.code == activeWallet.currency } ?: currency
    LaunchedEffect(walletId) { vm.setActiveWallet(walletId) }
    LaunchedEffect(tx, budgets) { if (tx.isNotEmpty() && budgets.isNotEmpty()) checkBudgetNotifications(ctx, tx, budgets, vm.shownAlerts, walletCurrency) }
    // Loading state
    if (loading) { Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Color(0xFF5C6BC0)); Spacer(Modifier.height(16.dp)); Text("กำลังโหลดข้อมูล...", color = MaterialTheme.colorScheme.outline) } }; return }
    Scaffold(bottomBar = { BottomNav(nav) }) { pad -> NavHost(nav, "home", Modifier.padding(pad)) {
        composable("home") { TransactionListScreen(vm, nav, isDark, onToggle, onLogout, bioEnabled, onBioToggle, walletCurrency, onCurChange, wallets, walletId, onWalletChange, activeWallet) }
        composable("add") { AddEditScreen(vm, nav, currency = walletCurrency) }
        composable("edit/{fid}") { b -> val fid = b.arguments?.getString("fid") ?: return@composable; val t = tx.find { it.fid == fid }; if (t != null) AddEditScreen(vm, nav, t, walletCurrency) }
        composable("summary") { SummaryScreen(vm, walletCurrency) }
        composable("budget") { BudgetScreen(vm, walletCurrency) }
        composable("recur") { RecurringScreen(vm, ctx, walletCurrency) }
        composable("calendar") { CalendarScreen(vm, nav, walletCurrency) }
    } }
}
@Composable fun BottomNav(nav: NavController) { val cur = nav.currentBackStackEntryAsState().value?.destination?.route; NavigationBar {
    NavigationBarItem(cur=="home", { nav.navigate("home") { launchSingleTop = true } }, { Icon(Icons.Default.List, null) }, label = { Text("รายการ") })
    NavigationBarItem(cur=="add", { nav.navigate("add") { launchSingleTop = true } }, { Icon(Icons.Default.Add, null) }, label = { Text("บันทึก") })
    NavigationBarItem(cur=="calendar", { nav.navigate("calendar") { launchSingleTop = true } }, { Icon(Icons.Default.CalendarMonth, null) }, label = { Text("ปฏิทิน") })
    NavigationBarItem(cur=="summary", { nav.navigate("summary") { launchSingleTop = true } }, { Icon(Icons.Default.BarChart, null) }, label = { Text("สรุป") })
    NavigationBarItem(cur=="budget", { nav.navigate("budget") { launchSingleTop = true } }, { Icon(Icons.Default.AccountBalance, null) }, label = { Text("งบ") })
} }

// ═══════════════════════════════════════════════════════════════
//  SCREEN 1 — TRANSACTION LIST + Voice + Wallet picker
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(vm: ExpenseViewModel, nav: NavController, isDark: Boolean, onToggle: () -> Unit, onLogout: () -> Unit,
    bioEnabled: Boolean, onBioToggle: () -> Unit, currency: CurrencyInfo, onCurChange: (CurrencyInfo) -> Unit,
    wallets: List<Wallet>, walletId: String, onWalletChange: (String) -> Unit, activeWallet: Wallet) {
    val tx by vm.transactions.collectAsState(); val savings by vm.savingsGoal.collectAsState()
    val cal = Calendar.getInstance(); val ctx = LocalContext.current
    var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }; var month by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    var search by remember { mutableStateOf("") }; var filterCat by remember { mutableStateOf<String?>(null) }; var showFilter by remember { mutableStateOf(false) }
    var receiptPreview by remember { mutableStateOf<String?>(null) }; var showLogout by remember { mutableStateOf(false) }; var sortBy by remember { mutableStateOf("date_desc") }
    var showSettings by remember { mutableStateOf(false) }; var showSavings by remember { mutableStateOf(false) }
    var showWalletDialog by remember { mutableStateOf(false) }; var showNewWallet by remember { mutableStateOf(false) }
    var sName by remember { mutableStateOf(savings.name) }; var sTarget by remember { mutableStateOf(if (savings.target > 0) savings.target.toLong().toString() else "") }; var sAdd by remember { mutableStateOf("") }
    // Wallet form state
    var wName by remember { mutableStateOf("") }; var wEmoji by remember { mutableStateOf("✈️") }; var wCur by remember { mutableStateOf("THB") }; var wRate by remember { mutableStateOf("1.0") }; var wBudget by remember { mutableStateOf("") }

    val monthTx = remember(tx, year, month) { vm.getMonthTx(year, month) }
    val sorted = remember(monthTx, search, filterCat, sortBy) { monthTx.filter { t -> (search.isBlank() || t.note.contains(search,true) || catById(t.category).label.contains(search,true) || t.amount.toString().contains(search)) && (filterCat == null || t.category == filterCat) }.let { l -> when(sortBy) { "date_asc" -> l.sortedBy { it.date }; "amount_desc" -> l.sortedByDescending { it.amount }; "amount_asc" -> l.sortedBy { it.amount }; "category" -> l.sortedBy { it.category }; else -> l } } }
    val income = monthTx.filter { it.type == "income" }.sumOf { it.amount }; val expense = monthTx.filter { it.type == "expense" }.sumOf { it.amount }

    // Voice input launcher
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) { val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return@rememberLauncherForActivityResult
            val parsed = parseVoiceInput(spoken)
            if (parsed != null) { val (catId, amount, note) = parsed; vm.save(if (catId == "salary") "income" else "expense", catId, amount, note)
                Toast.makeText(ctx, "บันทึก: ${catById(catId).emoji} $note ${formatMoney(amount, currency)} ✓", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(ctx, "ไม่เข้าใจ: \"$spoken\" ลองพูดเช่น \"กินข้าว 50 บาท\"", Toast.LENGTH_LONG).show()
        }
    }

    if (showLogout) AlertDialog({ showLogout = false }, { TextButton({ vm.clearData(); onLogout(); showLogout = false }) { Text("ออกจากระบบ", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ showLogout = false }) { Text("ยกเลิก") } }, title = { Text("ออกจากระบบ?") })
    if (showSettings) Dialog({ showSettings = false }) { Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("⚙️ ตั้งค่า", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("🔒 ล็อคด้วยลายนิ้วมือ/PIN"); Switch(bioEnabled, { onBioToggle() }) }
        Text("💱 สกุลเงินเริ่มต้น", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        CURRENCIES.chunked(4).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { c -> FilterChip(currency.code == c.code, { onCurChange(c) }, label = { Text("${c.symbol} ${c.label}", fontSize = 11.sp) }) } } }
        TextButton({ showSettings = false }, Modifier.align(Alignment.End)) { Text("ปิด") }
    } } }
    if (showSavings) { LaunchedEffect(savings) { sName = savings.name; sTarget = if (savings.target > 0) savings.target.toLong().toString() else "" }
        AlertDialog({ showSavings = false }, { TextButton({ val t = sTarget.toDoubleOrNull() ?: 0.0; val a = sAdd.toDoubleOrNull() ?: 0.0; if (a > 0) vm.addToSavings(a) else vm.saveSavingsGoal(sName, t, savings.current); sAdd = ""; showSavings = false }) { Text("บันทึก") } },
            dismissButton = { TextButton({ showSavings = false }) { Text("ยกเลิก") } }, title = { Text("🎯 เป้าหมายออม") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(sName, { sName = it }, label = { Text("ชื่อเป้าหมาย") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sTarget, { sTarget = it }, label = { Text("เป้าหมาย (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (savings.target > 0) OutlinedTextField(sAdd, { sAdd = it }, label = { Text("เพิ่มเงินออม (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()) } })
    }
    // Wallet picker dialog
    if (showWalletDialog) Dialog({ showWalletDialog = false }) { Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("💼 เลือกกระเป๋า", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        wallets.forEach { w -> val wCurrency = CURRENCIES.find { it.code == w.currency } ?: CURRENCIES[0]
            Card(Modifier.fillMaxWidth().clickable { onWalletChange(w.id); showWalletDialog = false }, shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (w.id == walletId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(12.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                    Text(w.emoji, fontSize = 24.sp); Column(Modifier.weight(1f)) { Text(w.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${wCurrency.symbol} ${wCurrency.label}" + if (w.exchangeRate != 1.0) " (1 ${wCurrency.code} = ฿${w.exchangeRate})" else "", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        if (w.budget > 0) Text("งบ: ${formatMoney(w.budget, wCurrency)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline) }
                    if (w.id != "default") IconButton({ vm.deleteWallet(w.id) }, Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
        OutlinedButton({ showNewWallet = true; showWalletDialog = false }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("สร้างกระเป๋าใหม่") }
        TextButton({ showWalletDialog = false }, Modifier.align(Alignment.End)) { Text("ปิด") }
    } } }
    // New wallet dialog
    if (showNewWallet) AlertDialog({ showNewWallet = false }, { TextButton({
        val id = "w_${System.currentTimeMillis()}"; vm.saveWallet(Wallet(id, wName.ifBlank { "กระเป๋าใหม่" }, wEmoji, wCur, wRate.toDoubleOrNull() ?: 1.0, wBudget.toDoubleOrNull() ?: 0.0))
        wName = ""; wEmoji = "✈️"; wCur = "THB"; wRate = "1.0"; wBudget = ""; showNewWallet = false
    }) { Text("สร้าง") } }, dismissButton = { TextButton({ showNewWallet = false }) { Text("ยกเลิก") } }, title = { Text("สร้างกระเป๋าใหม่") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(wName, { wName = it }, label = { Text("ชื่อ เช่น ทริปญี่ปุ่น") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("✈️","🏠","👨‍👩‍👧","💼","🎒","🏖️").forEach { e -> FilterChip(wEmoji == e, { wEmoji = e }, label = { Text(e) }) } }
            Text("สกุลเงิน", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            CURRENCIES.chunked(4).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { c -> FilterChip(wCur == c.code, { wCur = c.code }, label = { Text("${c.symbol} ${c.label}", fontSize = 11.sp) }) } } }
            OutlinedTextField(wRate, { wRate = it }, label = { Text("อัตราแลกเปลี่ยน (1 หน่วย = ? บาท)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(wBudget, { wBudget = it }, label = { Text("งบประมาณทริป (ไม่บังคับ)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        } })
    if (receiptPreview != null) Dialog({ receiptPreview = null }) { val bytes = Base64.decode(receiptPreview!!, Base64.DEFAULT); val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        Card(shape = RoundedCornerShape(16.dp)) { Box { Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.FillWidth); IconButton({ receiptPreview = null }, Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.Close, null, tint = Color.White) } } } }

    Scaffold(topBar = { TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showWalletDialog = true }) {
        Text("${activeWallet.emoji} ${activeWallet.name}", fontWeight = FontWeight.Bold); Spacer(Modifier.width(4.dp)); Icon(Icons.Default.ArrowDropDown, null, Modifier.size(20.dp)) } },
        actions = {
            // Voice input button
            IconButton({ voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH"); putExtra(RecognizerIntent.EXTRA_PROMPT, "พูดรายการ เช่น \"กินข้าว 50 บาท\"") }) }) { Icon(Icons.Default.Mic, null, tint = Color(0xFF5C6BC0)) }
            IconButton({ showFilter = !showFilter }) { Icon(Icons.Default.FilterList, null, tint = if (filterCat != null) MaterialTheme.colorScheme.primary else LocalContentColor.current) }
            IconButton({ exportCSV(ctx, monthTx, currency) }) { Icon(Icons.Default.Download, null) }
            IconButton(onToggle) { Icon(if (isDark) Icons.Default.WbSunny else Icons.Default.DarkMode, null) }
            IconButton({ showSettings = true }) { Icon(Icons.Default.Settings, null) }
            IconButton({ showLogout = true }) { Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error) }
        }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { val email = Firebase.auth.currentUser?.email ?: ""; if (email.isNotBlank()) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccountCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline); Spacer(Modifier.width(4.dp)); Text(email, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline) } }
            item { MonthNav(year, month, { if (month == 0) { month = 11; year-- } else month-- }, { if (month == 11) { month = 0; year++ } else month++ }) }
            // Balance card + wallet budget
            item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) { Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("ยอดคงเหลือ ${MONTH_TH[month]} ${year + 543}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.65f)); Text("${monthTx.size} รายการ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.65f)) }
                Text(formatMoney(income - expense, currency), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = if (income >= expense) Color(0xFF2E7D32) else Color(0xFFC62828))
                if (activeWallet.exchangeRate != 1.0) Text("≈ ฿${NumberFormat.getNumberInstance().apply { maximumFractionDigits = 0 }.format((income - expense) * activeWallet.exchangeRate)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.5f))
                Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) { AmountLabel("รายรับ","+${formatMoney(income,currency)}",Color(0xFF2E7D32)); AmountLabel("รายจ่าย","-${formatMoney(expense,currency)}",Color(0xFFC62828)) }
                if (activeWallet.budget > 0) { Spacer(Modifier.height(8.dp)); val totalSpent = tx.filter { it.type == "expense" }.sumOf { it.amount }; val pct = (totalSpent / activeWallet.budget).coerceAtMost(1.0).toFloat()
                    Text("งบทริป: ${formatMoney(totalSpent, currency)} / ${formatMoney(activeWallet.budget, currency)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.6f))
                    LinearProgressIndicator({ pct }, Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(99.dp)), color = if (pct >= 1f) MaterialTheme.colorScheme.error else Color(0xFF5C6BC0)) }
            } } }
            if (savings.target > 0) item { val pct = (savings.current / savings.target).coerceAtMost(1.0).toFloat()
                Card(Modifier.fillMaxWidth().clickable { showSavings = true }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) { Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("🎯 ${savings.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("${(pct * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(6.dp)); LinearProgressIndicator({ pct }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)), color = Color(0xFF4CAF50))
                    Spacer(Modifier.height(4.dp)); Text("${formatMoney(savings.current, currency)} / ${formatMoney(savings.target, currency)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(.7f))
                } } } else item { OutlinedButton({ showSavings = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("🎯 ตั้งเป้าหมายออม") } }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(search, { search = it }, placeholder = { Text("🔍 ค้นหา...") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), singleLine = true, trailingIcon = if (search.isNotBlank()) {{ IconButton({ search = "" }) { Icon(Icons.Default.Clear, null) } }} else null)
                var sortExp by remember { mutableStateOf(false) }; Box { IconButton({ sortExp = true }) { Icon(Icons.Default.Sort, null) }; DropdownMenu(sortExp, { sortExp = false }) { SORT_OPTIONS.forEach { (k, l) -> DropdownMenuItem({ Row { if (sortBy == k) Icon(Icons.Default.Check, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(l, fontSize = 13.sp) } }, { sortBy = k; sortExp = false }) } } }
            } }
            if (showFilter) item { Column { Text("กรองหมวดหมู่", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { FilterChip(filterCat == null, { filterCat = null }, label = { Text("ทั้งหมด", fontSize = 11.sp) }) }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { CATEGORIES.take(4).forEach { c -> FilterChip(filterCat == c.id, { filterCat = if (filterCat == c.id) null else c.id }, label = { Text("${c.emoji} ${c.label}", fontSize = 11.sp) }) } }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { CATEGORIES.drop(4).forEach { c -> FilterChip(filterCat == c.id, { filterCat = if (filterCat == c.id) null else c.id }, label = { Text("${c.emoji} ${c.label}", fontSize = 11.sp) }) } }
            } }
            if (sorted.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text(if (monthTx.isEmpty()) "ยังไม่มีรายการเดือนนี้" else "ไม่พบรายการที่ค้นหา", color = MaterialTheme.colorScheme.outline) } }
            else items(sorted, key = { it.fid }) { t -> TxItem(t, currency, { nav.navigate("edit/${t.fid}") }, { vm.delete(t.fid) }, { if (t.receipt != null) receiptPreview = t.receipt }) }
        }
    }
}

@Composable fun TxItem(tx: Transaction, cur: CurrencyInfo, onEdit: () -> Unit, onDelete: () -> Unit, onReceipt: () -> Unit) {
    val cat = catById(tx.category); var showMenu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(12.dp), Arrangement.spacedBy(11.dp), Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(cat.color.copy(.22f)), Alignment.Center) { Text(cat.emoji, fontSize = 21.sp) }
            Column(Modifier.weight(1f)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(cat.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); if (tx.isRecurring) { Spacer(Modifier.width(4.dp)); Text("🔄", fontSize = 10.sp) }; if (tx.receipt != null) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Receipt, null, Modifier.size(13.dp).clickable { onReceipt() }, tint = MaterialTheme.colorScheme.primary) } }
                Text(buildString { if (tx.note.isNotBlank()) append("${tx.note} · "); append(tx.date.takeLast(5).replace("-","/")) }, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Column(horizontalAlignment = Alignment.End) { Text("${if (tx.type == "income") "+" else "-"}${formatMoney(tx.amount, cur)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (tx.type == "income") Color(0xFF2E7D32) else Color(0xFFC62828))
                Row { Icon(Icons.Default.Edit, null, Modifier.size(18.dp).clickable { onEdit() }, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.Delete, null, Modifier.size(18.dp).clickable { showMenu = true }, tint = MaterialTheme.colorScheme.error) } }
        } }
    if (showMenu) AlertDialog({ showMenu = false }, { TextButton({ onDelete(); showMenu = false }) { Text("ลบ", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ showMenu = false }) { Text("ยกเลิก") } }, title = { Text("ลบรายการ?") }, text = { Text("${cat.emoji} ${cat.label} — ${formatMoney(tx.amount, cur)}") })
}

// ═══════════════════════════════════════════════════════════════
//  SCREEN 2 — ADD / EDIT (+ voice button)
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(vm: ExpenseViewModel, nav: NavController, editTx: Transaction? = null, currency: CurrencyInfo) {
    val ctx = LocalContext.current; val isEdit = editTx != null
    var amount by remember { mutableStateOf(editTx?.amount?.toString() ?: "") }; var note by remember { mutableStateOf(editTx?.note ?: "") }
    var type by remember { mutableStateOf(editTx?.type ?: "expense") }; var selCat by remember { mutableStateOf(catById(editTx?.category ?: "food")) }
    var expanded by remember { mutableStateOf(false) }; var date by remember { mutableStateOf(editTx?.date ?: SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(Date())) }
    var receiptB64 by remember { mutableStateOf(editTx?.receipt) }; var tempBmp by remember { mutableStateOf<Bitmap?>(null) }
    var slipScanning by remember { mutableStateOf(false) }; var slipResult by remember { mutableStateOf<String?>(null) }; var slipAmount by remember { mutableStateOf<Double?>(null) }
    val camLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp -> if (bmp != null) { val out = ByteArrayOutputStream(); val s = 400f / bmp.width; val sc = Bitmap.createScaledBitmap(bmp, 400, (bmp.height * s).toInt(), true); sc.compress(Bitmap.CompressFormat.JPEG, 70, out); receiptB64 = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT); tempBmp = sc } }
    val slipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) { slipScanning = true
            val out = ByteArrayOutputStream(); val s = 400f / bmp.width; val sc = Bitmap.createScaledBitmap(bmp, 400, (bmp.height * s).toInt(), true); sc.compress(Bitmap.CompressFormat.JPEG, 70, out)
            receiptB64 = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
            val image = InputImage.fromBitmap(bmp, 0)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                .addOnSuccessListener { result ->
                    slipScanning = false; slipResult = result.text
                    // หาจำนวนเงินจาก OCR text — จับตัวเลขที่มี , หรือ . แล้วเอาตัวใหญ่สุด
                    val amounts = Regex("(\\d{1,3}(?:[,.]\\d{3})*(?:\\.\\d{1,2})?)").findAll(result.text)
                        .map { it.value.replace(",","").toDoubleOrNull() ?: 0.0 }.filter { it > 0 }.toList()
                    slipAmount = amounts.maxOrNull()
                }
                .addOnFailureListener { slipScanning = false; Toast.makeText(ctx, "สแกนไม่สำเร็จ: ${it.message}", Toast.LENGTH_SHORT).show() }
        }
    }
    // Slip scan confirm dialog
    if (slipAmount != null) AlertDialog(
        onDismissRequest = { slipAmount = null; slipResult = null },
        title = { Text("🧾 ผลสแกนสลิป") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("จำนวนเงินที่พบ:", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            Text("${currency.symbol}${NumberFormat.getNumberInstance().format(slipAmount!!)}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))
            if (slipResult != null) { Divider(); Text("ข้อความจากสลิป:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Text(slipResult!!.take(200), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, maxLines = 6, overflow = TextOverflow.Ellipsis) }
        } },
        confirmButton = { TextButton({ amount = slipAmount!!.toLong().toString(); slipAmount = null; slipResult = null }) { Text("ใช้จำนวนนี้ ✓") } },
        dismissButton = { TextButton({ slipAmount = null; slipResult = null }) { Text("ยกเลิก") } }
    )
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) { val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return@rememberLauncherForActivityResult
            val parsed = parseVoiceInput(spoken); if (parsed != null) { selCat = catById(parsed.first); amount = parsed.second.toLong().toString(); note = parsed.third; type = if (parsed.first == "salary") "income" else "expense" }
            else Toast.makeText(ctx, "ไม่เข้าใจ: \"$spoken\"", Toast.LENGTH_LONG).show() } }
    Scaffold(topBar = { TopAppBar(title = { Text(if (isEdit) "แก้ไขรายการ" else "บันทึกรายการ", fontWeight = FontWeight.Bold) },
        navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
        actions = { IconButton({ voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH"); putExtra(RecognizerIntent.EXTRA_PROMPT, "พูดรายการ เช่น \"กินข้าว 50 บาท\"") }) }) { Icon(Icons.Default.Mic, null, tint = Color(0xFF5C6BC0)) } }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf("expense" to "💸 รายจ่าย","income" to "💵 รายรับ").forEach { (t, l) -> val a = type == t; Button({ type = t }, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = when { a && t == "expense" -> Color(0xFFC62828); a -> Color(0xFF2E7D32); else -> MaterialTheme.colorScheme.surfaceVariant }, contentColor = if (a) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)) { Text(l, fontWeight = FontWeight.Bold) } } } }
            item { OutlinedTextField(amount, { amount = it }, label = { Text("จำนวนเงิน (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)) }
            // S Pen handwriting input
            item { var showSPen by remember { mutableStateOf(false) }
                if (showSPen) SPenAmountInput(onRecognized = { amount = it; showSPen = false }, Modifier.fillMaxWidth())
                OutlinedButton({ showSPen = !showSPen }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(if (showSPen) "ซ่อน S Pen" else "✍️ เขียนด้วย S Pen / นิ้ว", fontSize = 13.sp) }
            }
            item { ExposedDropdownMenuBox(expanded, { expanded = !expanded }) { OutlinedTextField("${selCat.emoji} ${selCat.label}", {}, readOnly = true, label = { Text("หมวดหมู่") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded, { expanded = false }) { CATEGORIES.forEach { c -> DropdownMenuItem({ Text("${c.emoji} ${c.label}") }, { selCat = c; expanded = false }) } } } }
            item { OutlinedTextField(note, { note = it }, label = { Text("หมายเหตุ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)) }
            item { val c = Calendar.getInstance(); try { SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).parse(date)?.let { c.time = it } } catch (_: Exception) {}
                OutlinedTextField(date, {}, readOnly = true, label = { Text("วันที่") }, modifier = Modifier.fillMaxWidth().clickable { DatePickerDialog(ctx, { _, y, m, d -> date = "%04d-%02d-%02d".format(y, m + 1, d) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show() },
                    singleLine = true, shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.CalendarMonth, null) }, enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant)) }
            item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("ใบเสร็จ", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                if (receiptB64 != null) { val bytes = Base64.decode(receiptB64!!, Base64.DEFAULT); val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size); Box { Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop); IconButton({ receiptB64 = null }, Modifier.align(Alignment.TopEnd).background(Color.Black.copy(.5f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color.White) } } }
                OutlinedButton({ camLauncher.launch(null) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (receiptB64 != null) "ถ่ายใหม่" else "ถ่ายรูปใบเสร็จ") }
                OutlinedButton({ slipLauncher.launch(null) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5C6BC0))) {
                    if (slipScanning) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("กำลังสแกน...") }
                    else { Icon(Icons.Default.DocumentScanner, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("📱 สแกนสลิป (OCR)") }
                } } }
            item { Button({ val amt = amount.toDoubleOrNull(); if (amt == null || amt <= 0) { Toast.makeText(ctx,"กรุณากรอกจำนวนเงิน",Toast.LENGTH_SHORT).show(); return@Button }
                if (isEdit) vm.update(editTx!!.fid, type, selCat.id, amt, note, date, receiptB64) else vm.save(type, selCat.id, amt, note, receiptB64)
                Toast.makeText(ctx, if (isEdit) "แก้ไขสำเร็จ ✓" else "บันทึกสำเร็จ! 🎉", Toast.LENGTH_SHORT).show(); nav.navigate("home") { launchSingleTop = true }
            }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))) { Text(if (isEdit) "บันทึกการแก้ไข ✓" else "บันทึก ✓", fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SCREEN 3 — SUMMARY + PIE + BAR
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class) @Composable
fun SummaryScreen(vm: ExpenseViewModel, cur: CurrencyInfo) {
    val tx by vm.transactions.collectAsState(); val budgets by vm.budgets.collectAsState(); val cal = Calendar.getInstance()
    var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }; var month by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    val mTx = remember(tx, year, month) { vm.getMonthTx(year, month) }; val totalExp = mTx.filter { it.type == "expense" }.sumOf { it.amount }
    val chartData = remember(tx, year, month) { (5 downTo 0).map { i -> var m = month - i; var y = year; if (m < 0) { m += 12; y-- }; Triple(MONTH_TH[m], vm.monthIncome(y, m).toFloat(), vm.monthExpense(y, m).toFloat()) } }
    val catSum = CATEGORIES.map { c -> Triple(c, mTx.filter { it.type == "expense" && it.category == c.id }.sumOf { it.amount }, budgets[c.id] ?: 0.0) }.filter { it.second > 0 }.sortedByDescending { it.second }
    Scaffold(topBar = { TopAppBar(title = { Text("📊 สรุปยอด", fontWeight = FontWeight.Bold) }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { MonthNav(year, month, { if (month == 0) { month = 11; year-- } else month-- }, { if (month == 11) { month = 0; year++ } else month++ }) }
            item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(12.dp)) { Text("รายรับ-รายจ่าย 6 เดือน", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 8.dp))
                AndroidView({ ctx -> BarChart(ctx).apply { description.isEnabled = false; legend.isEnabled = true; setDrawGridBackground(false); setTouchEnabled(false); xAxis.apply { position = XAxis.XAxisPosition.BOTTOM; setDrawGridLines(false); granularity = 1f; textSize = 11f }; axisLeft.apply { setDrawGridLines(true); textSize = 10f }; axisRight.isEnabled = false } },
                    Modifier.fillMaxWidth().height(200.dp),
                    { chart -> val labels = chartData.map { it.first }; val dsI = BarDataSet(chartData.mapIndexed { i, d -> BarEntry(i * 2f, d.second) },"รายรับ").apply { color = Color(0xFF4CAF50).toArgb(); valueTextSize = 0f }; val dsE = BarDataSet(chartData.mapIndexed { i, d -> BarEntry(i * 2f + 1f, d.third) },"รายจ่าย").apply { color = Color(0xFFF44336).toArgb(); valueTextSize = 0f }
                        chart.data = BarData(dsI, dsE).apply { barWidth = 0.4f }; chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels.flatMap { listOf(it,"") }); chart.invalidate() })
            } } }
            if (catSum.isNotEmpty()) item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(12.dp)) { Text("สัดส่วนรายจ่าย", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 8.dp))
                AndroidView({ ctx -> PieChart(ctx).apply { description.isEnabled = false; isDrawHoleEnabled = true; holeRadius = 45f; transparentCircleRadius = 50f; setUsePercentValues(true); legend.isEnabled = true; setEntryLabelTextSize(10f) } },
                    Modifier.fillMaxWidth().height(220.dp),
                    { chart -> val entries = catSum.map { (c, s, _) -> PieEntry(s.toFloat(), c.emoji) }; val ds = PieDataSet(entries,"").apply { colors = catSum.map { it.first.color.toArgb() }; valueTextSize = 11f; valueFormatter = PercentFormatter(chart); sliceSpace = 2f }; chart.data = PieData(ds); chart.invalidate() })
            } } }
            item { Text("หมวดหมู่รายจ่าย", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline) }
            if (catSum.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) { Text("ยังไม่มีรายจ่าย", color = MaterialTheme.colorScheme.outline) } }
            else items(catSum) { (cat, spent, bdg) -> val isOver = bdg > 0 && spent > bdg; val pct = if (bdg > 0) (spent / bdg).coerceAtMost(1.0).toFloat() else 0f
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (isOver) MaterialTheme.colorScheme.errorContainer.copy(.3f) else MaterialTheme.colorScheme.surfaceVariant)) {
                    Column( Modifier.padding(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(cat.color.copy(.2f)), Alignment.Center) { Text(cat.emoji, fontSize = 18.sp) }
                        Column(Modifier.weight(1f)) { Text(cat.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); if (bdg > 0) Text("${formatMoney(spent,cur)} / ${formatMoney(bdg,cur)}", fontSize = 11.sp, color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline) } }
                        if (bdg > 0) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator({ pct }, Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)), color = if (isOver) MaterialTheme.colorScheme.error else cat.color, trackColor = MaterialTheme.colorScheme.outline.copy(.12f)) } } } }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SCREEN 4 — BUDGET
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class) @Composable
fun BudgetScreen(vm: ExpenseViewModel, cur: CurrencyInfo) {
    val budgets by vm.budgets.collectAsState(); val tx by vm.transactions.collectAsState(); val cal = Calendar.getInstance()
    var month by remember { mutableStateOf(cal.get(Calendar.MONTH)) }; var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
    val mTx = remember(tx, year, month) { vm.getMonthTx(year, month) }; var editCat by remember { mutableStateOf<Category?>(null) }; var bdgIn by remember { mutableStateOf("") }
    if (editCat != null) AlertDialog({ editCat = null }, { TextButton({ vm.saveBudget(editCat!!.id, bdgIn.toDoubleOrNull() ?: 0.0); editCat = null }) { Text("บันทึก") } },
        dismissButton = { TextButton({ editCat = null }) { Text("ยกเลิก") } }, title = { Text("ตั้งงบ ${editCat!!.emoji} ${editCat!!.label}") }, text = { OutlinedTextField(bdgIn, { bdgIn = it }, label = { Text("งบประมาณ (${cur.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()) })
    Scaffold(topBar = { TopAppBar(title = { Text("💰 งบประมาณ", fontWeight = FontWeight.Bold) }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MonthNav(year, month, { if (month == 0) { month = 11; year-- } else month-- }, { if (month == 11) { month = 0; year++ } else month++ }) }
            item { Text("${MONTH_TH[month]} ${year + 543} — แตะเพื่อตั้งงบ", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline) }
            items(CATEGORIES.filter { it.id != "salary" }) { cat -> val bdg = budgets[cat.id] ?: 0.0; val spent = mTx.filter { it.type == "expense" && it.category == cat.id }.sumOf { it.amount }; val pct = if (bdg > 0) (spent / bdg).coerceAtMost(1.0).toFloat() else 0f; val isOver = bdg > 0 && spent > bdg
                Card(Modifier.fillMaxWidth().clickable { editCat = cat; bdgIn = if (bdg > 0) bdg.toLong().toString() else "" }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (isOver) MaterialTheme.colorScheme.errorContainer.copy(.25f) else MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(cat.color.copy(.2f)), Alignment.Center) { Text(cat.emoji, fontSize = 18.sp) }
                        Column(Modifier.weight(1f)) { Text(cat.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(if (bdg > 0) "${formatMoney(spent,cur)} / ${formatMoney(bdg,cur)}" else "ยังไม่ได้ตั้งงบ", fontSize = 11.sp, color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline) }
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp)) }
                        if (bdg > 0) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator({ pct }, Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)), color = if (isOver) MaterialTheme.colorScheme.error else cat.color, trackColor = MaterialTheme.colorScheme.outline.copy(.12f)) } } } }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SCREEN 5 — RECURRING (confirm delete)
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class) @Composable
fun RecurringScreen(vm: ExpenseViewModel, ctx: Context, cur: CurrencyInfo) {
    val recurList by vm.recurring.collectAsState(); var showForm by remember { mutableStateOf(false) }; var editItem by remember { mutableStateOf<RecurringItem?>(null) }
    var rAmt by remember { mutableStateOf("") }; var rName by remember { mutableStateOf("") }; var rType by remember { mutableStateOf("expense") }; var rCat by remember { mutableStateOf(CATEGORIES[0]) }; var rFreq by remember { mutableStateOf("monthly") }; var rDay by remember { mutableStateOf("1") }; var rExp by remember { mutableStateOf(false) }
    var delConfirm by remember { mutableStateOf<RecurringItem?>(null) }
    fun reset() { rAmt = ""; rName = ""; rType = "expense"; rCat = CATEGORIES[0]; rFreq = "monthly"; rDay = "1"; rExp = false; editItem = null }
    if (delConfirm != null) { val item = delConfirm!!; AlertDialog({ delConfirm = null }, { TextButton({ vm.deleteRecurring(item.id); delConfirm = null }) { Text("ลบ", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ delConfirm = null }) { Text("ยกเลิก") } }, title = { Text("ลบรายการประจำ?") }, text = { Text("${catById(item.category).emoji} ${item.name} — ${formatMoney(item.amount, cur)}") }) }
    if (showForm) AlertDialog({ showForm = false; reset() }, { TextButton({ val amt = rAmt.toDoubleOrNull() ?: return@TextButton; vm.saveRecurring(RecurringItem(editItem?.id ?: "rc_${System.currentTimeMillis()}", rType, rCat.id, amt, rName, rFreq, rDay.toIntOrNull() ?: 1)); showForm = false; reset() }) { Text("บันทึก") } },
        dismissButton = { TextButton({ showForm = false; reset() }) { Text("ยกเลิก") } }, title = { Text(if (editItem != null) "แก้ไขรายการประจำ" else "เพิ่มรายการประจำ") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("expense" to "รายจ่าย","income" to "รายรับ").forEach { (t, l) -> FilterChip(rType == t, { rType = t }, label = { Text(l, fontSize = 12.sp) }) } }
            OutlinedTextField(rAmt, { rAmt = it }, label = { Text("จำนวนเงิน") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(rName, { rName = it }, label = { Text("ชื่อ เช่น ค่าเช่า") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(rExp, { rExp = !rExp }) { OutlinedTextField("${rCat.emoji} ${rCat.label}", {}, readOnly = true, label = { Text("หมวดหมู่") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rExp) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(rExp, { rExp = false }) { CATEGORIES.forEach { c -> DropdownMenuItem({ Text("${c.emoji} ${c.label}") }, { rCat = c; rExp = false }) } } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("monthly" to "รายเดือน","weekly" to "รายสัปดาห์").forEach { (f, l) -> FilterChip(rFreq == f, { rFreq = f }, label = { Text(l, fontSize = 12.sp) }) } }
            OutlinedTextField(rDay, { rDay = it }, label = { Text(if (rFreq == "monthly") "วันที่ (1–31)" else "วันในสัปดาห์ (1=จ.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        } })
    Scaffold(topBar = { TopAppBar(title = { Text("🔄 รายการประจำ", fontWeight = FontWeight.Bold) }, actions = { IconButton({ showForm = true }) { Icon(Icons.Default.Add, null) } }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("รายการที่เกิดซ้ำ กด 'บันทึกวันนี้' เพื่อเพิ่มเข้าประวัติ", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline) }
            if (recurList.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("ยังไม่มีรายการประจำ", color = MaterialTheme.colorScheme.outline) } }
            else items(recurList, key = { it.id }) { item -> val cat = catById(item.category)
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(13.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) { Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(cat.color.copy(.2f)), Alignment.Center) { Text(cat.emoji, fontSize = 19.sp) }
                        Column(Modifier.weight(1f)) { Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text("${cat.label} · ${FREQ_LABEL[item.freq]} · วันที่ ${item.day}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline) }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("${if (item.type == "income") "+" else "-"}${formatMoney(item.amount, cur)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (item.type == "income") Color(0xFF2E7D32) else Color(0xFFC62828))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { OutlinedButton({ vm.applyRecurring(item); Toast.makeText(ctx,"บันทึกแล้ว ✓",Toast.LENGTH_SHORT).show() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), modifier = Modifier.height(28.dp)) { Text("บันทึกวันนี้", fontSize = 10.sp) }
                                IconButton({ delConfirm = item }, Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) } } } } } }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SCREEN 6 — CALENDAR VIEW (ปฏิทินรายวัน)
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class) @Composable
fun CalendarScreen(vm: ExpenseViewModel, nav: NavController, cur: CurrencyInfo) {
    val tx by vm.transactions.collectAsState(); val cal = Calendar.getInstance()
    var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }; var month by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    val monthTx = remember(tx, year, month) { vm.getMonthTx(year, month) }
    // สร้าง map วันที่ → รายการ
    val dayMap = remember(monthTx) { monthTx.groupBy { it.date.takeLast(2).toIntOrNull() ?: 0 } }
    // หาจำนวนวันในเดือน + วันแรกของเดือนเริ่มวันอะไร
    val calMonth = remember(year, month) { Calendar.getInstance().apply { set(year, month, 1) } }
    val daysInMonth = calMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (calMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7 // จันทร์ = 0
    val dayTx = if (selectedDay != null) dayMap[selectedDay] ?: emptyList() else emptyList()

    Scaffold(topBar = { TopAppBar(title = { Text("📅 ปฏิทิน", fontWeight = FontWeight.Bold) }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            MonthNav(year, month, { if (month == 0) { month = 11; year-- } else month--; selectedDay = null }, { if (month == 11) { month = 0; year++ } else month++; selectedDay = null })
            Spacer(Modifier.height(8.dp))
            // Day of week headers
            Row(Modifier.fillMaxWidth()) { listOf("จ","อ","พ","พฤ","ศ","ส","อา").forEach { d ->
                Box(Modifier.weight(1f), Alignment.Center) { Text(d, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline) }
            } }
            Spacer(Modifier.height(4.dp))
            // Calendar grid
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth().height(52.dp)) {
                    for (col in 0..6) {
                        val idx = row * 7 + col
                        val day = idx - firstDayOfWeek + 1
                        if (day in 1..daysInMonth) {
                            val hasTx = dayMap.containsKey(day)
                            val isSelected = selectedDay == day
                            val dayIncome = dayMap[day]?.filter { it.type == "income" }?.sumOf { it.amount } ?: 0.0
                            val dayExpense = dayMap[day]?.filter { it.type == "expense" }?.sumOf { it.amount } ?: 0.0
                            val isToday = day == cal.get(Calendar.DAY_OF_MONTH) && month == cal.get(Calendar.MONTH) && year == cal.get(Calendar.YEAR)
                            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                                .background(when { isSelected -> MaterialTheme.colorScheme.primaryContainer; isToday -> MaterialTheme.colorScheme.tertiaryContainer.copy(.5f); else -> Color.Transparent })
                                .clickable { selectedDay = if (selectedDay == day) null else day }, Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$day", fontSize = 14.sp, fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    if (hasTx) {
                                        if (dayExpense > 0) Text("-${cur.symbol}${NumberFormat.getNumberInstance().apply { maximumFractionDigits = 0 }.format(dayExpense)}", fontSize = 8.sp, color = Color(0xFFC62828), maxLines = 1)
                                        else if (dayIncome > 0) Text("+${cur.symbol}${NumberFormat.getNumberInstance().apply { maximumFractionDigits = 0 }.format(dayIncome)}", fontSize = 8.sp, color = Color(0xFF2E7D32), maxLines = 1)
                                    }
                                }
                            }
                        } else Box(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // Selected day transactions
            if (selectedDay != null) {
                Text("${selectedDay} ${MONTH_TH[month]} ${year + 543}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                if (dayTx.isEmpty()) Text("ไม่มีรายการ", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(dayTx, key = { it.fid }) { t -> TxItem(t, cur, { nav.navigate("edit/${t.fid}") }, { vm.delete(t.fid) }, {}) }
                }
            } else {
                // สรุปเดือน
                val totalIncome = monthTx.filter { it.type == "income" }.sumOf { it.amount }
                val totalExpense = monthTx.filter { it.type == "expense" }.sumOf { it.amount }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("สรุปเดือน ${MONTH_TH[month]} ${year + 543}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column { Text("รายรับ", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline); Text("+${formatMoney(totalIncome, cur)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) }
                            Column { Text("รายจ่าย", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline); Text("-${formatMoney(totalExpense, cur)}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold) }
                            Column { Text("คงเหลือ", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline); Text(formatMoney(totalIncome - totalExpense, cur), fontWeight = FontWeight.Bold, color = if (totalIncome >= totalExpense) Color(0xFF2E7D32) else Color(0xFFC62828)) }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  CSV EXPORT + SHARED COMPOSABLES
// ═══════════════════════════════════════════════════════════════
fun exportCSV(ctx: Context, tx: List<Transaction>, cur: CurrencyInfo) {
    val sb = StringBuilder(); sb.appendLine("วันที่,ประเภท,หมวดหมู่,หมายเหตุ,จำนวนเงิน (${cur.symbol}),ประจำ")
    tx.forEach { t -> sb.appendLine("${t.date},${if (t.type == "income") "รายรับ" else "รายจ่าย"},${catById(t.category).label.replace(",","")},${t.note.replace(",", " ")},${t.amount},${if (t.isRecurring) "ใช่" else "ไม่"}") }
    try { val file = File(ctx.getExternalFilesDir(null), "expense_${System.currentTimeMillis()}.csv"); file.writeText("\uFEFF$sb")
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Export CSV"))
    } catch (e: Exception) { Toast.makeText(ctx,"ไม่สามารถ export ได้: ${e.message}",Toast.LENGTH_SHORT).show() }
}
@Composable fun MonthNav(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { IconButton(onPrev) { Text("‹", fontSize = 24.sp) }; Text("${MONTH_TH[month]} ${year + 543}", fontSize = 16.sp, fontWeight = FontWeight.Bold); IconButton(onNext) { Text("›", fontSize = 24.sp) } } }
@Composable fun AmountLabel(label: String, value: String, color: Color) { Column { Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.6f)); Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) } }


// ═══════════════════════════════════════════════════════════════
//  ONBOARDING SCREEN
// ═══════════════════════════════════════════════════════════════
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val pages = listOf(
        Triple("💰", "ยินดีต้อนรับ!", "บันทึกรายรับ-รายจ่ายง่ายๆ\nแค่กดหรือพูดก็บันทึกได้"),
        Triple("📊", "ควบคุมการเงิน", "ดูสรุปแบบกราฟ ตั้งงบประมาณ\nแจ้งเตือนเมื่อใกล้เกินงบ"),
        Triple("👛", "หลายกระเป๋า", "แยกบัญชีตามทริป ครอบครัว\nรองรับหลายสกุลเงิน + อัตราแลกเปลี่ยน"),
        Triple("🧾", "สแกนสลิป", "ถ่ายรูปสลิป OCR อ่านจำนวนเงินให้\nพร้อมปฏิทินดูรายการรายวัน"),
    )
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxWidth().padding(40.dp).align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(pages[page].first, fontSize = 72.sp)
            Text(pages[page].second, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(pages[page].third, fontSize = 15.sp, color = MaterialTheme.colorScheme.outline, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))
            // Dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { pages.indices.forEach { i ->
                Box(Modifier.size(if (i == page) 10.dp else 8.dp).clip(CircleShape).background(if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(.3f)))
            } }
            Spacer(Modifier.height(24.dp))
            if (page < pages.lastIndex) {
                Button({ page++ }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))) { Text("ถัดไป", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                TextButton({ onDone() }) { Text("ข้าม", color = MaterialTheme.colorScheme.outline) }
            } else {
                Button({ onDone() }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))) { Text("เริ่มใช้งาน 🚀", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
