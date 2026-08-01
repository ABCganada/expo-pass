import { Tags } from "lucide-react";
import { FeaturePlaceholder } from "@/features/shared/components/FeaturePlaceholder";

export default function AdminEventCategoriesPage() {
  return (
    <FeaturePlaceholder
      icon={Tags}
      title="카테고리 관리"
      description="행사 카테고리 목록을 조회하고 활성/비활성 상태를 전환하는 화면입니다."
    />
  );
}