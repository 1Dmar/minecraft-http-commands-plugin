# ملخص التعديلات على إضافة Minecraft HTTP Commands

تم تحديث الإضافة للسماح بتخصيص بيانات اللاعب في الـ endpoint الخاص بـ `/player/{username}`، مما يتيح لك تعديل الإطارات الثلاثة (Level, Balance, Server) أو أي حقول إضافية ترغب بها باستخدام **PlaceholderAPI**.

## 🚀 المميزات الجديدة

1.  **دعم PlaceholderAPI**: يمكنك الآن استخدام أي Placeholder من أي إضافة أخرى في Minecraft.
2.  **تخصيص الإطارات الثلاثة**: يمكنك تعديل الحقول الثلاثة التي تظهر في الـ endpoint عبر ملف `config.yml` لتناسب تصميم بطاقة اللاعب. (بحد أقصى 3 حقول).
3.  **أداء عالٍ**: تتم معالجة البيانات بشكل غير متزامن (Asynchronous) لضمان عدم حدوث أي تأخير (Lag) أو توقف (Timeout) في اللعبة.
4.  **دعم اللاعبين غير المتصلين**: تعمل الميزة حتى لو كان اللاعب غير متصل بالخادم (حسب دعم الـ Placeholder للاعبين الـ Offline).

## 🛠️ كيفية الاستخدام

تم إضافة قسم جديد في ملف `config.yml` باسم `player-endpoint-fields`. إليك مثال على كيفية ضبطه:

```yaml
# الحقول المخصصة في endpoint اللاعب
player-endpoint-fields:
  level:
    display-name: "Level"
    placeholder: "%player_level%"
  balance:
    display-name: "Balance"
    placeholder: "%vault_eco_balance%"
  server:
    display-name: "Server"
    placeholder: "Test Server"
  last_login:
    display-name: "Last Login"
    placeholder: "%player_last_join_date%"
```

## 📡 شكل الاستجابة (JSON)

عند طلب بيانات لاعب، ستجد الحقول المخصصة داخل كائن `customFields`:

```json
{
  "success": true,
  "username": "HEMOO",
  "uuid": "...",
  "isOnline": true,
  ...
  "customFields": {
    "level": "15",
    "balance": "5000.0",
    "server": "Test Server",
    "last_login": "2026-06-19"
  }
}
```

## 📝 الملفات المعدلة

- `src/main/resources/plugin.yml`: إضافة `PlaceholderAPI` كاعتماد اختياري.
- `src/main/resources/config.yml`: إضافة قسم الإعدادات الجديد.
- `build.gradle`: إضافة مكتبة `PlaceholderAPI` لعملية البناء.
- `src/main/java/me/lenaic/httpcommands/PlaceholderHook.java`: (جديد) فئة مساعدة للربط مع PlaceholderAPI.
- `src/main/java/me/lenaic/httpcommands/endpoints/GetPlayerEndpoint.java`: تحديث المنطق لاستخراج البيانات المخصصة.
