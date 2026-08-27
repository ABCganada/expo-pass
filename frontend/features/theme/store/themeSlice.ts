import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { DEFAULT_THEME, type ThemeName } from "../themeRegistry";

interface ThemeState {
  name: ThemeName;
  hydrated: boolean;
}

const initialState: ThemeState = {
  name: DEFAULT_THEME,
  hydrated: false,
};

const themeSlice = createSlice({
  name: "theme",
  initialState,
  reducers: {
    initializeTheme(state, action: PayloadAction<ThemeName>) {
      state.name = action.payload;
      state.hydrated = true;
    },
    setTheme(state, action: PayloadAction<ThemeName>) {
      state.name = action.payload;
      state.hydrated = true;
    },
    toggleTheme(state) {
      state.name = state.name === "dark" ? "light" : "dark";
      state.hydrated = true;
    },
  },
});

export const {
  initializeTheme,
  setTheme,
  toggleTheme,
} = themeSlice.actions;
export const themeReducer = themeSlice.reducer;
