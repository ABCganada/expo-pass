"use client";

import { useRef, useState, useEffect } from "react";
import {
  QrCode,
  CheckCircle2,
  AlertCircle,
  Camera,
  Keyboard,
} from "lucide-react";
import jsQR from "jsqr";
import { useCheckinMutation } from "../../api/adminApi";
import styles from "./CheckinContent.module.css";

export function CheckinContent() {
  const inputRef = useRef<HTMLInputElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const frameIdRef = useRef<number | null>(null);
  const lastScannedRef = useRef<string>("");

  const [mode, setMode] = useState<"input" | "camera">("input");
  const [cameraError, setCameraError] = useState<string>("");
  const [checkinResult, setCheckinResult] = useState<{
    type: "success" | "error";
    message: string;
    orderId?: string;
  } | null>(null);

  const [checkin, { isLoading }] = useCheckinMutation();

  const scanFrame = () => {
    if (!videoRef.current || !canvasRef.current) return;
    if (mode !== "camera") return;

    const video = videoRef.current;
    if (video.videoWidth === 0 || video.videoHeight === 0) {
      frameIdRef.current = requestAnimationFrame(scanFrame);
      return;
    }

    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    const code = jsQR(imageData.data, imageData.width, imageData.height);

    if (code && code.data !== lastScannedRef.current) {
      lastScannedRef.current = code.data;
      processCheckin(code.data);
    } else {
      frameIdRef.current = requestAnimationFrame(scanFrame);
    }
  };

  const processCheckin = async (qrCode: string) => {
    if (!qrCode) return;

    setCheckinResult(null);
    try {
      const result = await checkin(qrCode).unwrap();
      setCheckinResult({
        type: "success",
        message: `체크인 완료: ${result.orderId}`,
        orderId: result.orderId,
      });
      lastScannedRef.current = "";
      setTimeout(() => {
        if (mode === "camera") {
          frameIdRef.current = requestAnimationFrame(scanFrame);
        }
      }, 3000);
    } catch (e) {
      const errorMsg = e instanceof Error ? e.message : "체크인에 실패했습니다";
      console.error("[CheckinContent] Checkin error:", errorMsg);
      setCheckinResult({
        type: "error",
        message: errorMsg,
      });
      lastScannedRef.current = "";
      setTimeout(() => {
        if (mode === "camera") {
          frameIdRef.current = requestAnimationFrame(scanFrame);
        }
      }, 3000);
    }
  };

  useEffect(() => {
    if (mode === "input") {
      inputRef.current?.focus();
      if (frameIdRef.current) cancelAnimationFrame(frameIdRef.current);
    } else {
      const startScanning = async () => {
        try {
          setCameraError("");
          const stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: "environment" },
          });
          if (streamRef.current) {
            streamRef.current.getTracks().forEach((t) => t.stop());
          }
          streamRef.current = stream;
          if (videoRef.current) videoRef.current.srcObject = stream;
          frameIdRef.current = requestAnimationFrame(scanFrame);
        } catch (err) {
          setCameraError(
            err instanceof Error ? err.message : "카메라에 접근할 수 없습니다",
          );
        }
      };
      startScanning();
    }

    return () => {
      if (frameIdRef.current) cancelAnimationFrame(frameIdRef.current);
    };
    // scanFrame is intentionally not in dependencies - it's defined in the component body
    // and doesn't depend on external state
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mode]);

  async function handleInputCheckin() {
    const qrCode = inputRef.current?.value.trim();
    if (!qrCode) {
      setCheckinResult({ type: "error", message: "QR 코드를 입력해주세요" });
      return;
    }

    setCheckinResult(null);
    try {
      const result = await checkin(qrCode).unwrap();
      setCheckinResult({
        type: "success",
        message: `체크인 완료: ${result.orderId}`,
        orderId: result.orderId,
      });
      if (inputRef.current) inputRef.current.value = "";
      setTimeout(() => inputRef.current?.focus(), 100);
    } catch (e) {
      setCheckinResult({
        type: "error",
        message: e instanceof Error ? e.message : "체크인에 실패했습니다",
      });
    }
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <p className={styles.subtitle}>QR 코드를 스캔하거나 입력하세요</p>
      </header>

      <div className={styles.modeToggle}>
        <button
          type="button"
          className={styles.modeButton}
          data-active={mode === "camera"}
          onClick={() => setMode("camera")}
        >
          <Camera size={18} />
          카메라
        </button>
        <button
          type="button"
          className={styles.modeButton}
          data-active={mode === "input"}
          onClick={() => setMode("input")}
        >
          <Keyboard size={18} />
          입력
        </button>
      </div>

      <div className={styles.card}>
        {mode === "camera" ? (
          <>
            <div className={styles.cameraContainer}>
              <video
                ref={videoRef}
                autoPlay
                playsInline
                className={styles.video}
              />
              <canvas ref={canvasRef} className={styles.canvas} />
              <div className={styles.scanGuide} />
            </div>
            {cameraError && (
              <div className={styles.result} data-type="error">
                <AlertCircle size={20} />
                <span>{cameraError}</span>
              </div>
            )}
          </>
        ) : (
          <>
            <div className={styles.qrIcon}>
              <QrCode size={48} />
            </div>

            <div className={styles.inputGroup}>
              <input
                ref={inputRef}
                type="text"
                autoFocus
                className={styles.input}
                placeholder="QR 코드 입력..."
                disabled={isLoading}
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleInputCheckin();
                }}
              />
              <button
                type="button"
                className={styles.submitButton}
                onClick={handleInputCheckin}
                disabled={isLoading}
              >
                {isLoading ? "처리 중..." : "체크인"}
              </button>
            </div>
          </>
        )}

        {checkinResult && (
          <div className={styles.result} data-type={checkinResult.type}>
            {checkinResult.type === "success" ? (
              <CheckCircle2 size={20} />
            ) : (
              <AlertCircle size={20} />
            )}
            <span>{checkinResult.message}</span>
          </div>
        )}
      </div>
    </div>
  );
}
