"use client";

import { useState } from "react";
import { useGetAdminEventDetailQuery } from "../../api/adminEventDetailApi";
import { EventDetailHeader } from "./EventDetailHeader";
import { BasicInfoTab } from "./BasicInfoTab";
import { ContentTab } from "./ContentTab";
import { ImageGridManager } from "../ImageGridManager/ImageGridManager";
import { TicketCardList } from "../TicketCardList/TicketCardList";
import { Toast } from "../Toast/Toast";
import { queryErrorMessage } from "@/features/store/api/queryError";
import styles from "./AdminEventDetail.module.css";

type TabKey = "basic" | "content" | "images" | "tickets";

const TABS: { key: TabKey; label: string }[] = [
  { key: "basic", label: "기본정보" },
  { key: "content", label: "콘텐츠" },
  { key: "images", label: "이미지" },
  { key: "tickets", label: "티켓" },
];

interface AdminEventDetailProps {
  eventId: string;
}

export function AdminEventDetail({ eventId }: AdminEventDetailProps) {
  const [activeTab, setActiveTab] = useState<TabKey>("basic");
  const [toast, setToast] = useState<string | null>(null);
  const { data: detail, isLoading, isError, error } = useGetAdminEventDetailQuery(eventId);

  if (isLoading) return <div className={styles.state}>불러오는 중...</div>;
  if (isError || !detail) {
    return <div className={styles.state}>{queryErrorMessage(error, "행사 정보를 불러오지 못했습니다.")}</div>;
  }

  return (
    <section className={styles.page}>
      <EventDetailHeader eventId={eventId} title={detail.title} status={detail.status} onStatusChanged={setToast} />

      <nav className={styles.tabBar}>
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            className={styles.tabButton}
            data-active={activeTab === tab.key}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </nav>

      <div className={styles.tabPanel}>
        {activeTab === "basic" && <BasicInfoTab eventId={eventId} detail={detail} onSaved={setToast} />}
        {activeTab === "content" && <ContentTab eventId={eventId} detail={detail} onSaved={setToast} />}
        {activeTab === "images" && <ImageGridManager eventId={eventId} images={detail.images} />}
        {activeTab === "tickets" && <TicketCardList eventId={eventId} tickets={detail.tickets} />}
      </div>

      {toast && <Toast message={toast} onDismiss={() => setToast(null)} />}
    </section>
  );
}