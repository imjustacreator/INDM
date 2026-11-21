<div align="center">

# INDM

**A fast, lightweight download manager for Windows.**
Multi-connection acceleration, resume, queues, a browser extension, phone control and an Android companion app, with an English and Persian interface. Standard-library Python only.

[**Download the latest version**](../../releases/latest) · [Report a problem](../../issues)

</div>

<!-- Add a screenshot here: ![INDM](docs/screenshot.png) -->

---

## English

### Highlights

- **Download accelerator.** Up to 32 parallel connections per file with dynamic segmentation, so every connection stays busy until the very end. If a server refuses that many connections, INDM uses fewer by itself.
- **Reliable.** Resume after a pause, crash or restart. If the network drops, downloads wait and continue on their own. Fatal errors (full disk, 404, expired link) fail quickly with a clear message.
- **Mirrors.** Give a file several addresses; if one fails, INDM moves to the next and keeps what is already downloaded.
- **Queues and scheduling.** Several queues, each with its own start time, speed limit and simultaneous-download limit. Global speed limiter, and "exit or shut down when finished".
- **Checksums.** Verify MD5, SHA-1, SHA-256 or SHA-512 when a download ends.
- **Browser extension** for Chrome, Edge, Brave and Firefox. Downloads started in the browser are handed to INDM with the browser's cookies and referer. A pairing code keeps other programs on your PC from adding links.
- **Link folder.** Drop a text file with links into a watched folder and INDM downloads them.
- **Video download** through [yt-dlp](https://github.com/yt-dlp/yt-dlp), fetched on first use and checked against its official checksum.
- **Phone control.** Scan a QR code and manage downloads from your phone on the home Wi-Fi.
- **Android app (INDM Remote).** Built-in QR scanner, finished downloads can be saved from the PC to the phone, and a notification arrives when a download finishes or fails.
- **Automatic updates** from GitHub Releases, verified with a SHA-256 checksum.
- **Tidy and light.** Dark and light themes that follow the Windows accent colour, a tray icon, an installer, and a detailed log file for troubleshooting. English and Persian (Jalali dates, Persian digits).

### Install

1. Open the [Releases](../../releases/latest) page.
2. Download `INDM.exe` (portable) or `INDM_Setup.exe` (installer).
3. Run it. Windows SmartScreen or your antivirus may warn about an unsigned program; this is expected for a small independent project.

Requirements: Windows 10 or 11.

### Browser extension

1. In INDM open **Tools › Browser extension** and press the button for your browser. A pairing code is shown; copy it.
2. Chrome / Edge: open `chrome://extensions` (or `edge://extensions`), turn on *Developer mode*, choose *Load unpacked* and pick the `chrome_extension` folder.
   Firefox: open `about:debugging`, *This Firefox*, *Load Temporary Add-on*, and pick `firefox_extension/manifest.json` (temporary: it is removed when Firefox closes).
3. Paste the pairing code in the extension options.

### Phone control and the Android app

1. In INDM open **Tools › Phone control**, turn it on and save. A QR code appears.
2. On the phone (same Wi-Fi), scan the code with the INDM Remote app, or open the address in any browser.
3. The app can save finished files into `Downloads/INDM` and notify you when downloads end.

How to build the Android app (GitHub Actions, nothing to install) is in [`README_ANDROID.txt`](README_ANDROID.txt) inside the Android package.
The connection is plain HTTP on your own network and protected by a secret in the address, so use it on your own Wi-Fi only. "New code" in the dialog locks out old codes.

To enable update checks in your own fork, put `owner/repository` in `UPDATE_REPO` in `api.py` and publish releases that contain `INDM.exe` and `INDM.exe.sha256`.

### Status

INDM is an early release. Most features are tested with automated tests and fake servers, but real-world testing on many Windows systems is still ongoing. The Android app, video download, mirrors and the Firefox extension are the newest parts; please report anything odd together with the error report (Tools › Error report) or `indm.log`.

---

## فارسی

<div dir="rtl">

### امکانات

- **شتاب‌دهندهٔ دانلود:** تا ۳۲ اتصال همزمان برای هر فایل با تقسیم پویا؛ همهٔ اتصال‌ها تا آخرین لحظه مشغول می‌مانند. اگر سرور اتصال زیاد را رد کند، برنامه خودش تعداد را کم می‌کند.
- **مطمئن:** ادامهٔ دانلود بعد از توقف، خرابی یا بستن برنامه. اگر اینترنت قطع شود، دانلود صبر می‌کند و خودش ادامه می‌دهد. خطاهای قطعی (پر بودن دیسک، ۴۰۴، لینک منقضی) سریع و با پیام روشن نشان داده می‌شوند.
- **آینه‌ها:** برای یک فایل چند آدرس بدهید؛ اگر یکی خراب شد، برنامه به بعدی می‌رود و آنچه دانلود شده را نگه می‌دارد.
- **صف و زمان‌بندی:** چند صف، هرکدام با زمان شروع، محدودیت سرعت و تعداد دانلود همزمان خودش؛ محدودکنندهٔ سرعت کلی؛ و «خروج یا خاموش کردن بعد از پایان».
- **بررسی چک‌سام:** MD5، SHA-1، SHA-256 یا SHA-512.
- **افزونهٔ مرورگر** برای Chrome، Edge، Brave و Firefox: دانلودهای مرورگر با کوکی و Referer به INDM داده می‌شود. «کد اتصال» جلوی برنامه‌های دیگر کامپیوتر را برای افزودن لینک می‌گیرد.
- **پوشهٔ لینک:** فایل متنی حاوی لینک را در پوشهٔ تحت نظر بیندازید تا دانلود شود.
- **دانلود ویدیو** با [yt-dlp](https://github.com/yt-dlp/yt-dlp): بار اول از صفحهٔ رسمی گرفته و با چک‌سام رسمی سنجیده می‌شود.
- **کنترل با گوشی:** کد QR را اسکن کنید و روی وای‌فای خانه دانلودها را از گوشی کنترل کنید.
- **برنامهٔ اندروید (INDM Remote):** اسکنر QR داخلی، ذخیرهٔ فایل‌های کامل‌شده از کامپیوتر روی گوشی، و اعلان پایان یا شکست دانلود.
- **بروزرسانی خودکار** از Releases گیت‌هاب، همراه با بررسی SHA-256.
- **سبک و مرتب:** پوستهٔ تیره و روشن هماهنگ با رنگ تاکیدی ویندوز، آیکون سینی، نصب‌کننده، فایل لاگ دقیق برای عیب‌یابی، رابط فارسی و انگلیسی (تاریخ جلالی و ارقام فارسی).

### نصب

۱. صفحهٔ [Releases](../../releases/latest) را باز کنید.
۲. `INDM.exe` (قابل‌حمل) یا `INDM_Setup.exe` (نصب‌کننده) را دانلود کنید.
۳. اجرا کنید. ممکن است SmartScreen یا آنتی‌ویروس برای برنامهٔ بدون امضا هشدار بدهد؛ برای یک پروژهٔ مستقل کوچک طبیعی است.

نیازمندی: ویندوز 10 یا 11.
### افزونهٔ مرورگر

۱. در INDM به **ابزارها › افزونهٔ مرورگر** بروید و دکمهٔ مرورگر خود را بزنید. کد اتصال نشان داده می‌شود؛ کپی کنید.
۲. Chrome یا Edge: `chrome://extensions` (یا `edge://extensions`) ‹ Developer mode ‹ Load unpacked ‹ پوشهٔ `chrome_extension`.
   Firefox: `about:debugging` ‹ This Firefox ‹ Load Temporary Add-on ‹ فایل `firefox_extension/manifest.json` (موقت است و با بستن Firefox پاک می‌شود).
۳. کد اتصال را در تنظیمات افزونه بچسبانید.

### کنترل با گوشی و برنامهٔ اندروید

۱. در INDM به **ابزارها › کنترل با گوشی** بروید، روشن و ذخیره کنید. کد QR نشان داده می‌شود.
۲. در گوشی (روی همان وای‌فای) با برنامهٔ INDM Remote کد را اسکن کنید، یا آدرس را در هر مرورگری باز کنید.
۳. برنامه می‌تواند فایل‌های کامل‌شده را در `Downloads/INDM` ذخیره کند و پایان دانلود را اعلان بدهد.

ارتباط روی شبکهٔ خودتان و بدون رمزنگاری (HTTP) است و با یک بخش مخفی در آدرس محافظت می‌شود؛ پس فقط روی وای‌فای خودتان استفاده کنید. با «کد جدید» کدهای قبلی بی‌اثر می‌شوند.

### وضعیت

INDM یک نسخهٔ اولیه است. بیشتر قابلیت‌ها با تست خودکار و سرورهای ساختگی آزمایش شده‌اند، اما آزمایش واقعی روی سیستم‌های ویندوزی مختلف ادامه دارد. برنامهٔ اندروید، دانلود ویدیو، آینه‌ها و افزونهٔ Firefox تازه‌ترین بخش‌ها هستند؛ اگر چیز عجیبی دیدید، همراه با گزارش خطا (ابزارها › گزارش خطا) یا `indm.log` اعلام کنید.

</div>
