"use client";

import { CalendarDays, CreditCard, QrCode, ScanLine } from "lucide-react";
import styles from "./SupportContent.module.css";

const USAGE_STEPS = [
  {
    icon: CalendarDays,
    title: "박람회 둘러보기",
    description: "홈이나 박람회 목록에서 관심있는 행사를 검색·카테고리별로 찾아보세요.",
  },
  {
    icon: CreditCard,
    title: "티켓 예약 및 결제",
    description: "행사 상세 페이지에서 원하는 티켓을 선택하고 결제를 진행하세요.",
  },
  {
    icon: QrCode,
    title: "QR 티켓 확인",
    description: "결제가 완료되면 'QR 티켓' 메뉴에서 모바일 입장권을 바로 확인할 수 있어요.",
  },
  {
    icon: ScanLine,
    title: "현장 입장",
    description: "행사 당일 입구에서 QR 코드를 스캔하면 별도 서류 없이 바로 입장할 수 있어요.",
  },
];

const REFUND_TIERS = [
  { threshold: "행사 시작 7일 전까지", rate: "100%" },
  { threshold: "행사 시작 3일 전까지", rate: "50%" },
  { threshold: "행사 시작 1일 전까지", rate: "30%" },
  { threshold: "행사 시작일 당일 이후", rate: "환불 불가" },
];

const REFUND_NOTES = [
  "환불 신청은 별도 심사 없이 자동으로 승인돼요.",
  "환불은 '내 예약 내역'에서 결제 정보를 연 뒤 '환불 신청' 메뉴로 진행합니다.",
  "무료(0원) 티켓은 결제 자체가 없어 환불 대상이 아니에요.",
  "환불 처리 결과는 결제 수단에 따라 영업일 기준 3~5일 정도 소요될 수 있어요.",
];

export function SupportContent() {
  return (
    <div className={styles.page}>
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>이용 방법</h2>
        <div className={styles.stepGrid}>
          {USAGE_STEPS.map((step, index) => (
            <div key={step.title} className={styles.stepCard}>
              <div className={styles.stepHeader}>
                <span className={styles.stepNumber}>{index + 1}</span>
                <step.icon className={styles.stepIcon} />
              </div>
              <p className={styles.stepTitle}>{step.title}</p>
              <p className={styles.stepDescription}>{step.description}</p>
            </div>
          ))}
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>환불 정책</h2>
        <div className={styles.tierTable}>
          {REFUND_TIERS.map((tier) => (
            <div key={tier.threshold} className={styles.tierRow}>
              <span className={styles.tierThreshold}>{tier.threshold}</span>
              <span className={styles.tierRate} data-blocked={tier.rate === "환불 불가"}>
                {tier.rate}
              </span>
            </div>
          ))}
        </div>
        <ul className={styles.policyList}>
          {REFUND_NOTES.map((note) => (
            <li key={note} className={styles.policyItem}>
              {note}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
