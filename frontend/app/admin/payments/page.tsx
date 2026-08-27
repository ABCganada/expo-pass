import { AdminAdSettlementDashboardContent } from "@/features/payment/components/AdminAdSettlementDashboardContent";
import { AdminPaymentDashboardContent } from "@/features/payment/components/AdminPaymentDashboardContent";
import { AdminPaymentLogListContent } from "@/features/payment/components/AdminPaymentLogListContent";
import styles from "./page.module.css";

export default function AdminPaymentsPage() {
  return (
    <section className={styles.page}>
      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>행사 정산 현황</h2>
        <AdminPaymentDashboardContent />
      </div>

      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>광고 정산 현황</h2>
        <AdminAdSettlementDashboardContent />
      </div>

      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>결제 로그</h2>
        <AdminPaymentLogListContent />
      </div>
    </section>
  );
}
