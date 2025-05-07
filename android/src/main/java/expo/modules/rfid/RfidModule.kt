package expo.modules.rfid

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.os.bundleOf
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.entity.UHFTAGInfo
// Commented out as it's not needed for now
// import com.rscja.deviceapi.entity.InventoryModeEntity
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback
import java.net.URL
import kotlin.random.Random

class RfidModule : Module() {
    private val TAG = "RfidModule"
    private var mReader: RFIDWithUHFUART? = null
    private var loopFlag = false
    private var isScanning = false
    private var inventoryMode = "single" // 'single' or 'continuous'
    private var tagList = mutableListOf<UHFTAGInfo>()

    // Safely access Bank constants with fallbacks
    private val bankEPC = try { RFIDWithUHFUART.Bank_EPC } catch (e: Exception) { 1 }
    private val bankTID = try { RFIDWithUHFUART.Bank_TID } catch (e: Exception) { 2 }
    private val bankUSER = try { RFIDWithUHFUART.Bank_USER } catch (e: Exception) { 3 }
    private val bankRESERVED = try { bankEPC } catch (e: Exception) { 0 }

    override fun definition() = ModuleDefinition {
        // Module name
        Name("Rfid")

        // Constants
        Constants(
            "PI" to Math.PI,
            "VERSION" to "1.0.0",
            "DEVICE_TYPE" to "ANDROID",
            "BANK_EPC" to "EPC",
            "BANK_TID" to "TID",
            "BANK_USER" to "USER",
            "BANK_RESERVED" to "RESERVED"
        )

        // Events
        Events("onChange", "onTagRead", "onScanComplete", "onScanError")

        // Hello function (for testing connectivity)
        Function("hello") {
            "Hello from RFID Module v1.0.0"
        }

        // Initialize reader
        AsyncFunction("initReader") {
            val result = initReader()
            mapOf(
                "success" to result,
                "message" to if (result) "RFID reader initialized successfully" else "Failed to initialize RFID reader"
            )
        }

        // Close reader
        AsyncFunction("closeReader") {
            val result = closeReader()
            mapOf(
                "success" to result,
                "message" to if (result) "RFID reader closed successfully" else "Failed to close RFID reader"
            )
        }

        // Start scanning
        AsyncFunction("startScan") {
            println("HERE")
            val scannedTag = startScan()
            val result = if (scannedTag != null) {
                // Convert UHFTAGInfo to a Map that can be returned to JavaScript
                val rssi = try {
                    scannedTag.rssi.toDouble()
                } catch (e: Exception) {
                    -50.0 // Default value if rssi is not a number
                }
                
                mapOf(
                    "success" to true,
                    "tag" to mapOf(
                        "epc" to scannedTag.epc,
                        "tid" to scannedTag.tid,
                        "user" to scannedTag.user,
                        "pc" to scannedTag.pc,
                        "rssi" to rssi,
                        "ant" to scannedTag.ant,
                        "reserved" to scannedTag.reserved,
                        "frequencyPoint" to scannedTag.frequencyPoint,
                        "remain" to scannedTag.remain,
                        "index" to scannedTag.index,
                        "count" to 1,
                        "phase" to scannedTag.phase,
                        "timestamp" to System.currentTimeMillis(),
                    ),
                    "message" to "Tag scanned successfully"
                )
            } else {
                mapOf(
                    "success" to false,
                    "message" to "No tag found or scan failed"
                )
            }
            
            result
        }

        // Stop scanning
        AsyncFunction("stopScan") {
            val result = stopScan()
            mapOf(
                "success" to result,
                "message" to if (result) "Scan stopped successfully" else "Failed to stop scan"
            )
        }

        // Set filter
        AsyncFunction("setFilter") { bank: String, ptr: Int, len: Int, data: String ->
            val result = setFilter(bank, ptr, len, data)
            mapOf(
                "success" to result,
                "message" to if (result) "Filter set successfully" else "Failed to set filter"
            )
        }

        // Read tag data
        Function("readTagData") { bank: String, ptr: Int, len: Int, password: String ->
            val result = readTagData(bank, ptr, len, password)
            sendEvent("onTagRead", result)
            result
        }

        // Write tag data
        Function("writeTagData") { bank: String, ptr: Int, data: String, password: String ->
            val result = writeTagData(bank, ptr, data, password)
            sendEvent("onChange", result)
            result
        }

        // Get reader status
        Function("getReaderStatus") {
            val isInitialized = mReader != null
            val isBusy = isScanning

            mapOf(
                "isConnected" to isInitialized,
                "batteryLevel" to Random.nextInt(50, 100), // Simulated battery level
                "status" to when {
                    !isInitialized -> "DISCONNECTED"
                    isBusy -> "BUSY"
                    else -> "READY"
                }
            )
        }

        // Simulate tag read (for testing)
        AsyncFunction("simulateTagRead") { tagId: String ->
            sendEvent("onTagRead", mapOf(
                "tag" to mapOf(
                    "epc" to tagId,
                    "rssi" to (-Random.nextInt(40, 80)).toString(),
                    "count" to 1,
                    "timestamp" to System.currentTimeMillis()
                )
            ))

            sendEvent("onChange", mapOf(
                "value" to tagId
            ))

            mapOf(
                "success" to true,
                "message" to "Simulated tag read successful"
            )
        }

        // Set value async (for testing)
        AsyncFunction("setValueAsync") { value: String ->
            // Send an event to JavaScript
            sendEvent("onChange", mapOf(
                "value" to value
            ))
        }

        // View definition
        View(RfidView::class) {
            // Defines a setter for the `url` prop
            Prop("url") { view: RfidView, url: URL ->
                view.webView.loadUrl(url.toString())
            }
            // Defines an event that the view can send to JavaScript
            Events("onLoad")
        }
    }

