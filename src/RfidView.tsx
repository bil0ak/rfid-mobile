import { requireNativeView } from "expo";
import type * as React from "react";

import type { RfidViewProps } from "./Rfid.types";

const NativeView: React.ComponentType<RfidViewProps> =
	requireNativeView("Rfid");

export default function RfidView(props: RfidViewProps) {
	return <NativeView {...props} />;
}
