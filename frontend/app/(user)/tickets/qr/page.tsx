import { QrCode } from "lucide-react";
import { FeaturePlaceholder } from "@/features/shared/components/FeaturePlaceholder";

export default function QrTicketPage() {
  return <FeaturePlaceholder icon={QrCode} title="QR 티켓" description="박람회 입장 시 제시할 QR 코드를 크게 표시하는 전용 화면입니다." />;
}
