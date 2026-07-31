export interface EventCategory {
  id: string;
  code: string;
  name: string;
  active: boolean;
}

export interface EventListItem {
  id: string;
  title: string;
  categoryName: string;
  venueName: string;
  startDate: string;
  endDate: string;
  phase: string;
  viewCount: number;
  thumbnailUrl: string | null;
}