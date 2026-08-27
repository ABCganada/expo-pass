"use client";

import { Activity, Clock3, ListTree, RefreshCw } from "lucide-react";
import { useState } from "react";
import {
  useLazyGetCurrentMetricsQuery,
  useLazyGetDailyTrendsQuery,
} from "../../api/monitoringApi";
import type { MetricSample } from "../../types/metric";
import styles from "./MetricContent.module.css";
import { MetricTrendChart } from "./MetricTrendChart";

type MetricView = "trends" | "current";

function formatTimestamp(timestamp: number): string {
  return new Date(timestamp * 1000).toLocaleString("ko-KR");
}

function labelEntries(sample: MetricSample) {
  return Object.entries(sample.labels).sort(([left], [right]) => left.localeCompare(right));
}

function errorMessage(error: unknown): string {
  return error && typeof error === "object" && "message" in error
    ? String(error.message)
    : "메트릭 요청을 처리하지 못했습니다.";
}

export function MetricContent() {
  const [activeView, setActiveView] = useState<MetricView>("trends");
  const [getDailyTrends, trends] = useLazyGetDailyTrendsQuery();
  const [getCurrentMetrics, current] = useLazyGetCurrentMetricsQuery();
  const activeRequest = activeView === "trends" ? trends : current;

  const requestMetrics = () => {
    if (activeView === "trends") {
      getDailyTrends();
      return;
    }
    getCurrentMetrics();
  };

  return (
    <section className={styles.content}>
      <div className={styles.heading}>
        <div>
          <span className={styles.eyebrow}>PROMETHEUS</span>
          <h1>메트릭</h1>
          <p>LastMission 인스턴스의 주요 추이와 전체 현재값을 조회합니다.</p>
        </div>
        <button
          type="button"
          className={styles.requestButton}
          disabled={activeRequest.isFetching}
          onClick={requestMetrics}
        >
          <RefreshCw aria-hidden="true" data-spinning={activeRequest.isFetching} />
          {activeRequest.isFetching
            ? "요청 중..."
            : activeView === "trends" ? "24시간 추이 조회" : "전체 현재값 조회"}
        </button>
      </div>

      <div className={styles.tabs} role="tablist" aria-label="메트릭 조회 방식">
        <button
          type="button"
          role="tab"
          aria-selected={activeView === "trends"}
          className={styles.tab}
          onClick={() => setActiveView("trends")}
        >
          <Clock3 aria-hidden="true" />
          주요 메트릭 24시간
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeView === "current"}
          className={styles.tab}
          onClick={() => setActiveView("current")}
        >
          <ListTree aria-hidden="true" />
          전체 현재값 조회
        </button>
      </div>

      {activeView === "trends" ? (
        <div role="tabpanel">
          {trends.isUninitialized ? (
            <div className={styles.state}>
              <Clock3 aria-hidden="true" />
              <h2>최근 24시간 추이를 확인할 수 있습니다.</h2>
              <p>CPU, JVM, HTTP, DB, Kafka 주요 지표를 1분 간격으로 조회합니다.</p>
            </div>
          ) : trends.error ? (
            <div className={styles.state} data-error="true">
              <Activity aria-hidden="true" />
              <h2>24시간 메트릭을 불러오지 못했습니다.</h2>
              <p>{errorMessage(trends.error)}</p>
            </div>
          ) : trends.data ? (
            <div className={styles.trendResult}>
              <div className={styles.trendSummary}>
                <span><Clock3 aria-hidden="true" /> {formatTimestamp(trends.data.from)}부터</span>
                <strong>{formatTimestamp(trends.data.to)}까지</strong>
                <span>{trends.data.stepSeconds / 60}분 간격 · 주요 지표 {trends.data.metrics.length}개</span>
              </div>
              <div className={styles.chartGrid}>
                {trends.data.metrics.map((metric) => (
                  <MetricTrendChart key={metric.id} trend={metric} />
                ))}
              </div>
            </div>
          ) : null}
        </div>
      ) : (
        <div role="tabpanel">
          {current.isUninitialized ? (
            <div className={styles.state}>
              <ListTree aria-hidden="true" />
              <h2>아직 전체 현재값을 요청하지 않았습니다.</h2>
              <p>현재 인스턴스에 속한 모든 시계열의 최신값을 조회합니다.</p>
            </div>
          ) : current.error ? (
            <div className={styles.state} data-error="true">
              <Activity aria-hidden="true" />
              <h2>메트릭을 불러오지 못했습니다.</h2>
              <p>{errorMessage(current.error)}</p>
            </div>
          ) : current.data ? (
            <div className={styles.result}>
              <div className={styles.summary}>
                <div>
                  <span>실행 쿼리</span>
                  <strong>{current.data.query}</strong>
                </div>
                <div>
                  <span>수집 샘플</span>
                  <strong>{current.data.sampleCount}</strong>
                </div>
              </div>

              {current.data.samples.length === 0 ? (
                <div className={styles.state}>
                  <Activity aria-hidden="true" />
                  <h2>조회된 샘플이 없습니다.</h2>
                </div>
              ) : (
                <div className={styles.tableWrap}>
                  <table>
                    <thead>
                      <tr>
                        <th>라벨</th>
                        <th>값</th>
                        <th>수집 시각</th>
                      </tr>
                    </thead>
                    <tbody>
                      {current.data.samples.map((sample, index) => (
                        <tr key={`${sample.timestamp}-${index}`}>
                          <td>
                            <div className={styles.labels}>
                              {labelEntries(sample).map(([key, value]) => (
                                <span key={key}><b>{key}</b>={value}</span>
                              ))}
                            </div>
                          </td>
                          <td><strong className={styles.value}>{sample.value}</strong></td>
                          <td className={styles.timestamp}>{formatTimestamp(sample.timestamp)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          ) : null}
        </div>
      )}
    </section>
  );
}
