import { CalendarDays } from "lucide-react";
import { FeaturePlaceholder } from "@/features/shared/components/FeaturePlaceholder";
import { BannerSlider } from "@/features/marketing/components/BannerSlider";

export default function ExhibitionsPage() {
  return (
    <>
      <FeaturePlaceholder icon={CalendarDays} title="박람회 예약하기" description="행사 목록을 조회하고 원하는 박람회의 예약을 신청하는 화면입니다." />
      <BannerSlider />
    </>
  );
}
