# 💖 LoveCounter

**LoveCounter** is a feature-rich Android application designed for couples to celebrate their journey, stay organized, and keep their connection alive through real-time synchronization and shared goals.

Built with modern Android standards using **Jetpack Compose**, **Material 3**, and **Firebase**.

---

## ✨ Key Features

- **🗓️ Love Timer:** A real-time counter showing years, months, days, and seconds since your special date.
- **😊 Partner Status:** Share your current mood/emoji instantly with your partner via Firestore.
- **🔔 Shared Reminders:** Keep track of things to do together with real-time updates.
- **📅 Important Dates:** Never miss an anniversary or a special event with integrated countdowns.
- **🛒 Shared Shopping List:** A collaborative list for groceries or shared purchases.
- **⭐ Bucket List:** Plan and check off your shared dreams and adventures.
- **🎬 Movie List:** Keep track of movies and series you want to watch together.
- **🏠 Home Screen Widget:** A dynamic widget built with **Android Glance** to see your timer and reminders directly on your home screen.
- **💌 Quick Notifications:** Send "I miss you" or "I love you" signals with a single tap using Firebase Cloud Messaging.
- **🎨 Customization:** 
  - Dynamic Theme support (Light/Dark/System).
  - Multi-language support (English, Spanish, and more).
  - Customizable main screen titles.

---

## 🛠️ Tech Stack

- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3.
- **Backend:** [Firebase](https://firebase.google.com/) (Firestore for DB, Cloud Messaging for notifications).
- **Widget:** [Android Glance](https://developer.android.com/jetpack/androidx/releases/glance) for app widgets.
- **Architecture:** MVVM / Modern Android Development.
- **Animations:** Konfetti for celebration effects.
- **Image Loading:** Coil.
- **Networking:** Retrofit for potential API integrations.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- A Firebase Project.

### Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/lexnicholls/lovecounter.git
   ```
2. **Firebase Configuration:**
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android App with the package name `com.lexnicholls.lovecounter`.
   - Download the `google-services.json` file.
   - Place `google-services.json` in the `app/` directory of the project.
3. **Firestore Rules:**
   - Enable Firestore and set up rules to allow authenticated (or identified) users to read/write to `partner_status`, `quick_messages`, and other collections.
4. **Build & Run:**
   - Sync Gradle and run the app on your device or emulator.

---

## 🛡️ Security Note

The `google-services.json` and `local.properties` files are excluded from this repository for security reasons. Make sure to provide your own configuration files when cloning the project.

---

## ❤️ Credits

Developed by [Lex Nicholls](https://github.com/lexnicholls).
