# GPS Overlay for Android

Pixel 7a などの Android 端末向けに開発した、純正カメラ連動型の高精度 GPS 浮遊モニター ＆ Exif 自動同期アプリです。  
Google のブレンド測位（Fused Location）を介さず、ハードウェア GNSS チップから直接生データを取得し、`GPSTEST` 等と同等のリアルタイム測位精度（±m）を画面上に常時表示します。  
さらに、純正カメラで撮影した写真の保存をバックグラウンドで検知し、撮影瞬間の真の生座標（誤差 ±数m）を Exif データへ自動で上書き同期します。

---

## 開発の目的

スマートフォン標準の純正カメラアプリでは、撮影時に GPS 衛星を十分に捕捉できているかが画面上から判別できません。そのため、測位が粗い状態のままシャッターを切り、撮影後の Exif 位置情報が数十メートル以上大きくズレて記録されてしまう問題が頻繁に発生します。

本アプリは以下の2段階でこの課題を根本から解決します。
1. **撮影前の確認**: カメラ画面上に高精度な生測位ステータス（誤差メーター数と色判定）をオーバーレイ表示し、確実に高精度な測位が完了した瞬間を見極めてシャッターを切る。
2. **撮影後の自動補正**: 純正カメラの優れた画質・HDR処理はそのまま活かしつつ、保存された写真の Exif ジオタグを撮影直前コンマ数秒のハードウェア生データへ裏で自動上書きする。

---

## 主な機能

* **ハードウェア GNSS 直結測位**
  * `LocationManager.GPS_PROVIDER` を直接叩き、基地局・Wi-Fi 推定を排した純粋な衛星電波精度を 500ms 間隔で更新。
  * 誤差 5m 以下で緑、5m 超で赤にインジケーター色が切り替わるステータスサークル。
* **Exif ジオタグ自動同期（バックグラウンド処理）**
  * 取得した生 GPS ログをメモリ上のリングバッファ（直近 5 分間）に保持。
  * `ContentObserver` で Google カメラの写真保存をバックグラウンド検知。
  * 写真の撮影タイムスタンプ（`DATE_TAKEN`）と最も時間差が小さい生座標をミリ秒単位で照合し、Exif（緯度・経度・高度）を 1 度だけ自動上書き。
  * 同期完了時に「Exif同期完了: ±〇m」を画面下部に通知。
* **フローティング UI（オーバーレイ表示）**
  * 画面上の任意の位置へドラッグ＆ドロップで移動可能。
  * インジケーターを素早くダブルタップすることで即時終了。
* **スリープ耐性と即応性（Foreground Service）**
  * フォアグラウンド常駐型のため、画面消灯中も衛星の捕捉をバックグラウンドで継続。
  * 撮影前に起動しておけば、スリープ復帰時やロック画面からのカメラ起動時にも測位待ち時間ゼロで撮影可能。
  * 通知バーの「終了する」アクションボタンからワンタップでサービス停止可能。

---

## 開発・ビルド環境

ローカルの Android Studio 環境を必要とせず、GitHub Actions の CI/CD パイプライン上で APK の自動コンパイルを行います。

* **言語**: Kotlin
* **ターゲット SDK**: compileSdk 34 / minSdk 26
* **ビルドツール**: Gradle 8.4, AGP 8.2.2
* **主要ライブラリ**: `androidx.exifinterface:exifinterface:1.3.7`
* **CI/CD**: GitHub Actions (Ubuntu-latest / JDK 17)

---

## 導入時の初期設定（Pixel / Android 13+）

本アプリは野良 APK（サイドロード）としてインストールし、他アプリが生成した写真ファイルの Exif を直接編集するため、初回のみ以下の権限設定が必要です。

1. **制限付き設定の解除**:
   * **設定** → **アプリ** → **GPS Overlay** を開く。
   * 右上の **「︙（メニュー）」** から **「制限付き設定を許可」** をタップして生体認証または PIN を通す。
2. **オーバーレイ表示の許可**:
   * **「他のアプリの上に重ねて表示」** を **ON** にする。
3. **位置情報権限**:
   * 位置情報の権限を **「正確な位置情報の使用」** を有効にして許可する。
4. **全ファイルアクセス権限（Exif 上書き用）**:
   * アプリ初回起動時に開く設定画面で、**「すべてのファイルへのアクセス」**（`MANAGE_EXTERNAL_STORAGE`）を **ON** にする。

