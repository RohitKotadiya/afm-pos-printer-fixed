# AFM POS Printer - Android Bridge App

Android application that bridges your React PWA with D-CODE DC2M thermal printer.

## 🎯 Purpose

This app eliminates the need for the 4barcode app by providing direct printer integration with your React PWA through a WebView interface.

## ✨ Features

- ✅ Direct Bluetooth connection to D-CODE DC2M printer
- ✅ USB printer support
- ✅ JavaScript bridge for React PWA integration
- ✅ Automatic bill formatting for thermal printer
- ✅ Support for mixed dishes with ingredients
- ✅ Real-time connection status
- ✅ Paired device discovery

## 📋 Prerequisites

- Android Studio Arctic Fox or later
- Android SDK API 24+ (Android 7.0+)
- JDK 17
- D-CODE DC2M thermal printer
- Your React PWA deployed and accessible

## 🚀 Quick Start

### 1. Clone and Setup

```bash
# Extract the fixed project
cd afm-pos-printer-fixed

# Place the AAR file (already included)
# app/libs/printer-lib-3_4_4.aar
```

### 2. Configure Your PWA URL

Open `app/src/main/kotlin/com/achyutamfruitam/pos/MainActivity.kt` and update:

```kotlin
private val PWA_URL = "https://your-pwa-url.com"
```

For local testing:
- Android Emulator: `"http://10.0.2.2:3000"`
- Physical Device: `"http://192.168.x.x:3000"` (your computer's IP)

### 3. Build the APK

#### Option A: Using Android Studio (Recommended)

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `afm-pos-printer-fixed` folder
4. Wait for Gradle sync to complete
5. Click Build → Build Bundle(s) / APK(s) → Build APK(s)
6. APK will be in `app/build/outputs/apk/debug/app-debug.apk`

#### Option B: Using Command Line

```bash
# Make gradlew executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK (for production)
./gradlew assembleRelease

# APK location:
# Debug: app/build/outputs/apk/debug/app-debug.apk
# Release: app/build/outputs/apk/release/app-release.apk
```

### 4. Install on Device

```bash
# Via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Or transfer APK to phone and install manually
```

## 🔧 Integration with React PWA

See [REACT_INTEGRATION_GUIDE.md](REACT_INTEGRATION_GUIDE.md) for complete integration instructions.

### Quick Example

```typescript
// In your React app
import { androidPrinterService } from './services/androidPrinterService';

// Connect to printer
await androidPrinterService.connectBluetooth("00:11:22:33:44:55");

// Print bill
await androidPrinterService.printBill({
  billNo: 101,
  customerName: "John Doe",
  customerMobile: "9876543210",
  lineItems: [
    {
      productName: "Vanilla Ice Cream",
      quantity: 2,
      price: 50.00
    }
  ],
  paymentMethod: "Cash",
  grandTotal: 100.00
});
```

## 📱 Permissions

The app requests the following permissions:

- **Bluetooth**: For printer connection
- **Location**: Required for Bluetooth scanning on Android 6+
- **Internet**: To load your PWA

## 🐛 Troubleshooting

### Build Errors

**Error: Unresolved reference: net**
- ✅ **Fixed**: AAR file is now correctly included via `implementation(files("libs/printer-lib-3_4_4.aar"))`

**Error: Gradle sync failed**
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Runtime Issues

**Printer not connecting**
1. Ensure Bluetooth is enabled
2. Pair printer in Android Bluetooth settings first
3. Check printer is powered on
4. Try USB connection if available

**WebView not loading PWA**
1. Check PWA_URL is correct
2. For local testing, ensure device can reach your development server
3. Check Android logs: `adb logcat | grep MainActivity`

**Print not working**
1. Verify printer is connected: `androidPrinterService.isConnected()`
2. Check printer has paper
3. View Android logs for detailed errors

### Testing Connection

```kotlin
// Test printer connection directly in Android
val printerHelper = PrinterHelper(context)
val result = printerHelper.connectBluetooth("YOUR_MAC_ADDRESS")
```

## 🏗️ Project Structure

```
afm-pos-printer-fixed/
├── app/
│   ├── build.gradle.kts          # App build configuration
│   ├── libs/
│   │   └── printer-lib-3_4_4.aar # D-CODE printer library
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # App permissions & config
│   │   ├── kotlin/com/achyutamfruitam/pos/
│   │   │   ├── MainActivity.kt       # WebView & JS bridge
│   │   │   └── PrinterHelper.kt      # Printer operations
│   │   └── res/
│   │       ├── layout/
│   │       │   └── activity_main.xml # WebView layout
│   │       └── values/
│   │           └── strings.xml
│   └── proguard-rules.pro        # ProGuard config
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Project settings
├── gradle.properties             # Gradle properties
└── REACT_INTEGRATION_GUIDE.md    # React integration guide
```

## 📝 JavaScript Bridge API

### Methods

```javascript
// Connect via Bluetooth
window.AndroidPrinter.connectBluetooth("00:11:22:33:44:55")

// Connect via USB
window.AndroidPrinter.connectUSB()

// Print bill
window.AndroidPrinter.printBill(JSON.stringify(billData))

// Disconnect
window.AndroidPrinter.disconnect()

// Check connection
window.AndroidPrinter.isConnected() // Returns "true" or "false"

// Get paired devices
window.AndroidPrinter.showPairedDevices()
```

### Callbacks

```javascript
// Connection result
window.onPrinterConnected = (data) => {
  console.log(data.status, data.message);
};

// Print result
window.onPrintComplete = (data) => {
  console.log(data.status, data.message);
};

// Disconnection
window.onPrinterDisconnected = (data) => {
  console.log(data.status);
};

// Device list
window.onDeviceList = (data) => {
  console.log(data.devices); // Array of {name, address}
};
```

## 🔐 Security Notes

- For production, use HTTPS for your PWA
- Configure proper ProGuard rules
- Sign the release APK with your keystore
- Test thoroughly before deployment

## 📦 Release Build

For production release:

1. Create keystore:
```bash
keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

2. Update `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("my-release-key.keystore")
            storePassword = "your-password"
            keyAlias = "my-key-alias"
            keyPassword = "your-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
        }
    }
}
```

3. Build:
```bash
./gradlew assembleRelease
```

## 🤝 Support

For issues or questions:
1. Check the troubleshooting section
2. Review Android logs: `adb logcat`
3. Test printer with official D-CODE app first
4. Verify React integration is correct

## 📄 License

MIT License - Free to use and modify

## 🎉 Credits

- D-CODE for the printer SDK
- Achyutam Fruitam team for the use case

---

**Note**: Remember to update the PWA_URL before building!
