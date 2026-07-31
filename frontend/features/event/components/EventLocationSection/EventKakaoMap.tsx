"use client";

import { useEffect, useRef, useState } from "react";
import styles from "./EventLocationSection.module.css";

interface KakaoMaps {
  load: (callback: () => void) => void;
  LatLng: new (lat: number, lng: number) => unknown;
  Map: new (container: HTMLElement, options: { center: unknown; level: number }) => unknown;
  Marker: new (options: { position: unknown; map: unknown }) => unknown;
}

declare global {
  interface Window {
    kakao?: { maps: KakaoMaps };
  }
}

const SCRIPT_ID = "kakao-map-sdk";
const APP_KEY = process.env.NEXT_PUBLIC_KAKAO_MAP_APP_KEY;

interface EventKakaoMapProps {
  latitude: number;
  longitude: number;
  venueName: string;
}

function loadKakaoSdk(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.kakao?.maps) {
      resolve();
      return;
    }
    const existing = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null;
    if (existing) {
      existing.addEventListener("load", () => resolve());
      existing.addEventListener("error", () => reject(new Error("지도를 불러오지 못했습니다.")));
      return;
    }
    const script = document.createElement("script");
    script.id = SCRIPT_ID;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${APP_KEY}&autoload=false`;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("지도를 불러오지 못했습니다."));
    document.head.appendChild(script);
  });
}

export function EventKakaoMap({ latitude, longitude, venueName }: EventKakaoMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState<string | null>(null);
  const hasKey = !!APP_KEY;

  useEffect(() => {
    if (!hasKey) return;

    let cancelled = false;

    loadKakaoSdk()
      .then(() => {
        if (cancelled || !containerRef.current) return;
        window.kakao!.maps.load(() => {
          if (cancelled || !containerRef.current) return;
          const position = new window.kakao!.maps.LatLng(latitude, longitude);
          const map = new window.kakao!.maps.Map(containerRef.current, { center: position, level: 3 });
          new window.kakao!.maps.Marker({ position, map });
        });
      })
      .catch((e: Error) => {
        if (!cancelled) setError(e.message);
      });

    return () => {
      cancelled = true;
    };
  }, [hasKey, latitude, longitude]);

  if (!hasKey) {
    return <div className={styles.mapFallback}>지도 표시에 필요한 설정이 누락되었습니다.</div>;
  }

  if (error) {
    return <div className={styles.mapFallback}>{error}</div>;
  }

  return <div ref={containerRef} className={styles.map} role="img" aria-label={`${venueName} 위치 지도`} />;
}