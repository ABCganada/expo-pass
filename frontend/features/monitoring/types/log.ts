export type LogLevelFilter = "ALL" | "ERROR" | "WARN" | "INFO" | "DEBUG";

export interface LogSearchParams {
  rangeMinutes: 15 | 60 | 360 | 1440;
  level: LogLevelFilter;
  podName: string;
  searchText: string;
  limit: number;
  cursor?: string;
}

export interface LogEntry {
  id: string;
  timestamp: number;
  level: string;
  message: string;
  logger: string;
  thread: string;
  traceId: string;
  spanId: string;
  serviceName: string;
  podName: string;
  exception: boolean;
  labels: Record<string, string>;
}

export interface LogSearchResult {
  rangeMinutes: number;
  searchedAtEpochSeconds: number;
  logs: LogEntry[];
  nextCursor: string;
}

export interface LogLevelSummary {
  error: number;
  warn: number;
  info: number;
  debug: number;
  other: number;
}
