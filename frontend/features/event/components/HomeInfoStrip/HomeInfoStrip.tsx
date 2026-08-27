"use client";

import { QrCode, LayoutGrid, Bookmark } from "lucide-react";
import styles from "./HomeInfoStrip.module.css";

const ITEMS = [
    { icon: QrCode, title: "QR티켓 하나로 간편 입장", desc: "모바일 QR 티켓으로 빠르고 간편하게 입장하세요." },
    { icon: LayoutGrid, title: "카테고리별 맞춤 탐색", desc: "관심 분야 박람회를 카테고리로 빠르게 찾아보세요." },
    { icon: Bookmark, title: "북마크로 관심 박람회 관리", desc: "마음에 드는 박람회를 저장하고 한눈에 확인하세요." },
];

export function HomeInfoStrip() {
    return (
        <div className={styles.wrap}>
            {ITEMS.map(({ icon: Icon, title, desc }) => (
                <div key={title} className={styles.item}>
                    <Icon size={24} className={styles.icon} aria-hidden />
                    <div>
                        <p className={styles.title}>{title}</p>
                        <p className={styles.desc}>{desc}</p>
                    </div>
                </div>
            ))}
        </div>
    );
}