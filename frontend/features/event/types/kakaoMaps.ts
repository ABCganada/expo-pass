export interface KakaoLatLng {
  getLat: () => number;
  getLng: () => number;
}

export interface KakaoPlaceDocument {
  id: string;
  place_name: string;
  address_name: string;
  road_address_name: string;
  x: string;
  y: string;
}

export interface KakaoKeywordSearchOptions {
  page?: number;
  size?: number;
}

export interface KakaoPlacesService {
  keywordSearch: (
      keyword: string,
      callback: (data: KakaoPlaceDocument[], status: string) => void,
      options?: KakaoKeywordSearchOptions
  ) => void;
}

export interface KakaoRegionCodeDocument {
  region_type: "H" | "B";
  code: string;
  address_name: string;
}

export interface KakaoGeocoderService {
  coord2RegionCode: (
      lng: number,
      lat: number,
      callback: (data: KakaoRegionCodeDocument[], status: string) => void
  ) => void;
}

export interface KakaoMap {
  setCenter: (
      latlng: KakaoLatLng
  ) => void;

  setLevel: (
      level: number
  ) => void;
}


export interface KakaoMarker {
  setPosition: (
      latlng: KakaoLatLng
  ) => void;
}

export interface KakaoMapsServices {
  Places: new () => KakaoPlacesService;
  Geocoder: new () => KakaoGeocoderService;
}

export interface KakaoMaps {
  load: (
      callback: () => void
  ) => void;

  LatLng: new (
      lat: number,
      lng: number
  ) => KakaoLatLng;

  Map: new (
      container: HTMLElement,
      options: {
        center: KakaoLatLng;
        level: number;
      }
  ) => KakaoMap;

  Marker: new (
      options: {
        position: KakaoLatLng;
        map: KakaoMap;
      }
  ) => KakaoMarker;

  services: KakaoMapsServices;
}

declare global {
  interface Window {
    kakao?: { maps: KakaoMaps };
  }
}