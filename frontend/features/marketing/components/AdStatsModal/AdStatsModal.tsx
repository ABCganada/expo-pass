"use client";

import { useState } from "react";
import { Download } from "lucide-react";
import type { MarketerBannerAd } from "../../types/marketerBanner";
import { useGetAdStatsQuery } from "../../api/marketerBannerApi";
import { marketerBannerService } from "../../services/marketerBannerService";
import styles from "./AdStatsModal.module.css";

interface AdStatsModalProps {
  ad: MarketerBannerAd;
  onClose: () => void;
}

function toDateString(iso: string) {
  return iso.slice(0, 10);
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

export function AdStatsModal({ ad, onClose }: AdStatsModalProps) {
  const adStart = toDateString(ad.startsAt);
  const adEnd = toDateString(ad.endsAt);
  const defaultTo = today() < adEnd ? today() : adEnd;

  const [from, setFrom] = useState(adStart);
  const [to, setTo] = useState(defaultTo);
  const [downloading, setDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState("");

  const { data: stats, isFetching, isError } = useGetAdStatsQuery(
    { id: ad.id, from, to },
    { skip: !from || !to },
  );

  const handleDownload = async () => {
    setDownloadError("");
    setDownloading(true);
    try {
      await marketerBannerService.downloadStatsXlsx(ad.id, from, to);
    } catch {
      setDownloadError("다운로드에 실패했습니다.");
    } finally {
      setDownloading(false);
    }
  };

  const hasStats = !!stats;
  const canDownload = hasStats && !isFetching && !downloading;

  return (
    <div className={styles.overlay} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={styles.modal}>
        <div className={styles.header}>
          <div>
            <h2 className={styles.title}>광고 성과</h2>
            <p className={styles.adTitle}>{ad.title}</p>
          </div>
        </div>

        <div className={styles.dateRow}>
          <div className={styles.field}>
            <label className={styles.label}>시작일</label>
            <input
              type="date"
              className={styles.input}
              value={from}
              min={toDateString(ad.startsAt)}
              max={to}
              onChange={(e) => setFrom(e.target.value)}
            />
          </div>
          <div className={styles.field}>
            <label className={styles.label}>종료일</label>
            <input
              type="date"
              className={styles.input}
              value={to}
              min={from}
              max={defaultTo}
              onChange={(e) => setTo(e.target.value)}
            />
          </div>
        </div>

        {isFetching && <p className={styles.empty}>통계를 불러오는 중...</p>}

        {!isFetching && isError && (
          <p className={styles.error}>통계를 불러오지 못했습니다.</p>
        )}

        {!isFetching && hasStats && (
          <div className={styles.statsGrid}>
            <div className={styles.statCard}>
              <span className={styles.statLabel}>노출수</span>
              <span className={styles.statValue}>{stats.impressions.toLocaleString()}</span>
              <span className={styles.statUnit}>Impressions</span>
            </div>
            <div className={styles.statCard}>
              <span className={styles.statLabel}>클릭수</span>
              <span className={styles.statValue}>{stats.clicks.toLocaleString()}</span>
              <span className={styles.statUnit}>Clicks</span>
            </div>
            <div className={styles.statCard}>
              <span className={styles.statLabel}>CTR</span>
              <span className={styles.statValue}>{(stats.ctr * 100).toFixed(2)}</span>
              <span className={styles.statUnit}>%</span>
            </div>
          </div>
        )}

        {!isFetching && !hasStats && !isError && (
          <p className={styles.empty}>해당 기간에 데이터가 없습니다.</p>
        )}

        {downloadError && <p className={styles.error}>{downloadError}</p>}

        <div className={styles.actions}>
          <button type="button" className={styles.btnClose} onClick={onClose}>
            닫기
          </button>
          <button
            type="button"
            className={styles.btnDownload}
            onClick={handleDownload}
            disabled={!canDownload}
          >
            <Download size={16} />
            {downloading ? "다운로드 중..." : "XLSX 다운로드"}
          </button>
        </div>
      </div>
    </div>
  );
}
