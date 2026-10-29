# LightBrowser

Ultra-lightweight Android browser built with Kotlin, native Android Views and the platform WebView.

## Features in this initial implementation

- Minimal dark homepage, URL/search bar and quick shortcuts
- Back/forward/home and menu navigation
- Local bookmarks and recent history
- Direct HTTP(S) downloads through Android DownloadManager
- Basic page-DOM detection for directly exposed video/audio URLs
- Media resource list and download/open actions
- Lite mode controls, desktop-site toggle and find-in-page
- Clean exit controls and lightweight XML vector app icon
- GitHub Actions workflow produces debug and unsigned release APK artifacts

## Build APK

Go to **Actions → Build LightBrowser APK → Run workflow**. Pushes to `main` that change source/build files also trigger the workflow.

The artifact named `LightBrowser-APK` contains `app-debug.apk` and `app-release-unsigned.apk`. The debug APK is easiest for local installation. The unsigned release APK is not suitable for Play Store publishing until it is signed.

## App details

- App label: LightBrowser
- Application ID: `com.sudantha.lightbrowser`
- Minimum Android: API 23
- Target SDK: API 28 (review and raise before public distribution)

## Lightweight approach

No news feed, external homepage images, analytics/ad SDKs, heavy UI toolkit, or periodic background jobs.

## Limitations

Media detection only sees URLs exposed in the current page DOM. It may miss dynamically loaded or adaptive streams, and it does not bypass DRM, paywalls, logins, or platform restrictions. This initial implementation uses Android DownloadManager and does not implement aria2-style multi-connection segmented downloads; eight connections or 8× speed are not guaranteed. Background playback depends on the page and WebView lifecycle; robust playback with notification/system media controls requires a dedicated Media3/MediaSession service. Test on physical devices before production use.
