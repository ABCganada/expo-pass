import { LineChart } from "lucide-react";
import { FeaturePlaceholder } from "@/features/shared/components/FeaturePlaceholder";

export default function ManagerSettlementsPage() {
  return (
    <FeaturePlaceholder
      icon={LineChart}
      title="정산 현황"
      description="담당 행사의 정산 목록과 상세 내역을 확인하는 화면입니다."
    />
  );
}
