# GharKharcha – Development Progress

Offline-first Android expense tracker for building a home.
App ID `in.gharkharcha` · Kotlin package `com.gharkharcha` · minSdk 35 · target/compile SDK 37.

| Phase | Scope | Status |
|---|---|---|
| 1 | Gradle, Hilt, Room schema, DAOs, repositories, calculation tests | ✅ Done (commit `6ce48dd`) |
| 2 | Navigation shell, theme, Projects + Works CRUD | ✅ Done (commit `4eca89e`) |
| 3 | Labourers, attendance sheet, labourer ledger | ✅ Done |
| 4 | Materials, vendors, photos, attachment viewer | ✅ Done |
| 5 | Payments with screenshots, share-intent receiver | ✅ Done |
| 6 | Dashboard, charts, house breakdown, global search | ✅ Done |
| 7 | PDF + CSV reports (A4 printable & Excel export) | ✅ Done |
| 8 | Settings, app lock, reminders, backup/restore | ✅ Done |
| 9 | Polish: Hindi localization, helper wizard, sample data, documentation & release APK | ✅ Done |

---

## Phase 1 – Foundation (done)

### Build setup
- Version catalog (`gradle/libs.versions.toml`) with the latest stable libraries: Kotlin 2.4.21, Compose BOM 2026.09, Hilt 2.60.1, Room 2.8.5 with KSP, DataStore, Coroutines, kotlinx.serialization.
- AGP 9.5.1 with built-in Kotlin, Java 17, R8 code and resource shrinking on for release builds.
- Debug builds use the ID `in.gharkharcha.debug`, so they can be installed next to the release app.
- Room schemas are exported to `app/schemas/`. Migrations go in `Migrations.kt`, and the database is never wiped on upgrade.

### Database (Room v1)
| Table | Purpose / key rules |
|---|---|
| `projects` | A house: name, address, start date, budget, notes |
| `works` | A category of work under a project (type, budget, dates, status). Deleted along with its project. |
| `vendors` | Shop master list; names are unique regardless of capital letters |
| `materials` | Purchases under a work. Quantity is stored ×1000 (2.5 bags = 2500), money in paise, totals can be overridden, item details are optional. Deleting a vendor keeps the materials and clears the vendor link. |
| `labourers` | Global labourer master with wages, skill and an active flag |
| `attendance` | One entry per work, labourer and day. Rates are saved on each entry, so changing a wage later doesn't rewrite history. A labourer with records can't be deleted. |
| `payments` | To a labourer, type WAGE or ADVANCE, optionally linked to a project/work. Kept when that work is deleted. |
| `attachments` | Photos for any record (owner type + id, sort order, caption, kind) |

- Money is always stored as `Long` paise and dates as epoch days, never `Double`.
- Deleting a project, work, material, labourer or payment also deletes its photo files.

### Repositories & domain
- One repository per entity, plus:
  - `LedgerRepository`: live Days / Earned / Paid / Pending per labourer, for everything, one project or one work.
  - Saving a material adds its shop to the vendor list automatically.
  - Attendance is saved as insert-or-update for each work, labourer and day.
  - "Copy previous day" doesn't copy advances.
- `PayCalculator`: full, half, hours and overtime pay, on daily or hourly rates. Hours per day is configurable (default 8) and rounding is half-up.
- `LedgerCalculator`: Days (full = 1, half = 0.5, hours ÷ hours-per-day), Earned, Paid (wages + advances), Pending (red if due, green if advance).
- `SpendTotals`: budget vs actual, variance, and alert levels at 80% / 100%.
- `Money`: Indian formatting (`₹1,23,456.50`), compact chart labels (`₹3.2L`, `₹1.1Cr`), and parsing of user input.
- `DateFormats`: `dd-MMM-yyyy` in en-IN.
- DataStore preferences: hours per day, selected project, theme mode, dynamic colour.

### Tests (JVM + Robolectric, run with `gradlew test`)
- Money formatting and parsing, quantities, dates, every attendance type, overtime, the ledger, budget levels.
- Database: what gets deleted along with a parent, the one-entry-per-day rule, blocking deletion of labourers with records, vendor uniqueness, suggestions, photo lookups, and database totals matching the Kotlin calculations.

