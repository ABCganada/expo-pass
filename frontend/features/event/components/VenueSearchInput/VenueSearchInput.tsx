"use client";

import { useEffect, useRef, useState } from "react";
import { MapPin, Search, X } from "lucide-react";
import type { KakaoGeocoderService, KakaoPlaceDocument, KakaoPlacesService } from "@/features/event/types/kakaoMaps";
import { KAKAO_MAP_APP_KEY, loadKakaoMapsSdk } from "@/features/event/utils/kakaoMapsLoader";
import styles from "./VenueSearchInput.module.css";

export interface VenueSelection {
  venueName: string;
  address: string;
  latitude: number;
  longitude: number;
  kakaoPlaceId: string;
  legalDongCode: string | null;
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
  const [asyncError, setError] = useState<string | null>(null);
  const configError = KAKAO_MAP_APP_KEY ? null : "장소 검색에 필요한 설정이 누락되었습니다.";
  const error = configError ?? asyncError;
  const placesRef = useRef<KakaoPlacesService | null>(null);
  const geocoderRef = useRef<KakaoGeocoderService | null>(null);

  useEffect(() => {
    if (!KAKAO_MAP_APP_KEY) return;
    let cancelled = false;
    loadKakaoMapsSdk()
      .then(() => {
        if (cancelled) return;
        placesRef.current = new window.kakao!.maps.services.Places();
        geocoderRef.current = new window.kakao!.maps.services.Geocoder();
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
    const latitude = Number(place.y);
    const longitude = Number(place.x);
    const base = {
      venueName: place.place_name,
      address: place.road_address_name || place.address_name,
      latitude,
      longitude,
      kakaoPlaceId: place.id,
    };
    setResults([]);
    setQuery("");

    if (!geocoderRef.current) {
      onSelect({ ...base, legalDongCode: null });
      return;
    }
    geocoderRef.current.coord2RegionCode(longitude, latitude, (data, status) => {
      const legalDongCode = status === "OK" ? (data.find((region) => region.region_type === "B")?.code ?? null) : null;
      onSelect({ ...base, legalDongCode });
    });
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