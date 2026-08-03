import { Suspense } from "react";
import { PaymentResultContent } from "@/features/reservation/components/PaymentResultContent";

export default function ReservationPaymentSuccessPage() {
  return (
    <Suspense>
      <PaymentResultContent />
    </Suspense>
  );
}
