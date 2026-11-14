# GharKharcha (घर का खर्चा) 🏠💰

> **Offline-first House Construction & Renovation Expense Tracker**  
> Built natively for Android using modern Jetpack Compose, Material 3, Room, and Kotlin Coroutines.

---

## 📌 Overview

Building or renovating a house involves hundreds of material purchases, daily labour wages, advance payments, shop credits, and fluctuating contractor estimates. Keeping track of bills in diaries or loose spreadsheets often leads to budget overruns, misplaced receipts, and wage disputes.

**GharKharcha** is a fast, 100% offline-first Android application designed specifically for Indian homeowners, civil contractors, and site supervisors. It allows you to track every single rupee spent on your house project, categorized by work (trade), with one-tap attendance, bill receipt capture, interactive visual analytics, professional A4 PDF/CSV reports, biometric security, and complete ZIP backup.

---

## ✨ Key Features

### 1. Multi-House & Project Management
- Track multiple house constructions or renovation projects simultaneously.
- Set total house budgets and monitor real-time spend, percentage utilized, and remaining balance.
- Visual cover photos and project address/notes.

### 2. Works & Cost Breakdown by Trade
- Organize projects by trade categories: **Brick Masonry & Plaster**, **Electrical**, **Plumbing & Sanitary**, **Painting**, **Tiling**, **Carpentry**, or Custom works.
- Set category-specific budgets with automatic **Budget Warning** (80%) and **Over Budget** alert banners.
- View exact Material vs. Labour cost ratios.

### 3. Labour Management & One-Tap Daily Attendance
- Master labourer directory with trade/skill, phone, photo, standard daily wage, and hourly rates.
- One-tap daily attendance sheet: **Full Day**, **Half Day**, **Hourly**, **Overtime**, or **Absent**.
- Automatic wage computation based on configurable workday hours (1–24 hours, default 8 hrs).
- **Copy Previous Day** and **Repeat for Date Range** tools to mark recurring attendance in seconds.
- Direct phone call and WhatsApp action buttons.

### 4. Labourer Ledgers & Payment Settlements
- Individual running ledger for each labourer showing days worked, total hours, earned pay, paid amounts, and net pending due or advance.
- Record payments with payment types (**Wage** or **Advance**) and payment modes (**Cash**, **UPI**, **Bank Transfer**, **Cheque**).
- Capture UTR references and attach payment screenshots/receipts.
- One-tap "Settle Up" action pre-filled with the exact pending balance.

### 5. Materials, Shop Credits & Photo Bills
- Log purchases with item name, brand, quantity, unit (bag, piece, kg, metre, sq ft, etc.), rate, and total.
- Flexible entry: full itemized details or quick total-only mode.
- In-app **Camera Scanner** and gallery picker to photograph physical paper bills and store them in internal scoped storage.
- Vendor directory with shop name, phone number, address, and GSTIN.
- **Shop Credit Tracking:** Flag purchases as paid or unpaid to immediately see outstanding dues to building material shops.

### 6. Interactive Visual Charts & House Breakdown
- **Pure Compose Canvas Spend Donut Chart:** Animated sweep angles, center total cutout, and color-coded trade legend.
- **30-Day Daily Spend Bar Chart:** Interactive bars plotting combined daily expenses; tap any bar to inspect date and amount.
- **House Breakdown Screen:** Comprehensive 5-tab analysis:
  1. *By Work:* Spend, budget progress, material vs. labour.
  2. *By Category:* Total spent, material/labour ratio bar, unpaid shop dues.
  3. *By Month:* Chronological monthly expense timeline.
  4. *By Labourer:* Days worked, earned pay, and color-coded balance states (Due / Advance / Settled).
  5. *By Vendor:* Purchase counts, total spend, and unpaid shop amounts.

### 7. Real-Time Global Search
- Fast debounced cross-entity search indexing Works, Labourers, Materials, Payments, and Vendors.
- Jump directly from search results to any editor or ledger.

### 8. Professional A4 PDF & RFC 4180 CSV Reports
- **House Summary Report:** Overall financial status, work-by-work tables, category ratios, and monthly breakdown.
- **Work Report:** Detailed materials purchase register, labour attendance log, payments register, and optional photo appendix of bills (2×3 thumbnail grid).
- **Labourer Statement:** Formal wages and attendance log with days worked, hours, advance payments, and **Dual Signature Lines** (Labourer & Owner/Contractor) for dispute-free settlements.
- Native Android `PdfDocument` generation (zero third-party binary dependencies).
- RFC 4180 CSV generation with UTF-8 BOM for Microsoft Excel compatibility.
- Instant export via Android Share Sheet (WhatsApp, Google Drive, Gmail, Print).

### 9. Security, Reminders & Backup/Restore
- **Biometric & Screen Lock:** Integrated `BiometricPrompt` supporting fingerprint, face unlock, and device PIN/pattern with an automatic lock scrim when returning from background.
- **Daily Attendance Reminder:** WorkManager background notification at your chosen time (interactive TimePicker) if today's attendance has not been logged.
- **Weekly Payday Reminder:** Aggregates outstanding labour dues across all houses and notifies the user before the weekend.
- **Full ZIP Backup & Restore:** Flushes SQLite WAL via `PRAGMA wal_checkpoint(FULL)`, packages database, photos, and `manifest.json` into a single `.zip` via Storage Access Framework (`ACTION_CREATE_DOCUMENT`), and provides safe, confirmed restoration (`ACTION_OPEN_DOCUMENT`).

