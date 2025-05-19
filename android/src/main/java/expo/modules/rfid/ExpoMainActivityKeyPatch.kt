package expo.modules.rfid

import android.view.KeyEvent

/**
 * INSTRUCTIONS:
 * 
 * To enable RFID hardware button handling, add the following code to your MainActivity.
 * 
 * Kotlin Version:
 * 
 * ```kotlin
 * override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
 *     // Forward to RFID module for handling hardware scanner buttons
 *     if (expo.modules.rfid.RfidModule.onKeyDown(keyCode)) {
 *         return true
 *     }
 *     return super.onKeyDown(keyCode, event)
 * }
 * ```
 */
class ExpoMainActivityKeyPatch {
    // This class is just documentation for how to patch the MainActivity
    // No actual code is needed here
} 