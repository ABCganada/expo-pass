"use client";

import Link from "next/link";
import { Clock3, Copy, ExternalLink, FileWarning, ListFilter, Logs, RefreshCw, Search, Server } from "lucide-react";
import { useMemo, useState, type FormEvent } from "react";
import { useLazyGetLogSummaryQuery, useLazySearchLogsQuery } from "../../api/logApi";
import type { LogEntry, LogLevelFilter, LogSearchParams } from "../../types/log";
import styles from "./LogContent.module.css";

const DEFAULT_PARAMS: LogSearchParams = {
  rangeMinutes: 60,
  level: "ALL",
  podName: "",
  searchText: "",
  limit: 100,
};

function formatTimestamp(timestamp: number): string {
  return new Date(timestamp * 1000).toLocaleString("ko-KR", { hour12: false });
}

function errorMessage(error: unknown): string {
  return error && typeof error === "object" && "message" in error
    ? String(error.message)
    : "로그 요청을 처리하지 못했습니다.";
}

function levelClass(level: string): string {
  const normalized = level.toLowerCase();
  return ["error", "warn", "info", "debug"].includes(normalized) ? normalized : "other";
}

export function LogContent() {
  const [params, setParams] = useState<LogSearchParams>(DEFAULT_PARAMS);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [nextCursor, setNextCursor] = useState("");
  const [searchedAt, setSearchedAt] = useState<number>();
  const [searchLogs, searchState] = useLazySearchLogsQuery();
  const [getSummary, summaryState] = useLazyGetLogSummaryQuery();
  const summary = summaryState.data;

  const podNames = useMemo(
    () => Array.from(new Set(logs.map((entry) => entry.podName).filter(Boolean))).sort(),
    [logs],
  );

  const runSearch = async (next: LogSearchParams, append = false) => {
    const request = { ...next, cursor: append ? nextCursor : "" };
    try {
      const result = await searchLogs(request).unwrap();
      setLogs((current) => append ? [...current, ...result.logs] : result.logs);
      setNextCursor(result.nextCursor);
      setSearchedAt(result.searchedAtEpochSeconds);
      if (!append) getSummary({ rangeMinutes: next.rangeMinutes, podName: next.podName });
    } catch {
      if (!append) setLogs([]);
    }
  };

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    runSearch({ ...params, searchText: params.searchText.trim() });
  };

  const selectTab = (level: LogLevelFilter) => {
    const next = { ...params, level };
    setParams(next);
    runSearch(next);
  };

  return (
    <section className={styles.content}>
      <div className={styles.heading}>
        <div>
          <span className={styles.eyebrow}>LOKI · LOGQL</span>
          <h1>로그</h1>
          <p>애플리케이션 로그와 오류 Stack trace를 검색하고 관련 트레이스로 이동합니다.</p>
        </div>
        <button type="button" className={styles.refreshButton}
          disabled={searchState.isFetching} onClick={() => runSearch(params)}>
          <RefreshCw aria-hidden="true" data-spinning={searchState.isFetching} />
          {searchState.isFetching ? "조회 중..." : "새로고침"}
        </button>
      </div>

      <div className={styles.tabs} role="tablist" aria-label="로그 분류">
        <button type="button" role="tab" aria-selected={params.level !== "ERROR"}
          onClick={() => selectTab("ALL")}>
          <Logs aria-hidden="true" /><span>전체 로그<small>모든 레벨</small></span>
        </button>
        <button type="button" role="tab" aria-selected={params.level === "ERROR"}
          onClick={() => selectTab("ERROR")}>
          <FileWarning aria-hidden="true" /><span>오류·예외<small>ERROR와 Stack trace</small></span>
        </button>
      </div>

      <form className={styles.filters} onSubmit={submitSearch}>
        <label><span>조회 기간</span>
          <select value={params.rangeMinutes} onChange={(event) => setParams({
            ...params, rangeMinutes: Number(event.target.value) as LogSearchParams["rangeMinutes"],
          })}>
            <option value={15}>최근 15분</option><option value={60}>최근 1시간</option>
            <option value={360}>최근 6시간</option><option value={1440}>최근 24시간</option>
          </select>
        </label>
        <label><span>레벨</span>
          <select value={params.level} onChange={(event) => setParams({
            ...params, level: event.target.value as LogLevelFilter,
          })}>
            <option value="ALL">전체</option><option value="ERROR">ERROR</option>
            <option value="WARN">WARN</option><option value="INFO">INFO</option><option value="DEBUG">DEBUG</option>
          </select>
        </label>
        <label><span>인스턴스(Pod)</span>
          <select value={params.podName} onChange={(event) => setParams({ ...params, podName: event.target.value })}>
            <option value="">전체 Pod</option>
            {podNames.map((pod) => <option key={pod} value={pod}>{pod}</option>)}
          </select>
        </label>
        <label className={styles.searchField}><span>메시지·Logger·Trace ID</span>
          <div><Search aria-hidden="true" /><input value={params.searchText} maxLength={100}
            placeholder="Exception 또는 32자리 Trace ID"
            onChange={(event) => setParams({ ...params, searchText: event.target.value })} /></div>
        </label>
        <button type="submit" className={styles.searchButton} disabled={searchState.isFetching}>
          <ListFilter aria-hidden="true" />검색
        </button>
      </form>

      {summary ? (
        <div className={styles.summary} aria-label="선택 기간 로그 레벨 요약">
          {(["error", "warn", "info", "debug", "other"] as const).map((level) => (
            <div key={level} data-level={level}>
              <span>{level === "other" ? "기타" : level.toUpperCase()}</span>
              <strong>{summary[level].toLocaleString()}</strong>
            </div>
          ))}
        </div>
      ) : null}

      {searchState.error && logs.length === 0 ? (
        <div className={styles.state} data-error="true"><FileWarning aria-hidden="true" />
          <h2>로그를 불러오지 못했습니다.</h2><p>{errorMessage(searchState.error)}</p></div>
      ) : searchedAt ? (
        <section className={styles.logCard}>
          <div className={styles.listSummary}>
            <span><Clock3 aria-hidden="true" />최근 {params.rangeMinutes}분</span>
            <strong>{logs.length.toLocaleString()}건</strong>
            <small>{formatTimestamp(searchedAt)} 조회</small>
          </div>
          {logs.length === 0 ? (
            <div className={styles.state}><Logs aria-hidden="true" /><h2>조건에 맞는 로그가 없습니다.</h2>
              <p>기간, 레벨 또는 검색어 조건을 조정해 보세요.</p></div>
          ) : (
            <div className={styles.logList}>
              {logs.map((entry) => (
                <details key={entry.id} className={styles.logRow} data-level={levelClass(entry.level)}>
                  <summary>
                    <time>{formatTimestamp(entry.timestamp)}</time>
                    <span className={styles.level}>{entry.level}</span>
                    <span className={styles.preview}>{entry.message.split("\n", 1)[0]}</span>
                    <span className={styles.pod}><Server aria-hidden="true" />{entry.podName || "-"}</span>
                    {entry.traceId ? <span className={styles.traceMark}>TRACE</span> : <span />}
                  </summary>
                  <div className={styles.detail}>
                    <div className={styles.detailActions}>
                      <button type="button" onClick={() => navigator.clipboard.writeText(entry.message)}>
                        <Copy aria-hidden="true" />메시지 복사
                      </button>
                      {entry.traceId ? <Link href={`/developer/traces?traceId=${entry.traceId}`}>
                        <ExternalLink aria-hidden="true" />트레이스에서 보기
                      </Link> : null}
                    </div>
                    <dl>
                      <div><dt>Logger</dt><dd>{entry.logger || "-"}</dd></div>
                      <div><dt>Thread</dt><dd>{entry.thread || "-"}</dd></div>
                      <div><dt>Trace ID</dt><dd>{entry.traceId || "-"}</dd></div>
                      <div><dt>Span ID</dt><dd>{entry.spanId || "-"}</dd></div>
                      <div><dt>Pod</dt><dd>{entry.podName || "-"}</dd></div>
                    </dl>
                    <pre>{entry.message}</pre>
                  </div>
                </details>
              ))}
            </div>
          )}
          {nextCursor ? <div className={styles.loadMore}>
            <button type="button" disabled={searchState.isFetching} onClick={() => runSearch(params, true)}>
              {searchState.isFetching ? "불러오는 중..." : "이전 로그 더 보기"}
            </button>
          </div> : null}
        </section>
      ) : (
        <div className={styles.state}><Search aria-hidden="true" /><h2>조회 조건을 선택해 로그를 검색하세요.</h2>
          <p>기본값은 최근 1시간이며 최신 로그부터 최대 100건을 가져옵니다.</p></div>
      )}
    </section>
  );
}
