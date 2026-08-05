export type TraceCategory = "REQUEST" | "BACKGROUND";
export type TraceStatusFilter = "ALL" | "ERROR";

export interface TraceSearchParams {
  category: TraceCategory;
  rangeMinutes: 15 | 60 | 360 | 1440;
  status: TraceStatusFilter;
  minDurationMs: number;
  operation: string;
  limit: number;
}

export interface TraceSummary {
  traceId: string;
  rootServiceName: string;
  rootTraceName: string;
  startTime: number;
  durationMs: number;
  spanCount: number;
  error: boolean;
}

export interface TraceSearchResult {
  rangeMinutes: number;
  searchedAtEpochSeconds: number;
  traces: TraceSummary[];
}

export interface TraceSpanEvent {
  name: string;
  offsetMs: number;
  attributes: Record<string, string>;
}

export interface TraceSpan {
  spanId: string;
  parentSpanId?: string | null;
  name: string;
  serviceName: string;
  kind: string;
  status: string;
  startOffsetMs: number;
  durationMs: number;
  attributes: Record<string, string>;
  events: TraceSpanEvent[];
}

export interface TraceDetail {
  traceId: string;
  rootServiceName: string;
  rootTraceName: string;
  startTime: number;
  durationMs: number;
  error: boolean;
  spans: TraceSpan[];
}
