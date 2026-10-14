# افزودن SNI spoof (بدون روت) با موتور Rsta

روی **آخرین نسخه‌ی خودتان** ساخته شده؛ فقط فایل‌های لازم داخل بسته است و تغییرات دیگر شما دست نخورده می‌ماند.

## چه اضافه شد
- در منوی افزودن، گزینه‌ی **«افزودن SNI spoof (بدون روت)»** کنار گزینه‌ی قبلی (که روت می‌خواهد).
- موتور واقعی Rsta: `libsnispf.so` (Go) + `libandroidasdd_jni.so` برای arm64-v8a، armeabi-v7a و x86_64، و کلاس `GoNativeBridge` با همان اسم (JNI به اسم کلاس وصل است؛ قانون keep برایش در `proguard-rules.pro` گذاشته شد).
- هنگام اتصال: Xray خروجی proxy را به `127.0.0.1:10811` می‌برد، موتور Rsta به IP واقعی سرور با SNI جعلی و روش انتخابی وصل می‌شود. بدون su.
- روش (combined، fragment، random_split، seg2delay، sni_triplicate، oob، fake_sni، auto_ttl) و پورت اتصال در شیت انتخاب می‌شود.
- لینک‌های ساخته‌شده برای خود برنامه‌ی Rsta (آدرس 127.0.0.1:40443): در این حالت آدرس واقعی سرور فقط از «IP تمیز» معلوم می‌شود؛ شیت بدون آن اجازه‌ی ذخیره نمی‌دهد.

## آپلود/دانلود
ترافیک از Xray خوانده می‌شود (مثل سایر پروفایل‌های Xray)، پس عدد واقعی است.

## پینگ واقعی
آدرس این پروفایل‌ها 127.0.0.1 است و پینگ به آن بی‌معنی است. حالا برای هر پروفایل یک جلسه‌ی موتور Rsta روی پورت آزاد بالا می‌آید، خروجی proxy کانفیگ به آن اشاره داده می‌شود و تأخیر **واقعی** (از میان موتور و سرور) اندازه‌گیری و نشان داده می‌شود. دکمه‌ی پینگ کارت و «تست همه» هر دو این کار را می‌کنند. به روت یا اتصال فعال نیازی نیست.

## فایل‌ها
جدید: `SniSpoofConfig.kt`، `RstaSpoofEngine.kt`، `GoNativeBridge.java`، رشته‌ها (`snispoof_noroot_strings.xml` در shared و app)، کتابخانه‌های native.
تغییر: `SniSpoofProfile/Link/Sheet`، `VpnProfile`، `PingService`، `ServersScreen`، `HomeServerSection`، `ServerCardItem`، `StartPlanner`، `ZedVpnService`، `ZedSecureApp`، `proguard-rules.pro`، `AetherCommandsTest.kt`.

اینجا Gradle نداشتم؛ کامپایل و اجرا نشده.
