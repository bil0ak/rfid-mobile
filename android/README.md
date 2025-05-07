# RFID Module for Expo/React Native

This module provides a bridge to access UHF RFID reader functionality in React Native applications.

## Setup

### Required UHF RFID Library

This module requires the RFIDWithUHFUART library to function properly. To use it:

1. Download the UHF reader SDK from your device manufacturer
2. Extract the SDK and find the JAR files (usually something like `RFIDWithUHFUART.jar`)
3. Place the JAR file in the `packages/rfid/android/libs` directory
4. Rebuild the project

Note: Different UHF readers may have slightly different APIs. You might need to adjust the `RfidModule.kt` file to match your specific hardware.

## API Notes

This module has been adapted to work with the common UHF RFID reader API found in Android RFID SDKs. Key changes:

- We're using the `writeData(password, bankCode, ptr, wordCount, data)` signature for writing tag data
- We've removed references to `Bank_Reserved` as it's not available in all libraries
- The continuous scan mode is implemented with a timeout parameter

## Troubleshooting

### Common Build Errors

#### Unresolved reference: RFIDWithUHFUART

Make sure you've added the proper JAR file to the `libs` directory and the build.gradle file includes:

```gradle
implementation fileTree(dir: 'libs', include: ['*.jar'])
```

#### Method not found errors

If you see errors like:

```
None of the following functions can be called with the arguments supplied
```

This usually means the signature of the methods in your specific UHF library is different from what our code expects. You'll need to:

1. Look up the actual method signature in your SDK documentation
2. Update the `RfidModule.kt` file to match

## Device Support

This module is designed to work with Android UHF RFID readers. It has been tested with:

- Generic UHF RFID readers
- Chainway devices
- RFID Sled attachments

If your device uses a different API, you may need to modify the code.
