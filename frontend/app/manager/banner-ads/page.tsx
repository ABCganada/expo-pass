"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
import { useGetMyAdsQuery } from "@/features/marketing/api/marketerBannerApi";
import { MarketerAdCard } from "@/features/marketing/components/MarketerAdCard/MarketerAdCard";
import { RegisterAdModal } from "@/features/marketing/components/RegisterAdModal/RegisterAdModal";
import { AdStatsModal } from "@/features/marketing/components/AdStatsModal/AdStatsModal";
import { AdPaymentModal } from "@/features/marketing/components/AdPaymentModal/AdPaymentModal";
import { AdCheckoutModal } from "@/features/marketing/components/AdCheckoutModal/AdCheckoutModal";
import type { MarketerBannerAd } from "@/features/marketing/types/marketerBanner";
import styles from "./page.module.css";

export default function BannerAdsPage() {
  const { data: ads = [], isLoading, isError } = useGetMyAdsQuery();
  const [registerOpen, setRegisterOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<MarketerBannerAd | undefined>(undefined);
  const [statsTarget, setStatsTarget] = useState<MarketerBannerAd | undefined>(undefined);
  const [paymentTarget, setPaymentTarget] = useState<MarketerBannerAd | undefined>(undefined);
  const [checkoutTarget, setCheckoutTarget] = useState<MarketerBannerAd | undefined>(undefined);

  const handleEdit = (ad: MarketerBannerAd) => setEditTarget(ad);
  const handlePay = (ad: MarketerBannerAd) => setCheckoutTarget(ad);
  const handleStats = (ad: MarketerBannerAd) => setStatsTarget(ad);
  const handlePaymentDetail = (ad: MarketerBannerAd) => setPaymentTarget(ad);

  const handleCloseRegister = () => setRegisterOpen(false);
  const handleCloseEdit = () => setEditTarget(undefined);
  const handleCloseStats = () => setStatsTarget(undefined);
  const handleClosePayment = () => setPaymentTarget(undefined);
  const handleCloseCheckout = () => setCheckoutTarget(undefined);

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div />
        <button type="button" className={styles.btnRegister} onClick={() => setRegisterOpen(true)}>
          <Plus size={16} />
          광고 등록
        </button>
      </div>

      {isLoading && <p className={styles.state}>광고를 불러오는 중...</p>}
      {isError && <p className={styles.stateError}>광고 목록을 불러오지 못했습니다.</p>}

      {!isLoading && !isError && ads.length === 0 && (
        <div className={styles.empty}>
          <p className={styles.emptyTitle}>등록된 광고가 없습니다.</p>
          <p className={styles.emptyDesc}>오른쪽 상단 버튼으로 첫 광고를 등록해보세요.</p>
        </div>
      )}

      {!isLoading && ads.length > 0 && (
        <div className={styles.grid}>
          {ads.map((ad) => (
            <MarketerAdCard
              key={ad.id}
              ad={ad}
              onEdit={handleEdit}
              onPay={handlePay}
              onStats={handleStats}
              onPaymentDetail={handlePaymentDetail}
            />
          ))}
        </div>
      )}

      {registerOpen && (
        <RegisterAdModal onClose={handleCloseRegister} />
      )}
      {editTarget && (
        <RegisterAdModal editTarget={editTarget} onClose={handleCloseEdit} />
      )}
      {statsTarget && (
        <AdStatsModal ad={statsTarget} onClose={handleCloseStats} />
      )}
      {paymentTarget && (
        <AdPaymentModal
          orderId={paymentTarget.orderId}
          adTitle={paymentTarget.title}
          onClose={handleClosePayment}
        />
      )}
      {checkoutTarget && (
        <AdCheckoutModal
          orderId={checkoutTarget.orderId}
          amount={checkoutTarget.totalAmount}
          orderName={checkoutTarget.title}
          onClose={handleCloseCheckout}
        />
      )}
    </div>
  );
}
