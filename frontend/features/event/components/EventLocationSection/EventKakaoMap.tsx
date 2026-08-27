"use client";

import { useEffect, useRef, useState } from "react";
import type { KakaoMap } from "@/features/event/types/kakaoMaps";
import { KAKAO_MAP_APP_KEY, loadKakaoMapsSdk } from "@/features/event/utils/kakaoMapsLoader";
import styles from "./EventLocationSection.module.css";

interface EventKakaoMapProps {
  latitude: number;
  longitude: number;
  venueName: string;
}

export function EventKakaoMap({ latitude, longitude, venueName }: EventKakaoMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<KakaoMap | null>(null);

  const [error, setError] = useState<string | null>(null);
  const hasKey = !!KAKAO_MAP_APP_KEY;

  useEffect(() => {
    if (!hasKey) return;

    let cancelled = false;

    loadKakaoMapsSdk()
      .then(() => {
        if (cancelled || !containerRef.current) return;

        const position = new window.kakao!.maps.LatLng(latitude, longitude); // 카카오 좌표 객체 반환
        const map = new window.kakao!.maps.Map(containerRef.current, { center: position, level: 3 }); // 지도 생성

        mapRef.current = map;

        new window.kakao!.maps.Marker({ position, map });
      })
      .catch((e: Error) => {
        if (!cancelled) setError(e.message);
      });

    return () => {
      cancelled = true;
      mapRef.current = null;
    };
  }, [hasKey, latitude, longitude, venueName]);

  if (!hasKey) {
    return <div className={styles.mapFallback}>지도 표시에 필요한 설정이 누락되었습니다.</div>;
  }

  if (error) {
    return <div className={styles.mapFallback}>{error}</div>;
  }

  return <div ref={containerRef} className={styles.map} role="img" aria-label={`${venueName} 위치 지도`} />;
}