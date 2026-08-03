import { LineChart } from "lucide-react";
import { FeaturePlaceholder } from "@/features/shared/components/FeaturePlaceholder";

export default function AdminPaymentsPage() {
  return (
    <FeaturePlaceholder
      icon={LineChart}
      title="매출 현황"
      description="전체 매출 대시보드와 결제 로그 내역을 확인하는 화면입니다."
    />
  );
}
