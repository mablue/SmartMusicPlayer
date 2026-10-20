# اسمارتمیوزیک — پخش‌کننده موسیقی با رتبه‌بندی رفتاری و شافل هوشمند

<p dir="rtl">
  <a href="README.md">نسخه انگلیسی</a>
  <span> | </span>
  <strong>فارسی</strong>
</p>

<p align="center" dir="ltr">
  <a href="https://github.com/mablue/SmartMusicPlayer/actions/workflows/release.yml">
    <img src="https://github.com/mablue/SmartMusicPlayer/actions/workflows/release.yml/badge.svg" alt="Build &amp; Release" />
  </a>
  <img src="https://img.shields.io/badge/Platform-Android%2024%2B-3DDC84?logo=android&amp;logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&amp;logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&amp;logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Playback-Media3%20ExoPlayer-orange" alt="Media3" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" /></a>
</p>

<<<<<<< HEAD

[![Build & Release](https://github.com/mablue/SmartMusicPlayer/actions/workflows/release.yml/badge.svg)](https://github.com/mablue/SmartMusicPlayer/actions/workflows/release.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%2024%2B-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)](#)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](#)
[![Media3](https://img.shields.io/badge/Playback-Media3%20ExoPlayer-orange)](#)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
=======
این پروژه یک پخش‌کننده موسیقی آفلاین برای اندروید است که با Jetpack Compose (Material 3) و Media3 ExoPlayer ساخته شده. به‌جای امتیازدهی دستی، اسمارتمیوزیک امتیاز هر قطعه را از رفتار واقعی شنیداری — تکمیل پخش، اسکیپ، تکرار — می‌سازد و از این امتیازها برای شافل وزنی استفاده می‌کند.
>>>>>>> 9351d4c (docs(fa): rewrite README.fa.md cleanly)

> **وضعیت پروژه:** شخصی و آزمایشی. بیلد از طریق تنها ورک‌فلوی موجود در `.github/workflows/release.yml` قابل بازتولید است. APIها، رابط کاربری و ساختار فایل‌ها ممکن است بین تگ‌ها تغییر کند.

## فهرست

- [امکانات](#امکانات)
- [معماری](#معماری)
- [پشته فناوری](#پشته-فناوری)
- [CI/CD](#cicd)
- [بیلد محلی](#بیلد-محلی)
- [بومی‌سازی](#بومی‌سازی)
- [نقشه راه و محدودیت‌ها](#نقشه-راه-و-محدودیت‌ها)
- [مجوز](#مجوز)

## امکانات

### ۱. رتبه‌بندی رفتاری

- **امتیاز اکتشاف:** هر قطعه جدید با **۵۰** شروع می‌شود.
- **کالیبراسیون اولین پخش:** اسکیپ زودهنگام (کمتر از ۲۰ ثانیه یا کمتر از ۲۰٪) → **۲۰**؛ پخش کامل (حداقل ۷۵٪) → **۸۰**؛ در غیر این صورت **۵۵**.
- **پخش‌های بعدی:** اسکیپ زودهنگام **−۲۰** (با محافظت خستگی ۸ دقیقه‌ای)، پخش کامل **+۲۰**، تکرار پشت‌سرهم **+۲۸**، پخش نسبی **+۵**. امتیاز در بازه ۵ تا ۱۰۰ محدود می‌شود.
- **شافل وزنی:** احتمال انتخاب هر قطعه متناسب با `score^1.3` است.
- **سطوح:** الماس ≥ ۸۰، طلایی ≥ ۶۵، نقره‌ای ≥ ۴۵، کم‌علاقه < ۴۵، نشنیده.
- **بازخورد زنده:** بنر ارزیابی بعد از هر اسکیپ یا شنیدن، به‌همراه دیالوگ تحلیل الگوریتم برای هر قطعه.

### ۲. هسته پخش Media3

- موتور پخش **ExoPlayer** از Media3 نسخه ۱.۶ با `handleAudioFocus = true` و `handleAudioBecomingNoisy = true`.
- کلاس **PlaybackService** از MediaSessionService ارث می‌برد؛ callback سفارشی MediaSession کلیدهای بلوتوث و هدست (Next، Previous، Play/Pause، HeadsetHook) را به SmartAudioPlayer می‌فرستد تا اسکیپ آگاه از صف درست کار کند.
- **پنج حالت پخش:** شافل وزنی هوشمند، برترین‌ها، اکتشاف (نشنیده‌ها اول)، ترتیبی، تکرار یک قطعه.
- **صف پخش (Up Next):** پخش بعدی، افزودن به صف، حذف، پاکسازی. پیش از رتبه‌بندی، صف کاربر بررسی می‌شود.
- **ادامه از موقعیت قبلی هر قطعه:** آخرین قطعه، موقعیت پخش و حالت پخش بین اجراها حفظ می‌شود.
- **تایمر خواب** با محو تدریجی صدا، **حلقه A-B**، **سرعت پخش ۰.۷۵× تا ۲×**.

### ۳. نمایشگر لیریک

- **پنل تمام‌صفحه** با برجسته‌سازی خط فعال، اسکرول خودکار، و جست‌وجو با لمس.
- **منابع به ترتیب:** LRCLIB (جست‌وجوی مستقیم) → LRCLIB (جست‌وجوی آزاد) → lyrics.ovh → Lyrica.
- **پارسر LRC** برای لیریک زمان‌دار؛ لیریک ساده به مدت قطعه نگاشت می‌شود.
- **کلیدهای جست‌وجوی خودکار** و **ترجمه خودکار** در تنظیمات ذخیره می‌شوند.
- **ترجمه** با endpoint عمومی Google Translate؛ فارسی به فارسی ترجمه نمی‌شود.
- **کش:** لیریک و ترجمه برای هر قطعه در Room ذخیره می‌شوند و پیش از هر درخواست شبکه استفاده می‌شوند. خطوط ترجمه‌نشده با نشانگر null ذخیره می‌شوند تا ردیف‌ها جابه‌جا نشوند.

### ۴. کتابخانه، پلی‌لیست‌ها، برگزیده‌ها

- **چهار تب پایین:** کتابخانه، پوشه‌ها، پادکست‌ها، پلی‌لیست‌ها.
- **کتابخانه:** جست‌وجو، چیپ‌های فیلتر سطح، منوی مرتب‌سازی (امتیاز هوشمند، مدت، حجم، عنوان، خواننده)، انتخاب چندگانه (انتخاب همه، پخش، افزودن به پلی‌لیست، تغییر موسیقی/پادکست، اشتراک، حذف).
- **پوشه‌ها:** گروه‌بندی بر اساس پوشه، drill-down، پخش همه.
- **پادکست‌ها:** فیلتر دسته (همه/پادکست/کتاب صوتی)، ترتیب شماره قسمت، چیپ سرعت، پرش سریع −۱۵s و +۳۰s، تبدیل به موسیقی.
- **پلی‌لیست‌ها:** کارت برگزیده‌ها (امتیاز ≥ ۶۵ یا حداقل ۲ پخش کامل) به‌همراه پلی‌لیست‌های کاربر و دکمه ساخت.
- **اسکن MediaStore** با حذف تکراری بر اساس URI و عنوان/خواننده، و تشخیص heuristic زبان و ژانر.
- **ویرایش تگ‌ها**، **اشتراک تکی و چندتایی**، **حذف** با `MediaStore.createDeleteRequest` در اندروید ۱۱ و بالاتر.

### ۵. افکت‌های صوتی

- **اکولایزر ۵ باندی** (یا تعداد باند بومی دستگاه در صورت وجود سخت‌افزار) با **۸ پریست:** تخت، بیس عمیق، وکال، راک، پاپ، جاز، الکترونیک، کلاسیک.
- **Bass Boost** و **3D Virtualizer**.
- افکت‌ها **خاموش** ساخته می‌شوند و فقط از دیالوگ اکولایزر روشن می‌شوند. بنابراین اپ هرگز در شروع پخش تقویت صدا را خودکار فعال نمی‌کند. با تغییر نشست صوتی ExoPlayer، افکت‌ها دوباره متصل می‌شوند.

### ۶. یکپارچگی با سیستم

- **نوتیفیکیشن رسانه Foreground** از طریق Media3.
- **گیرنده پخش خودکار بلوتوث** و مدیریت `AUDIO_BECOMING_NOISY`.
- **Predictive Back** برای پخش‌کننده تمام‌صفحه (پیش‌نمایش ژست اندروید ۱۵).
- **Car Mode HUD** با کنترل‌های بزرگ.
- **پشتیبانی Dynamic Color**، به‌صورت پیش‌فرض خاموش (`dynamicColor = false` در `MyApplicationTheme`).

### ۷. عیب‌یابی

- **FileLogger** در `smartmusic_logs/errors.log` زیر پوشه external files اپ می‌نویسد، با چرخش در ۲ مگابایت و هندلر استثناهای uncaught.
- در تنظیمات، اقدام «اشتراک لاگ‌ها» وجود دارد.

## معماری

```text
app/src/main/java/com/example
├── MainActivity.kt
├── data
│   ├── db            Room v4: songs, playlists, DAOs, MIGRATION_3_4
│   ├── lyrics        LyricsService: LRCLIB, lyrics.ovh, Lyrica, LRC, translation
│   ├── model         Song, RankTier, SongSortOrder, SessionEvaluation
│   ├── preferences   PlaybackPreferences (resume, toggles, language)
│   └── repository    SongRepository, PlaylistRepository
├── engine            SmartRankingEngine, PlaybackMode
├── player
│   ├── SmartAudioPlayer.kt        ExoPlayer wrapper, queue, lyrics state
│   ├── PlaybackService.kt         MediaSessionService + media button callback
│   ├── BluetoothAudioReceiver.kt
│   ├── AudioEffectManager.kt      EQ, BassBoost, Virtualizer (disabled by default)
│   ├── ProceduralAudioGenerator.kt
│   ├── WaveControlManager.kt      Proximity gesture (not wired to UI yet)
│   └── ReplayGain*.kt             Infrastructure present, disabled
├── ui
│   ├── components    LyricsPanel, MiniPlayerBar, NowPlayingSection, dialogs, tabs
│   ├── screens       HomeScreen
│   ├── theme         Dark neon M3 theme + Motion tokens
│   └── viewmodel     MusicPlayerViewModel, PlayerUiState, AppTab
└── util              FileLogger, SongShareHelper
```

پشته فناوری

لایه انتخاب
رابط کاربری Jetpack Compose، Material 3، تم تیره نئونی، توکن‌های Motion
پخش Media3 ExoPlayer نسخه ۱.۶، MediaSessionService
ذخیره‌سازی Room v4 (songs، playlists)، SharedPreferences برای وضعیت پخش
لیریک LRCLIB، lyrics.ovh، Lyrica؛ Google Translate برای ترجمه
بیلد AGP + Gradle wrapper؛ compileSdk 36، minSdk 24، targetSdk 36، Java 11
CI GitHub Actions، JDK 21، Android SDK 36، Keystore Vault پایدار

CI/CD

مخزن یک ورک‌فلوی واحد دارد: .github/workflows/release.yml (نام: Build & Release).

· تریگرها: push تگ مطابق v*، یا اجرای دستی با ورودی نسخه.
· محیط: JDK 21 (Temurin)، Android platform 36، build-tools 36.0.0، Gradle wrapper.
· ساخت تگ: در اجرای دستی، اگر تگ وجود نداشته باشد ساخته و push می‌شود.
· Keystore امضای پایدار (Vault): در اولین اجرا، فایل my-upload-key.jks ساخته و به‌عنوان asset یک Release با تگ _keystore-vault ذخیره می‌شود. اجراهای بعدی همان فایل را دانلود می‌کنند، بنابراین همه نسخه‌ها با یک کلید امضا می‌شوند.
· هشدار: Release با تگ _keystore-vault را حذف نکنید، مگر اینکه بخواهید امضای اپ تغییر کند و آپدیت‌ها بشکنند.
· Secret اختیاری: GOOGLE_SERVICES_JSON_BASE64 برای Firebase. بدون آن، یک google-services.json ساختگی ساخته می‌شود.
· خروجی: APK دیباگ و ریلیز به‌عنوان artifact ورک‌فلو و ضمیمه GitHub Release آپلود می‌شوند.

نکته امنیتی: فایل Keystore در Vault یک asset قابل دانلود است و رمز آن در حال حاضر داخل فایل ورک‌فلو تعریف شده. این برای پروژه شخصی راحت است ولی یعنی کلید محرمانه نیست. قبل از انتشار در Google Play یا توزیع گسترده، Keystore و رمزها را به Secretهای مخزن منتقل کنید و دسترسی به Release Vault را محدود کنید.

بیلد محلی

```bash
git clone https://github.com/mablue/SmartMusicPlayer.git
cd SmartMusicPlayer
chmod +x gradlew

# دیباگ
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk

# ریلیز (نیازمند KEYSTORE_PATH / STORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD
# یا وجود my-upload-key.jks در ریشه مخزن)
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

نکته Termux

در Termux، بسته‌های openjdk-21، gradle، aapt2، android-tools و یک Android SDK نصب کنید. سپس این خط را به ~/.gradle/gradle.properties اضافه کنید:

```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

این کار باعث می‌شود Gradle از aapt2 خود Termux استفاده کند، نه نسخه x86_64 مخصوص Maven.

بومی‌سازی

· رشته‌های پیش‌فرض: app/src/main/res/values/strings.xml (انگلیسی).
· رشته‌های فارسی: app/src/main/res/values-fa/strings.xml (فالبک خودکار وقتی زبان دستگاه fa باشد).
· README فارسی: همین فایل.

نقشه راه و محدودیت‌ها

· پوشش لیریک فارسی و ترکی ضعیف است: داده‌های LRCLIB و Lyrica محدودند و هنوز API عمومی لیریک فارسی وصل نشده است.
· ReplayGain کلاس‌هایش وجود دارد ولی پردازنده صوتی برای عملکرد غیرفعال شده است.
· کنترل موج (WaveControlManager، سنسور مجاورت) وجود دارد ولی در تنظیمات نمایش داده نمی‌شود.
· کامپوننت‌های مرده: AnalyticsTab، RankingsTabContent، DiscoveryTabContent، LyricsViewDialog وجود دارند ولی به ناوبری وصل نیستند.
· نسخه‌دهی: versionCode = 1 و versionName = "1.0" — قبل از انتشار عمومی افزایش دهید.
· پشتیبانی نمی‌کند از: Android Auto، ویجت صفحه اصلی، Wear OS، Chromecast.
· متد fallbackToDestructiveMigration() در کنار MIGRATION_3_4 به‌عنوان آخرین راه‌حل باقی مانده است.

مشارکت

ایشو و پول‌ریکوئست خوش‌آمد است. برای تغییرات بزرگ، اول ایشو باز کنید تا رویکرد بررسی شود.

مجوز

مجوز MIT — فایل LICENSE را ببینید.
