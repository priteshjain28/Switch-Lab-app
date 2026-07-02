# OfferVerse SwitchLab — Native Android App

A native Kotlin + Jetpack Compose Android app that estimates whether a job switch beats staying at your current company over a chosen time window.

The default setup is prefilled with the Amazon → Capital One example:

- Amazon base: `$110,000`
- Amazon take-home estimate: `76%`
- First sign-on: `$18,200`
- Second-year sign-on: `$13,500`, earned daily through year 2
- Amazon RSUs: `170` shares
- Default stock price: `$241.70`
- Relocation received: `$7,000 net`
- Relocation payback if switching: `$3,500 net`
- Join date: `2025-10-20`
- Switch date: `2026-11-01`
- End date: `2029-11-01`
- New company take-home estimate: `70%`
- Salary scenarios: `$120k, $125k, $130k, $135k, $140k, $145k, $150k`


## Upload to GitHub → get an APK (no Android Studio needed)

1. Create a new GitHub repository (private is fine).
2. Upload the **contents of this folder** so that `settings.gradle.kts`, `gradlew`, and the `app/` folder sit at the repository root (not nested inside another folder).
3. GitHub runs the build automatically on push. Or go to **Actions → Android Debug APK → Run workflow**.
4. When it finishes (green check), open the run and download the artifact **`SwitchLab-debug-apk`**. Inside is `SwitchLab.apk`.
5. Copy it to your Android phone, tap it, allow install from unknown sources, and open **SwitchLab**.

The project ships with a pinned Gradle wrapper (8.11.1) and a `gradle.properties` that enables AndroidX, so the cloud build is self-contained and repeatable. The unit tests run *after* the APK is built and never block the download.

## Features

- Native Android app, not a web/PWA wrapper
- Material 3 dark Android UI
- Stay vs switch dashboard
- Break-even base salary
- What-if salary ladder
- RSU vesting math
- Relocation repayment math
- First sign-on clawback if leaving before 1 year
- Second-year sign-on daily proration
- Cumulative cash path chart
- Switch advantage bar chart
- Local persistence using SharedPreferences
- Shareable report text
- Unit tests for the default scenario
- GitHub Actions workflow to build a debug APK

## Open in Android Studio

1. Install Android Studio.
2. Open this folder as a project.
3. Let Gradle sync finish.
4. Click **Run** to install on an emulator or plugged-in Android phone.

## Build a debug APK locally

If you have Gradle installed:

```bash
./gradlew assembleDebug
```

The APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build APK from GitHub

Push this project to GitHub, then open:

```text
Actions → Android Debug APK → Run workflow
```

After it completes, download the APK artifact named:

```text
SwitchLab-debug-apk
```

## Install APK on your phone

1. Copy/download `app-debug.apk` to your Android phone.
2. Tap it.
3. Allow install from unknown sources when Android asks.
4. Install and open **SwitchLab**.

## Accuracy notes

This is an estimator, not tax/legal advice. RSU value uses the share price entered in the app. Tax is approximated using take-home percentages. Relocation and bonus clawback can vary by company payroll/legal handling, so verify the exact repayment amount in your offer documents.