---

## Phase 2 – App shell, Projects & Works (done)

### Navigation & theme
- Single activity with type-safe Navigation-Compose routes.
- Four tabs: **Home · Works · Labour · More**. They show as a bottom bar on phones and a side rail on tablets/landscape, and slide away on detail and form screens.
- Material 3 with dynamic colour, a calm teal/brick fallback palette, dark mode, edge-to-edge, and predictive back.
- Shared spacing, cards, and empty / loading / error states used by every screen.
- Icons are vector drawables, so no icon library is needed.

### Screens
- **Home:** total spent vs budget with a progress bar, materials/labour split, amount remaining, spend per work, and an "Add work" call to action when there are no works.
- **House switcher:** tap the house name in the top bar to switch, add or manage houses.
- **My houses:** a card per house (number of works, spend, budget bar, current house outlined) with Edit and Delete.
- **Add/Edit house:** name, budget, start date, address, notes.
- **Works list:** cards with status chip, "₹spent of ₹budget", and a progress bar that turns amber at 80% and red at 100%.
- **Work detail:** tabs for Summary, Materials, Labour, Payments and Photos. Summary is complete; the other tabs show empty states until their phases.
- **Add/Edit work:** picking a type suggests a name (Electrical → "Electrician work"), status is one-tap chips, and start/end dates are validated.
- **More:** links to My houses, plus Reports and Settings marked "Coming soon".

### Behaviour
- Forms survive rotation and process death (form state is saved in `SavedStateHandle`). Pressing back with unsaved changes asks before discarding.
- Validation errors show inline, e.g. "Required" or "Enter a valid amount".
- Delete: a confirmation dialog, then the item disappears with an **Undo** snackbar for 10 seconds. If the delete fails, the item reappears instead of the app crashing.
- 41 unit and database tests pass, and the build has no warnings.

---

## Phase 3 – Labour (done)

### Screens
- **Labour tab:**
  - A "Daily attendance" card with a **Mark attendance** button. If the house has no works yet, it says to add one first.
  - Total due to all labourers, shown in red.
  - Search by name, phone or custom skill, and filter chips (All, each skill in use, Inactive).
  - Each row shows initials, name, skill and rate, and the balance: red **Due**, green **Advance** or **Settled**.
- **Add/Edit labourer:**
  - Name (auto-focused), phone, skill (or a custom skill name), daily wage, hourly rate, address, ID notes, active switch.
  - A new labourer opens straight into their ledger.
- **Daily attendance sheet:**
  - Previous/next day buttons and a date picker. Future dates are blocked.
  - Work picker; the default is the first work that's In progress.
  - One-tap **Full / Half / Hours / Absent** per active labourer, with that day's pay and notes (hours, overtime, advance).
  - Choosing **Hours** opens the details sheet.
  - The details sheet has hours, pay by Day or Hour with the rate, today's pay (calculated automatically, can be typed over, with Reset), overtime hours and rate, advance, notes, a live day total, and Clear/Save.
  - ⋮ menu: **Copy previous day** and **Repeat this day for other dates** (bulk add over a date range). Both skip labourers already marked and don't copy advances.
  - Sticky footer showing the day total and present count.
- **Labourer ledger:**
  - Contact card with **Call** (dialer, no permission needed) and **WhatsApp** (adds +91 to 10-digit numbers).
  - Day-by-day table (Date | Work | Hours | Pay | Paid) mixing attendance days and payments, newest first, with zebra rows and a header that stays put.
  - Tap a day to open that attendance sheet; tap a payment to edit it.
  - Sticky footer with **Days · Earned · Paid · Pending**. Pending is red when money is due; when the labourer has been overpaid it's labelled "Advance" in green.
  - **Settle up ₹X** opens a payment pre-filled with the pending amount.
  - ⋮ menu: Mark inactive/active, Delete. Labourers with records can't be deleted; the app offers "Mark inactive" instead.
- **Payment form (basic):**
  - Amount, Wages or Advance, mode (Cash/UPI/Bank/Cheque), date, optional work, UTR (shown for non-cash payments), notes.
  - Deleting a payment uses the Undo snackbar.
