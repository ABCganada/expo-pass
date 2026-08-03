import { ManagerSettlementListContent } from "@/features/payment/components/ManagerSettlementListContent";
import styles from "./page.module.css";

export default function ManagerSettlementsPage() {
  return (
    <section className={styles.page}>
      <ManagerSettlementListContent />
    </section>
  );
}
