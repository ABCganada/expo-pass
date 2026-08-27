import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import "../features/theme/themes/light.css";
import "../features/theme/themes/dark.css";
import { SocketBridge } from "@/features/socket/SocketBridge";
import { StoreProvider } from "@/features/store/StoreProvider";
import { ThemeSync } from "@/features/theme/store/ThemeSync";
import React from "react";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "EXPO PASS",
  description: "박람회 예약과 현장 입장을 연결하는 EXPO PASS",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="ko"
      data-theme="light"
      className={`${geistSans.variable} ${geistMono.variable}`}
    >
      <body>
        <StoreProvider>
          <ThemeSync />
          <SocketBridge />
          {children}
        </StoreProvider>
      </body>
    </html>
  );
}
