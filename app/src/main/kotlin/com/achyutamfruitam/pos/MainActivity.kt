package com.achyutamfruitam.pos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var printerHelper: PrinterHelper
    private val TAG = "MainActivity"
    
    // Your PWA URL - CHANGE THIS TO YOUR ACTUAL PWA URL
    private val PWA_URL = "https://v0-achyutamfruitam.vercel.app/pos"  // TODO: Update this
    // For testing locally: "http://10.0.2.2:3000" (Android emulator)
    // For testing with device: "http://192.168.x.x:3000" (your local IP)
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        printerHelper = PrinterHelper(this)
        
        // Request Bluetooth permissions
        requestBluetoothPermissions()
        
        setupWebView()
    }

    private fun setupWebView() {
        webView = findViewById(R.id.webView)
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // Enable debugging for WebView (remove in production)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        }

        // Add JavaScript interface for printer communication
        webView.addJavascriptInterface(PrinterBridge(), "AndroidPrinter")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page loaded: $url")
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "WebView error: ${error?.description}")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d(TAG, "Console: ${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                }
                return true
            }
        }

        // Load your PWA
        webView.loadUrl(PWA_URL)
    }

    /**
     * JavaScript Interface - This is called from your React app
     */
    inner class PrinterBridge {
        
        /**
         * Connect to Bluetooth printer
         * Call from React: window.AndroidPrinter.connectBluetooth("00:11:22:33:44:55")
         */
        @JavascriptInterface
        fun connectBluetooth(macAddress: String) {
            Log.d(TAG, "JS called connectBluetooth with address: $macAddress")
            
            CoroutineScope(Dispatchers.Main).launch {
                val result = withContext(Dispatchers.IO) {
                    printerHelper.connectBluetooth(macAddress)
                }
                
                result.fold(
                    onSuccess = { message ->
                        showToast(message)
                        notifyReact("onPrinterConnected", """{"status":"success","message":"$message"}""")
                    },
                    onFailure = { error ->
                        showToast("Connection failed: ${error.message}")
                        notifyReact("onPrinterConnected", """{"status":"error","message":"${error.message}"}""")
                    }
                )
            }
        }

        /**
         * Connect to USB printer
         * Call from React: window.AndroidPrinter.connectUSB()
         */
        @JavascriptInterface
        fun connectUSB() {
            Log.d(TAG, "JS called connectUSB")
            
            CoroutineScope(Dispatchers.Main).launch {
                val result = withContext(Dispatchers.IO) {
                    printerHelper.connectUSB()
                }
                
                result.fold(
                    onSuccess = { message ->
                        showToast(message)
                        notifyReact("onPrinterConnected", """{"status":"success","message":"$message"}""")
                    },
                    onFailure = { error ->
                        showToast("USB Connection failed: ${error.message}")
                        notifyReact("onPrinterConnected", """{"status":"error","message":"${error.message}"}""")
                    }
                )
            }
        }

        /**
         * Print bill - receives JSON string from React
         * Call from React: window.AndroidPrinter.printBill(JSON.stringify(billData))
         */
        @JavascriptInterface
        fun printBill(jsonData: String) {
            Log.d(TAG, "JS called printBill with data: $jsonData")
            
            try {
                val gson = Gson()
                val billData = gson.fromJson(jsonData, BillData::class.java)
                
                CoroutineScope(Dispatchers.Main).launch {
                    val result = withContext(Dispatchers.IO) {
                        printerHelper.printBill(
                            billNo = billData.billNo,
                            customerName = billData.customerName,
                            customerMobile = billData.customerMobile,
                            dateStr = billData.date,
                            timeStr = billData.time,
                            lineItems = billData.lineItems.map { item ->
                                LineItem(
                                    productName = item.productName,
                                    quantity = item.quantity,
                                    price = item.price,
                                    isMixDish = item.isMixDish ?: false,
                                    ingredients = item.ingredients ?: emptyList()
                                )
                            },
                            paymentMethod = billData.paymentMethod,
                            remarks = billData.remarks,
                            grandTotal = billData.grandTotal
                        )
                    }
                    
                    result.fold(
                        onSuccess = { message ->
                            showToast(message)
                            notifyReact("onPrintComplete", """{"status":"success","message":"$message"}""")
                        },
                        onFailure = { error ->
                            showToast("Print failed: ${error.message}")
                            notifyReact("onPrintComplete", """{"status":"error","message":"${error.message}"}""")
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing bill data", e)
                showToast("Error: ${e.message}")
                notifyReact("onPrintComplete", """{"status":"error","message":"${e.message}"}""")
            }
        }

        /**
         * Disconnect from printer
         * Call from React: window.AndroidPrinter.disconnect()
         */
        @JavascriptInterface
        fun disconnect() {
            Log.d(TAG, "JS called disconnect")
            printerHelper.disconnect()
            showToast("Disconnected from printer")
            notifyReact("onPrinterDisconnected", """{"status":"success"}""")
        }

        /**
         * Check if printer is connected
         * Call from React: window.AndroidPrinter.isConnected()
         * Returns: "true" or "false" as string
         */
        @JavascriptInterface
        fun isConnected(): String {
            return printerHelper.isConnected().toString()
        }

        /**
         * Show list of paired Bluetooth devices (helper function)
         * Call from React: window.AndroidPrinter.showPairedDevices()
         */
        @JavascriptInterface
        fun showPairedDevices() {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                showToast("Bluetooth permission not granted")
                return
            }

            try {
                val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                val pairedDevices = bluetoothAdapter?.bondedDevices

                if (pairedDevices.isNullOrEmpty()) {
                    showToast("No paired devices found")
                    return
                }

                val deviceList = pairedDevices.map { 
                    """{"name":"${it.name}","address":"${it.address}"}""" 
                }.joinToString(",")
                
                val json = """{"devices":[$deviceList]}"""
                notifyReact("onDeviceList", json)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting paired devices", e)
                showToast("Error: ${e.message}")
            }
        }
    }

    /**
     * Notify React app of events
     */
    private fun notifyReact(eventName: String, jsonData: String) {
        runOnUiThread {
            webView.evaluateJavascript(
                """
                if (typeof window.$eventName === 'function') {
                    window.$eventName($jsonData);
                }
                """.trimIndent(),
                null
            )
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        printerHelper.disconnect()
    }
}

/**
 * Data classes for JSON parsing
 */
data class BillData(
    @SerializedName("billNo") val billNo: String,
    @SerializedName("customerName") val customerName: String,
    @SerializedName("customerMobile") val customerMobile: String?,
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String,
    @SerializedName("lineItems") val lineItems: List<BillLineItem>,
    @SerializedName("paymentMethod") val paymentMethod: String,
    @SerializedName("remarks") val remarks: String?,
    @SerializedName("grandTotal") val grandTotal: Double
)

data class BillLineItem(
    @SerializedName("productName") val productName: String,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("price") val price: Double,
    @SerializedName("isMixDish") val isMixDish: Boolean?,
    @SerializedName("ingredients") val ingredients: List<String>?
)