---

## 操作方法

* **移動**: インジケーターをドラッグ
* **終了**: インジケーターをダブルタップ、または通知バーの「終了する」をタップ
* **撮影**: 通常通り純正カメラで撮影するだけで、1〜2 秒後に Exif が高精度生データへ自動上書きされます。

---
---

# GPS Overlay for Android (English)

A high-precision floating GPS monitor and automated Exif synchronization tool designed for Android devices (optimized for Google Pixel 7a), operating seamlessly alongside the stock camera.  
By bypassing Google's Fused Location Provider and querying the hardware GNSS receiver directly, it displays real-time satellite accuracy (±m) equivalent to dedicated diagnostic utilities like `GPSTEST`.  
Furthermore, it monitors image creation in the background and silently overwrites the photo's Exif geotag with the precise raw coordinates captured at that exact sub-second moment.

---

## Motivation & Purpose

Stock smartphone camera apps typically provide no visual feedback indicating whether GNSS satellites are sufficiently locked before a photo is taken. Consequently, users frequently capture images while the position fix is still coarse, resulting in recorded Exif location coordinates drifting by dozens of meters.

This application resolves the issue via a two-fold approach:
1. **Pre-shot verification**: Renders a floating real-time status overlay over the camera viewfinder (displaying margin of error in meters alongside dynamic color feedback) to confirm optimal satellite lock prior to releasing the shutter.
2. **Post-shot automated synchronization**: Retains the stock Google Camera's advanced computational photography and HDR pipeline while automatically overwriting the recorded Exif geotag with raw, pinpoint GNSS hardware coordinates in the background.

---

## Key Features

* **Direct Hardware GNSS Polling**
  * Queries `LocationManager.GPS_PROVIDER` directly at 500ms intervals, eliminating cellular and Wi-Fi triangulation inaccuracies.
  * Visual feedback: Green indicator for ≤ 5m error; Red indicator for > 5m error.
* **Automated Exif Synchronization**
  * Maintains an in-memory 5-minute rolling ring buffer of raw GPS data points.
  * Detects new image storage events via `ContentObserver` on `MediaStore`.
  * Matches the photo's `DATE_TAKEN` timestamp against the GPS buffer to identify the nearest sub-second coordinate, overwriting Exif latitude, longitude, and altitude in a single pass.
  * Displays a brief confirmation toast upon completion: "Exif同期完了: ±Xm".
* **Movable Floating Overlay**
  * Fully draggable across the screen to stay clear of viewfinder controls.
  * Quick double-tap dismisses and terminates the service instantly.
* **Sleep-Persistent Foreground Service**
  * Continues running as a persistent Foreground Service with screen off, maintaining hot satellite tracking.
  * Allows instantaneous, zero-latency shooting directly from the lock screen.
  * Persistent notification tray provides a single-tap "Exit" action.

---

## Build Environment

Constructed to compile entirely within GitHub Actions CI/CD workflows without requiring a local Android Studio environment.

* **Language**: Kotlin
* **Target SDK**: compileSdk 34 / minSdk 26
* **Build Tooling**: Gradle 8.4, AGP 8.2.2
* **Key Dependencies**: `androidx.exifinterface:exifinterface:1.3.7`
* **CI/CD Platform**: GitHub Actions (`ubuntu-latest` / Temurin JDK 17)

---

## Initial Setup (Android 13+ / Pixel)

Because this application is side-loaded and modifies media files created by another application, the following one-time permissions are required:

1. **Allow Restricted Settings**:
   * Navigate to **Settings** → **Apps** → **GPS Overlay**.
   * Tap the **three-dot menu (⋮)** in the upper-right corner, select **Allow restricted settings**, and authenticate.
2. **Overlay Permission**:
   * Enable **Display over other apps**.
3. **Location Permission**:
   * Grant Location access with **Use precise location** enabled.
4. **All Files Access (for Exif Writing)**:
   * When prompted on first launch, grant **All files access** (`MANAGE_EXTERNAL_STORAGE`).

---

## Usage

* **Reposition**: Drag and drop the status bubble.
* **Dismiss**: Double-tap the indicator, or tap **Exit** in the notification drawer.
* **Capture**: Shoot photos normally with the stock camera; Exif coordinates are automatically updated within 1–2 seconds.
