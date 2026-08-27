"use client";

import { Activity, Clock3, ListFilter, RefreshCw, Search, ServerCog, Waypoints } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { useLazyGetTraceDetailQuery, useLazySearchTracesQuery } from "../../api/traceApi";
import type { TraceCategory, TraceSearchParams, TraceStatusFilter, TraceSummary } from "../../types/trace";
import styles from "./TraceContent.module.css";
import { TraceWaterfall } from "./TraceWaterfall";

const DEFAULT_PARAMS: TraceSearchParams = {
  category: "REQUEST",
  rangeMinutes: 60,
  status: "ALL",
  minDurationMs: 0,
  operation: "",
  limit: 50,
};

const TRACE_ID_PATTERN = /^[0-9a-fA-F]{32}$/;

function formatTimestamp(timestamp: number): string {
  return new Date(timestamp * 1000).toLocaleString("ko-KR");
}

function formatDuration(durationMs: number): string {
  if (durationMs >= 60_000) return `${(durationMs / 60_000).toFixed(1)}분`;
  if (durationMs >= 1_000) return `${(durationMs / 1_000).toFixed(2)}초`;
  if (durationMs >= 1) return `${durationMs.toFixed(1)}ms`;
  return `${(durationMs * 1_000).toFixed(0)}μs`;
}

function errorMessage(error: unknown): string {
  return error && typeof error === "object" && "message" in error
    ? String(error.message)
    : "트레이스 요청을 처리하지 못했습니다.";
}

