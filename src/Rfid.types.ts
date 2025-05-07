import type { StyleProp, ViewStyle } from "react-native";

export type OnLoadEventPayload = {
	url: string;
};

export interface TagInfo {
	epc: string;
	tid?: string;
	user?: string;
	pc?: string;
	rssi: number;
	ant?: string;
	reserved?: string;
	frequencyPoint?: number;
	remain?: number;
	index?: number;
	count: number;
	phase?: number;
	timestamp: number;
}

export interface ReaderStatus {
	isConnected: boolean;
	status: "READY" | "BUSY" | "ERROR" | "DISCONNECTED";
}

export type FilterBank = "EPC" | "TID" | "USER" | "RESERVED";

export interface TagReadPayload {
	tag: TagInfo;
}

export interface ScanCompletePayload {
	totalTags: number;
	timeTaken: number;
	success: boolean;
}

export interface ScanErrorPayload {
	code: string;
	message: string;
}

// Using a Record type with proper intersection for EventsMap constraint
export type RfidModuleEvents = {
	onTagRead: (event: TagReadPayload) => void;
	onScanComplete: (event: ScanCompletePayload) => void;
	onScanError: (event: ScanErrorPayload) => void;
} & Record<string, (event: Record<string, unknown>) => void>;

export interface FilterParams {
	bank: FilterBank;
	offset: number;
	length: number;
	data: string;
}

export interface ReadTagResult {
	success: boolean;
	data?: string;
	bank?: string;
	ptr?: number;
	len?: number;
	message?: string;
	logs?: string[];
}

export interface WriteTagResult {
	success: boolean;
	bank?: string;
	ptr?: number;
	message?: string;
}

export interface OperationResult {
	success: boolean;
	message: string;
}

export type RfidViewProps = {
	url: string;
	onLoad: (event: { nativeEvent: OnLoadEventPayload }) => void;
	style?: StyleProp<ViewStyle>;
};