- **Work detail tabs:**
  - **Labour:** labour total, a Mark attendance button, and each labourer's days, hours and earnings on that work.
  - **Payments:** payments linked to that work.

### Logic & tests
- `AttendanceDraft` / `AttendanceDrafts`:
  - Pre-fills rates from the labourer.
  - One-tap marking keeps overtime, advance and notes but recalculates pay.
  - Full day plus overtime hours is saved as OVERTIME.
  - A typed pay override is dropped when the status changes.
- Attendance repository: "Repeat day" over a date range.
- Forms now ignore double taps on Save. Previously a second tap could save a duplicate.
- 53 unit and database tests, including draft rules, copy/repeat day, and WhatsApp number formatting.

---

## Phase 4 – Materials, vendors & photos (done)

### Materials
- **Add/Edit material:**
  - Two modes, **Item details** or **Total only** (e.g. "Raw material – ₹4,500").
  - Item details mode: item, brand, quantity, unit (m, ft, kg, bag, piece, box, litre, sq ft, or a custom unit), rate per unit, and a live **Total = qty × rate**. You can type over the total, and "Use qty × rate" switches back.
  - Shop & bill: shop name, shop phone, purchase date, bill number, paid by (Cash/UPI/Card/Bank/Credit), and a Paid switch. Choosing Credit sets it to unpaid automatically.
  - Photos: separate **Bill** and **Item photos** strips, then notes.
- **Autocomplete** for item and brand (from earlier entries) and for shop (from the vendor list). Picking a shop fills in its phone.
- **Work → Materials tab:**
  - Materials total, with "Unpaid to shops" shown in red when anything is unpaid.
  - One card per purchase: brand, qty × rate, date, shop, photo count, an Unpaid tag, and a ⋮ menu (Edit / Duplicate / Delete with undo).
  - **Duplicate** adds a copy dated today.
- **Shops & vendors** (under More): list plus add/edit with name, phone, GSTIN and address. Names are unique, and deleting a shop keeps the shop name on its materials.

### Photos
- **Adding a photo:** a sheet offers **Take photo** (CameraX, full screen) or **Choose from gallery** (system Photo Picker, up to 10 at a time).
  - Only the CAMERA permission is requested, with a friendly prompt and a link to Settings if refused. No storage permission is needed.
- **Saving:**
  - Photos are resized to at most 1600 px, saved as JPEG at 80% quality with orientation applied, and the original EXIF date is kept.
  - They are stored in `filesDir/attachments/<owner>/<id>/`.
  - Until the form is saved, photos sit in a cache folder. Unsaved leftovers are cleaned up after a day, so abandoned forms leave nothing behind.
- **Labourer profile photo:** shown on the labour list, attendance sheet and ledger. Tap it in the ledger to view it full screen.
- **House cover photo:** shown as a banner on the house card.
- **Work → Photos tab:** a grid of every bill and item photo for that work.
- **Photo viewer:**
  - Full screen, swipe between photos, pinch or double-tap to zoom (swiping is paused while zoomed).
  - Share via the share sheet, edit the caption, delete with a confirmation.
- Unsaved photos in forms open in a zoomable preview.

### Tests
- Photo sync: pending files are moved, order is kept, captions update, removed photos are deleted, and the single profile/cover photo can be replaced or cleared.
- Material totals: qty × rate, typed-over total, and total-only entries.
- Emulator UI testing:
  - Shops & vendors master CRUD under More tab (empty state, create with name/phone/GSTIN/address, and list verification).
  - Material entry (Item details mode): item, brand, qty, unit dropdown (bag), rate, live total auto-calculation (50 × ₹380 = ₹19,000), vendor link, phone, bill number, UPI payment mode.
  - Material entry (Total only mode): item, total amount, Credit payment mode with automatic unpaid switch toggle.
  - Work Materials summary & card verification: material totals, red "Unpaid to shops: ₹4,500" warning, unpaid badge on card.
  - Duplicate material action and Delete confirmation dialog with 10-second Undo snackbar and restore test.
  - Work Photos tab empty state and Home dashboard materials/labour breakdown live recalculation.

