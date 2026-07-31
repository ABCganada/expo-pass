import { Building2 } from "lucide-react";
import { FeaturePlaceholder } from "@/features/shared/components/FeaturePlaceholder";

export default function AdminEventsPage() {
  return (
    <FeaturePlaceholder
      icon={Building2}
      title="박람회 관리"
      description="전체 박람회 목록을 조회하고, 기본 정보·콘텐츠·이미지·티켓·상태를 관리하는 화면입니다."
    />
  );
}