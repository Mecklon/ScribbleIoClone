import { useContext } from "react";
import { websocketContext } from "../context/WebSocketProvider";

const useWebSocketContext = ()=>{
    return useContext(websocketContext)
}

export default useWebSocketContext