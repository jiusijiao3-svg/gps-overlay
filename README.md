# GPS Overlay for Android

Pixel 7a などの Android 端末向けに開発した、純正カメラ連動型の高精度 GPS 浮遊モニターアプリです。  
Google のブレンド測位（Fused Location）を介さず、ハードウェア GNSS チップから直接生データを取得することで、`GPSTEST` 等と同等のリアルタイム測位精度（±m）を画面上に常時表示します。

---

## 開発の目的

スマートフォン標準の純正カメラアプリでは、撮影時に GPS 衛星を十分に捕捉できているかが画面上から判別できません。そのため、測位が甘い状態のままシャッターを切り、撮影後の Exif 位置情報が数十メートル以上大きくズレて記録されてしまう問題が頻繁に発生します。

本アプリは、カメラ画面上に高精度な生測位ステータス（誤差メーター数と合致判定カラー）をリアルタイムでオーバーレイ表示し、**「確実に高精度な測位が完了した瞬間を見極めてシャッターを切る」** ことを可能にするために開発しました。

---

## 主な機能

* **ハードウェア GNSS 直結測位**
  * `LocationManager.GPS_PROVIDER` を直接叩き、基地局・Wi-Fi 推定を排した純粋な衛星電波精度を 500ms 間隔で更新。
  * 誤差 5m 以下で緑、5m 超で赤にインジケーター色が切り替わるステータスサークル。
* **フローティング UI（オーバーレイ表示）**
  * 画面上の任意の位置へドラッグ＆ドロップで移動可能。
  * インジケーターを素早くダブルタップすることで即時終了。
* **スリープ耐性と即応性（Foreground Service）**
  * フォアグラウンド常駐型のため、画面消灯中も衛星の捕捉をバックグラウンドで継続。
  * 撮影前に起動しておけば、スリープ復帰時やロック画面からのカメラ起動時にも測位待ち時間ゼロで撮影可能。
  * 通知バーの「終了する」アクションボタンからワンタップでサービス停止可能。
* **カメラ連動**
  * アプリ起動と同時に Pixel 純正カメラ（Google Camera）を自動呼び出し。

---

## 開発・ビルド環境

ローカルの Android Studio 環境を必要とせず、GitHub Actions の CI/CD パイプライン上で APK の自動コンパイルを行います。

* **言語**: Kotlin
* **ターゲット SDK**: compileSdk 34 / minSdk 26
* **ビルドツール**: Gradle 8.4, AGP 8.2.2
* **CI/CD**: GitHub Actions (Ubuntu-latest / JDK 17)

---

## 導入時の注意点（Pixel / Android 13+）

本アプリは野良 APK（サイドロード）としてインストールするため、Android のセキュリティ機能により「他のアプリの上に重ねて表示」の権限が初期状態で制限されます。

1. **設定** → **アプリ** → **GPS Overlay** を開く。
2. 右上の **「︙（メニュー）」** から **「制限付き設定を許可」** をタップして認証を通す。
3. **「他のアプリの上に重ねて表示」** を **ON** に変更する。
4. 位置情報の権限を **「正確な位置情報の使用」** を有効にして許可する。

---

## 操作方法

* **移動**: インジケーターをドラッグ
* **終了**: インジケーターをダブルタップ、または通知バーの「終了する」をタップ

---
---

# GPS Overlay for Android (English)

A high-precision floating GPS monitor app designed for Android devices (optimized for Google Pixel 7a), integrated seamlessly with the stock camera interface.  
By bypassing Google's Fused Location Provider and fetching raw data directly from the hardware GNSS receiver, it displays real-time satellite accuracy (±m) matching dedicated diagnostic tools like `GPSTEST`.

---

## Motivation & Purpose

Stock smartphone camera apps offer no visual feedback indicating whether GNSS satellites are sufficiently locked before capturing a photo. As a result, photos are often taken while the position fix is still coarse, leading to recorded geotag metadata (Exif) being inaccurate by dozens of meters.

This application solves that problem by rendering a lightweight floating indicator directly over the camera viewfinder. By displaying real-time accuracy and dynamic color coding, users can **verify pinpoint accuracy before pressing the shutter button**, ensuring flawless geotagging.

---

## Key Features

* **Direct Hardware GNSS Access**
  * Utilizes `LocationManager.GPS_PROVIDER` directly, eliminating Wi-Fi/cellular triangulation approximations and updating raw satellite accuracy every 500ms.
  * Real-time status indicator changes color dynamically: Green for ≤ 5m error, Red for > 5m error.
* **Movable Floating Overlay**
  * Freely draggable to any position on screen to avoid obstructing camera controls.
  * Quick double-tap anywhere on the indicator instantly stops and dismisses the overlay.
* **Sleep-Resistant Foreground Service**
  * Runs as a persistent Foreground Service, retaining satellite lock even while the screen is off.
  * Launching before heading into the field allows instantaneous, zero-latency geotagging directly from the lock screen.
  * Quick-stop action button integrated into the persistent notification tray.
* **Camera Launch Integration**
  * Automatically invokes the stock Google Camera app upon service initiation.

---

## Build Environment

Constructed to build entirely within GitHub Actions CI/CD workflows without requiring a local Android Studio development suite.

* **Language**: Kotlin
* **Target SDK**: compileSdk 34 / minSdk 26
* **Build Tooling**: Gradle 8.4, AGP 8.2.2
* **CI/CD Platform**: GitHub Actions (`ubuntu-latest` / Temurin JDK 17)

---

## Installation Notes (Android 13+ / Pixel)

Because this app is side-loaded via APK, Android applies restricted settings by default.

1. Navigate to **Settings** → **Apps** → **GPS Overlay**.
2. Tap the **three-dot menu (⋮)** in the top right corner and select **Allow restricted settings**, then authenticate with PIN/biometrics.
3. Enable **Display over other apps**.
4. Grant Location permission with **Use precise location** enabled.

---

## Usage

* **Reposition**: Drag and drop the status bubble.
* **Dismiss**: Double-tap the indicator, or tap **Exit** in the notification shade.
