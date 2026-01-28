# 🚀 Quick Start Guide

## Get Started in 3 Steps!

### Step 1: Update Your PWA URL (2 minutes)

Open `app/src/main/kotlin/com/achyutamfruitam/pos/MainActivity.kt`

Find line ~21 and update:

```kotlin
private val PWA_URL = "https://your-actual-pwa-url.com"
```

**Examples:**
- Production: `"https://pos.achyutamfruitam.com"`
- Local dev (emulator): `"http://10.0.2.2:3000"`
- Local dev (real device): `"http://192.168.1.100:3000"` (your PC's IP)

### Step 2: Build the APK (5 minutes)

#### Using Android Studio (Easiest):
1. Open Android Studio
2. File → Open → Select `afm-pos-printer-fixed` folder
3. Wait for Gradle sync (first time takes ~5 min)
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. Click "locate" in the notification to find your APK

#### Using Command Line:
```bash
cd afm-pos-printer-fixed
chmod +x gradlew
./gradlew assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

### Step 3: Install and Test (5 minutes)

1. **Transfer APK to your phone**
   - USB: `adb install app/build/outputs/apk/debug/app-debug.apk`
   - Or email it to yourself and download on phone

2. **Pair your printer**
   - Go to Android Settings → Bluetooth
   - Pair with "D-CODE DC2M" or similar
   - Note the MAC address (like `00:11:22:33:44:55`)

3. **Run the app**
   - Open "AFM POS Printer" app
   - It will load your PWA
   - Grant Bluetooth permissions when asked

4. **Test printing**
   - In your PWA, connect to printer first
   - Create a test bill
   - Click save/print - it should print! 🎉

## 🔧 Integrate with Your React App

Add this to your React project:

### 1. Create printer service file

Create `src/services/androidPrinterService.ts` - see [REACT_INTEGRATION_GUIDE.md](REACT_INTEGRATION_GUIDE.md) for complete code.

### 2. Update your bill save function

```typescript
import { androidPrinterService } from '@/services/androidPrinterService';

const handleSaveBill = async () => {
  // Check if running in Android app
  if (androidPrinterService.isAvailable()) {
    try {
      // Print directly to D-CODE printer
      await androidPrinterService.printBill(billData);
      alert('✅ Bill printed successfully!');
    } catch (error) {
      alert('❌ Print failed: ' + error.message);
    }
  } else {
    // Fallback to browser print for web
    const html = generatePrintHTML(billNo, billData);
    const printWindow = window.open('', '_blank');
    printWindow.document.write(html);
    printWindow.document.close();
  }
};
```

### 3. Add a settings page

See [REACT_INTEGRATION_GUIDE.md](REACT_INTEGRATION_GUIDE.md) for complete printer settings UI code.

## ✅ Verification Checklist

After installation, verify:

- [ ] App launches successfully
- [ ] Your PWA loads in the WebView
- [ ] Bluetooth permissions are granted
- [ ] Can see paired Bluetooth devices
- [ ] Can connect to D-CODE printer
- [ ] Connection status shows "Connected"
- [ ] Test bill prints correctly
- [ ] Bill formatting looks good
- [ ] Can disconnect and reconnect

## 🐛 Common Issues

### "Build failed"
- Make sure you have JDK 17 installed
- Clean: `./gradlew clean` then rebuild

### "PWA not loading"
- Check PWA_URL is correct
- For local dev, make sure phone can reach your PC
- Check firewall allows connections

### "Printer not connecting"
- Pair in Bluetooth settings first
- Make sure printer is on
- Check MAC address is correct
- Try power cycling the printer

### "Print looks wrong"
- D-CODE DC2M uses 58mm paper width
- Check paper is loaded correctly
- Adjust formatting in PrinterHelper.kt if needed

## 📚 Next Steps

- **Production**: Create a signed release APK (see README.md)
- **Customization**: Modify bill format in `PrinterHelper.kt`
- **Features**: Add USB support, multiple printers, etc.
- **Distribution**: Upload to Google Play or distribute directly

## 💡 Pro Tips

1. **Keep printer paired**: Once paired, you don't need to pair again
2. **Test thoroughly**: Print several bills to ensure everything works
3. **Update regularly**: Keep your PWA URL updated if it changes
4. **Backup**: Keep the source code safe for future updates
5. **Monitor logs**: Use `adb logcat` to debug issues

## 🎯 Success Indicators

You'll know it's working when:
- ✅ App loads your PWA without errors
- ✅ Bluetooth icon shows in status bar when connected
- ✅ Toast shows "Connected successfully"
- ✅ Print button actually prints to thermal printer
- ✅ Bills come out formatted correctly
- ✅ No need to use 4barcode app anymore!

## 🆘 Need Help?

1. Check [README.md](README.md) for detailed documentation
2. Review [BUILD_FIX_EXPLANATION.md](BUILD_FIX_EXPLANATION.md) for technical details
3. See [REACT_INTEGRATION_GUIDE.md](REACT_INTEGRATION_GUIDE.md) for React integration
4. Check Android logs: `adb logcat | grep "MainActivity\|PrinterHelper"`

---

**Ready to print? Let's go! 🚀**
