import React, { createContext, useState } from "react";
import { useWebSocket } from "./hooks/useWebSocket";

export const websocketContext = createContext(null);

function WebSocketProvider({ children }) {
  const ws = useWebSocket(localStorage.getItem("JwtToken"));
  const [wsConnected, setWsConnected] = useState(false)
  ws.onConnect = ()=>{
    setWsConnected(true)
  }
  ws.onDisconnect = ()=>{
    "hey disconnected"
  }
  return (
    <websocketContext.Provider value={{client: ws, wsConnected}}>{children}</websocketContext.Provider>
  );
}

export default WebSocketProvider;
