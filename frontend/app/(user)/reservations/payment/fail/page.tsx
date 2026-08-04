import { Suspense } from "react";
import { PaymentResultContent } from "@/features/reservation/components/PaymentResultContent";

export default function ReservationPaymentFailPage() {
  return (
    <Suspense>
      <PaymentResultContent />
    </Suspense>
  );
}