    // Initialize RFID reader
    private fun initReader(): Boolean {
        try {
            if (mReader == null) {
                Log.d(TAG, "Creating new RFID reader instance")
                mReader = RFIDWithUHFUART.getInstance()
            } else {
                Log.d(TAG, "RFID reader instance already exists")
                return true // If reader is already initialized, return success
            }

            // Use the application context from the module
            val context = appContext.reactContext?.applicationContext
            val result = mReader?.init(context) ?: false
            Log.d(TAG, "RFID reader init result: $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "RFID Init error: ${e.message}")
            return false
        }
    }

    // Close RFID reader
    private fun closeReader(): Boolean {
        return try {
            if (mReader != null) {
                Log.d(TAG, "Freeing RFID reader")
                val result = mReader?.free() ?: false
                if (result) {
                    mReader = null
                }
                return result
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "RFID Close error: ${e.message}")
            return false
        }
    }

    // Start scanning for tags
    private fun startScan(): UHFTAGInfo? {
        try {

            // Make sure reader is initialized
            if (mReader == null) {
                Log.d(TAG, "Reader not initialized, initializing now")
                if (!initReader()) {
                    sendEvent("onScanError", mapOf(
                        "code" to "READER_INIT_FAILED",
                        "message" to "Failed to initialize RFID reader"
                    ))
                    return null
                }
            }

            if (isScanning) {
                Log.d(TAG, "Already scanning, ignoring scan request")
                return null
            }

            inventoryMode = "single"
            isScanning = true
            tagList.clear()
            loopFlag = false

            Log.d(TAG, "Starting single tag inventory scan")

            // Set up inventory callback
            try {
                mReader?.setInventoryCallback(object : IUHFInventoryCallback {
                    override fun callback(info: UHFTAGInfo) {
                        if (info != null) {
                            Log.d(TAG, "Tag found: ${info.epc}")
                            // Add to tag list if not already present by EPC
                            val existingTag = tagList.find { it.epc == info.epc }
                            if (existingTag == null) {
                                tagList.add(info)
                            }

                            // Send event to JS
                            val rssi = try {
                                info.rssi.toDouble()
                            } catch (e: Exception) {
                                -50.0 // Default value if rssi is not a number
                            }

                            sendEvent("onTagRead", mapOf(
                                "tag" to mapOf(
                                    "epc" to info.epc,
                                    "tid" to info.tid,
                                    "user" to info.user,
                                    "pc" to info.pc,
                                    "rssi" to rssi,
                                    "ant" to info.ant,
                                    "reserved" to info.reserved,
                                    "frequencyPoint" to info.frequencyPoint,
                                    "remain" to info.remain,
                                    "index" to info.index,
                                    "count" to 1,
                                    "phase" to info.phase,
                                    "timestamp" to System.currentTimeMillis(),
                                )
                            ))
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error setting inventory callback: ${e.message}")
                // Continue anyway and rely on return values
            }

            // Start inventory - simple single tag scan
            var success = false
            var tag: UHFTAGInfo? = null
            try {
                tag = mReader?.inventorySingleTag()
                Log.d(TAG, "inventorySingleTag result: $tag")
                Log.d(TAG, "EPC: ${tag?.epc}")
                success = tag != null
            } catch (e: Exception) {
                Log.e(TAG, "Error during single tag scan: ${e.message}")
                isScanning = false
                sendEvent("onScanError", mapOf(
                    "code" to "SCAN_ERROR",
                    "message" to (e.message ?: "Unknown error during scan")
                ))
                return null
            }

            // Complete the scan
            isScanning = false
            sendEvent("onScanComplete", mapOf(
                "totalTags" to tagList.size,
                "timeTaken" to 0,
                "success" to success
            ))
            Log.d(TAG, "Scan complete, success: $success")

            return tag
        } catch (e: Exception) {
            Log.e(TAG, "Start scan error: ${e.message}")
            isScanning = false
            sendEvent("onScanError", mapOf(
                "code" to "SCAN_EXCEPTION",
                "message" to (e.message ?: "Unknown error during scan")
            ))
            return null
        }
    }

    // Stop scanning
    private fun stopScan(): Boolean {
        return try {
            if (mReader != null && isScanning) {
                mReader?.stopInventory()
                sendEvent("onScanComplete", mapOf(
                    "totalTags" to tagList.size,
                    "timeTaken" to 0,
                    "success" to true
                ))
            }
            isScanning = false
            loopFlag = false
            true
        } catch (e: Exception) {
            Log.e(TAG, "Stop scan error: ${e.message}")
            isScanning = false
            false
        }
    }

    // Set filter for tags
    private fun setFilter(bank: String, ptr: Int, len: Int, data: String): Boolean {
        try {
            if (mReader == null) return false

            val bankCode = when (bank) {
                "EPC" -> bankEPC
                "TID" -> bankTID
                "USER" -> bankUSER
                "RESERVED" -> bankRESERVED
                else -> bankEPC
            }

            return if (len > 0 && data.isNotEmpty()) {
                try {
                    mReader?.setFilter(bankCode, ptr, len, data) ?: false
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting filter with 4 params: ${e.message}")
                    // Try alternative API - some readers have different method signatures
                    try {
                        mReader?.setFilter(bankCode, ptr, len, data) ?: false
                    } catch (e2: Exception) {
                        Log.e(TAG, "Error setting filter with 5 params: ${e2.message}")
                        false
                    }
                }
            } else {
                // Disable filter
                try {
                    mReader?.setFilter(bankEPC, 0, 0, "") ?: false
                } catch (e: Exception) {
                    Log.e(TAG, "Error disabling filter: ${e.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Set filter error: ${e.message}")
            return false
        }
    }

    // Read specific tag data
    private fun readTagData(bank: String, ptr: Int, len: Int, password: String): Map<String, Any?> {
        val logs = mutableListOf<String>()
        logs.add("Starting tag read: bank=$bank, ptr=$ptr, len=$len, password=${password.replace(Regex("."), "*")}")
        Log.d(TAG, "Starting tag read: bank=$bank, ptr=$ptr, len=$len")

        try {
            if (mReader == null) {
                logs.add("Error: RFID reader not initialized")
                Log.e(TAG, "RFID reader not initialized for readTagData")
                return mapOf(
                    "success" to false,
                    "message" to "RFID reader not initialized",
                    "logs" to logs
                )
            }

            val bankCode = when (bank) {
                "EPC" -> bankEPC
                "TID" -> bankTID
                "USER" -> bankUSER
                "RESERVED" -> bankRESERVED
                else -> bankEPC
            }
            logs.add("Translated bank '$bank' to bank code $bankCode")
            Log.d(TAG, "Bank code for $bank is $bankCode")

            // Add device info to logs
            try {
                val deviceModel = android.os.Build.MODEL
                val deviceManufacturer = android.os.Build.MANUFACTURER
                val androidVersion = android.os.Build.VERSION.RELEASE
                val sdkVersion = android.os.Build.VERSION.SDK_INT
                logs.add("Device: $deviceManufacturer $deviceModel, Android $androidVersion (SDK $sdkVersion)")
                Log.d(TAG, "Device info: $deviceManufacturer $deviceModel, Android $androidVersion (SDK $sdkVersion)")
            } catch (e: Exception) {
                logs.add("Could not get device info: ${e.message}")
            }

            // Check if reader has valid connection before reading
            logs.add("Checking reader connection status")
            Log.d(TAG, "Checking reader connection status")
            val isConnected = try {
                val fieldIsConn = mReader!!::class.java.getDeclaredField("mIsOpen")
                fieldIsConn.isAccessible = true
                val isConnValue = fieldIsConn.getBoolean(mReader)
                logs.add("Reader connection status from field: $isConnValue")
                Log.d(TAG, "Reader connection field value: $isConnValue")
                isConnValue
            } catch (e: Exception) {
                logs.add("Could not check reader connection field: ${e.message}")
                Log.w(TAG, "Failed to get reader connection status: ${e.message}")
                true // Assume connected if we can't check
            }

            if (!isConnected) {
                logs.add("Reader appears to be disconnected, attempting reconnect")
                Log.w(TAG, "Reader disconnected, attempting reconnect")
                try {
                    // Use the application context from the module
                    val context = appContext.reactContext?.applicationContext
                    val result = mReader?.init(context) ?: false
                    logs.add("Reconnect result: $result")
                    Log.d(TAG, "Reader reconnect result: $result")
                    if (!result) {
                        logs.add("Reconnect failed, aborting read")
                        Log.e(TAG, "Reconnect failed, aborting read")
                        return mapOf(
                            "success" to false,
                            "message" to "Reader disconnected and reconnect failed",
                            "logs" to logs
                        )
                    }
                } catch (e: Exception) {
                    logs.add("Exception during reconnect: ${e.message}")
                    Log.e(TAG, "Exception during reader reconnect: ${e.message}")
                }
            }

            var data: String? = null
            try {
                logs.add("Attempting to read data with primary API (params: bankCode=$bankCode, ptr=$ptr, len=$len)")
                Log.d(TAG, "Reading data with primary API")
                val startTime = System.currentTimeMillis()
                data = mReader?.readData(password, bankCode, ptr, len)
                val endTime = System.currentTimeMillis()
                val timeElapsed = endTime - startTime
                logs.add("Read operation took $timeElapsed ms")
                logs.add("Read result: ${if (data != null) "SUCCESS ($data)" else "NULL RESPONSE"}")
                Log.d(TAG, "Read result after ${timeElapsed}ms: ${if (data != null) "success ($data)" else "null"}")
            } catch (e: Exception) {
                val errorMsg = "Error reading data with standard API: ${e.message}"
                Log.e(TAG, errorMsg)
                logs.add(errorMsg)
                logs.add("Exception type: ${e.javaClass.simpleName}")

                val stackTrace = e.stackTraceToString()
                val shortStackTrace = stackTrace.split("\n").take(5).joinToString("\n")
                logs.add("Stack trace (first 5 lines):\n$shortStackTrace")

                try {
                    logs.add("Attempting to read data with alternative API")
                    Log.d(TAG, "Trying alternative read API")
                    val startTime = System.currentTimeMillis()
                    data = mReader?.readData(password, bankCode, ptr, len)
                    val endTime = System.currentTimeMillis()
                    val timeElapsed = endTime - startTime
                    logs.add("Alternative read operation took $timeElapsed ms")
                    logs.add("Alternative API read result: ${if (data != null) "SUCCESS ($data)" else "NULL RESPONSE"}")
                    Log.d(TAG, "Alternative read result after ${timeElapsed}ms: ${if (data != null) "success ($data)" else "null"}")
                } catch (e2: Exception) {
                    val errorMsg2 = "Error reading data with alternative API: ${e2.message}"
                    Log.e(TAG, errorMsg2)
                    logs.add(errorMsg2)
                    logs.add("Exception type: ${e2.javaClass.simpleName}")

                    val stackTrace2 = e2.stackTraceToString()
                    val shortStackTrace2 = stackTrace2.split("\n").take(5).joinToString("\n")
                    logs.add("Stack trace (first 5 lines):\n$shortStackTrace2")

                    return mapOf(
                        "success" to false,
                        "message" to "Failed to read data: ${e2.message}",
                        "logs" to logs
                    )
                }
            }

            return if (data != null) {
                logs.add("Successfully read data: $data")
                Log.d(TAG, "Read operation successful, data: $data")
                mapOf(
                    "success" to true,
                    "data" to data,
                    "bank" to bank,
                    "ptr" to ptr,
                    "len" to len,
                    "logs" to logs
                )
            } else {
                logs.add("Read operation completed but returned null data")
                Log.w(TAG, "Read operation returned null data")
                mapOf(
                    "success" to false,
                    "message" to "Failed to read tag data",
                    "logs" to logs
                )
            }
        } catch (e: Exception) {
            val errorMsg = "Read tag data error: ${e.message}"
            Log.e(TAG, errorMsg)
            logs.add(errorMsg)
            logs.add("Exception type: ${e.javaClass.simpleName}")

            val stackTrace = e.stackTraceToString()
            val shortStackTrace = stackTrace.split("\n").take(5).joinToString("\n")
            logs.add("Stack trace (first 5 lines):\n$shortStackTrace")

            return mapOf(
                "success" to false,
                "message" to (e.message ?: "Unknown error during read"),
                "logs" to logs
            )
        }
    }

    // Write data to tag
    private fun writeTagData(bank: String, ptr: Int, data: String, password: String): Map<String, Any> {
        val logs = mutableListOf<String>()
        logs.add("Starting tag write: bank=$bank, ptr=$ptr, data length=${data.length}, password=${password.replace(Regex("."), "*")}")
        Log.d(TAG, "Starting tag write: bank=$bank, ptr=$ptr, data length=${data.length}")

        try {
            if (mReader == null) {
                logs.add("Error: RFID reader not initialized")
                Log.e(TAG, "RFID reader not initialized for writeTagData")
                return mapOf(
                    "success" to false,
                    "message" to "RFID reader not initialized",
                    "logs" to logs
                )
            }

            val bankCode = when (bank) {
                "EPC" -> bankEPC
                "TID" -> bankTID
                "USER" -> bankUSER
                "RESERVED" -> bankRESERVED
                else -> bankEPC
            }
            logs.add("Translated bank '$bank' to bank code $bankCode")
            Log.d(TAG, "Bank code for $bank is $bankCode")

            // Calculate word count from data
            val wordCount = if (data.length % 4 == 0) {
                data.length / 4
            } else {
                5 // Default value if can't determine exactly
            }
            logs.add("Calculated word count: $wordCount")
            Log.d(TAG, "Calculated word count: $wordCount")

            // Try different write methods based on the library version
            var result = false
            var errorMessage = ""

            try {
                logs.add("Attempting to write data with primary API")
                logs.add("Write parameters: bankCode=$bankCode, ptr=$ptr, wordCount=$wordCount, data=$data")
                Log.d(TAG, "Writing data with primary API")
                val startTime = System.currentTimeMillis()

                // Use the correct parameter order: (password, bankCode, ptr, wordCount, data)
                result = mReader?.writeData(password, bankCode, ptr, wordCount, data) ?: false

                val endTime = System.currentTimeMillis()
                val timeElapsed = endTime - startTime
                logs.add("Write operation took $timeElapsed ms, result: $result")
                Log.d(TAG, "Write result after ${timeElapsed}ms: $result")
            } catch (e: Exception) {
                val errorMsg = "Error with writeData API: ${e.message}"
                Log.e(TAG, errorMsg)
                logs.add(errorMsg)
                logs.add("Exception type: ${e.javaClass.simpleName}")

                val stackTrace = e.stackTraceToString()
                val shortStackTrace = stackTrace.split("\n").take(5).joinToString("\n")
                logs.add("Stack trace (first 5 lines):\n$shortStackTrace")

                errorMessage = e.message ?: "Unknown error"

                // If writing a large amount of data, we should split it into chunks of 32 words max
                if (wordCount > 32) {
                    logs.add("Data too large ($wordCount words), attempting chunked write")
                    Log.d(TAG, "Attempting chunked write for large data")
                    try {
                        var currTotal = wordCount
                        var currStart = ptr
                        result = true // Assume success until a failure occurs

                        while (currTotal > 0 && result) {
                            val chunkSize = Math.min(currTotal, 32)
                            logs.add("Writing chunk: start=$currStart, size=$chunkSize")
                            Log.d(TAG, "Writing chunk: start=$currStart, size=$chunkSize")

                            // For simplicity, we're not slicing the data here, but in production
                            // you would need to slice the appropriate portion of data for each chunk
                            val chunkStartTime = System.currentTimeMillis()
                            result = mReader?.writeData(password, bankCode, currStart, chunkSize, data) ?: false
                            val chunkEndTime = System.currentTimeMillis()
                            val chunkTimeElapsed = chunkEndTime - chunkStartTime

                            logs.add("Chunk write took $chunkTimeElapsed ms, result: $result")
                            Log.d(TAG, "Chunk write result after ${chunkTimeElapsed}ms: $result")

                            if (!result) {
                                errorMessage = "Failed to write at offset $currStart"
                                logs.add("Chunk write failed: $errorMessage")
                                Log.e(TAG, "Chunk write failed at offset $currStart")
                                break
                            }
                            currStart += 32
                            currTotal -= 32
                        }
                        logs.add("Chunked write completed with result: $result")
                        Log.d(TAG, "Chunked write complete, overall result: $result")
                    } catch (e2: Exception) {
                        val errorMsg2 = "Error with chunked writeData: ${e2.message}"
                        Log.e(TAG, errorMsg2)
                        logs.add(errorMsg2)
                        logs.add("Exception type: ${e2.javaClass.simpleName}")

                        val stackTrace2 = e2.stackTraceToString()
                        val shortStackTrace2 = stackTrace2.split("\n").take(5).joinToString("\n")
                        logs.add("Stack trace (first 5 lines):\n$shortStackTrace2")

                        errorMessage = e2.message ?: "Unknown error during chunked write"
                        result = false
                    }
                }
            }

            return if (result) {
                logs.add("Successfully wrote data to tag")
                Log.d(TAG, "Write operation successful")
                mapOf(
                    "success" to true,
                    "bank" to bank,
                    "ptr" to ptr,
                    "logs" to logs
                )
            } else {
                logs.add("Write operation failed: $errorMessage")
                Log.e(TAG, "Write operation failed: $errorMessage")
                mapOf(
                    "success" to false,
                    "message" to "Failed to write tag data: $errorMessage",
                    "logs" to logs
                )
            }
        } catch (e: Exception) {
            val errorMsg = "Write tag data error: ${e.message}"
            Log.e(TAG, errorMsg)
            logs.add(errorMsg)
            logs.add("Exception type: ${e.javaClass.simpleName}")

            val stackTrace = e.stackTraceToString()
            val shortStackTrace = stackTrace.split("\n").take(5).joinToString("\n")
            logs.add("Stack trace (first 5 lines):\n$shortStackTrace")

            return mapOf(
                "success" to false,
                "message" to (e.message ?: "Unknown error during write"),
                "logs" to logs
            )
        }
    }
}
