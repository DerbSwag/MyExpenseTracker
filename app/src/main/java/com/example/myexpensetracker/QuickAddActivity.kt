package com.example.myexpensetracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexpensetracker.ui.theme.MyExpenseTrackerTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * Quick Add — เปิดจาก shortcut / deep link
 * ใส่จำนวนเงิน + เลือกหมวด → บันทึกทันที → ปิด
 */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MyExpenseTrackerTheme { QuickAddScreen { finish() } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val uid = Firebase.auth.currentUser?.uid
    var amount by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(CATEGORIES[0]) }
    var type by remember { mutableStateOf("expense") }
    var note by remember { mutableStateOf("") }

    if (uid == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("กรุณาเข้าสู่ระบบก่อน", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("⚡ บันทึกด่วน", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("expense" to "💸 รายจ่าย", "income" to "💵 รายรับ").forEach { (t, l) ->
                val active = type == t
                Button(
                    { type = t }, Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            active && t == "expense" -> Color(0xFFC62828)
                            active -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) { Text(l, fontWeight = FontWeight.Bold, color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        OutlinedTextField(
            amount, { amount = it },
            label = { Text("จำนวนเงิน (฿)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)
        )

        Text("หมวดหมู่", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CATEGORIES.take(4).forEach { cat ->
                FilterChip(
                    selectedCat.id == cat.id, { selectedCat = cat },
                    label = { Text("${cat.emoji}", fontSize = 18.sp) }
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CATEGORIES.drop(4).forEach { cat ->
                FilterChip(
                    selectedCat.id == cat.id, { selectedCat = cat },
                    label = { Text("${cat.emoji}", fontSize = 18.sp) }
                )
            }
        }

        OutlinedTextField(
            note, { note = it }, label = { Text("หมายเหตุ (ไม่บังคับ)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val amt = amount.toDoubleOrNull()
                if (amt == null || amt <= 0) {
                    Toast.makeText(ctx, "กรุณากรอกจำนวนเงิน", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                Firebase.firestore.collection("users").document(uid)
                    .collection("wallets").document("default")
                    .collection("transactions")
                    .add(hashMapOf(
                        "type" to type, "category" to selectedCat.id, "amount" to amt,
                        "note" to note, "date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        "createdAt" to System.currentTimeMillis(), "isRecurring" to false, "receipt" to null
                    ))
                Toast.makeText(ctx, "${selectedCat.emoji} บันทึกแล้ว ✓", Toast.LENGTH_SHORT).show()
                onDone()
            },
            Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))
        ) { Text("บันทึก ✓", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}
