package com.achyutamfruitam.pos

import android.content.Context
import android.util.Log
import net.posprinter.POSConnect
import net.posprinter.POSPrinter
import net.posprinter.IConnectListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PrinterHelper(private val context: Context) {
    
    private var posConnect: POSConnect? = null
    private var posPrinter: POSPrinter? = null
    private val TAG = "PrinterHelper"

    /**
     * Connect to D-CODE printer via Bluetooth
     * @param macAddress: Bluetooth MAC address of the printer (e.g., "00:11:22:33:44:55")
     */
    suspend fun connectBluetooth(macAddress: String): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            Log.d(TAG, "Attempting to connect to Bluetooth printer: $macAddress")
            
            posConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)
            
            posConnect?.connect(macAddress, object : IConnectListener {
                override fun onStatus(code: Int, msg: String?) {
                    Log.d(TAG, "Connection status: code=$code, msg=$msg")
                    
                    when (code) {
                        POSConnect.CONNECT_SUCCESS -> {
                            Log.d(TAG, "Connected successfully")
                            posPrinter = POSPrinter(posConnect)
                            if (continuation.isActive) {
                                continuation.resume(Result.success("Connected successfully"))
                            }
                        }
                        POSConnect.CONNECT_FAIL -> {
                            Log.e(TAG, "Connection failed: $msg")
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("Connection failed: $msg")))
                            }
                        }
                        POSConnect.CONNECT_INTERRUPT -> {
                            Log.e(TAG, "Connection interrupted: $msg")
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("Connection interrupted: $msg")))
                            }
                        }
                        else -> {
                            Log.d(TAG, "Unknown status code: $code")
                        }
                    }
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to printer", e)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Connect to D-CODE printer via USB
     */
    suspend fun connectUSB(): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            Log.d(TAG, "Attempting to connect to USB printer")
            
            posConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_USB)
            
            posConnect?.connect(null, object : IConnectListener {
                override fun onStatus(code: Int, msg: String?) {
                    Log.d(TAG, "USB Connection status: code=$code, msg=$msg")
                    
                    when (code) {
                        POSConnect.CONNECT_SUCCESS -> {
                            Log.d(TAG, "USB Connected successfully")
                            posPrinter = POSPrinter(posConnect)
                            if (continuation.isActive) {
                                continuation.resume(Result.success("USB Connected successfully"))
                            }
                        }
                        POSConnect.CONNECT_FAIL -> {
                            Log.e(TAG, "USB Connection failed: $msg")
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("USB Connection failed: $msg")))
                            }
                        }
                        else -> {
                            Log.d(TAG, "Unknown USB status code: $code")
                        }
                    }
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to USB printer", e)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Print bill receipt
     */
    fun printBill(
        billNo: String,
        customerName: String,
        customerMobile: String?,
        dateStr: String,
        timeStr: String,
        lineItems: List<LineItem>,
        paymentMethod: String,
        remarks: String?,
        grandTotal: Double
    ): Result<String> {
        return try {
            if (posPrinter == null) {
                return Result.failure(Exception("Printer not connected"))
            }

            Log.d(TAG, "Starting print job for bill #$billNo")

            // Initialize printer
            posPrinter?.initializePrinter()
            
            // Print header
            posPrinter?.setAlignment(1) // Center align
            posPrinter?.setFontSize(1) // Large font
            posPrinter?.printString("ACHYUTAM FRUITAM\n")
            posPrinter?.setFontSize(0) // Normal font
            posPrinter?.printString("Ice Cream & Desserts\n")
            posPrinter?.printString("--------------------------------\n")
            
            // Print bill info
            posPrinter?.setAlignment(0) // Left align
            posPrinter?.printString("Bill No: #$billNo\n")
            posPrinter?.printString("Date: $dateStr\n")
            posPrinter?.printString("Time: $timeStr\n")
            posPrinter?.printString("Customer: $customerName\n")
            if (!customerMobile.isNullOrEmpty()) {
                posPrinter?.printString("Mobile: $customerMobile\n")
            }
            posPrinter?.printString("--------------------------------\n")
            
            // Print items header
            posPrinter?.printString("Item                 Qty  Price\n")
            posPrinter?.printString("--------------------------------\n")
            
            // Print line items
            lineItems.forEach { item ->
                val itemName = item.productName.take(20).padEnd(20)
                val qty = item.quantity.toString().padStart(3)
                val price = "₹${String.format("%.2f", item.price * item.quantity)}".padStart(8)
                posPrinter?.printString("$itemName$qty $price\n")
                
                // Print mix dish ingredients if any
                if (item.isMixDish && item.ingredients.isNotEmpty()) {
                    val ingredientStr = "  (Mix: ${item.ingredients.joinToString(", ")})"
                    posPrinter?.printString("$ingredientStr\n")
                }
            }
            
            posPrinter?.printString("--------------------------------\n")
            
            // Print totals
            posPrinter?.printString("Payment: $paymentMethod\n")
            if (!remarks.isNullOrEmpty()) {
                posPrinter?.printString("Note: $remarks\n")
            }
            posPrinter?.setFontSize(1) // Large font
            posPrinter?.printString("TOTAL: ₹${String.format("%.2f", grandTotal)}\n")
            posPrinter?.setFontSize(0) // Normal font
            posPrinter?.printString("--------------------------------\n")
            
            // Print footer
            posPrinter?.setAlignment(1) // Center align
            posPrinter?.printString("Thank You! Visit Again!\n")
            posPrinter?.printString("Fresh • Delicious • Premium\n")
            
            // Feed paper and cut
            posPrinter?.feedLine(3)
            posPrinter?.cutPaper()
            
            Log.d(TAG, "Print job completed successfully")
            Result.success("Print successful")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error printing bill", e)
            Result.failure(e)
        }
    }

    /**
     * Disconnect from printer
     */
    fun disconnect() {
        try {
            posConnect?.close()
            posConnect = null
            posPrinter = null
            Log.d(TAG, "Disconnected from printer")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }

    /**
     * Check if printer is connected
     */
    fun isConnected(): Boolean {
        return posConnect != null && posPrinter != null
    }
}

/**
 * Data class for line items
 */
data class LineItem(
    val productName: String,
    val quantity: Double,
    val price: Double,
    val isMixDish: Boolean = false,
    val ingredients: List<String> = emptyList()
)
