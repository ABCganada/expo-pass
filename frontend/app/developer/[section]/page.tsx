import { notFound } from "next/navigation";
import styles from "../page.module.css";

const SECTION_LABELS = {
  metrics: "메트릭",
  traces: "트레이스",
  logs: "로그",
} as const;

type DeveloperSection = keyof typeof SECTION_LABELS;

export default async function DeveloperSectionPage({
  params,
}: {
  params: Promise<{ section: string }>;
}) {
  const { section } = await params;

  if (!(section in SECTION_LABELS)) notFound();

  const label = SECTION_LABELS[section as DeveloperSection];

  return (
    <section className={styles.page}>
      <div>
        <h1>{label} 페이지 준비 중입니다.</h1>
        <p>Monitoring Core 연동 화면을 준비하고 있습니다.</p>
      </div>
    </section>
  );
}
