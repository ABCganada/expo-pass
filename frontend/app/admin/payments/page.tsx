import { AdminPaymentDashboardContent } from "@/features/payment/components/AdminPaymentDashboardContent";
import { AdminPaymentLogListContent } from "@/features/payment/components/AdminPaymentLogListContent";
import styles from "./page.module.css";

export default function AdminPaymentsPage() {
  return (
    <section className={styles.page}>
      <AdminPaymentDashboardContent />

      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>결제 로그</h2>
        <AdminPaymentLogListContent />
      </div>
    </section>
  );
}