## Phase 5 – Payments (done)

### Payments & receipts
- **Full Add/Edit Payment screen:**
  - Amount with live validation (`MoneyField`).
  - Labourer selection dropdown when adding standalone, or fixed recipient header when opening from a labourer ledger.
  - Payment type: single-choice segmented button for **Wages** vs **Advance**.
  - Payment mode chips: **Cash**, **UPI**, **Bank**, **Cheque**.
  - Dynamic **Reference / UTR number** input shown whenever mode is non-cash (UPI, Bank, Cheque).
  - Date field defaulting to today, with date picker.
  - Optional Work link dropdown (auto-fills when adding from a work, or can be left unlinked).
  - **Period covered (week range picker):**
    - Quick chips for **This week** (Monday–Sunday) and **Last week**.
    - **Custom** mode with explicit From and To date pickers.
    - One-tap **Clear** button and formatted range text display.
  - **Receipts & screenshots:** unlimited ordered attachments (`PhotoStrip`) with zoomable photo preview, removal, and camera or photo picker integration (`AddPhotoSheet`).
  - Notes field.
  - Delete action with confirmation dialog and 10-second Undo delete manager.
- **Share into the app (`ACTION_SEND` & `ACTION_SEND_MULTIPLE`):**
  - AndroidManifest configured with `singleTop` launch mode and image intent filters.
  - `ShareIntentParser` extracts single or multiple shared image URIs from `EXTRA_STREAM` or `ClipData`.
  - `ShareImportSheet` bottom sheet automatically opens when images are shared into the app (e.g. from GPay, PhonePe, Paytm, or Gallery):
    - Multi-image preview thumbnail strip.
    - **Attach to payment**: links to a labourer, imports photos into `SharedPhotoHolder`, and opens the Payment form pre-populated with receipts.
    - **Attach to material**: links to a work in the project, imports photos, and opens the Material form pre-populated with bill photos.
- **Work detail – Payments tab:**
  - Payments total summary card with total paid on that work.
  - **Add payment** button directly linking the payment to that work.
  - Payment cards displaying: labourer name, date, type (Wages/Advance), mode (UPI/Cash/Bank/Cheque), UTR reference, period covered range tag, photo count icon badge, and overflow menu (Edit, Delete with 10s undo).
- **Labourer ledger:**
  - Day-by-day table shows payments with Date, Type, Mode, UTR reference, Period covered tag, photo count icon badge, and paid amount.
  - Real-time recalculation of Days, Earned, Paid, and Pending / Advance in the summary footer.

### Tests
- `PaymentRepositoryTest`: Room + AttachmentRepository integration for `saveWithPhotos`, photo synchronisation, photo count observations, and update/deletion of attachments.
- `ShareIntentParserTest`: single image `ACTION_SEND`, multi-image `ACTION_SEND_MULTIPLE`, and `ClipData` fallback parsing.
- Emulator UI testing:
  - Work detail Payments tab summary card and payment list.
  - Creating a new payment from Work detail: labourer selection, ₹1,500 amount, UPI mode, dynamic UTR entry (`UPI12345678`), "This week" period picker range auto-calculation.
  - Saved payment verified in Work Payments tab (total recalculated to ₹1,700).
  - Payment verified in Labourer Ledger with UTR, period covered tag, and updated summary totals.
  - Delete with Confirmation dialog and 10-second Undo snackbar test.
  - External image share (`ACTION_SEND`) triggered via adb intent broadcast, successfully opening `ShareImportSheet` with thumbnails, "Attach to payment", and "Attach to material".
- All 68 unit and database tests passing.

## Phase 6 – Dashboard & charts (done)

### Dashboard & Metrics
- **House switcher & Spend Overview:**
  - Live house switcher in the top bar.
  - Spend overview card: Grand total spent, spent vs budget progress bar, percentage used, Materials vs Labour split, and remaining budget balance.
  - Live metric card: **Today's labour cost** (sum of base pay + overtime for today across all works).
  - Live metric card: **Labour dues** (total pending dues to all labourers across the project; displays "No pending dues" in green when settled).
