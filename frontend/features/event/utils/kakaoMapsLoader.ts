import "@/features/event/types/kakaoMaps";

const SCRIPT_ID = "kakao-map-sdk";
export const KAKAO_MAP_APP_KEY = process.env.NEXT_PUBLIC_KAKAO_MAP_APP_KEY;

const READY_POLL_INTERVAL_MS = 100;
const READY_TIMEOUT_MS = 10000;

let kakaoSdkPromise: Promise<void> | null = null;

// 카카오 SDK script가 있으면 재사용, 없으면 생성
function ensureScriptTag(): HTMLScriptElement {
  const existing = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null;
  if (existing) {
    return existing;
  }

  const script = document.createElement("script");
  script.id = SCRIPT_ID;
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_MAP_APP_KEY}&autoload=false&libraries=services`;
  script.async = true;
  script.onerror = () => {
    script.dataset.failed = "true";
  };
  document.head.appendChild(script);
  return script;
}

/**
 * 이벤트 대신 window.kakao.maps.load 함수 등장 여부를 폴링
 */
function waitForKakaoLoader(script: HTMLScriptElement): Promise<void> {
  return new Promise((resolve, reject) => {
    const start = Date.now();
    const check = () => {
      if (window.kakao?.maps?.load) {
        resolve();
        return;
      }
      if (script.dataset.failed === "true") {
        reject(new Error("지도 SDK를 불러오지 못했습니다."));
        return;
      }
      if (Date.now() - start > READY_TIMEOUT_MS) {
        reject(new Error("지도 SDK 로딩이 지연되고 있습니다. 잠시 후 다시 시도해주세요."));
        return;
      }
      setTimeout(check, READY_POLL_INTERVAL_MS);
    };
    check();
  });
}

/**
 * EventKakaoMap(지도)과 VenueSearchInput(장소 검색) 모두 이 로더 하나만 사용해서 항상 libraries=services 로 로드
 */
export function loadKakaoMapsSdk(): Promise<void> {
  if (kakaoSdkPromise) {
    return kakaoSdkPromise;
  }

  kakaoSdkPromise = (async () => {
    if (window.kakao?.maps?.services) {
      return;
    }
    if (!KAKAO_MAP_APP_KEY) {
      throw new Error("장소 검색에 필요한 설정이 누락되었습니다.");
    }

    const script = ensureScriptTag();
    await waitForKakaoLoader(script);
    await new Promise<void>((resolve) => window.kakao!.maps.load(() => resolve()));
  })();

  return kakaoSdkPromise;
}