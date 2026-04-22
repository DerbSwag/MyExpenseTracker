// Top-level build file
plugins {
    id("com.android.application") version "9.2.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false

    // 🚀 เพิ่มบรรทัดนี้เข้าไปครับ (ต้องใช้เวอร์ชันเดียวกับ Kotlin คือ 2.0.0)
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}