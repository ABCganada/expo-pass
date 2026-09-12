import { EventSelectContent } from "@/features/reservation/components/EventSelectContent";

export default function CheckInPage() {
  return (
    <EventSelectContent
      basePath="/manager/check-in"
      title="QR 체크인"
      subtitle="체크인을 진행할 행사를 선택하세요"
      phaseFilter="ONGOING"
      showSearch
    />
  );
}
