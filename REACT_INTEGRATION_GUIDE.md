# React PWA Integration Guide

## Overview
This guide shows how to integrate your React PWA with the Android printer bridge app.

## 1. Create a Printer Service in Your React App

Create a new file: `src/services/androidPrinterService.ts`

```typescript
// Type definitions for the Android bridge
interface AndroidPrinter {
  connectBluetooth(macAddress: string): void;
  connectUSB(): void;
  printBill(jsonData: string): void;
  disconnect(): void;
  isConnected(): string; // Returns "true" or "false"
  showPairedDevices(): void;
}

// Extend window object
declare global {
  interface Window {
    AndroidPrinter?: AndroidPrinter;
    onPrinterConnected?: (data: { status: string; message: string }) => void;
    onPrintComplete?: (data: { status: string; message: string }) => void;
    onPrinterDisconnected?: (data: { status: string }) => void;
    onDeviceList?: (data: { devices: Array<{ name: string; address: string }> }) => void;
  }
}

class AndroidPrinterService {
  private isAndroid: boolean;

  constructor() {
    // Check if running in Android WebView
    this.isAndroid = typeof window.AndroidPrinter !== 'undefined';
  }

  /**
   * Check if printer bridge is available
   */
  isAvailable(): boolean {
    return this.isAndroid;
  }

  /**
   * Connect to Bluetooth printer
   */
  connectBluetooth(macAddress: string): Promise<{ status: string; message: string }> {
    return new Promise((resolve, reject) => {
      if (!this.isAndroid) {
        reject(new Error('Not running in Android app'));
        return;
      }

      // Set up callback
      window.onPrinterConnected = (data) => {
        if (data.status === 'success') {
          resolve(data);
        } else {
          reject(new Error(data.message));
        }
        window.onPrinterConnected = undefined;
      };

      // Call Android method
      window.AndroidPrinter!.connectBluetooth(macAddress);

      // Timeout after 30 seconds
      setTimeout(() => {
        if (window.onPrinterConnected) {
          window.onPrinterConnected = undefined;
          reject(new Error('Connection timeout'));
        }
      }, 30000);
    });
  }

  /**
   * Connect to USB printer
   */
  connectUSB(): Promise<{ status: string; message: string }> {
    return new Promise((resolve, reject) => {
      if (!this.isAndroid) {
        reject(new Error('Not running in Android app'));
        return;
      }

      window.onPrinterConnected = (data) => {
        if (data.status === 'success') {
          resolve(data);
        } else {
          reject(new Error(data.message));
        }
        window.onPrinterConnected = undefined;
      };

      window.AndroidPrinter!.connectUSB();

      setTimeout(() => {
        if (window.onPrinterConnected) {
          window.onPrinterConnected = undefined;
          reject(new Error('Connection timeout'));
        }
      }, 30000);
    });
  }

  /**
   * Print bill
   */
  printBill(billData: BillPrintData): Promise<{ status: string; message: string }> {
    return new Promise((resolve, reject) => {
      if (!this.isAndroid) {
        reject(new Error('Not running in Android app'));
        return;
      }

      // Prepare bill data
      const now = new Date();
      const dateStr = now.toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
      });
      const timeStr = now.toLocaleTimeString("en-IN", {
        hour: "2-digit",
        minute: "2-digit",
        hour12: true,
      });

      const printData = {
        billNo: billData.billNo.toString(),
        customerName: billData.customerName || 'Guest',
        customerMobile: billData.customerMobile || null,
        date: dateStr,
        time: timeStr,
        lineItems: billData.lineItems.map(item => ({
          productName: item.product?.name || item.productName || 'Unknown',
          quantity: parseFloat(item.quantity.toString()) || 0,
          price: parseFloat(item.price.toString()) || 0,
          isMixDish: item.isMixDish || false,
          ingredients: item.ingredients?.map(ing => ing.name) || []
        })),
        paymentMethod: billData.paymentMethod || 'Cash',
        remarks: billData.remarks || null,
        grandTotal: parseFloat(billData.grandTotal.toString()) || 0
      };

      // Set up callback
      window.onPrintComplete = (data) => {
        if (data.status === 'success') {
          resolve(data);
        } else {
          reject(new Error(data.message));
        }
        window.onPrintComplete = undefined;
      };

      // Call Android method
      try {
        window.AndroidPrinter!.printBill(JSON.stringify(printData));
      } catch (error) {
        window.onPrintComplete = undefined;
        reject(error);
      }

      // Timeout after 30 seconds
      setTimeout(() => {
        if (window.onPrintComplete) {
          window.onPrintComplete = undefined;
          reject(new Error('Print timeout'));
        }
      }, 30000);
    });
  }

  /**
   * Disconnect from printer
   */
  disconnect(): void {
    if (this.isAndroid) {
      window.AndroidPrinter!.disconnect();
    }
  }

  /**
   * Check if printer is connected
   */
  isConnected(): boolean {
    if (!this.isAndroid) return false;
    return window.AndroidPrinter!.isConnected() === 'true';
  }

  /**
   * Get list of paired Bluetooth devices
   */
  getPairedDevices(): Promise<Array<{ name: string; address: string }>> {
    return new Promise((resolve, reject) => {
      if (!this.isAndroid) {
        reject(new Error('Not running in Android app'));
        return;
      }

      window.onDeviceList = (data) => {
        resolve(data.devices);
        window.onDeviceList = undefined;
      };

      window.AndroidPrinter!.showPairedDevices();

      setTimeout(() => {
        if (window.onDeviceList) {
          window.onDeviceList = undefined;
          reject(new Error('Timeout getting device list'));
        }
      }, 10000);
    });
  }
}

// Types
export interface BillPrintData {
  billNo: number;
  customerName?: string;
  customerMobile?: string;
  lineItems: Array<{
    product?: { name: string };
    productName?: string;
    quantity: number;
    price: number;
    isMixDish?: boolean;
    ingredients?: Array<{ name: string }>;
  }>;
  paymentMethod?: string;
  remarks?: string;
  grandTotal: number;
}

// Export singleton instance
export const androidPrinterService = new AndroidPrinterService();
```

