export type ActionTone = "neutral" | "success" | "danger";

const ACTION_TONE: Record<string, ActionTone> = {
  APPROVE: "success",
  CANCEL: "danger",
  CANCEL_STATUS_CHANGED: "danger",
};

export function actionTone(action: string): ActionTone {
  return ACTION_TONE[action] ?? "neutral";
}
