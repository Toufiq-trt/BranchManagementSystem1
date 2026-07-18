# AI Agent Rules for Bank Solution (Branch Management System)

These rules are permanently loaded by the AI Studio environment. Every developer or coding agent working on this app **must** strictly adhere to these rules to prevent data loss, ensure smooth software updates, and maintain high standards of code craft.

## 🚨 RULE #1: ABSOLUTE ZERO DATA LOSS ON SOFTWARE UPDATES

### 1. Room Database Migrations
- **Never allow Room to auto-wipe tables during updates.**
- Whenever the database schema is modified (adding, editing, or deleting a column or table):
  - **Do NOT** rely on `.fallbackToDestructiveMigration()` as a default behavior.
  - You **must** increment the database `version` in `/app/src/main/java/com/example/data/AppDatabase.kt`.
  - You **must** write an explicit `MIGRATION_X_Y` object defining the exact SQL statements needed (e.g., `ALTER TABLE`, `CREATE TABLE`).
  - You **must** register this migration using `.addMigrations(MIGRATION_X_Y)` on the Room database builder.
- Ensure SQL column additions are safe (use `DEFAULT ''` or nullable fields where appropriate).

### 2. Application ID Preservation
- **Never change the `applicationId`** in `/app/build.gradle.kts`.
- It must remain constantly defined as `com.aistudio.smartbanking.tfqsys`. Changing this would cause the Android OS to treat the update as a completely different app rather than upgrading the existing one, causing user data to not transfer.

### 3. Keystore & Signature Continuity
- **Never modify or delete `debug.keystore` or `debug.keystore.base64`.**
- Never modify the `signingConfigs` block in `/app/build.gradle.kts`.
- Consistent signatures are an absolute prerequisite for Android to perform an in-place update. If the signature changes, the phone will reject the installation with a signature mismatch error.

---

## 🛠️ RULE #2: GITHUB SOFTWARE UPDATES (OTA)

### 1. Release Version Configuration
- When preparing an update:
  - Increment the `versionCode` (e.g., from `23` to `24`) and `versionName` (e.g., from `"1.23"` to `"1.24"`) in `/app/build.gradle.kts`.
  - Sync these values exactly in the root `/version.json` file.
  - The `version.json` should have:
    - `"versionCode"`: Match the gradle `versionCode`.
    - `"versionName"`: Match the gradle `versionName`.
    - `"apkUrl"`: Points to `"https://raw.githubusercontent.com/Toufiq-trt/BranchManagementSystem1/main/ToufiqBranch.apk"`.
    - `"releaseNotes"`: Descriptive bullet points outlining what has changed.

---

## 🎨 RULE #3: INTERFACE & VISUAL STANDARDS

- All UI changes must adhere strictly to Material Design 3.
- Use the **GoldPrimary** and **SlateDark** palette consistently across all screen views.
- Ensure all interactive elements have a touch target of at least `48.dp`.
