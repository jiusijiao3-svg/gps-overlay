package com.example.photomapviewer

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    data class PhotoItem(
        val uri: Uri,
        val name: String,
        val lastModified: Long,
        var lat: Double? = null,
        var lon: Double? = null
    )

    private val photoList = mutableListOf<PhotoItem>()
    private var currentIndex = 0
    private var isMapReady = false

    private lateinit var ivThumbnail: ImageView
    private lateinit var tvImageInfo: TextView
    private lateinit var mapWebView: WebView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnGsi: Button
    private lateinit var btnGoogleMap: Button

    // dp単位変換用ヘルパー
    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            loadPhotosFromFolder(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- プログラム側でUI全体を生成（XML不要） ---
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. フォルダ選択ボタン
        val btnSelectFolder = Button(this).apply {
            text = "📁 フォルダ選択"
            textSize = 15f
            setBackgroundColor(Color.parseColor("#2D2D2D"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48.dp()
            )
            setOnClickListener { folderPicker.launch(null) }
        }
        rootLayout.addView(btnSelectFolder)

        // 2. 画像サムネイル枠（FrameLayoutでテキストを重ねる）
        val thumbContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                210.dp()
            ).apply { topMargin = 6.dp() }
            setBackgroundColor(Color.BLACK)
        }

        ivThumbnail = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        thumbContainer.addView(ivThumbnail)

        tvImageInfo = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            setBackgroundColor(Color.parseColor("#99000000"))
            setPadding(6.dp(), 4.dp(), 6.dp(), 4.dp())
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.START
            ).apply { setMargins(6.dp(), 6.dp(), 6.dp(), 6.dp()) }
        }
        thumbContainer.addView(tvImageInfo)
        rootLayout.addView(thumbContainer)

        // 3. 中段ボタン行（左：国土地理院 / 右：Googleマップ）
        val midRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                44.dp()
            ).apply { topMargin = 6.dp() }
        }

        btnGsi = Button(this).apply {
            text = "国土地理院で開く"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#333333"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = 4.dp()
            }
            setOnClickListener { openInGsiMap() }
        }
        btnGoogleMap = Button(this).apply {
            text = "Googleマップで開く"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#333333"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = 4.dp()
            }
            setOnClickListener { openInGoogleMap() }
        }
        midRow.addView(btnGsi)
        midRow.addView(btnGoogleMap)
        rootLayout.addView(midRow)

        // 4. 地図ビュー（WebView + Leaflet）
        mapWebView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = 6.dp()
                bottomMargin = 6.dp()
            }
        }
        rootLayout.addView(mapWebView)

        // 5. 最下部ボタン行（左：次ファイル / 右：前ファイル）
        val btmRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48.dp()
            )
        }

        btnNext = Button(this).apply {
            text = "◀ 次のファイル (古い)"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#2D2D2D"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = 4.dp()
            }
            setOnClickListener { navigateTo(currentIndex + 1) }
        }
        btnPrev = Button(this).apply {
            text = "前のファイル (新しい) ▶"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#2D2D2D"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = 4.dp()
            }
            setOnClickListener { navigateTo(currentIndex - 1) }
        }
        btmRow.addView(btnNext)
        btmRow.addView(btnPrev)
        rootLayout.addView(btmRow)

        setContentView(rootLayout)

        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.settings.domStorageEnabled = true
        mapWebView.addJavascriptInterface(WebAppInterface(), "Android")

        mapWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                isMapReady = true
                renderCurrentPhotoOnMap()
            }
        }

        // 国土地理院標準タイル ＆ オルソ写真（航空写真）切替対応 Leaflet
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #1a1a1a; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var stdLayer = L.tileLayer('https://cyberjapandata.gsi.go.jp/xyz/std/{z}/{x}/{y}.png', {
                        maxZoom: 18, attribution: '地理院タイル(標準)'
                    });
                    var photoLayer = L.tileLayer('https://cyberjapandata.gsi.go.jp/xyz/seamlessphoto/{z}/{x}/{y}.jpg', {
                        maxZoom: 18, attribution: '地理院タイル(航空写真)'
                    });

                    var map = L.map('map', {
                        center: [34.722690, 135.017698],
                        zoom: 17,
                        layers: [stdLayer],
                        zoomControl: true
                    });

                    // レイヤー切り替えスイッチ（標準 vs 航空写真）
                    var baseMaps = {
                        "標準地図": stdLayer,
                        "衛星写真": photoLayer
                    };
                    L.control.layers(baseMaps, null, { position: 'topright' }).addTo(map);

                    var markerLayer = L.layerGroup().addTo(map);

                    // 縮尺（ズーム倍率）は維持したまま移動
                    function updatePoints(targetLat, targetLon, hasCoords, allPointsJson) {
                        markerLayer.clearLayers();
                        var points = JSON.parse(allPointsJson);

                        // 他の写真：赤丸（クリックでその写真へジャンプ）
                        points.forEach(function(p) {
                            if (p.hasGps && !p.isCurrent) {
                                var marker = L.circleMarker([p.lat, p.lon], {
                                    radius: 6,
                                    color: '#ff3333',
                                    fillColor: '#ff0000',
                                    fillOpacity: 0.8,
                                    weight: 2
                                }).addTo(markerLayer);
                                marker.on('click', function() {
                                    Android.onMarkerClicked(p.index);
                                });
                            }
                        });

                        // 選択中の写真：青丸
                        if (hasCoords) {
                            L.circleMarker([targetLat, targetLon], {
                                radius: 9,
                                color: '#ffffff',
                                fillColor: '#0066ff',
                                fillOpacity: 0.95,
                                weight: 3
                            }).addTo(markerLayer);

                            // ズームは変えずにパンのみ実行
                            map.panTo([targetLat, targetLon], { animate: true, duration: 0.4 });
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        mapWebView.loadDataWithBaseURL("https://maps.gsi.go.jp", htmlContent, "text/html", "UTF-8", null)
    }

    private fun loadPhotosFromFolder(folderUri: Uri) {
        val rootDoc = DocumentFile.fromTreeUri(this, folderUri) ?: return
        val files = rootDoc.listFiles()

        photoList.clear()
        for (file in files) {
            if (file.isFile && (file.name?.endsWith(".jpg", true) == true || file.name?.endsWith(".jpeg", true) == true)) {
                val item = PhotoItem(file.uri, file.name ?: "", file.lastModified())
                try {
                    contentResolver.openInputStream(file.uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        val latLong = FloatArray(2)
                        if (exif.getLatLong(latLong)) {
                            item.lat = latLong[0].toDouble()
                            item.lon = latLong[1].toDouble()
                        }
                    }
                } catch (_: Exception) {}
                photoList.add(item)
            }
        }

        // 新しい順（降順）にソート
        photoList.sortByDescending { it.lastModified }

        if (photoList.isNotEmpty()) {
            currentIndex = 0
            displayPhoto(currentIndex)
        } else {
            Toast.makeText(this, "JPEG写真が見つかりませんでした", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateTo(newIndex: Int) {
        if (photoList.isEmpty()) return
        if (newIndex in photoList.indices) {
            currentIndex = newIndex
            displayPhoto(currentIndex)
        }
    }

    private fun displayPhoto(index: Int) {
        val item = photoList[index]

        // メモリ消費対策（縮小展開）
        try {
            contentResolver.openInputStream(item.uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                val bitmap = BitmapFactory.decodeStream(stream, null, options)
                ivThumbnail.setImageBitmap(bitmap)
            }
        } catch (_: Exception) {
            ivThumbnail.setImageDrawable(null)
        }

        val gpsText = if (item.lat != null && item.lon != null) {
            "GPS: ${"%.6f".format(item.lat)}, ${"%.6f".format(item.lon)}"
        } else {
            "GPS: なし"
        }
        tvImageInfo.text = "[${index + 1}/${photoList.size}] ${item.name}\n$gpsText"

        btnNext.isEnabled = (index < photoList.size - 1)
        btnPrev.isEnabled = (index > 0)

        val hasGps = (item.lat != null && item.lon != null)
        btnGsi.isEnabled = hasGps
        btnGoogleMap.isEnabled = hasGps

        renderCurrentPhotoOnMap()
    }

    private fun renderCurrentPhotoOnMap() {
        if (!isMapReady || photoList.isEmpty()) return

        val current = photoList[currentIndex]
        val jsonArray = JSONArray()

        photoList.forEachIndexed { idx, p ->
            if (p.lat != null && p.lon != null) {
                val obj = JSONObject().apply {
                    put("index", idx)
                    put("lat", p.lat)
                    put("lon", p.lon)
                    put("hasGps", true)
                    put("isCurrent", idx == currentIndex)
                }
                jsonArray.put(obj)
            }
        }

        val hasCoords = current.lat != null && current.lon != null
        val lat = current.lat ?: 0.0
        val lon = current.lon ?: 0.0

        mapWebView.evaluateJavascript(
            "updatePoints($lat, $lon, $hasCoords, '${jsonArray}');",
            null
        )
    }

    private fun openInGsiMap() {
        val item = photoList.getOrNull(currentIndex) ?: return
        val lat = item.lat ?: return
        val lon = item.lon ?: return
        val url = "https://maps.gsi.go.jp/#18/${lat}/${lon}/"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun openInGoogleMap() {
        val item = photoList.getOrNull(currentIndex) ?: return
        val lat = item.lat ?: return
        val lon = item.lon ?: return
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(撮影地点)")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lon")))
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onMarkerClicked(index: Int) {
            runOnUiThread {
                navigateTo(index)
            }
        }
    }
}