## 2. Update Your Print Component

Modify your existing print component to use the Android printer:

```typescript
import { androidPrinterService } from '@/services/androidPrinterService';
import { generatePrintHTML } from './generatePrintHTML'; // Your existing function

// Add this to your component
const handleSaveBill = async () => {
  try {
    // Check if running in Android app
    if (androidPrinterService.isAvailable()) {
      // Check if printer is connected
      if (!androidPrinterService.isConnected()) {
        // Show connection dialog
        const shouldConnect = confirm('Printer not connected. Connect now?');
        if (shouldConnect) {
          await showPrinterConnectionDialog();
          return;
        }
      }

      // Print directly via Android
      await androidPrinterService.printBill({
        billNo: billNo,
        customerName: billData.customerName,
        customerMobile: billData.customerMobile,
        lineItems: billData.lineItems,
        paymentMethod: billData.paymentMethod,
        remarks: billData.remarks,
        grandTotal: billData.grandTotal
      });

      alert('Bill printed successfully!');
    } else {
      // Fallback to HTML print for web browser
      const htmlContent = generatePrintHTML(billNo, billData);
      const printWindow = window.open('', '_blank');
      if (printWindow) {
        printWindow.document.write(htmlContent);
        printWindow.document.close();
      }
    }
  } catch (error) {
    console.error('Print error:', error);
    alert(`Print failed: ${error.message}`);
  }
};

// Connection dialog
const showPrinterConnectionDialog = async () => {
  try {
    const devices = await androidPrinterService.getPairedDevices();
    
    // Show device selection UI (implement your own UI)
    // For now, using prompt as example
    const deviceList = devices.map((d, i) => `${i + 1}. ${d.name} (${d.address})`).join('\n');
    const selection = prompt(`Select printer:\n${deviceList}\n\nEnter number:`);
    
    if (selection) {
      const index = parseInt(selection) - 1;
      if (index >= 0 && index < devices.length) {
        await androidPrinterService.connectBluetooth(devices[index].address);
        alert('Connected to printer!');
      }
    }
  } catch (error) {
    console.error('Connection error:', error);
    alert(`Connection failed: ${error.message}`);
  }
};
```

## 3. Add a Settings Page for Printer Connection

Create a settings page where users can connect to the printer:

```typescript
import React, { useState, useEffect } from 'react';
import { androidPrinterService } from '@/services/androidPrinterService';

export function PrinterSettingsPage() {
  const [isConnected, setIsConnected] = useState(false);
  const [devices, setDevices] = useState<Array<{ name: string; address: string }>>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    checkConnection();
  }, []);

  const checkConnection = () => {
    setIsConnected(androidPrinterService.isConnected());
  };

  const loadDevices = async () => {
    try {
      setLoading(true);
      const pairedDevices = await androidPrinterService.getPairedDevices();
      setDevices(pairedDevices);
    } catch (error) {
      alert('Failed to load devices: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const connectToDevice = async (address: string) => {
    try {
      setLoading(true);
      await androidPrinterService.connectBluetooth(address);
      setIsConnected(true);
      alert('Connected successfully!');
    } catch (error) {
      alert('Connection failed: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const disconnect = () => {
    androidPrinterService.disconnect();
    setIsConnected(false);
  };

  if (!androidPrinterService.isAvailable()) {
    return (
      <div className="p-4">
        <p>Printer features are only available in the Android app.</p>
      </div>
    );
  }

  return (
    <div className="p-4">
      <h2 className="text-2xl font-bold mb-4">Printer Settings</h2>
      
      <div className="mb-4">
        <p>Status: {isConnected ? '✅ Connected' : '❌ Not Connected'}</p>
      </div>

      {isConnected ? (
        <button
          onClick={disconnect}
          className="bg-red-500 text-white px-4 py-2 rounded"
        >
          Disconnect
        </button>
      ) : (
        <>
          <button
            onClick={loadDevices}
            disabled={loading}
            className="bg-blue-500 text-white px-4 py-2 rounded mb-4"
          >
            {loading ? 'Loading...' : 'Scan for Printers'}
          </button>

          {devices.length > 0 && (
            <div className="mt-4">
              <h3 className="font-bold mb-2">Paired Devices:</h3>
              <div className="space-y-2">
                {devices.map((device) => (
                  <button
                    key={device.address}
                    onClick={() => connectToDevice(device.address)}
                    disabled={loading}
                    className="block w-full text-left p-3 border rounded hover:bg-gray-100"
                  >
                    <div className="font-semibold">{device.name}</div>
                    <div className="text-sm text-gray-600">{device.address}</div>
                  </button>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
```

## 4. Testing

1. Build the APK
2. Install on your Android device
3. Pair your D-CODE DC2M printer via Bluetooth settings
4. Open the app (it will load your PWA)
5. Go to printer settings and connect
6. Create a bill and save - it should print directly!

## Important Notes

- The app loads your PWA URL in a WebView
- Update `PWA_URL` in MainActivity.kt to your actual PWA URL
- For local testing, use your computer's local IP address
- The printer bridge is automatically available via `window.AndroidPrinter`
- All printer operations are asynchronous
- Always check if printer is connected before printing
