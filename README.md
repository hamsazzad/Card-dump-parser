# 💳 Card Dump Parser (Android Native)

<div align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Architecture-armeabi--v7a-ff69b4?style=for-the-badge" />
  <img src="https://img.shields.io/badge/UI-Terminal_Style-000000?style=for-the-badge&logo=gnome-terminal&logoColor=00FF00" />
</div>

<br>

A sleek, highly optimized native Android application designed to parse raw virtual card dump data. Converted from a web-based terminal UI, this app brings a hacker-style Matrix environment right to your mobile device. Built with strict optimization for low-end devices (runs smoothly even on 2GB RAM).

---

## ✨ Features

* **🟢 Matrix Terminal UI**: An immersive, custom-built terminal interface with a live Matrix digital rain background.
* **🔍 Smart Parsing**: Instantly extracts Card Number, Expiry Date, and CVV from raw text dumps.
* **👤 Auto Name Generation**: Intelligently assigns dummy names (e.g., *James Smith*) to parsed data for testing/formatting purposes.
* **📋 One-Tap Copy**: Quick copy buttons for Names, Card Numbers, Expiries, and CVVs.
* **🌍 Billing Address Support**: Optional fields to attach localized billing addresses to the parsed data.
* **💾 HTML Export**: Generates and downloads a beautifully formatted, standalone HTML file containing all parsed cards and addresses.

---

## 🛠️ Supported Card Types

<details>
  <summary><b>Click to view supported formats</b></summary>
  <br>
  <ul>
    <li>💳 <b>VISA</b></li>
    <li>💳 <b>MASTERCARD</b></li>
    <li>💳 <b>UNIONPAY</b></li>
  </ul>
</details>

---

## 🚀 How to Build & Run Locally (Termux)

This project is optimized to be built directly on an Android device using Termux, bypassing the need for a heavy PC IDE like Android Studio.

<details>
  <summary><b>👨‍💻 Show Termux Build Instructions</b></summary>
  <br>
  
  **1. Clone the Repository:**
  ```bash
  git clone [https://github.com/hamsazzad/Card-dump-parser.git](https://github.com/hamsazzad/Card-dump-parser.git)
  cd Card-dump-parser
