"use client";

import { useEffect, useRef, useState } from "react";
import { MapPin, Search, X } from "lucide-react";
import type { KakaoPlaceDocument, KakaoPlacesService } from "@/features/event/types/kakaoMaps";
import "@/features/event/types/kakaoMaps";
import styles from "./VenueSearchInput.module.css";

const SCRIPT_ID = "kakao-map-sdk-services";
const APP_KEY = process.env.NEXT_PUBLIC_KAKAO_MAP_APP_KEY;

function loadKakaoServicesSdk(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.kakao?.maps?.services) {
      resolve();
      return;
    }
    const existing = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null;
    if (existing) {
      existing.addEventListener("load", () => resolve());
      existing.addEventListener("error", () => reject(new Error("장소 검색 기능을 불러오지 못했습니다.")));
      return;
    }
    const script = document.createElement("script");
    script.id = SCRIPT_ID;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${APP_KEY}&autoload=false&libraries=services`;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("장소 검색 기능을 불러오지 못했습니다."));
    document.head.appendChild(script);
  });
}

export interface VenueSelection {
  venueName: string;
  address: string;
  latitude: number;
  longitude: number;
  kakaoPlaceId: string;
}

interface VenueSearchInputProps {
  selectedVenueName: string;
  onSelect: (venue: VenueSelection) => void;
  onClear: () => void;
}

export function VenueSearchInput({ selectedVenueName, onSelect, onClear }: VenueSearchInputProps) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<KakaoPlaceDocument[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const placesRef = useRef<KakaoPlacesService | null>(null);

  useEffect(() => {
    if (!APP_KEY) {
      setError("장소 검색에 필요한 설정이 누락되었습니다.");
      return;
    }
    let cancelled = false;
    loadKakaoServicesSdk()
      .then(() => {
        if (cancelled) return;
        window.kakao!.maps.load(() => {
          if (cancelled) return;
          placesRef.current = new window.kakao!.maps.services.Places();
        });
      })
      .catch((e: Error) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleSearch = () => {
    if (!query.trim() || !placesRef.current) return;
    setIsSearching(true);
    setError(null);
    placesRef.current.keywordSearch(query.trim(), (data, status) => {
      setIsSearching(false);
      if (status !== "OK") {
        setResults([]);
        setError(status === "ZERO_RESULT" ? "검색 결과가 없습니다." : "장소 검색에 실패했습니다.");
        return;
      }
      setResults(data);
    });
  };

  const handleSelect = (place: KakaoPlaceDocument) => {
    onSelect({
      venueName: place.place_name,
      address: place.road_address_name || place.address_name,
      latitude: Number(place.y),
      longitude: Number(place.x),
      kakaoPlaceId: place.id,
    });
    setResults([]);
    setQuery("");
  };

  if (selectedVenueName) {
    return (
      <div className={styles.chip}>
        <MapPin size={16} strokeWidth={2} />
        <span>{selectedVenueName}</span>
        <button type="button" className={styles.clearButton} onClick={onClear} aria-label="장소 선택 해제">
          <X size={18} strokeWidth={2.5} />
        </button>
      </div>
    );
  }

  return (
    <div className={styles.picker}>
      <div className={styles.searchBox}>
        <Search size={16} aria-hidden />
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault();
              handleSearch();
            }
          }}
          placeholder="장소명으로 검색 (예: 코엑스, 킨텍스)"
          autoComplete="off"
        />
        <button type="button" className={styles.searchButton} onClick={handleSearch} disabled={!query.trim()}>
          검색
        </button>
      </div>

      {isSearching && <p className={styles.hint}>검색 중...</p>}
      {error && <p className={styles.hint}>{error}</p>}

      {results.length > 0 && (
        <div className={styles.results}>
          {results.map((place) => (
            <button key={place.id} type="button" className={styles.result} onClick={() => handleSelect(place)}>
              <strong>{place.place_name}</strong>
              <span>{place.road_address_name || place.address_name}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}