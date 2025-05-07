// Reexport the native module. On web, it will be resolved to RfidModule.web.ts
// and on native platforms to RfidModule.ts
export { default } from "./RfidModule";
export { default as RfidView } from "./RfidView";
export * from "./Rfid.types";
