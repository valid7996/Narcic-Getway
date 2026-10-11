# اصلاحات Aether در نرسیس (دور دوم)

## علت اصلی مشکل WireGuard over MASQUE
هسته‌ی داخل پروژه (`libaether.so`) **Aether 2.1.0** است. در این نسخه `gool` همان WARP-in-WARP است و **WireGuard over MASQUE اصلاً وجود ندارد** (از 2.3 اضافه شده). همین‌طور `--masque-sni` در 2.1.0 نیست و PattNG چون هسته‌ی جدید دارد کار می‌کند.
- کد قبلی برای WG-over-MASQUE پرچم‌های WARP-in-WARP می‌فرستاد (`--wiw-scan`...). اصلاح شد: `--peer` برای دروازه‌ی MASQUE و `--gool-peer` برای WireGuard داخلی، و برای WARP-in-WARP پرچم `--gool-classic`.
- خروجی اسکن «gool ready: masque X carries wireguard Y» اصلاً خوانده نمی‌شد، پس IP هیچ‌وقت در فیلد نمی‌نشست. اضافه شد و IP دروازه در فیلد Outer پر می‌شود.
- **MASQUE SNI برگشت** (فیلد و پرچم). برنامه از خود هسته می‌پرسد چه پرچم‌هایی را می‌شناسد (`--help`) و پرچمی که هسته‌ی نصب‌شده نمی‌شناسد را حذف/تغییر نام می‌دهد (`--api-address` ↔ `--enroll-address`، `--gool-classic`).
- **برای کار کردن واقعی این دو قابلیت باید `libaether.so` را با نسخه‌ی PattNG عوض کنید**: APK بیلد جدید PattNG را unzip کنید و `lib/<abi>/libaether.so` را در `app/src/main/jniLibs/<abi>/` بگذارید. تا آن موقع برنامه یک یادداشت در شیت Aether می‌گذارد و WG-over-MASQUE با پیام روشن متوقف می‌شود (به‌جای قطع ناگهانی).

## «وصل می‌شود و بلافاصله قطع می‌شود»
حلقه‌ی آمار سرویس برای Aether شاخه نداشت و وضعیت «در حال اجرا» را از Xray می‌خواند؛ وقتی پروکسی خروجی Xray بالا نبود (یا command دستی خودش upstream داشت) تونل در اولین tick بسته می‌شد. حالا `aether.isRunning` چک می‌شود.

## آپلود/دانلود
همان حلقه برای Aether آمار Xray می‌خواند (صفر). حالا مثل Psiphon از شمارنده‌ی UID برنامه خوانده می‌شود.

## تست پینگ
پروفایل‌های Aether از فهرست پینگ حذف بودند. حالا برای هر پروفایل یک هسته‌ی موقت بالا می‌آید، درخواست از طریق SOCKS آن زمان‌گیری می‌شود و بسته می‌شود (`AetherDelayProbe`). منوی «پینگ» کارت هم برای Aether فعال شد. هنگام اتصال فعال Aether، پینگ بقیه -1 می‌دهد (هسته‌ی دوم با جلسه‌ی فعال تداخل می‌کند). هر تست تا چند دقیقه زمان می‌برد (اسکن).

## هنگ کردن گوشی هنگام دریافت کلید
`startExitProxy` تا ۱۰ ثانیه با `Thread.sleep` روی ترد اصلی منتظر می‌ماند. حالا کل ثبت کلید روی IO اجرا می‌شود.

## اشتراک‌گذاری و دریافت با لینک / QR
- علت: `rawPayload()` برای Aether `null` بود (دکمه‌های Share/QR غیرفعال) و `importZedLink` هم Aether را رد می‌کرد.
- حالا لینک `aether://...` (هم‌قالب PattNG، با چند کلید اضافه برای نرسیس) ساخته و خوانده می‌شود: اشتراک، QR، چسباندن، اسکن QR، deep-link (طرح `aether` در Manifest ثبت شد). لینک `narcicgetway://` برای Aether هم وارد می‌شود.

## فایل‌های جدید
`AetherLink.kt`، `AetherDelayProbe.kt`، `PsiphonServerList.kt`، `tools/fetch-psiphon-servers.sh`، تست‌ها در `AetherCommandsTest.kt`. اینجا Gradle نداشتم؛ کامپایل نشده.
