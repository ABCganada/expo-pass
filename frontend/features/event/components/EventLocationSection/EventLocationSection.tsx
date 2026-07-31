import { ExternalLink, MapPin } from "lucide-react";
import type { EventDetail } from "../../types/event";
import { EventKakaoMap } from "./EventKakaoMap";
import styles from "./EventLocationSection.module.css";

interface EventLocationSectionProps {
  detail: EventDetail;
}

export function EventLocationSection({ detail }: EventLocationSectionProps) {
  const { venueName, address, detailAddress, latitude, longitude, kakaoPlaceId } = detail;
  const hasCoordinates = latitude != null && longitude != null;

  return (
    <section className={styles.section}>
      <h2 className={styles.title}>오시는 길</h2>

      {hasCoordinates ? (
        <EventKakaoMap latitude={latitude} longitude={longitude} venueName={venueName ?? "행사장"} />
      ) : (
        <div className={styles.mapFallback}>위치 정보가 없습니다.</div>
      )}

      {kakaoPlaceId && (
        <a
          className={styles.placeLink}
          href={`https://place.map.kakao.com/${kakaoPlaceId}`}
          target="_blank"
          rel="noopener noreferrer"
        >
          카카오맵에서 자세히 보기
          <ExternalLink size={14} />
        </a>
      )}

      <div className={styles.addressBlock}>
        <MapPin size={16} className={styles.addressIcon} />
        <div>
          {venueName && <p className={styles.venueName}>{venueName}</p>}
          {(address || detailAddress) && (
            <p className={styles.address}>{[address, detailAddress].filter(Boolean).join(" ")}</p>
          )}
        </div>
      </div>
    </section>
  );
}