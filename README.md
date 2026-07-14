# Expense Tracker v2 — Firebase Edition

Matches the System Architecture (Figure 3.1) from your Chapter 3: Presentation
Layer (Android/Compose) -> Application Layer (business logic in the ViewModel/
Repository) -> Firebase Authentication + Cloud Firestore (Data Layer).

## What's implemented
- Register / Login - Firebase Authentication (email + password)
- Dashboard - income, expense, and balance totals for the current month, recent activity feed
- Add Income and Add Expense
- Categories - default list + your own custom categories
- Budget Management - set a monthly limit per category, with over-budget warnings
- Reports & Analytics - pie chart (spending by category) and bar chart (6-month trend)
- Profile / Logout

All data is stored per-user in Cloud Firestore (Firebase's free-tier NoSQL database),
matching the collections in your diagram: Users, Income, Expense, Category, Budget.

## One manual step required: add your google-services.json
This file is what connects the app to YOUR Firebase project. It must sit at:
```
app/google-services.json
```
(same folder as app/build.gradle.kts). If it's missing, the build will fail.
You downloaded this earlier from the Firebase Console (Project settings -> your
Android app -> download google-services.json). Upload it into that exact folder
in GitHub before pushing/building.

## Firestore security - before your final submission
Right now Firestore is in test mode, which means anyone with your config
can read/write your database. Fine for development, but before you submit,
go to Firebase Console -> Firestore Database -> Rules, and paste this in:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

This restricts every user to only reading/writing their own data - click Publish.

## Tech stack (for your report)
- Language: Kotlin, Jetpack Compose (UI)
- Backend: Firebase Authentication + Cloud Firestore
- Architecture: MVVM (ViewModel + Repository pattern)
- Charts: custom-drawn with Compose Canvas (no third-party chart library -
  keeps the build lightweight and dependency-free)
- Cost: NGN 0 - Firebase's Spark (free) plan comfortably covers a student project's usage
