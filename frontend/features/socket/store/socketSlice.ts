import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export type SocketState = "connecting" | "live" | "offline";

interface SocketStatusState {
  state: SocketState;
  error: string | null;
  connectedAt: string | null;
}

const initialState: SocketStatusState = {
  state: "offline",
  error: null,
  connectedAt: null,
};

const socketSlice = createSlice({
  name: "socket",
  initialState,
  reducers: {
    socketConnecting(state) {
      state.state = "connecting";
      state.error = null;
    },
    socketConnected(state, action: PayloadAction<string>) {
      state.state = "live";
      state.error = null;
      state.connectedAt = action.payload;
    },
    socketDisconnected(state) {
      state.state = "offline";
    },
    socketFailed(state, action: PayloadAction<string>) {
      state.state = "offline";
      state.error = action.payload;
    },
  },
});

export const {
  socketConnecting,
  socketConnected,
  socketDisconnected,
  socketFailed,
} = socketSlice.actions;
export const socketReducer = socketSlice.reducer;
