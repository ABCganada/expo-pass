import { AdminPaymentDashboardContent } from "@/features/payment/components/AdminPaymentDashboardContent";
import styles from "./page.module.css";

export default function AdminPaymentsPage() {
  return (
    <section className={styles.page}>
      <AdminPaymentDashboardContent />
    </section>
  );
}
