import { CalendarPlus } from "lucide-react";
import { FeaturePlaceholder } from "@/features/shared/components/FeaturePlaceholder";

export default function AdminEventCreatePage() {
  return (
    <FeaturePlaceholder
      icon={CalendarPlus}
      title="새 행사 등록"
      description="행사 제목·카테고리·담당자를 입력해 DRAFT 상태의 행사를 생성하는 화면입니다."
    />
  );
}