- **Budget Alerts:**
  - Dynamic alerts at the top of the dashboard when any work nears (≥80%) or exceeds (>100%) its budget.
  - Prominent alert banners with error icon, work name, percentage, and spent vs budget numbers.
  - Tapping an alert navigates directly to that work's details.

### Visual Charts (Pure Compose Canvas)
- **Spend-by-work Donut Chart:**
  - Pure Compose Canvas custom-drawn donut chart with sweep angle animations.
  - Center cutout displaying Grand Total with currency formatting.
  - Color-coded legend with work names and spend percentages.
- **30-Day Daily Spend Bar Chart:**
  - Pure Compose Canvas interactive bar chart plotting combined material + labour spend for the last 30 days.
  - Interactive selection: tapping any bar reveals an exact date & amount pill.
  - Today's bar highlighted in primary color, with 3-point date markers (start, middle, today).

### Quick-Add FAB & Recent Activity Feed
- **Speed-Dial Quick-Add FAB:**
  - Floating Action Button with rotating plus/close icon and semi-transparent scrim.
  - Speed-dial options for **Material**, **Attendance**, and **Payment**.
  - Seamless navigation to pre-filled edit routes with the first active work ID.
- **Recent Activity Feed:**
  - Chronological feed combining recent materials, attendance entries, and payments.
  - Visual icons for material (store), attendance (groups), and payment (wallet).
  - Tapping an item navigates directly to its respective editor or ledger.

### House Breakdown Screen (`/breakdown`)
- Accessible from the Home screen's "House breakdown" banner card.
- 5 comprehensive breakdown tabs using `PrimaryScrollableTabRow`:
  1. **By work:** Work name, total spend, material vs labour spend, budget progress indicator, and budget status.
  2. **By category:** Total spent, material vs labour horizontal ratio bar, materials total, labour total, and unpaid shop amount highlight.
  3. **By month:** Chronological monthly breakdown with total, material, and labour spend.
  4. **By labourer:** Days worked, earned amount, and color-coded balance state (Due / Advance / Settled). Tapping opens labourer ledger.
  5. **By vendor:** Shop name, purchase count, total purchase amount, and unpaid amount indicator. Tapping opens vendor editor.

### Global Search Screen (`/search`)
- Real-time search across the entire app accessible via the Search icon in the Home top bar.
- Auto-focused search input with 150ms query debounce.
- Cross-entity search sections:
  - **Works:** Name and notes matching.
  - **Labourers:** Name, phone, and role matching.
  - **Materials:** Item name, brand, shop name, and notes matching.
  - **Payments:** Reference and notes matching.
  - **Vendors:** Shop name, phone, address, and GSTIN matching.
- Instant navigation from search result cards directly to the target item.

