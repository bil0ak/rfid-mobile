import { NativeModule, registerWebModule } from "expo";

import type { RfidModuleEvents } from "./Rfid.types";

class RfidModule extends NativeModule<RfidModuleEvents> {
	PI = Math.PI;
	async setValueAsync(value: string): Promise<void> {
		this.emit("onChange", { value });
	}
	hello() {
		return "Hello world! 👋";
	}
}

export default registerWebModule(RfidModule);
