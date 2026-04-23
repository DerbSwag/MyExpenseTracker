# 💰 MyExpenseTracker

แอปบันทึกรายรับ-รายจ่ายบน Android สร้างด้วย Kotlin + Jetpack Compose + Firebase Firestore

## ✨ ฟีเจอร์หลัก

- **ระบบสมาชิก** — Login/Register ด้วย Email + Password ผ่าน Firebase Auth ข้อมูลแยกตาม user
- **บันทึกรายรับ-รายจ่าย** — แยกหมวดหมู่ 8 ประเภท พร้อม emoji
- **สรุปยอดรายเดือน** — กราฟแท่งเปรียบเทียบ 6 เดือนด้วย MPAndroidChart
- **ตั้งงบประมาณ** — กำหนดงบแต่ละหมวด พร้อม progress bar แสดงสถานะ
- **แจ้งเตือนงบ** — Push notification เมื่อใช้ถึง 80% และเกินงบ 100%
- **รายการประจำ** — จัดการรายจ่าย/รายรับที่เกิดซ้ำ (รายเดือน/รายสัปดาห์)
- **ถ่ายรูปใบเสร็จ** — แนบรูปใบเสร็จกับแต่ละรายการ
- **Export CSV** — ส่งออกข้อมูลเป็นไฟล์ CSV แชร์ผ่าน Intent
- **Dark Mode** — สลับธีมมืด/สว่าง บันทึกค่าด้วย DataStore
- **ค้นหา & กรอง** — ค้นหาตามชื่อ/หมวดหมู่/จำนวนเงิน + กรองตามหมวด
- **Firebase Cloud Messaging** — รองรับ push notification จาก server

## 🛠️ Tech Stack

| เทคโนโลยี | รายละเอียด |
|---|---|
| Kotlin | ภาษาหลัก |
| Jetpack Compose | UI framework (Material 3) |
| Firebase Auth | ระบบ Login/Register |
| Firebase Firestore | ฐานข้อมูล realtime (แยกตาม user) |
| Firebase Cloud Messaging | Push notifications |
| MPAndroidChart | กราฟแท่งสรุปยอด |
| DataStore Preferences | เก็บค่า Dark Mode |
| Navigation Compose | จัดการหน้าจอ |
| FileProvider | Export CSV |

## 📱 หน้าจอ

1. **รายการ** — แสดงรายรับ-รายจ่ายรายเดือน พร้อมยอดคงเหลือ
2. **บันทึก** — เพิ่ม/แก้ไขรายการ เลือกวันที่จาก DatePicker
3. **สรุป** — กราฟ 6 เดือน + สรุปตามหมวดหมู่พร้อมเทียบงบ
4. **งบประมาณ** — ตั้งงบแต่ละหมวด ดูย้อนหลังได้
5. **รายการประจำ** — จัดการรายจ่ายซ้ำ กดบันทึกวันนี้ได้เลย

## ⚙️ การติดตั้ง

1. Clone โปรเจกต์
   ```bash
   git clone https://github.com/DerbSwag/MyExpenseTracker.git
   ```

2. เปิดด้วย Android Studio

3. เพิ่มไฟล์ `google-services.json` จาก Firebase Console ไว้ใน `app/`

4. Build & Run

## 📋 Requirements

- Android Studio Ladybug หรือใหม่กว่า
- Min SDK 24 (Android 7.0)
- Target SDK 35
- Firebase project ที่เปิดใช้ Firestore + Cloud Messaging

## 📄 License

MIT
