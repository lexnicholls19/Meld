# ✨ Meld

A unified space for your most meaningful connections.
**Meld** is a versatile Android application designed to centralize and enhance the bond between people who share a journey—whether they are partners, best friends, or family members. It provides a private, synchronized environment to stay organized, keep in touch, and celebrate your unique relationship.

Built with modern Android standards using **Jetpack Compose**, **Material 3**, and **Firebase**.

---

## 🌟 Key Features

- **🕰️ Connection Timer:** Track and celebrate the time you’ve shared. Whether it’s how long you’ve been best friends or how long you’ve been together, visualize every second of your journey.
- **🎨 Shared Canvas (Drawing):** A real-time creative space. Leave spontaneous sketches, handwritten notes, or quick doodles that appear instantly on your friend's or partner's screen.
- **💬 Daily Connection:** Strengthen your bond with curated daily prompts and questions designed to spark meaningful conversations and help you learn more about each other every day.
- **🚀 Instant Signals (Quick Notifications):** Let them know they're on your mind. Send quick, pre-defined signals like "Thinking of you" or "Miss you" with a single tap using Firebase Cloud Messaging.
- **📋 Collaborative Life Modules:** Meld centralizes your shared productivity and planning in one place:
  - Shared Lists: Real-time synchronized grocery and shopping lists.
  - Meld Reminders: Stay on top of shared tasks and goals.
  - Important Dates: A dedicated timeline for milestones and events with integrated countdowns.
  - Bucket List: Plan and track your shared dreams and upcoming adventures.
  - Cinema Room: A collaborative watchlist for movies and series you want to experience together.
- **🏠 Meld Glance (Widgets):** Keep your connection at your fingertips. Dynamic widgets built with Android Glance allow you to see your timer and most important updates directly from your home screen.
- **🎨 Customization:** 
  - Dynamic Theme support (Light/Dark/System).
  - Multi-language support (English, Spanish, and more).
  - Customizable main screen titles.

---

## 🛠️ Tech Stack

- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3.
- **Backend:** [Firebase](https://firebase.google.com/) (Firestore for DB, Cloud Messaging for notifications).
- **Widget:** [Android Glance](https://developer.android.com/jetpack/androidx/releases/glance) for app widgets.
- **Architecture:** MVVM (Model-View-ViewModel) + Modern Android Development.
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

Meld is a project focused on human connection, developed with love by [Lex Nicholls](https://github.com/lexnicholls).
