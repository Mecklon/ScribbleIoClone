import { useMemo ,useEffect} from "react";
import { Client } from "@stomp/stompjs";

export function useWebSocket(token) {
  const client = useMemo(() => {
    return new Client({
      brokerURL: "ws://localhost:9090/ws", 
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      connectHeaders: {
        Authorization: `Bearer ${token}`, 
      },
   
      reconnectDelay: 5000, 
    });
  }, [token]);

  useEffect(() => {
    client.activate();  

    return () => {
      client.deactivate(); 
    };
  }, [client]);

  return client;
}
