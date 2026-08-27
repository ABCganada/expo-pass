export interface MetricSample {
  labels: Record<string, string>;
  timestamp: number;
  value: string;
}

export interface MetricSnapshot {
  query: string;
  sampleCount: number;
  samples: MetricSample[];
}

export interface MetricPoint {
  timestamp: number;
  value: string;
}

export interface MetricTrend {
  id: string;
  title: string;
  unit: string;
  query: string;
  points: MetricPoint[];
}

export interface MetricTrendDashboard {
  from: number;
  to: number;
  stepSeconds: number;
  metrics: MetricTrend[];
}