export function TraceContent() {
  const [params, setParams] = useState<TraceSearchParams>(DEFAULT_PARAMS);
  const [selectedTraceId, setSelectedTraceId] = useState<string>();
  const [searchTraces, searchState] = useLazySearchTracesQuery();
  const [getTraceDetail, detailState] = useLazyGetTraceDetailQuery();

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const traceId = new URLSearchParams(window.location.search).get("traceId") ?? "";
      if (TRACE_ID_PATTERN.test(traceId)) {
        setSelectedTraceId(traceId);
        getTraceDetail(traceId);
      }
    }, 0);
    return () => window.clearTimeout(timer);
  }, [getTraceDetail]);

  const runSearch = (next: TraceSearchParams) => {
    setSelectedTraceId(undefined);
    searchTraces(next);
  };

  const selectCategory = (category: TraceCategory) => {
    const next = { ...params, category, operation: "" };
    setParams(next);
    runSearch(next);
  };

  const openTrace = (traceId: string) => {
    setSelectedTraceId(traceId);
    getTraceDetail(traceId);
  };

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const searchText = params.operation.trim();
    if (TRACE_ID_PATTERN.test(searchText)) {
      openTrace(searchText);
      return;
    }
    runSearch({ ...params, operation: searchText });
  };

  return (
    <section className={styles.content}>
      <div className={styles.heading}>
        <div>
          <span className={styles.eyebrow}>TEMPO · TRACEQL</span>
          <h1>트레이스</h1>
          <p>요청 경로와 백그라운드 작업을 검색하고 Span 워터폴로 분석합니다.</p>
        </div>
        <button
          type="button"
          className={styles.refreshButton}
          disabled={searchState.isFetching}
          onClick={() => runSearch(params)}
        >
          <RefreshCw aria-hidden="true" data-spinning={searchState.isFetching} />
          {searchState.isFetching ? "조회 중..." : "새로고침"}
        </button>
      </div>

      <div className={styles.tabs} role="tablist" aria-label="트레이스 분류">
        <button
          type="button"
          role="tab"
          aria-selected={params.category === "REQUEST"}
          onClick={() => selectCategory("REQUEST")}
        >
          <Waypoints aria-hidden="true" />
          요청 트레이스
          <small>HTTP · Kafka</small>
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={params.category === "BACKGROUND"}
          onClick={() => selectCategory("BACKGROUND")}
        >
          <ServerCog aria-hidden="true" />
          백그라운드 작업
          <small>Scheduler</small>
        </button>
      </div>

      <form className={styles.filters} onSubmit={submitSearch}>
        <label>
          <span>조회 기간</span>
          <select
            value={params.rangeMinutes}
            onChange={(event) => setParams({
              ...params,
              rangeMinutes: Number(event.target.value) as TraceSearchParams["rangeMinutes"],
            })}
          >
            <option value={15}>최근 15분</option>
            <option value={60}>최근 1시간</option>
            <option value={360}>최근 6시간</option>
            <option value={1440}>최근 24시간</option>
          </select>
        </label>
        <label>
          <span>상태</span>
          <select
            value={params.status}
            onChange={(event) => setParams({
              ...params,
              status: event.target.value as TraceStatusFilter,
            })}
          >
            <option value="ALL">전체</option>
            <option value="ERROR">오류만</option>
          </select>
        </label>
        <label>
          <span>최소 소요시간</span>
          <select
            value={params.minDurationMs}
            onChange={(event) => setParams({ ...params, minDurationMs: Number(event.target.value) })}
          >
            <option value={0}>제한 없음</option>
            <option value={100}>100ms 이상</option>
            <option value={500}>500ms 이상</option>
            <option value={1000}>1초 이상</option>
            <option value={5000}>5초 이상</option>
          </select>
        </label>
        <label className={styles.searchField}>
          <span>작업명 또는 Trace ID</span>
          <div>
            <Search aria-hidden="true" />
            <input
              value={params.operation}
              maxLength={100}
              placeholder={params.category === "REQUEST" ? "/api/v1/reservations" : "waitingRoomScheduler"}
              onChange={(event) => setParams({ ...params, operation: event.target.value })}
            />
          </div>
        </label>
        <button type="submit" className={styles.searchButton} disabled={searchState.isFetching}>
          <ListFilter aria-hidden="true" />검색
        </button>
      </form>

      {searchState.error ? (
        <div className={styles.state} data-error="true">
          <Activity aria-hidden="true" />
          <h2>트레이스를 불러오지 못했습니다.</h2>
          <p>{errorMessage(searchState.error)}</p>
        </div>
      ) : searchState.data ? (
        <div className={styles.workspace} data-has-detail={Boolean(selectedTraceId)}>
          <section className={styles.listCard}>
            <div className={styles.listSummary}>
              <span><Clock3 aria-hidden="true" />최근 {searchState.data.rangeMinutes}분</span>
              <strong>{searchState.data.traces.length}건</strong>
              <small>{formatTimestamp(searchState.data.searchedAtEpochSeconds)} 조회</small>
            </div>
            {searchState.data.traces.length === 0 ? (
              <div className={styles.emptyList}>
                <Waypoints aria-hidden="true" />
                <strong>조건에 맞는 트레이스가 없습니다.</strong>
                <span>기간이나 최소 소요시간 조건을 조정해 보세요.</span>
              </div>
            ) : (
              <div className={styles.traceList}>
                {searchState.data.traces.map((trace: TraceSummary) => (
                  <button
                    type="button"
                    key={trace.traceId}
                    className={styles.traceRow}
                    data-selected={trace.traceId === selectedTraceId}
                    onClick={() => openTrace(trace.traceId)}
                  >
                    <span className={styles.traceStatus} data-error={trace.error}>
                      {trace.error ? "오류" : "정상"}
                    </span>
                    <span className={styles.traceName}>
                      <strong>{trace.rootTraceName}</strong>
                      <small>{formatTimestamp(trace.startTime)} · {trace.traceId.slice(0, 12)}</small>
                    </span>
                    <span className={styles.traceMeta}>
                      <strong>{formatDuration(trace.durationMs)}</strong>
                      <small>Span {trace.spanCount}</small>
                    </span>
                  </button>
                ))}
              </div>
            )}
          </section>

          {selectedTraceId ? (
            detailState.isFetching ? (
              <div className={styles.detailLoading}><RefreshCw aria-hidden="true" />상세 Span을 불러오는 중입니다.</div>
            ) : detailState.error ? (
              <div className={styles.state} data-error="true">
                <Activity aria-hidden="true" />
                <h2>Trace 상세를 불러오지 못했습니다.</h2>
                <p>{errorMessage(detailState.error)}</p>
              </div>
            ) : detailState.data ? <TraceWaterfall trace={detailState.data} /> : null
          ) : (
            <div className={styles.detailPlaceholder}>
              <Waypoints aria-hidden="true" />
              <strong>Trace를 선택하세요.</strong>
              <span>요청을 선택하면 부모·자식 Span과 실행 시간축을 표시합니다.</span>
            </div>
          )}
        </div>
      ) : (
        <div className={styles.state}>
          <Search aria-hidden="true" />
          <h2>조회 조건을 선택해 트레이스를 검색하세요.</h2>
          <p>기본값은 최근 1시간의 요청 트레이스이며 최대 50건을 가져옵니다.</p>
        </div>
      )}
    </section>
  );
}
