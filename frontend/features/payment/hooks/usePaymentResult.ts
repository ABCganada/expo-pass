"use client";

import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import { queryErrorMessage } from "@/features/store/api/queryError";
import {
  useConfirmPaymentMutation,
  useFailPaymentMutation,
} from "../api/paymentApi";
import type { OrderType } from "../types/payment";

export type PaymentResultStatus = "loading" | "success" | "fail" | "invalid";

export interface PaymentResult {
  status: PaymentResultStatus;
  orderId: string | null;
  amount: number | null;
  failReason: string | null;
}

// 토스 결제위젯이 successUrl로 돌려주는 쿼리파라미터: paymentKey, orderId, amount
// failUrl로 돌려주는 쿼리파라미터: code, message, orderId
// orderType은 호출 측(예약/광고)이 지정한다 — confirm/fail API가 orderType으로 분기하기 때문.
export function usePaymentResult(orderType: OrderType): PaymentResult {
  const searchParams = useSearchParams();
  const [confirmPayment] = useConfirmPaymentMutation();
  const [failPayment] = useFailPaymentMutation();
  const requested = useRef(false);

  const orderId = searchParams.get("orderId");
  const paymentKey = searchParams.get("paymentKey");
  const amountParam = searchParams.get("amount");
  const message = searchParams.get("message");
  const code = searchParams.get("code");

  const [result, setResult] = useState<PaymentResult>(() =>
    orderId
      ? {
          status: "loading",
          orderId,
          amount: amountParam ? Number(amountParam) : null,
          failReason: null,
        }
      : { status: "invalid", orderId: null, amount: null, failReason: null },
  );

  useEffect(() => {
    if (requested.current || !orderId) return;
    requested.current = true;

    if (paymentKey && amountParam) {
      confirmPayment({
        orderId,
        request: {
          orderType,
          pgOrderId: orderId,
          paymentKey,
          amount: Number(amountParam),
        },
      })
        .unwrap()
        .then((payment) => {
          setResult({
            status: "success",
            orderId: payment.orderId,
            amount: payment.amount,
            failReason: null,
          });
        })
        .catch((error) => {
          setResult({
            status: "fail",
            orderId,
            amount: Number(amountParam),
            failReason: queryErrorMessage(error, "결제 승인에 실패했습니다."),
          });
        });
      return;
    }

    const reason = message ?? code ?? "결제가 실패했습니다.";
    failPayment({ orderId, request: { orderType, reason } })
      .unwrap()
      .then(() => {
        setResult({
          status: "fail",
          orderId,
          amount: null,
          failReason: reason,
        });
      })
      .catch((error) => {
        setResult({
          status: "fail",
          orderId,
          amount: null,
          failReason: queryErrorMessage(error, reason),
        });
      });
  }, [
    orderId,
    paymentKey,
    amountParam,
    message,
    code,
    orderType,
    confirmPayment,
    failPayment,
  ]);

  return result;
}