### 10. Hindi & English Bilingual Support
- Complete native Hindi translation (`values-hi/strings.xml`) using domain-accurate Indian construction terminology.
- Respects Android 13+ per-app language settings and system locales.

### 11. Interactive Helper Wizard & Demo Data
- Built-in 5-step interactive **App Tour & Helper Wizard** explaining core app workflows.
- One-tap **Sample Data Seeder** in Settings to populate a realistic house ("Dream Villa"), 3 works, 4 labourers, materials, and 2 weeks of attendance for testing.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 2.1.10
- **UI Framework:** Jetpack Compose (Compose BOM 2025.02.00, Material 3)
- **Navigation:** Jetpack Navigation Compose with Navigation 3 type-safe routes (`@Serializable`)
- **Dependency Injection:** Dagger Hilt 2.55
- **Local Database:** Room 2.7.0-alpha13 (SQLite with WAL mode, type converters, indices, cascade deletes)
- **Preferences:** AndroidX DataStore (Preferences DataStore)
- **Background Tasks:** AndroidX WorkManager (Periodic background workers)
- **Security:** AndroidX Biometric (BiometricManager & BiometricPrompt)
- **Camera & Images:** CameraX (Camera2, Lifecycle, Viewfinder Compose) + Coil 3
- **Document Generation:** Android Native `android.graphics.pdf.PdfDocument` + Java NIO UTF-8 CSV
- **Testing:** JUnit 4, Robolectric, Turbine, Kotlinx Coroutines Test (78 unit & DAO tests)

---

## 📁 Project Structure

```text
com.gharkharcha/
├── data/
│   ├── dao/             # Room DAOs (ProjectDao, WorkDao, LabourerDao, AttendanceDao, MaterialDao, PaymentDao, VendorDao, AttachmentDao)
│   ├── db/              # GharKharchaDatabase & type converters
│   ├── entity/          # Room Entities & domain enums
│   ├── files/           # AttachmentFileStore (scoped internal storage)
│   ├── prefs/           # UserPreferences & UserPreferencesRepository (DataStore)
│   ├── repo/            # Repositories (BackupRepository, ReportRepository, AttachmentRepository, etc.)
│   ├── reports/         # PdfReportGenerator, CsvReportGenerator, ReportModels
│   └── sample/          # SampleDataSeeder (one-tap demo data loader)
├── di/                  # Hilt Dependency Injection modules (DatabaseModule, DataStoreModule, CoroutineModule)
├── domain/              # Pure domain business logic & calculators (LedgerCalculator, PayCalculator, AttendanceDraft)
├── reminders/           # WorkManager workers (DailyAttendanceWorker, WeeklyPaydayWorker, ReminderScheduler)
├── security/            # AppLockManager (BiometricPrompt & device credential handler)
├── ui/
│   ├── analytics/       # HouseBreakdownScreen & custom spend charts
│   ├── app/             # GharKharchaAppUi (NavigationSuiteScaffold, AppViewModel, lock scrim)
│   ├── attendance/      # AttendanceScreen & attendance dialogs
│   ├── components/      # Reusable M3 UI components, top bars, form fields, scaffolds, dialogs
│   ├── home/            # HomeScreen, metric cards, spend donut, 30-day bar chart, quick-add FAB
│   ├── labour/          # LabourersScreen, LabourerEditScreen, LabourerLedgerScreen
│   ├── materials/       # MaterialEditScreen, suggestions & calculations
│   ├── more/            # MoreScreen (settings, reports, vendors, houses, wizard)
│   ├── navigation/      # AppNavHost, TopLevelDestination & type-safe Routes
│   ├── payments/        # PaymentEditScreen
│   ├── photos/          # CameraScreen (CameraX viewfinder), PhotoViewerScreen
│   ├── projects/        # ProjectsScreen, ProjectEditScreen
│   ├── reports/         # ReportsScreen & export workflow
│   ├── search/          # SearchScreen (debounced cross-entity search)
│   ├── settings/        # SettingsScreen (appearance, work hours, app lock, reminders, backup/restore, demo data)
│   ├── theme/           # Color palettes, typography, shapes, dynamic color theming
│   └── wizard/          # HelperWizardDialog (interactive 5-step walkthrough)
└── util/                # Money, Quantity, Date, UndoDeleteManager, ShareIntentParser
```

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17 or JDK 21 (bundled Android Studio JBR recommended)
- Android SDK 35+ (Target SDK 37)

### Build via Command Line
```bash
# Clone the repository
git clone https://github.com/your-username/GharKharcha.git
cd GharKharcha

# Run all unit and DAO tests (78 tests)
./gradlew :app:testDebugUnitTest

# Build the ready-to-install APK
./gradlew :app:assembleDebug

# Publish clean APK named GharKharcha-v1.0.apk
./gradlew :app:publishApk
```

---

## 📲 Installing on Your Phone

The assembled installable APK is located at:
```text
app/build/outputs/apk/GharKharcha-v1.0.apk
(or app/build/outputs/apk/debug/app-debug.apk)
```

1. **Via USB / ADB:**
   ```bash
   adb install -r app/build/outputs/apk/GharKharcha-v1.0.apk
   ```
2. **Via Direct File Transfer:**
   Copy `GharKharcha-v1.0.apk` to your phone via USB cable, Google Drive, or WhatsApp, and tap the file in your phone's File Manager to install.

---

## 📖 User Guide
For complete step-by-step instructions on using all features, refer to the [USER_GUIDE.md](USER_GUIDE.md).

---

## 📄 License
This project is licensed under the Apache License 2.0.
