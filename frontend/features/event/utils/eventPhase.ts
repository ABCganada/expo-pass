export type EventDisplayPhase = "UPCOMING" | "ONGOING" | "ENDED";

export const EVENT_PHASE_LABEL: Record<EventDisplayPhase, string> = {
  UPCOMING: "예정",
  ONGOING: "예약중",
  ENDED: "종료",
};

function todayDateString(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function computeEventPhase(startDate: string, endDate: string): EventDisplayPhase {
  const today = todayDateString();
  if (today < startDate) return "UPCOMING";
  if (today > endDate) return "ENDED";
  return "ONGOING";
}

function diffDays(from: string, to: string): number {
  const fromTime = new Date(`${from}T00:00:00`).getTime();
  const toTime = new Date(`${to}T00:00:00`).getTime();
  return Math.ceil((toTime - fromTime) / (1000 * 60 * 60 * 24));
}

/** UPCOMING만 시작일까지 남은 일수로 D-day 표기, ONGOING은 "진행중", ENDED는 "종료". */
export function computeDdayLabel(phase: EventDisplayPhase, startDate: string): string {
  if (phase === "ENDED") return "종료";
  if (phase === "ONGOING") return "진행중";

  const days = diffDays(todayDateString(), startDate);
  return days === 0 ? "D-DAY" : `D-${days}`;
}

export function formatPeriod(startDate: string, endDate: string): string {
  return `${startDate.replaceAll("-", ".")} ~ ${endDate.replaceAll("-", ".")}`;
}