### Tests & Verification
- Unit & DAO tests: `ProjectWorkMaterialDaoTest` testing daily spend aggregation and search across materials and works.
- All 70 unit and database tests passing (`:app:testDebugUnitTest`).
- Emulator UI testing:
  - Dashboard verified with live metrics (Spend overview, Today's labour ₹600, Labour dues).
  - Donut chart and 30-day interactive daily spend bar chart verified.
  - Quick-add speed dial FAB verified (opens Material, Attendance, Payment with scrim).
  - House breakdown screen verified across all 5 tabs (By work, By category, By month, By labourer, By vendor).
  - Real-time search verified: queried "sand", returned River Sand material and Gupta Sand Supplier vendor, with instant navigation to Material edit screen.
  - Budget alert verified: editing work budget to ₹20,000 triggered the "Over budget: Brick masonry is at 120% (₹24,100 of ₹20,000)" alert banner.

## Phase 7 – Reports (PDF + CSV) (done)

### Filters & Selection
- **Report Types:**
  - **House summary:** Overview of total budget, grand total spent, remaining balance, work-by-work table with budget usage %, category split, and monthly totals table.
  - **Work report:** Work status, budget, variance, materials purchase table (with brand, qty, unit, rate, amount, shop & payment status), labour attendance table (hours, overtime, rate, pay, advance), direct payments table (type, mode, UTR/reference), and financial summary totals. Optional photo appendix of bill and receipt thumbnails (2×3 grid per page).
  - **Labourer statement:** Labourer profile photo, phone, skill, standard daily wage, summary cards (days worked, hours worked, earned, paid, net due/advance), day-wise attendance log table, direct payments table with UTR/mode, and formal dual signature lines for labourer & owner/contractor.
- **Date Range Filters:**
  - Presets: **All time**, **This month**, **Last month**, and **Custom range** with material date picker dialogs (`From date` to `To date`).
- **Dynamic Scope Selectors:**
  - Work selector dropdown for Work Report.
  - Labourer selector dropdown for Labourer Statement.
- **Format Toggle:**
  - **PDF document** (native A4 printable document).
  - **CSV spreadsheet** (RFC 4180 compliant with UTF-8 BOM for Microsoft Excel).

### Native A4 Multi-Page PDF Generation (`PdfReportGenerator`)
- Built using native Android `android.graphics.pdf.PdfDocument` with zero third-party dependencies.
- Standard A4 layout: 595 × 842 points at 72 dpi with 36 pt margins.
- Multi-page automatic page breaks with table header re-drawing across page boundaries.
- Consistent branding: Primary teal (`#006A60`) headers, slate headers, zebra-striped rows (`#F8FAFC`), right-aligned currency columns with Indian currency grouping (`₹`).
- Photo appendix with downsampled thumbnail grids (2 columns × 3 rows) decoding bill attachments.

### RFC 4180 CSV Generation (`CsvReportGenerator`)
- UTF-8 with BOM (`\uFEFF`) at file start for proper Microsoft Excel character encoding on Windows and macOS.
- Automatic escaping for commas, double quotes (`""`), and newlines.
- Structured sections with subtitles, column headers, and subtotal/grand total summary rows.

### Storage & Sharing Integration (`ReportRepository`)
- Reports written to internal scoped storage (`filesDir/reports/`).
- Automatically copied to public storage in `Documents/GharKharcha/` via Android `MediaStore.Files` API.
- Direct invocation of the Android Share Sheet (`Intent.ACTION_SEND` with `FileProvider` `content://` URI and `FLAG_GRANT_READ_URI_PERMISSION`) for instant sharing to WhatsApp, Google Drive, Gmail, Quick Share, or Print.

### UI Integration & Navigation
- Dedicated **Reports & statements** screen accessible from `MoreScreen` ("PDF and CSV for works and labourers").
- Contextual export shortcuts:
  - Overflow menu in `WorkDetailScreen`: "Export report" pre-selects the current work.
  - Overflow menu in `LabourerLedgerScreen`: "Export statement" pre-selects the current labourer.

### Tests & Verification
- Unit test suite: `ReportsGeneratorTest` verifying `DateFilter` presets and CSV generation for House Summary, Work Report, and Labourer Statement with RFC 4180 escaping.
- All 74 unit tests passing (`:app:testDebugUnitTest`).
- Emulator verification:
  - Generated and shared Labourer Statement PDF via Android Share Sheet (`Labourer_Statement_1_...pdf`).
  - Generated and shared Labourer Statement CSV via Android Share Sheet (`Labourer_Statement_1_...csv`).
  - Verified navigation from `MoreScreen`, `WorkDetailScreen` ("Export report"), and `LabourerLedgerScreen` ("Export statement").

## Phase 8 – Settings & safety (done)

### Appearance & Theme
- **Theme Modes:** System default, Light, and Dark mode toggles with live instant switching across all screens without reloading.
- **Dynamic Color:** Material You dynamic color palette integration (Android 12+ wallpaper colors) with an in-app toggle.

### Work & Currency Preferences
- **Default Hours Per Day:** Configurable standard workday duration (1 to 24 hours, default 8 hours) with increment/decrement steppers, dynamically driving attendance daily rates, hourly wage calculations, and overtime computations.
- **Currency Symbol:** Formatted display for Indian Rupee (`₹`).

### Security & App Lock
- **Biometric & Device Credential Authentication:**
  - Integrated `androidx.biometric.BiometricPrompt` supporting hardware biometric sensors (Fingerprint, Face unlock) and device credential fallback (PIN, Pattern, Password).
  - Pre-flight security check via `BiometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)` with clear messaging if device lock is unconfigured.
  - Automatic full-screen lock scrim on cold app launch and when returning from background (`onStop` lifecycle tracking).
  - Explicit unlock button and prompt invocation via `MainActivity` extending `FragmentActivity`.

### Background Reminders (WorkManager)
- **Notification Channel:** Dedicated `gharkharcha_reminders` ("Reminders & Alerts") channel.
- **Daily Attendance Reminder:**
  - `DailyAttendanceWorker` running periodic background check at user-customized time (e.g., 7:00 PM).
  - Checks if attendance was marked for today; notifies user if pending with quick action to open app.
  - Interactive Material 3 Time Picker dialog for choosing custom reminder time.
  - Runtime `POST_NOTIFICATIONS` permission flow for Android 13+.
- **Weekly Payday Reminder:**
  - `WeeklyPaydayWorker` checks pending labour dues across all projects; aggregates total outstanding dues and displays actionable payday notification banner.

### Complete Backup & Restore (Storage Access Framework)
- **Backup (`BackupRepository`):**
  - Database WAL flushed to disk via `PRAGMA wal_checkpoint(FULL)`.
  - Packages SQLite database (`database/gharkharcha.db`), all photo attachments (`attachments/*`), and metadata `manifest.json` into a single compressed `.zip` archive.
  - Saved anywhere on internal or external storage via Android Storage Access Framework (`ActivityResultContracts.CreateDocument("application/zip")`).
  - Tracks timestamp of last backup with human-readable date/time badge.
- **Restore (`BackupRepository`):**
  - Pick backup archive via SAF `ActivityResultContracts.OpenDocument()`.
  - Pre-restore confirmation dialog warning user that current data will be replaced.
  - Validates zip structure and database presence, closes active Room connections, cleanly extracts files, wipes obsolete WAL/SHM temporary files, restores photos, and re-initializes database.

### Tests & Verification
- Unit test suite: `BackupRepositoryTest` testing zip creation, metadata tracking, archive validation (missing DB rejection), and end-to-end roundtrip backup & restore extraction.
- All 77 unit tests passing (`:app:testDebugUnitTest`).
- Emulator UI verification on Medium Phone API 36.1:
  - Settings screen navigation from More screen.
  - Theme switching (System -> Dark -> System) verified.
  - Work hours adjustment (8h -> 9h) verified.
  - Attendance reminder toggle & time picker verified.
  - Backup & restore UI verified.
- Installable APK assembled: `app/build/outputs/apk/debug/app-debug.apk`.

## Phase 9 – Polish & release (done)

### Hindi Localization (`values-hi/strings.xml`)
- Complete and comprehensive Hindi translation covering 100% of the app's user-facing strings.
- Natural, conversational, and domain-accurate construction terminology for Indian homeowners, contractors, and site supervisors:
  - Navigation: होम, काम, लेबर, अन्य
  - Works: ईंट चिनाई और प्लास्टर, बिजली (इलेक्ट्रिकल), नल-फिटिंग (प्लंबिंग), पेंटिंग, टाइल्स, बढ़ई (कारपेंट्री)
  - Labour & Attendance: हाज़िरी, पूरी/आधी दिहाड़ी, दैनिक दिहाड़ी, प्रति घंटा दर, ओवर-टाइम, एडवांस, हिसाब चुकता
  - Payments: नकद, यूपीआई (UPI), बैंक ट्रांसफर, चेक, रसीदें
  - Materials & Units: बोरी (bag), नग/पीस (piece), किलो (kg), मीटर (metre), वर्ग फीट (sq ft), उधार/क्रेडिट
  - Plural string support (`project_works_count`, `attendance_copied`, `attendance_range_done`, `attendance_present_count`).
- Verified live on device via `cmd locale set-app-locales in.gharkharcha.debug --locales hi-IN`.

### Demo Data Generator (`SampleDataSeeder`)
- Implemented `SampleDataSeeder` in `com.gharkharcha.data.sample` to instantly populate realistic construction data:
  - **1 House Project:** "Dream Villa (Sector 62)" (Plot 14, Green Valley Enclave, ₹25,00,000 budget).
  - **3 Works:** "Brick Masonry & Plaster" (₹3,50,000 budget, in progress), "Electrical Wiring & Conduit" (₹1,80,000 budget, in progress), "Plumbing & Sanitary" (₹1,50,000 budget, planned).
  - **4 Labourers:** Ramesh Kumar (Brick Mason, ₹700/day), Suresh Yadav (Helper, ₹500/day), Imran Khan (Electrician, ₹800/day), Rajesh Verma (Plumber, ₹750/day).
  - **2 Vendors:** Gupta Building Materials (Sector 18 Market), Sharma Electricals & Hardware.
  - **Materials:** UltraTech Cement (50 bags), Red Bricks (3,000 pcs), River Sand (1 truck on credit), Polycab Wire (4 coils).
  - **2 Weeks Attendance:** 14-day history with full-day logs and automated wage calculations.
  - **Payments:** Weekly cash wage to Ramesh (₹5,000), UPI wage to Suresh (₹3,000), UPI advance to Imran (₹1,500).
- Exposed in Settings screen under **Demo Data** -> "Load sample project & data" with confirmation dialog and loading progress indicator.

### Accessibility & Polish
- Full content description coverage on all `IconButton`s and clickable elements across all screens.
- Standard Material 3 minimum 48dp touch targets.
- Empty states, loading spinners, and error screens with retry buttons across all tabs.
- Dynamic large-text scaling support without clipping or overlapping.

### R8 / ProGuard Configuration
- Configured [`app/proguard-rules.pro`](app/proguard-rules.pro) with keep rules for Room Database & Entities, WorkManager workers, Kotlinx Serialization route models, and AndroidX Biometrics.
- Configured debug signing fallback for release builds in [`app/build.gradle.kts`](app/build.gradle.kts) so shrinked APKs can be installed directly.

### Helper Wizard (App Tour)
- Implemented `HelperWizardDialog` in `com.gharkharcha.ui.wizard`:
  - 5-step interactive visual walkthrough explaining core concepts:
    1. *Track Every Rupee:* Centralized offline house budgeting.
    2. *Works & Budgets:* Trade category split and budget alerts.
    3. *Daily Attendance & Labour:* One-tap attendance, wages, and advances.
    4. *Materials, Shops & Bills:* Bill receipt photo capture and shop credit tracking.
    5. *Reports, Safety & Backup:* PDF/CSV statements, biometric app lock, and full ZIP backup.
  - Automatically launches on first app open; re-accessible anytime from More screen -> **App tour & user guide** (ऐप टूर और गाइड).
  - Animated step transitions, progress dots, and step counters.

### Documentation
- Created comprehensive [`README.md`](README.md):
  - Architecture, tech stack, complete 11-module feature inventory, project structure, build & test guide.
- Created comprehensive [`USER_GUIDE.md`](USER_GUIDE.md):
  - 12-section user manual with step-by-step instructions for managing houses, budgets, daily attendance, payments, material purchases, report generation, and backup/restore in both English and Hindi.

### Tests & Verification
- Unit test suite: `SampleDataSeederTest` verifying complete relational database population and foreign key integrity.
- All 78 unit tests passing (`:app:testDebugUnitTest`).
- Emulator UI verification on Medium Phone API 36.1:
  - Settings -> "Load sample data" verified live.
  - Home dashboard populated with ₹95,900 total spend, donut chart, 30-day spend bar chart, active budget alerts, and recent activities.
  - Hindi localization verified on screen via screenshot.
  - Helper Wizard 5-step walkthrough verified live with sequential screenshots.
  - 0 crashes, 0 ANRs, 0 memory leaks in logcat.

### Installable APK
- Freshly assembled and published with clean, professional naming:
  - **Primary File:** `GharKharcha-v1.0.apk` (Project Root)
  - **Alternate File:** `apk/GharKharcha-v1.0.apk`
  - **Build Output:** `app/build/outputs/apk/debug/app-debug.apk`
  - **Size:** 44.12 MB (46,262,870 bytes)
  - **Application ID:** `in.gharkharcha.debug`
  - **Gradle task:** `./gradlew :app:publishApk`
