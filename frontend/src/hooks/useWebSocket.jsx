import { useMemo } from "react";
import { Client } from "@stomp/stompjs";

export function useWebSocket(token) {
  const client = useMemo(() => {
    return new Client({
      brokerURL: "ws://localhost:9090/ws", 
      connectHeaders: {
        Authorization: `Bearer ${token}`, 
      },
   
      reconnectDelay: 5000, 
    });
  }, [token]);

  return client;
}
