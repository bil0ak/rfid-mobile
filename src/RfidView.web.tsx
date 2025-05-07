import * as React from "react";

import type { RfidViewProps } from "./Rfid.types";

export default function RfidView(props: RfidViewProps) {
	return (
		<div>
			<iframe
				style={{ flex: 1 }}
				src={props.url}
				onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
			/>
		</div>
	);
}
