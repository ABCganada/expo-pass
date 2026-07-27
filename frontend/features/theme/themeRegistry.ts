export const themeRegistry = {
  light: { label: "라이트", colorScheme: "light" },
  dark: { label: "다크", colorScheme: "dark" },
} as const;

export type ThemeName = keyof typeof themeRegistry;

export const DEFAULT_THEME: ThemeName = "light";

export function isThemeName(value: string | null): value is ThemeName {
  return value !== null && value in themeRegistry;
}
