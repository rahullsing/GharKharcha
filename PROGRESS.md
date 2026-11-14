# GharKharcha – Development Progress

Offline-first Android expense tracker for building a home.
App ID `in.gharkharcha` · Kotlin package `com.gharkharcha` · minSdk 35 · target/compile SDK 37.

| Phase | Scope | Status |
|---|---|---|
| 1 | Gradle, Hilt, Room schema, DAOs, repositories, calculation tests | ✅ Done (commit `6ce48dd`) |
| 2 | Navigation shell, theme, Projects + Works CRUD | ✅ Done (commit `4eca89e`) |
| 3 | Labourers, attendance sheet, labourer ledger | ✅ Done |
| 4 | Materials, vendors, photos, attachment viewer | ⏳ Pending |
| 5 | Payments with screenshots, share-intent receiver | ⏳ Pending |
| 6 | Dashboard and charts | ⏳ Pending |
| 7 | PDF + CSV reports | ⏳ Pending |
| 8 | Settings, app lock, reminders, backup/restore | ⏳ Pending |
| 9 | Polish: Hindi, accessibility, sample data, release build | ⏳ Pending |

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

## Phase 4 – Materials, vendors & photos
- Add/Edit material:
  - Item name, brand, quantity, unit (m, ft, kg, bag, piece, box, litre, sq ft, custom), rate, and a live total (qty × rate) that can be overridden.
  - Shop name and phone, purchase date, bill number, payment mode (Cash/UPI/Card/Bank/Credit), paid/unpaid, notes.
  - Quick "total only" entry (e.g. "Raw material – ₹4,500").
- Brand, item and vendor autocomplete; vendor master list (shop, phone, address, GSTIN).
- Duplicate a material entry.
- Work detail Materials tab with totals and unpaid (credit) amounts.
- Photos:
  - CameraX capture and Photo Picker multi-select. Only the CAMERA permission is asked for; no storage permission is needed.
  - Photos are compressed to at most 1600 px as JPEG at about 80% quality, keeping the EXIF date, and stored in `filesDir/attachments/<entity>/<id>/`.
  - Separate item photos and bill photos per material, a profile photo for labourers, a cover photo for houses.
- Photo viewer: full screen, pinch-zoom, swipe, share, delete, caption.
- Work detail Photos tab.

## Phase 5 – Payments
- Full Add Payment screen: amount, mode (Cash/UPI/Bank/Cheque), UTR, period covered (week range picker), optional work.
- Unlimited, ordered payment screenshots/receipts with captions, from gallery multi-select or camera.
- **Share into the app:** receive `ACTION_SEND` / `ACTION_SEND_MULTIPLE` images (e.g. a GPay/PhonePe screenshot), then choose "Attach to payment / material".
- Work detail Payments tab; payments with UTR shown in the ledger.

## Phase 6 – Dashboard & charts
- House switcher, total spent vs budget, today's labour cost, pending dues to all labourers.
- Spend-by-work donut chart and last-30-days bar chart, drawn with Compose Canvas.
- Recent activity feed.
- Quick-add FAB: Material / Attendance / Payment.
- Budget alerts when a work crosses 80% or 100% of its budget.
- House totals by work, by category (material vs labour), by month, by labourer and by vendor.
- Search across everything: labourer, item, brand, shop, notes.

## Phase 7 – Reports (PDF + CSV)
- Filters: date range, work, labourer.
- All PDFs: A4, built with the native `PdfDocument`, multi-page with automatic page breaks, "Page x of y", generated date, ₹ formatting, zebra rows.
  - **Work report:**
    - Header: house, work name, dates, status.
    - Materials table (S.No, Date, Item, Brand, Qty, Unit, Rate, Amount, Shop).
    - Labour table (Date, Name, Skill, Hours, Pay) and a Payments table.
    - Totals: materials, labour, grand total, budget, variance.
    - Optional appendix of bill/screenshot thumbnails, 2×3 per page.
  - **Labourer statement:** photo, details, day-wise table, payments with UTR, Days/Earned/Paid/Pending summary, signature line.
  - **House summary:** totals per work, category split, monthly totals.
- CSV export of the same data.
- Saved to `Documents/GharKharcha/` via MediaStore, then the share sheet opens (WhatsApp, email, Drive).

## Phase 8 – Settings & safety
- Settings: currency display, hours per day, theme (system/light/dark, dynamic colour), app lock, backup/restore, reminders.
- App lock with BiometricPrompt or the device PIN.
- Daily "Mark today's attendance" reminder at a chosen time (WorkManager, POST_NOTIFICATIONS permission).
- Weekly payday reminder showing pending dues.
- Backup: the full database plus photos in a single `.zip` saved to a folder you pick (Storage Access Framework), with optional automatic weekly backup.
- Restore from a zip, after a confirmation.

## Phase 9 – Polish & release
- Complete Hindi translation (`values-hi/strings.xml`); all text already lives in `strings.xml`.
- Accessibility: content descriptions, large-text support, touch target sizes.
- Final review of empty, loading and error states on every screen.
- Debug-only sample data: 1 house, 3 works, 4 labourers, 2 weeks of attendance, payments with dummy images.
- R8/ProGuard rules and a release build check.
- Verify there are no crashes on rotation or process death.
