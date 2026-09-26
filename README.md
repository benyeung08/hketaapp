# 巴士 App1933 Clone - 原生 Android 版 (方案二)

似足九巴 App1933 功能嘅原生 Android App，用 Kotlin + Jetpack Compose + 九巴開放API整。

## 功能對照
- [x] 一App睇晒 - 九巴/龍運路線、站名、方向
- [x] 預計到站時間 - 3班 ETA，每20秒自動更新
- [x] 實時載客量 - 用 rmk_tc 判斷 🟢有位 🟡 滿 🟡
- [x] 鄰近路線 - 定位 + Haversine 1km內車站
- [x] 收藏車站 - DataStore 持久化
- [x] 落車提示 - 架構已留位 (Geofencing + Vibrate)

## 點樣用 GitHub 一鍵出 APK
1. 喺 GitHub 開新 Repo，叫 `bus-1933-clone`
2. 將呢個 folder 全部 upload 上去
3. 去 `Actions` tab，啟用 workflow，push 就會自動 build
4. Build 完喺 Artifacts 下載 `app-debug.apk`，直接裝落手機

唔使裝 Android Studio，唔使 keystore。

## API
- `https://data.etabus.gov.hk/v1/transport/kmb/*` - 九巴官方開放數據
- 無需 API key，免費，CORS支援

## 本地跑
```
gradle :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 下一步可以加
- 城巴/小巴 API (rt.data.gov.hk / data.etagmb.gov.hk)
- 真正地圖 (Google Maps Compose)
- 落車提示 Geofence 實作
- Material You 主題

MIT License - Ben Yeung 2026