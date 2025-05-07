import { NativeModule, requireNativeModule } from "expo";

import type {
	FilterBank,
	OperationResult,
	ReadTagResult,
	ReaderStatus,
	RfidModuleEvents,
	TagInfo,
	WriteTagResult,
} from "./Rfid.types";

/**
 * Native RFID Module interface that matches the Kotlin implementation.
 * This provides RFID/UHF functionality ported from com.example.uhf
 */
declare class RfidModule extends NativeModule<RfidModuleEvents> {
	// Constants
	VERSION: string;
	DEVICE_TYPE: string;
	BANK_EPC: "EPC";
	BANK_TID: "TID";
	BANK_USER: "USER";
	BANK_RESERVED: "RESERVED";

	/**
	 * Initialize the RFID reader
	 * This must be called before any other operations
	 */
	initReader(): Promise<OperationResult>;

	/**
	 * Close the RFID reader to release resources
	 */
	closeReader(): Promise<OperationResult>;

	/**
	 * Get the current reader status including connection state and battery level
	 */
	getReaderStatus(): ReaderStatus;

	/**
	 * Start scanning for RFID tags - single scan only
	 */
	startScan(): Promise<TagInfo>;

	/**
	 * Set a filter for tag operations to target specific tags
	 * @param bank - Memory bank to filter on
	 * @param ptr - Offset to start filtering from
	 * @param len - Length of data to filter (in bits)
	 * @param data - Hex pattern to match
	 */
	setFilter(
		bank: FilterBank,
		ptr: number,
		len: number,
		data: string,
	): Promise<OperationResult>;

	/**
	 * Read data from a tag's memory bank
	 * @param bank - Memory bank to read from
	 * @param ptr - Address to start reading from
	 * @param len - Number of words (16 bits) to read
	 * @param password - Access password (usually "00000000" for unprotected tags)
	 */
	readTagData(
		bank: FilterBank,
		ptr: number,
		len: number,
		password: string,
	): Promise<ReadTagResult>;

	/**
	 * Write data to a tag's memory bank
	 * @param bank - Memory bank to write to
	 * @param ptr - Address to start writing from
	 * @param data - Hex data to write
	 * @param password - Access password (usually "00000000" for unprotected tags)
	 */
	writeTagData(
		bank: FilterBank,
		ptr: number,
		data: string,
		password: string,
	): Promise<WriteTagResult>;
}

// Load the native module from the JSI
export default requireNativeModule<RfidModule>("Rfid");
