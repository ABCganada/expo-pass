import type { CSSProperties } from "react";
import { AlertTriangle, Boxes, Clock3 } from "lucide-react";
import type { TraceDetail, TraceSpan } from "../../types/trace";
import styles from "./TraceContent.module.css";

function formatDuration(durationMs: number): string {
  if (durationMs >= 60_000) return `${(durationMs / 60_000).toFixed(1)}분`;
  if (durationMs >= 1_000) return `${(durationMs / 1_000).toFixed(2)}초`;
  if (durationMs >= 1) return `${durationMs.toFixed(1)}ms`;
  return `${(durationMs * 1_000).toFixed(0)}μs`;
}

function spanDepth(span: TraceSpan, spanMap: Map<string, TraceSpan>): number {
  let depth = 0;
  let parentId = span.parentSpanId;
  const visited = new Set<string>();
  while (parentId && spanMap.has(parentId) && !visited.has(parentId) && depth < 12) {
    visited.add(parentId);
    depth += 1;
    parentId = spanMap.get(parentId)?.parentSpanId;
  }
  return depth;
}

function attributeEntries(attributes: Record<string, string>) {
  return Object.entries(attributes).sort(([left], [right]) => left.localeCompare(right));
}

export function TraceWaterfall({ trace }: { trace: TraceDetail }) {
  const spanMap = new Map(trace.spans.map((span) => [span.spanId, span]));
  const totalDuration = Math.max(trace.durationMs, 0.001);

  return (
    <section className={styles.detailCard} aria-label="Trace 상세 워터폴">
      <div className={styles.detailHeading}>
        <div>
          <span className={styles.eyebrow}>TRACE DETAIL</span>
          <h2>{trace.rootTraceName}</h2>
          <code>{trace.traceId}</code>
        </div>
        <div className={styles.detailStats}>
          <span data-error={trace.error}><AlertTriangle aria-hidden="true" />{trace.error ? "오류" : "정상"}</span>
          <span><Clock3 aria-hidden="true" />{formatDuration(trace.durationMs)}</span>
          <span><Boxes aria-hidden="true" />Span {trace.spans.length}</span>
        </div>
      </div>

      <div className={styles.waterfallHeader}>
        <span>Span</span>
        <span>실행 시간축</span>
        <span>소요시간</span>
      </div>
      <div className={styles.waterfall}>
        {trace.spans.map((span) => {
          const depth = spanDepth(span, spanMap);
          const left = Math.min(99, Math.max(0, (span.startOffsetMs / totalDuration) * 100));
          const width = Math.max(0.7, Math.min(100 - left, (span.durationMs / totalDuration) * 100));
          const barStyle = { "--span-left": `${left}%`, "--span-width": `${width}%` } as CSSProperties;
          const hasDetail = Object.keys(span.attributes).length > 0 || span.events.length > 0;
          return (
            <details key={span.spanId} className={styles.spanRow} data-error={span.status === "ERROR"}>
              <summary>
                <span className={styles.spanName} style={{ paddingLeft: `${12 + depth * 16}px` }}>
                  <b>{span.name}</b>
                  <small>{span.serviceName} · {span.kind.replace("SPAN_KIND_", "")}</small>
                </span>
                <span className={styles.timeline} aria-hidden="true">
                  <i style={barStyle} />
                </span>
                <strong>{formatDuration(span.durationMs)}</strong>
              </summary>
              {hasDetail ? (
                <div className={styles.spanDetail}>
                  {attributeEntries(span.attributes).length > 0 ? (
                    <div>
                      <h3>속성</h3>
                      <dl>
                        {attributeEntries(span.attributes).map(([key, value]) => (
                          <div key={key}><dt>{key}</dt><dd>{value}</dd></div>
                        ))}
                      </dl>
                    </div>
                  ) : null}
                  {span.events.length > 0 ? (
                    <div>
                      <h3>이벤트</h3>
                      <ul>
                        {span.events.map((event, index) => (
                          <li key={`${event.name}-${event.offsetMs}-${index}`}>
                            <strong>{event.name}</strong>
                            <span>+{formatDuration(event.offsetMs)}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  ) : null}
                </div>
              ) : <div className={styles.spanDetailEmpty}>추가 속성이 없습니다.</div>}
            </details>
          );
        })}
      </div>
    </section>
  );
}
