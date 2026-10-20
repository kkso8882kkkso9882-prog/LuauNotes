# Luau Notes

แอปโน้ตเก็บสคริป Luau (ตั้งชื่อ/เปลี่ยนชื่อ/ทำสำเนา/ลบ/ค้นหา/คัดลอก/วาง/แชร์) เก็บข้อมูลในเครื่องเท่านั้น

## ดาวน์โหลด APK

- ไปที่ [Actions](https://github.com/kkso8882kkkso9882-prog/LuauNotes/actions) → run ล่าสุดที่สำเร็จ → Artifact **LuauNotes-debug-apk**
- หรือใช้ workflow_dispatch เพื่อ build ใหม่

## Build เอง

```bash
# ต้องมี JDK 17 + Android SDK
gradle assembleDebug
# APK อยู่ที่ app/build/outputs/apk/debug/app-debug.apk
```

หรือเปิดด้วย Android Studio แล้ว Build > Build APK(s)
