package com.sudantha.lightbrowser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import java.net.URLEncoder
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val bg = Color.rgb(7, 17, 30)
    private val panel = Color.rgb(17, 31, 47)
    private val blue = Color.rgb(25, 169, 255)
    private val white = Color.rgb(240, 247, 255)
    private lateinit var root: LinearLayout
    private lateinit var address: EditText
    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private lateinit var pageTitle: TextView
    private var homeVisible = true
    private val mediaUrls = linkedSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        configureWebView()
        if (savedInstanceState != null && web.restoreState(savedInstanceState) != null) showBrowser() else showHome()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    !homeVisible && web.canGoBack() -> web.goBack()
                    !homeVisible -> showHome()
                    else -> showExitDialog()
                }
            }
        })
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float = 14f, color: Int = white) =
        TextView(this).apply { text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER }
    private fun button(value: String, action: () -> Unit) = label(value).apply {
        setPadding(dp(10), dp(8), dp(10), dp(8))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(panel); cornerRadius = dp(14).toFloat(); setStroke(dp(1), Color.rgb(38, 61, 82))
        }
        isClickable = true; isFocusable = true; setOnClickListener { action() }
    }

    private fun buildUi() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(12), dp(8), dp(12), dp(8)) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(label("◉  LightBrowser", 23f, blue))
        brand.addView(label("FAST  ·  PRIVATE  ·  LIGHTWEIGHT", 10f, Color.LTGRAY).apply { gravity = Gravity.START })
        header.addView(brand, LinearLayout.LayoutParams(0, dp(54), 1f))
        header.addView(button("☾") { toast("Dark theme active") })
        header.addView(button("⋮") { showMenu(null) })
        root.addView(header)
        address = EditText(this).apply {
            hint = "Search or type URL"; setHintTextColor(Color.rgb(155, 174, 196)); setTextColor(white); textSize = 15f
            setSingleLine(true); imeOptions = EditorInfo.IME_ACTION_GO; setPadding(dp(16), 0, dp(14), 0)
            background = android.graphics.drawable.GradientDrawable().apply { setColor(panel); cornerRadius = dp(24).toFloat(); setStroke(dp(1), Color.rgb(50, 79, 105)) }
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO || event?.keyCode == KeyEvent.KEYCODE_ENTER) { navigate(text.toString()); true } else false
            }
        }
        root.addView(address, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) })
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progressTintList = android.content.res.ColorStateList.valueOf(blue); visibility = View.GONE }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))
        pageTitle = label("Your lightweight web", 13f, Color.LTGRAY).apply { gravity = Gravity.START; setPadding(dp(4), dp(4), dp(4), dp(4)) }
        root.addView(pageTitle, LinearLayout.LayoutParams(-1, dp(30)))
        web = WebView(this).apply { setBackgroundColor(bg); visibility = View.GONE }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val nav = listOf(
            "‹" to { if (web.canGoBack()) web.goBack() else showHome() },
            "›" to { if (web.canGoForward()) web.goForward() },
            "⌂" to { showHome() }, "⇩" to { showDownloads() }, "☰" to { showMenu(null) }
        )
        nav.forEachIndexed { index, pair -> bottom.addView(button(pair.first, pair.second), LinearLayout.LayoutParams(0, dp(46), 1f).apply { if (index > 0) leftMargin = dp(6) }) }
        root.addView(bottom, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(6) })
        setContentView(root)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        web.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = true; databaseEnabled = false
            mediaPlaybackRequiresUserGesture = true; cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true; builtInZoomControls = true; displayZoomControls = false
            setSupportMultipleWindows(false); javaScriptCanOpenWindowsAutomatically = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        web.isVerticalScrollBarEnabled = false; web.isHorizontalScrollBarEnabled = false
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                return when (uri.scheme?.lowercase(Locale.ROOT)) {
                    "http", "https" -> false
                    "mailto", "tel" -> { try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}; true }
                    else -> true
                }
            }
            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                progress.visibility = View.VISIBLE; progress.progress = 5; url?.let { address.setText(it) }; showBrowser(); mediaUrls.clear(); saveHistory(url)
            }
            override fun onPageFinished(view: WebView, url: String?) {
                progress.visibility = View.GONE; pageTitle.text = view.title?.take(72) ?: "LightBrowser"; url?.let { address.setText(it) }; detectMedia()
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) { progress.progress = newProgress; progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE }
        }
        web.setDownloadListener(DownloadListener { url, userAgent, disposition, mimeType, _ ->
            if (url.startsWith("https://") || url.startsWith("http://")) enqueueDownload(url, userAgent, disposition, mimeType) else toast("Unsupported download URL")
        })
    }

    private fun showHome() {
        homeVisible = true; web.visibility = View.GONE; progress.visibility = View.GONE; pageTitle.text = "Your lightweight web"; address.setText("")
        root.findViewWithTag<View>("home")?.let { root.removeView(it) }
        val home = LinearLayout(this).apply { tag = "home"; orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(6), dp(12), dp(6), dp(10)) }
        home.addView(label("Explore the web.", 27f)); home.addView(label("Fast. Private. Lightweight.", 13f, Color.LTGRAY))
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val sites = listOf(listOf("▶\nYouTube", "G\nGoogle", "f\nFacebook", "◎\nInstagram"), listOf("𝕏\nX", "♪\nTikTok", "➤\nTelegram", "+\nAdd"))
        sites.forEach { row ->
            val line = LinearLayout(this).apply { gravity = Gravity.CENTER }
            row.forEach { site ->
                line.addView(button(site) {
                    val key = site.substringBefore('\n')
                    val url = when (key) {
                        "▶" -> "https://m.youtube.com"; "G" -> "https://www.google.com"; "f" -> "https://m.facebook.com"
                        "◎" -> "https://www.instagram.com"; "𝕏" -> "https://x.com"; "♪" -> "https://www.tiktok.com"
                        "➤" -> "https://web.telegram.org"; else -> { shortcutDialog(); "" }
                    }
                    if (url.isNotEmpty()) navigate(url)
                }, LinearLayout.LayoutParams(0, dp(70), 1f).apply { setMargins(dp(3), dp(5), dp(3), dp(5)) })
            }
            grid.addView(line)
        }
        home.addView(grid)
        home.addView(button("Downloads  ·  Bookmarks  ·  History") { showMenu(null) }, LinearLayout.LayoutParams(-1, dp(46)).apply { setMargins(dp(3), dp(14), dp(3), dp(14)) })
        home.addView(label("No news feed. No background widgets.", 11f, Color.GRAY))
        root.addView(home, root.indexOfChild(web), LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun showBrowser() { homeVisible = false; root.findViewWithTag<View>("home")?.let { root.removeView(it) }; web.visibility = View.VISIBLE; pageTitle.text = web.title ?: "Browsing" }
    private fun navigate(input: String) {
        val value = input.trim(); if (value.isEmpty()) return
        val url = when { value.startsWith("https://") || value.startsWith("http://") -> value; value.contains(".") && !value.contains(" ") -> "https://$value"; else -> "https://www.google.com/search?q=" + URLEncoder.encode(value, "UTF-8") }
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(address.windowToken, 0); address.clearFocus(); showBrowser(); web.loadUrl(url)
    }

    private fun detectMedia() {
        val js = """(function(){try{var f=[];document.querySelectorAll('video,audio').forEach(function(e){var u=e.currentSrc||e.src;if(!u){var s=e.querySelector('source');if(s)u=s.src;}if(u&&/^https?:/i.test(u))f.push(u);});document.querySelectorAll('a[href]').forEach(function(a){if(/\\.(mp4|webm|mp3|m4a|ogg)(\\?|$)/i.test(a.href))f.push(a.href);});return JSON.stringify(Array.from(new Set(f)));}catch(e){return '[]';}})();"""
        web.evaluateJavascript(js) { encoded -> try {
            val decoded = org.json.JSONTokener(encoded).nextValue() as? String ?: return@evaluateJavascript
            val arr = org.json.JSONArray(decoded); for (i in 0 until arr.length()) mediaUrls.add(arr.getString(i))
            if (mediaUrls.isNotEmpty()) pageTitle.text = "Media found · ${'$'}{mediaUrls.size} resource(s)"
        } catch (_: Exception) { } }
    }

    private fun showMenu(anchor: View?) {
        val popup = PopupMenu(this, anchor ?: root.getChildAt(0))
        val actions = listOf<Pair<String, () -> Unit>>(
            "New tab / Home" to { showHome() }, "Bookmarks" to { showList("Bookmarks", pref("bookmarks")) },
            "Bookmark this page" to { bookmark() }, "History" to { showList("History", pref("history")) },
            "Media resources (${ '$' }{mediaUrls.size})" to { showMedia() }, "Downloads" to { showDownloads() },
            "Find in page" to { findInPage() }, "Desktop site" to { desktopSite() },
            "Lite mode for this site" to { liteDialog() }, "Clean exit" to { cleanExitDialog() }, "Settings" to { settingsDialog() }
        )
        actions.forEach { item -> popup.menu.add(item.first).setOnMenuItemClickListener { item.second(); true } }; popup.show()
    }

    private fun showMedia() {
        val urls = mediaUrls.toList()
        if (urls.isEmpty()) { android.app.AlertDialog.Builder(this).setTitle("Media resources").setMessage("No directly exposed media URL was detected. DRM and some adaptive streams are not available to this basic detector.").setPositiveButton("OK", null).show(); return }
        android.app.AlertDialog.Builder(this).setTitle("Media resources").setItems(urls.mapIndexed { i, u -> "${'$'}{i + 1}. ${'$'}{Uri.parse(u).lastPathSegment ?: u.take(50)}" }.toTypedArray()) { _, index ->
            val url = urls[index]
            android.app.AlertDialog.Builder(this).setTitle("Media preview").setMessage(url).setNegativeButton("Cancel", null)
                .setPositiveButton("Download") { _, _ -> enqueueDownload(url, web.settings.userAgentString, null, mimeFor(url)) }
                .setNeutralButton("Open") { _, _ -> web.loadUrl(url) }.show()
        }.show()
    }
    private fun mimeFor(url: String) = when { url.contains(".mp4", true) -> "video/mp4"; url.contains(".webm", true) -> "video/webm"; url.contains(".mp3", true) -> "audio/mpeg"; url.contains(".m4a", true) -> "audio/mp4"; else -> "application/octet-stream" }
    private fun enqueueDownload(url: String, agent: String?, disposition: String?, mime: String?) {
        try {
            val filename = URLUtil.guessFileName(url, disposition, mime)
            val request = DownloadManager.Request(Uri.parse(url)).setTitle(filename).setDescription("LightBrowser").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setAllowedOverMetered(true).setAllowedOverRoaming(false)
            CookieManager.getInstance().getCookie(url)?.let { request.addRequestHeader("Cookie", it) }; agent?.let { request.addRequestHeader("User-Agent", it) }
            if (android.os.Build.VERSION.SDK_INT >= 29) request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request); toast("Download queued")
        } catch (e: Exception) { toast("Download failed: ${'$'}{e.message ?: "unsupported resource"}") }
    }
    private fun showDownloads() { try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) } catch (_: Exception) { toast("Open Downloads in your file manager") } }
    private fun pref(key: String) = getSharedPreferences("browser", MODE_PRIVATE).getString(key, "") ?: ""
    private fun bookmark() {
        val url = web.url ?: return toast("Open a page first"); val entry = "${'$'}{web.title ?: url}\t${'$'}url"
        getSharedPreferences("browser", MODE_PRIVATE).edit().putString("bookmarks", (pref("bookmarks") + "\n" + entry).trim()).apply(); toast("Bookmark saved")
    }
    private fun saveHistory(url: String?) {
        if (url.isNullOrBlank() || !url.startsWith("http")) return
        val lines = (pref("history").lines() + "${'$'}{web.title ?: url}\t${'$'}url").filter { it.isNotBlank() }.distinct().takeLast(100)
        getSharedPreferences("browser", MODE_PRIVATE).edit().putString("history", lines.joinToString("\n")).apply()
    }
    private fun showList(name: String, raw: String) {
        val items = raw.lines().filter { it.isNotBlank() }
        android.app.AlertDialog.Builder(this).setTitle(name).setItems(items.toTypedArray()) { _, index -> val url = items[index].substringAfterLast('\t', ""); if (url.startsWith("http")) navigate(url) }.setPositiveButton("OK", null).show()
    }
    private fun findInPage() { val input = EditText(this).apply { hint = "Find text" }; android.app.AlertDialog.Builder(this).setTitle("Find in page").setView(input).setPositiveButton("Find") { _, _ -> web.findAllAsync(input.text.toString()) }.setNegativeButton("Close") { _, _ -> web.clearMatches() }.show() }
    private fun desktopSite() {
        val desktop = !web.settings.userAgentString.contains("X11; Linux x86_64")
        web.settings.userAgentString = if (desktop) "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" else WebSettings.getDefaultUserAgent(this)
        web.settings.useWideViewPort = desktop; web.settings.loadWithOverviewMode = desktop; web.reload()
    }
    private fun liteDialog() {
        val opts = arrayOf("Block images", "Require user gesture for media", "Block JavaScript")
        val checked = booleanArrayOf(web.settings.blockNetworkImage, web.settings.mediaPlaybackRequiresUserGesture, !web.settings.javaScriptEnabled)
        android.app.AlertDialog.Builder(this).setTitle("Lite mode").setMultiChoiceItems(opts, checked) { _, which, enabled -> when (which) { 0 -> web.settings.blockNetworkImage = enabled; 1 -> web.settings.mediaPlaybackRequiresUserGesture = enabled; 2 -> web.settings.javaScriptEnabled = !enabled } }.setPositiveButton("Done", null).setNegativeButton("Reset") { _, _ -> web.settings.blockNetworkImage = false; web.settings.mediaPlaybackRequiresUserGesture = true; web.settings.javaScriptEnabled = true }.show()
    }
    private fun cleanExitDialog() {
        val options = arrayOf("Browsing history", "Cookies & site data", "Cached files", "Close current page"); val selected = booleanArrayOf(true, false, true, true)
        android.app.AlertDialog.Builder(this).setTitle("Clean exit").setMultiChoiceItems(options, selected) { _, i, checked -> selected[i] = checked }.setPositiveButton("Clean") { _, _ ->
            val p = getSharedPreferences("browser", MODE_PRIVATE); if (selected[0]) p.edit().remove("history").apply(); if (selected[1]) CookieManager.getInstance().removeAllCookies(null); if (selected[2]) web.clearCache(true); if (selected[3]) showHome(); toast("Selected data cleared")
        }.setNegativeButton("Cancel", null).show()
    }
    private fun settingsDialog() {
        val options = arrayOf("Media resources", "Turbo download information", "Allow page audio without tap", "Clear cache", "About LightBrowser")
        android.app.AlertDialog.Builder(this).setTitle("Settings").setItems(options) { _, which -> when (which) {
            0 -> showMedia()
            1 -> android.app.AlertDialog.Builder(this).setTitle("Turbo downloads").setMessage("This build uses Android DownloadManager for supported direct links. It does not guarantee eight connections; segmented downloads require a separate downloader and server Range support.").setPositiveButton("OK", null).show()
            2 -> { web.settings.mediaPlaybackRequiresUserGesture = false; toast("Playback preference updated") }
            3 -> { web.clearCache(true); toast("Cache cleared") }
            4 -> android.app.AlertDialog.Builder(this).setTitle("LightBrowser").setMessage("Version 1.0.0\nPackage: com.sudantha.lightbrowser\nNative Android Views + WebView").setPositiveButton("OK", null).show()
        } }.show()
    }
    private fun shortcutDialog() { val input = EditText(this).apply { hint = "https://example.com" }; android.app.AlertDialog.Builder(this).setTitle("Open website").setView(input).setPositiveButton("Open") { _, _ -> navigate(input.text.toString()) }.setNegativeButton("Cancel", null).show() }
    private fun showExitDialog() { android.app.AlertDialog.Builder(this).setTitle("Exit LightBrowser?").setMessage("Close the browser?").setNegativeButton("Stay", null).setPositiveButton("Exit") { _, _ -> finish() }.show() }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    override fun onSaveInstanceState(outState: Bundle) { web.saveState(outState); super.onSaveInstanceState(outState) }
    override fun onDestroy() { web.stopLoading(); web.destroy(); super.onDestroy() }
}
