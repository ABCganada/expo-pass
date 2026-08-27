export type EventRole = "admin" | "manager";

export function getEventsBasePath(mode: EventRole): string {
  return mode === "admin" ? "/admin/events" : "/manager/events";
}