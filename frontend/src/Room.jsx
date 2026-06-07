import React, { useEffect, useRef, useState } from "react";
import { PiScribbleDuotone } from "react-icons/pi";
import { useWebSocket } from "./hooks/useWebSocket";
import useWebSocketContext from "./hooks/useWebSocketContext";
import { useNavigate, useParams } from "react-router-dom";
import usePostFetch from "./hooks/usePostFetch";
import useGetFetch from "./hooks/useGetFetch";
import Image from "./hooks/Image";
import { IoAlarmOutline } from "react-icons/io5";
import avatar from './assets/defaultAvatar.webp'
import { RiWifiOffLine } from "react-icons/ri";
import { useSelector } from "react-redux";
import { button, div } from "motion/react-client";
import { RxDividerVertical } from "react-icons/rx";
import { FaPencilAlt } from "react-icons/fa";
import { RiDeleteBin6Line } from "react-icons/ri";
import rolling from './assets/rolling.gif';
import { LuPaintBucket } from "react-icons/lu";
import { IoMdColorFill } from "react-icons/io";
import { BiDoorOpen } from "react-icons/bi";

const colors = [
    "#808080",
    "#A9A9A9",
    "#D3D3D3",
    "#F5F5F5",
    "black",
    "red",
    "orange",
    "yellow",
    "green",
    "blue",
    "indigo",
    "violet",
    "brown",
    "pink",
    "cyan",
    "lime",
    "teal",
    "navy",
    "maroon",
    "gold"
];

const isSame = (idx ,color, pixels) => {
    const [r2, g2, b2, a2] = color
    const r = pixels[idx];
    const g = pixels[idx + 1];
    const b = pixels[idx + 2];
    const a = pixels[idx + 3];
    return r === r2 && g === g2 && b === b2 && a === a2;
};

const setColor = (color, pixels, idx) => {
    const [r, g, b, a] = color

    pixels[idx] = r;
    pixels[idx + 1] = g;
    pixels[idx + 2] = b;
    pixels[idx + 3] = a;
};

const floodFillIterative = (pixels, x , y , oldColor, newColor)=>{
    const queue = [[x,y]];
    
    while(queue.length!=0){
    const [currX, currY] = queue.pop();
    if(currX === -1 || currY === -1 || currX === 1000 || currY === 1000)continue;
    const idx = (currY * 1000 + currX)*4;
    if(!isSame(idx, oldColor, pixels))continue;
        setColor(newColor, pixels, idx)
        queue.push([currX-1,currY])
        queue.push([currX,currY-1])
        queue.push([currX+1,currY])
        queue.push([currX,currY+1])    
    }
}


function Room() {

    const {roomId} = useParams();
    const [messages, setMessages] = useState([])
    const {client,wsConnected} = useWebSocketContext();
    const {loading, error, fetch} = useGetFetch();
    const [timer,setTimer] = useState(null)
    const [roomState, setRoomState] = useState({
            players:[],
            host:null, 
            hostUsername:null, 
            rounds:null, 
            timePerRound:null,
            status: null,
            phaseDeadLine: null,
            currentRound:null,
            drawer: null,
            drawerId:null,
            currentWord: null,
            currentHiddenWord:null,
        })
    const drawerIdRef = useRef(roomState.drawerId)
    useEffect(()=>{
        drawerIdRef.current = roomState.drawerId
    },[roomState.drawerId])

    const auth = useSelector(store=>store.auth)
    const authIdRef = useRef(auth.id)
    useEffect(()=>{
        authIdRef.current = auth.id
    },[auth.id])

    const [words, setWords] = useState([])
    const {error:wordsLoadingError, loading:wordsLoading , fetch:fetchWords} = useGetFetch();
    const {error:wordLoadingError, loading:wordLoading , fetch:fetchWord} = useGetFetch();
    const {error:chooseWordError, loading:chooseWordLoading, fetch: fetchChooseWord} = usePostFetch();
    const {fetch: exitGameFetch, loading: exiting} = useGetFetch();
    const {error:chatSendError, loading:sendingChat, fetch: fetchSendWord} = usePostFetch();
    const [events, setEvents] = useState([])
    const [eventIndex, setEventIndex] = useState(null)

    const bufferEvents = useRef(false);
    const eventBuffer = useRef([]);
    const navigate = useNavigate();


    const [showComfirmationBox, setShowConfirmationBox] = useState(false);

    const moveToLeaderBoard = useRef(false);
 

    useEffect(() => {
            if (!wsConnected) return;
            if (!roomId) {
                navigate("/",{replace:true});
                return;
            }
            const sub = client.subscribe(
                "/topic/room/" + roomId,(payload) => {
                    const event = JSON.parse(payload.body)
                    if(bufferEvents.current===true){
                        eventBuffer.current.push(event)
                        return;
                    }

                    if(event.type === "NEW_MESSAGE"){
                        setMessages(prev=>{
                            return [{message: event.data.message, username: event.initiator.username, id : event.data.id, correct:event.data.correct},...prev]
                        })
                    }else if(event.type === "MEMBER_JOINED_GAME"){
                        setRoomState((prev) => {
                            return {
                                ...prev,
                                players: prev.players.map((player) => {
                                    if (player.player.id !== event.initiator.id) {
                                        return player;
                                    } else {
                                        return {
                                            ...player,
                                            connected: true
                                        };
                                    }
                                })
                            };
                        });
                    }else if(event.type === "PLAYER_DISCONNECTED"){
                        setRoomState((prev) => {
                            return {
                                ...prev,
                                players: prev.players.map((player) => {
                                    if (player.player.id !== event.initiator.id) {
                                        return player;
                                    } else {
                                        return {
                                            ...player,
                                            connected: false
                                        };
                                    }
                                })
                            };
                        });
                    }else if(event.type === "DRAWER_SELECTING_WORD"){
                
                        setRoomState(prev=>{
                            return {...prev, 
                                    status: event.type,
                                    phaseDeadLine: event.data.phaseDeadLine,
                                    drawer: event.data.drawer,
                                    drawerId: event.data.drawerId,
                                    currentHiddenWord:null
                                }
                        })
                        const ctx = canvasRef.current.getContext("2d");
                        ctx.clearRect(0,0,canvasRef.current.width, canvasRef.current.height)
                        ctx.beginPath()
                        let newEvents = [];
                        if("points" in event.data){
                            newEvents.push({
                                type:"POINTS",
                                playerPoints:event.data.points
                            })
                            setRoomState((prev)=>{
                                event.data.points.sort((a, b) => b.points - a.points)
                                let rankMap = new Map();
                                let rank = 0, curr = -1;
                                for(let i = 0;i<event.data.points.length;i++){
                                    const player = event.data.points[i];
                                    if(curr!=player.points){
                                        curr = player.points;
                                        rank++;
                                    }
                                    rankMap.set(player.id, rank)
                                }
                                let map = new Map();
                                event.data.points.forEach(player=> map.set(player.id, player.points))
                                return {...prev, players: prev.players.map(info=>{
                                    return {
                                        ...info,
                                        points: info.points+ map.get(info.player.id),
                                        rank: rankMap.get(info.player.id)
                                    }
                                })}
                            })
                        }
                        newEvents.push({
                            type:"DRAWER_INTRO",
                        })
                        
                        setEvents(newEvents)
                        setEventIndex(0)
                    }else if(event.type === "PLAYER_DRAWING"){
                        setEvents([])
                        setEventIndex(0)
                        setRoomState((prev)=>{
                            return {
                                ...prev,
                                status: event.type,
                                drawer: event.initiator.username,
                                drawerId: event.initiator.id,
                                currentHiddenWord: event.data.word,
                                currentWord:null,
                                phaseDeadLine: event.data.phaseDeadLine
                            }
                        })
                    }else if(event.type === "ROUND_END"){

                        setRoomState(prev => {
                            const totalPoints = prev.players.map(info => ({
                                id: info.player.id,
                                points: info.points + (event.data.points.find(
                                    p => p.id === info.player.id
                                )?.points ?? 0)
                            }));

                            totalPoints.sort((a, b) => b.points - a.points);

                            const rankMap = new Map();
                            let rank = 0;
                            let curr = -1;

                            for (const player of totalPoints) {
                                if (player.points !== curr) {
                                    curr = player.points;
                                    rank++;
                                }
                                rankMap.set(player.id, rank);
                            }

                            const roundPointsMap = new Map();
                            event.data.points.forEach(p =>
                                roundPointsMap.set(p.id, p.points)
                            );

                            return {
                                ...prev,
                                status: "DRAWER_SELECTING_WORD",
                                phaseDeadLine: event.data.phaseDeadLine,
                                drawer: event.data.drawer,
                                drawerId: event.data.drawerId,
                                currentHiddenWord: null,
                                currentRound: event.data.newRoundIndex,
                                players: prev.players.map(info => ({
                                    ...info,
                                    points: info.points + (roundPointsMap.get(info.player.id) ?? 0),
                                    rank: rankMap.get(info.player.id)
                                }))
                            };
                        });

                        const ctx = canvasRef.current.getContext("2d");
                        ctx.clearRect(0,0,canvasRef.current.width, canvasRef.current.height)
                        ctx.beginPath()
                        let newEvents = [];
                        newEvents.push({
                            type:"POINTS",
                            playerPoints:event.data.points
                        })
                        newEvents.push({
                            type:"NEW_ROUND"
                        })
                        newEvents.push({
                            type:"DRAWER_INTRO",
                        })
                        setEvents(newEvents)
                        setEventIndex(0)
                    }else if(event.type === "CANVAS_EVENT"){
                        if(authIdRef.current === drawerIdRef.current)return;
                        const events = event.data;
                        const canvas = canvasRef.current;
                        const ctx = canvas.getContext("2d",{ willReadFrequently: true });
                        ctx.lineCap = "round";
                        ctx.lineJoin = "round";
                        for(let i = 0;i< events.length;i++){
                            let canvasEvent = events[i];
                            if(canvasEvent.type === "STROKE"){
                                if(ctx.lineWidth!== canvasEvent.lineWidth || ctx.strokeStyle!==canvasEvent.color){
                                    ctx.stroke();
                                    ctx.beginPath()
                                    ctx.lineWidth = canvasEvent.lineWidth
                                    ctx.strokeStyle = canvasEvent.color
                                }

                                ctx.moveTo(canvasEvent.from.x, canvasEvent.from.y);   
                                ctx.lineTo(canvasEvent.to.x, canvasEvent.to.y); 
                                
                            }else{
                                ctx.stroke();


                                const x = canvasEvent.from.x
                                const y = canvasEvent.from.y

                                const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                                const pixels = imageData.data;

                                const oldColors = canvasEvent.fillColors.oldColor
                                const newColors = canvasEvent.fillColors.newColor

                                floodFillIterative(pixels, x,y,oldColors, newColors)
                                ctx.putImageData(imageData, 0, 0);

                                ctx.beginPath();                            
                            }
                        }
                        ctx.stroke()
                    }else if(event.type === "GAME_END"){
                        moveToLeaderBoard.current = true
                        navigate("/leaderboard/"+roomId,{replace:true})
                    }else if(event.type === "PLAYER_EXIT"){
                        console.log("got exit message");
                        setRoomState(prev=>{
                            return {...prev,
                                players: prev.players.filter(playerData=>{
                                    return playerData.player.id != event.initiator.id;
                                })
                            }
                        })
                    }
                }
            );
            const join = async () => {
                bufferEvents.current = true;
                let data = await fetch("joinGame/"+roomId)
                bufferEvents.current = false;
                data.chats
                let newEvents = []
                for(let i =0;i<eventBuffer.current.length;i++){
                    let event = eventBuffer.current[i];

                    if(event.type === "NEW_MESSAGE"){
                        data.chats.push({message: event.data.message, username: event.initiator.username, id : event.data.id})
                    }else if(event.type === "MEMBER_JOINED_GAME"){
                        data.players = data.players.map(player=>{
                            if (player.player.id !== event.initiator.id) {
                                        return player;
                                    } else {
                                        return {
                                            ...player,
                                            connected: true
                                        };
                                    }
                            })
                    }else if(event.type === "PLAYER_DISCONNECTED"){
                        data.players = data.players.map(player=>{
                            if (player.player.id !== event.initiator.id) {
                                        return player;
                                    } else {
                                        return {
                                            ...player,
                                            connected: false
                                        };
                                    }
                            })
                    }else if(event.type === "DRAWER_SELECTING_WORD"){
                        data = {
                            ...data,
                            status: event.type,
                            phaseDeadLine: event.data.phaseDeadLine,
                            drawer: event.data.drawer,
                            drawerId: event.data.drawerId,
                            currentHiddenWord:null
                        }
                        newEvents = []

                        const ctx = canvasRef.current.getContext("2d");
                        ctx.clearRect(0,0,canvasRef.current.width, canvasRef.current.height)
                        ctx.beginPath()

                        if("points" in event.data){
                            newEvents.push({
                                type:"POINTS",
                                playerPoints:event.data.points
                            })

                            event.data.points.sort((a, b) => b.points - a.points)
                            let rankMap = new Map();
                            let rank = 0, curr = -1;
                            for(let i = 0;i<event.data.points.length;i++){
                                const player = event.data.points[i];
                                if(curr!=player.points){
                                    curr = player.points;
                                    rank++;
                                }
                                rankMap.set(player.id, rank)
                            }

                            let map = new Map();
                            event.data.points.forEach(player=> map.set(player.id, player.points))

                            data = {...data, players: data.players.map(info=>{
                                return {
                                    ...info,
                                    points: info.points+ map.get(info.player.id),
                                    rank: rankMap.get(info.player.id)
                                }
                            })}
                        }
                        newEvents.push({
                            type:"DRAWER_INTRO",
                        })
                        
                    }else if(event.type === "PLAYER_DRAWING"){
                        newEvents = []
                        data = {
                            ...data,
                            status: event.type,
                            drawer: event.initiator.username,
                            drawerId: event.data.id,
                            currentWord: null,
                            currentHiddenWord:event.data.word,
                            phaseDeadLine: event.data.phaseDeadLine,
                        }
                    }else if(event.type === "ROUND_END"){

                        const roundPointsMap = new Map();
                        event.data.points.forEach(player => {
                            roundPointsMap.set(player.id, player.points);
                        });

                        const totalScores = data.players.map(info => ({
                            id: info.player.id,
                            points: info.points + (roundPointsMap.get(info.player.id) ?? 0)
                        }));

                        totalScores.sort((a, b) => b.points - a.points);

                        const rankMap = new Map();
                        let rank = 0;
                        let curr = -1;

                        for (const player of totalScores) {
                            if (player.points !== curr) {
                                curr = player.points;
                                rank++;
                            }
                            rankMap.set(player.id, rank);
                        }

                        data = {
                            ...data,
                            status: "DRAWER_SELECTING_WORD",
                            phaseDeadLine: event.data.phaseDeadLine,
                            drawer: event.data.drawer,
                            drawerId: event.data.drawerId,
                            currentHiddenWord: null,
                            currentRound: event.data.newRoundIndex,
                            players: data.players.map(info => ({
                                ...info,
                                points: info.points + (roundPointsMap.get(info.player.id) ?? 0),
                                rank: rankMap.get(info.player.id)
                            }))
                        };
                        
                        const ctx = canvasRef.current.getContext("2d");
                        ctx.clearRect(0,0,canvasRef.current.width, canvasRef.current.height)
                        ctx.beginPath()
                        newEvents = [];
                        newEvents.push({
                            type:"POINTS",
                            playerPoints:event.data.points
                        })
                        newEvents.push({
                            type:"NEW_ROUND"
                        })
                        newEvents.push({
                            type:"DRAWER_INTRO",
                        })
                    }else if(event.type === "CANVAS_EVENT"){
                        data.canvasEvents.push(event.data)

                    }else if(event.type === "GAME_END"){
                        game.status = "ENDED"
                    }else if(event.type === "PLAYER_EXIT"){
                        console.log("got exit message");
                        
                        data = {...data,
                                players: data.players.filter(playerData=>{
                                    return playerData.player.id != event.initiator.id;
                                })
                            }
                    } 
                }

                if(data.status === "ENDED"){
                    moveToLeaderBoard.current = true
                    navigate("/leaderboard/"+roomId,{replace:true})
                }

                if(data.canvasEvents.length!=0){
                    console.log("redrawing")
                    const canvasEvents = data.canvasEvents
                    const canvas = canvasRef.current;
                    const ctx = canvas.getContext("2d",{ willReadFrequently: true });
                    ctx.lineCap = "round";
                    ctx.lineJoin = "round";

                    for(let i =0;i< canvasEvents.length;i++){
                        let canvasEvent = canvasEvents[i];
                        if(canvasEvent.type === "STROKE"){
                            if(ctx.lineWidth!== canvasEvent.lineWidth || ctx.strokeStyle!==canvasEvent.color){
                                ctx.stroke();
                                ctx.beginPath()
                                ctx.lineWidth = canvasEvent.lineWidth
                                ctx.strokeStyle = canvasEvent.color
                            }

                            ctx.moveTo(canvasEvent.from.x, canvasEvent.from.y);   
                            ctx.lineTo(canvasEvent.to.x, canvasEvent.to.y); 
                            
                        }else{
                            ctx.stroke();


                            const x = canvasEvent.from.x
                            const y = canvasEvent.from.y

                            const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                            const pixels = imageData.data;

                            const oldColors = canvasEvent.fillColors.oldColor
                            const newColors = canvasEvent.fillColors.newColor

                            floodFillIterative(pixels, x,y,oldColors, newColors)
                            ctx.putImageData(imageData, 0, 0);

                            ctx.beginPath();                            
                        }
                    }
                }

                eventBuffer.current = []
                setEvents(newEvents)
                setEventIndex(0)

                setMessages(data.chats)
                delete data.chats
                setRoomState(data)


            };
            join();
            return () => sub.unsubscribe();
        }, [wsConnected, roomId]);

    useEffect(()=>{
        if(events.length==0 || events.length <= eventIndex)return;
        let event = events[eventIndex];
        let signal = null;
        if(event.type === "POINTS"){
            signal = setTimeout(()=>{
                setEventIndex(prev=>prev+1);
            },7000)
        }else if(event.type === "NEW_ROUND"){
            signal = setTimeout(() => {
                setEventIndex(prev=>prev+1);
            }, 5000);
            
        }
        return ()=>{
            if(signal!=null){
                clearTimeout(signal)
            }
        }
        
    },[eventIndex, events])


    useEffect(() => {
        if ( roomState.status === null || roomState.phaseDeadLine === null) return;
        const signal = setInterval(() => {
        let diff =roomState.phaseDeadLine - Date.now();
        if (diff < 0) {
            setTimer(0);
            clearInterval(signal);
            return;
            }
            setTimer(Math.floor(diff / 1000));
        }, 1000);
        return () => clearInterval(signal);
    }, [roomState.status, roomState.phaseDeadLine]);


    useEffect(()=>{
        const handleRoomStateChange = async()=>{
            if(roomState.status==null)return;
            if(roomState.status=="DRAWER_SELECTING_WORD" && roomState.drawerId==auth.id){
                const data = await fetchWords("/getRandomWords")
                setWords(data);
            }else if(roomState.status=="PLAYER_DRAWING" && roomState.drawerId==auth.id  && roomState.currentWord === null){
                const data = await fetchWord("/getCurrentWord")
                setRoomState(prev=>{
                    return {
                        ...prev,
                        currentWord:data
                    }
                })
            }
        }
        handleRoomStateChange();
    },[roomState.status, roomState.drawerId])


    const chatInput = async(e)=>{
        if(e.code!=="Enter")return;
        if(e.target.value.trim() ==="")return;
        const data = await fetchSendWord("/chatInput", {message:e.target.value})
        let mask = data.mask;
        if(mask.length!==0){
            setRoomState((prev)=>{
                if(prev.currentHiddenWord==null)return prev;
                let newHiddenWord = "";
                let currentHiddenWord = prev.currentHiddenWord
                for(let i =0;i< mask.length;i++){
                    if(mask.charAt(i)!=='_'){
                        newHiddenWord += mask.charAt(i)
                    }else if(currentHiddenWord && currentHiddenWord.length> i && currentHiddenWord.charAt(i) && currentHiddenWord.charAt(i)!=='_'){
                        newHiddenWord += currentHiddenWord.charAt(i);
                    }else{
                        newHiddenWord += "_"
                    }
                }
                return {...prev,currentHiddenWord:newHiddenWord}
            })
        }
        e.target.value = ""
    }

    const chooseWord = async(word)=>{
        await fetchChooseWord('/chooseWord', {word})
    }

    const exitGame = async()=>{
        await exitGameFetch("/exitGame");
        navigate("/",{replace:true})
    }


    
    // canvas 


    const canvasRef = useRef(null);
    const cursorSize= useRef(4)
    const color = useRef("black")
    const mode = useRef("STROKE")
    const canvasEvent = useRef([])
    const {fetch: sendCanvasEvents} = usePostFetch()
    const sendingEvents = useRef(false)
    const currentColorDisplay = useRef(null)


    useEffect(()=>{
        if(roomState.status !== "PLAYER_DRAWING" || roomState.drawerId !== auth.id) return;
        const signal = setInterval(async() => {
            if(canvasEvent.current.length===0 || sendingEvents.current )return;
            sendingEvents.current = true
            const batch = canvasEvent.current;
            canvasEvent.current = []
            let data = null;
            try {
                data = await sendCanvasEvents("/sendCanvasEvents", batch);
            }catch {
                canvasEvent.current.unshift(...batch);
            }finally {
                sendingEvents.current = false;
            }
        }, 600);

        return ()=>{
            clearInterval(signal)
        }
    },[roomState.drawerId, auth.id, roomState.status])

    useEffect(()=>{
            const canvas = canvasRef.current;
            const ctx = canvas.getContext("2d",{ willReadFrequently: true });
            let oldX=0;
            let oldY=0;
            let isMouseDown = false;
            const tempCanvas = document.createElement("canvas");

            const mouseDownEvent = (e)=>{
                if(mode.current === "STROKE"){
                    ctx.lineCap = "round";
                    ctx.lineJoin = "round";
                    oldX = e.offsetX/canvas.clientWidth*canvas.width
                    oldY = e.offsetY/canvas.clientHeight*canvas.height
                    isMouseDown = true;
                    ctx.beginPath();
                }else{
                    const x = Math.round(e.offsetX / canvas.clientWidth * canvas.width);
                    const y = Math.round(e.offsetY / canvas.clientHeight * canvas.height);

                    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                    const pixels = imageData.data;

                    const idx = (y * canvas.width + x) * 4;

                    const r = pixels[idx];
                    const g = pixels[idx + 1];
                    const b = pixels[idx + 2];
                    const a = pixels[idx + 3];

                    const oldColors = [r,g,b,a]
                    tempCanvas.width = 1;
                    tempCanvas.height = 1;
                    
                    const tempCtx = tempCanvas.getContext("2d",{ willReadFrequently: true });
                    tempCtx.fillStyle = color.current;
                    tempCtx.fillRect(0, 0, 1, 1);
                    
                    const newColors = Array.from(tempCtx.getImageData(0, 0, 1, 1).data);
                    
                    if (
                        oldColors[0] === newColors[0] &&
                        oldColors[1] === newColors[1] &&
                        oldColors[2] === newColors[2] &&
                        oldColors[3] === newColors[3]
                    ) {
                        return;
                    }
                    canvasEvent.current.push({
                        type:"FILL",
                        from:{
                            x,
                            y
                        },
                        fillColors:{
                            oldColor: oldColors,
                            newColor: newColors
                        }
                    })
                    floodFillIterative(pixels, x,y,oldColors, newColors)
                    ctx.putImageData(imageData, 0, 0);

                }
            }
            const mouseUpOrMoueLeaveEvent = (e)=>{
                isMouseDown = false;
            }
            const mouseMoveEvent = (e)=>{
                if(!isMouseDown || mode.current === "FILL")return
                ctx.moveTo(oldX, oldY);   
                ctx.lineCap = "round";
                ctx.lineWidth = cursorSize.current
                ctx.strokeStyle = color.current;
                ctx.lineJoin = "round";

                canvasEvent.current.push({
                    type:"STROKE",
                    color: color.current,
                    lineWidth: cursorSize.current,
                    from:{
                        x: oldX, y:oldY
                    },
                    to:{
                        x: e.offsetX/canvas.clientWidth*canvas.width,
                        y: e.offsetY/canvas.clientHeight*canvas.height
                    },
                })
                
                oldX = e.offsetX/canvas.clientWidth*canvas.width
                oldY = e.offsetY/canvas.clientHeight*canvas.height
                ctx.lineTo(oldX, oldY);   
                ctx.stroke()  
            }
            canvas.addEventListener("mousedown", mouseDownEvent)
            canvas.addEventListener("mouseup", mouseUpOrMoueLeaveEvent)
            canvas.addEventListener("mouseleave", mouseUpOrMoueLeaveEvent)
            canvas.addEventListener("mousemove",mouseMoveEvent)


            return ()=>{
                if(moveToLeaderBoard.current === false){
                    exitGame()
                }
            }

    },[])
   
  return (
    <div className="py-7 px-5 flex flex-col gap-3 h-screen">
        <BiDoorOpen onClick={()=>setShowConfirmationBox(true)} className="text-white text-5xl duration-300 hover:scale-110 fixed top-6 right-6"/>
        {
            showComfirmationBox &&
            <div onClick={()=>{setShowConfirmationBox(false)}} className="fixed inset-0 z-10 backdrop-blur-sm flex items-center justify-center">
                <div onClick={e=>e.stopPropagation()} className="bg-white p-7 shadow-2xl border border-gray-500 rounded-3xl text-3xl font-semibold">
                    Are you sure you want to exit this room
                    <div className="flex justify-around mt-15 text-white">
                        <button onClick={(e)=>{
                            e.stopPropagation();
                            exitGame();
                        }} className="p-3 rounded-2xl px-6 duration-300 hover:scale-110 bg-red-500 flex gap-2 items-center">Yes {exiting ? <img src={rolling} className="h-12"></img>:""}</button>
                        <button onClick={()=>setShowConfirmationBox(false)} className="p-3 rounded-2xl px-6 duration-300 hover:scale-110 bg-gray-700">No</button>
                    </div>
                </div>
            </div>
        }
      <div className="flex gap-1 items-center text-white">
        <PiScribbleDuotone className="text-6xl " />
        <div className="text-4xl font-bold">Scribblr</div>
      </div>
        {
            (roomState.status === "DRAWER_SELECTING_WORD" || roomState.status === "PLAYER_DRAWING" || true) &&  (<>
                <div className="flex h-15 bg-white justify-between font-bold text-4xl p-5 gap-1 items-center">
                    <div className="flex gap-2 items-center"> 
                        <div className="flex items-center relative justify-center">
                        <IoAlarmOutline className="text-7xl scale-110"/>
                        <div className="bg-white h-9 w-9 absolute rounded-full text-2xl top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 flex items-center justify-center">{timer}</div>  
                        </div>
                        <div>Round {roomState.currentRound} of {roomState.rounds} </div>
                    </div>
                    {roomState.currentHiddenWord !==null && <div className="tracking-widest">{roomState.drawerId === auth.id ? roomState.currentWord: roomState.currentHiddenWord}</div>}
                </div>
            </>
            )
        }
      <div className="grow w-full flex gap-3  min-h-0">
        <div className="w-75  gap-3 shrink-0 flex flex-col h-full min-h-0">
            <div className="grow w-full overflow-auto noScrollBar rounded-lg">
                {
                    roomState.players.map((data, index)=>{
                        return <div key={data.player.id} className={`flex p-1 relative ${index/2===0 ? "bg-stone-400":"bg-white"}`}>
                                    {!data.connected &&
                                        <RiWifiOffLine className="top-1 left-1 absolute text-2xl"/>
                                    }
                                    <div className="shrink-0 text-3xl font-semibold flex items-center justify-center  ml-2">
                                    #{data.rank}
                                    </div>
                                    <div className="grow text-center flex flex-col justify-center">
                                        <div className="font-semibold text-xl">{data.player.username}</div>
                                        <div className="text-xl">Points: {data.points}</div>
                                    </div>
                                    <Image path={data.player.profile} fallback={avatar} className="aspect-square rounded-full shrink-0 w-16"></Image>
                                </div>
                    })
                }
            </div>
            <div className="text-center bg-amber-600 font-bold text-2xl p-2 rounded-md">Kick player</div>
        </div>
        <div className="grow relative">
                {
                    eventIndex!=null && events.length!=0 &&
                    <div className="bg-black/50 absolute top-0 left-0 right-0 h-full text-white p-3">
                        {
                            events[eventIndex].type === "POINTS" && 
                            <div className="flex flex-col h-full text-4xl justify-center items-center">
                                {
                                    events[eventIndex].playerPoints.map((player)=>{
                                        return <div key={player.id} className="flex gap-3">
                                            <div>{player.username}</div>
                                            <div className={`${player.points !== 0 ? "text-green-600":""}`}>{player.points!==0 ? "+":""}{player.points}</div>
                                        </div>
                                    })
                                }
                            </div>

                        }
                        {
                            events[eventIndex].type === "DRAWER_INTRO" && roomState.drawerId !== auth.id &&
                            <div className="flex flex-col h-full text-4xl justify-center items-center">
                                {`${roomState.drawer} is picking a word`}
                            </div>
                        }
                        {
                            events[eventIndex].type === "DRAWER_INTRO" && roomState.drawerId === auth.id &&
                            <div className="flex flex-col gap-10 h-full text-4xl justify-center items-center">
                                <div className="text-center text-3xl">Your are the drawer select the word you want to draw: </div>
                                
                                <div className="flex flex-wrap gap-4">
                                    {                                    
                                    words.map(word=>{
                                        return <div key={word} onClick={()=>chooseWord(word)} className="bg-black/24 p-2 px-4 rounded-md cursor-pointer duration-300 hover:bg-black/50 hover:scale-110">{word}</div>
                                    })}

                                </div>
                            </div>
                        }
                        {
                            events[eventIndex].type === "NEW_ROUND" &&
                            <div className="text-center text-7xl h-full font-semibold flex items-center justify-center ">
                                Round: {roomState.currentRound}
                            </div>
                        }
                    </div>
                }
                <canvas  
                        ref={canvasRef} 
                        height="600" width="1000" 
                        className={`w-full bg-white ${(auth.id !== null && roomState.drawerId === auth.id) ?  "pointer-events-auto" : "pointer-events-none"}`}>
                </canvas>
                {roomState.drawerId === auth.id &&
                    <div className={`mt-2 gap-2 flex justify-center`}>
                        <div ref={currentColorDisplay} className="h-14 w-14 aspect-square rounded-md bg-black">
                                
                        </div>
                        <div>
                            <div className="flex">
                                {colors.slice(0, 10).map((c) => (
                                    <div
                                        key={c}
                                        className="h-7 w-7  cursor-pointer"
                                        style={{ backgroundColor: c }}
                                        onClick={() => {
                                            color.current = c;
                                            currentColorDisplay.current.style.backgroundColor = c
                                        }}
                                    />
                                ))}
                            </div>

                            <div className="flex">
                                {colors.slice(10).map((c) => (
                                    <div
                                        key={c}
                                        className="h-7 w-7 cursor-pointer"
                                        style={{ backgroundColor: c }}
                                        onClick={() => {
                                            color.current = c;
                                            currentColorDisplay.current.style.backgroundColor = c
                                        }}
                                    />
                                ))}
                            </div>
                        </div>
                        <div onClick={()=>{mode.current = "STROKE"}} className="h-14 w-14 aspect-square bg-white rounded-md flex justify-center items-center">
                            <FaPencilAlt className="text-4xl"/>
                        </div>
                        <div onClick={()=>{mode.current = "FILL"}}  className="h-14 w-14 aspect-square bg-white rounded-md flex justify-center items-center">
                            <IoMdColorFill className="text-4xl"/>
                        </div>
                        <div onClick={()=>{cursorSize.current = 4}} className="h-14 w-14 aspect-square bg-white rounded-md flex justify-center items-center">
                            <div className="h-4 w-4 bg-black rounded-full"></div>
                        </div>
                        <div onClick={()=>{cursorSize.current = 8}} className="h-14 w-14 aspect-square bg-white rounded-md flex justify-center items-center">
                            <div className="h-6 w-6 bg-black rounded-full"></div>
                        </div>
                        <div onClick={()=>{cursorSize.current = 18}} className="h-14 w-14 aspect-square bg-white rounded-md flex justify-center items-center">
                            <div className="h-8 w-8 bg-black rounded-full"></div>
                        </div>
                        <div onClick={()=>{
                                const ctx = canvasRef.current.getContext("2d");
                                ctx.clearRect(0,0,canvasRef.current.width, canvasRef.current.height)
                                ctx.beginPath()
                        }} className="h-14 w-14 aspect-square bg-white rounded-md flex justify-center items-center">
                            <RiDeleteBin6Line  className="text-4xl"/>
                        </div>
                    </div>
                }
        </div>
        <div className="w-100 bg-white gap-2 p-2 flex shrink-0 flex-col h-full">
            <div className=" grow min-h-0 overflow-auto noScrollBar flex flex-col-reverse text-xl p-1">
                {
                    messages.map((item,index) => {
                    return item.correct === true ? (
                        <div key={item.id} className={`flex gap-1 px-2 text-green-500 ${(index%2===0 && messages.length%2 === 0) || (index%2===1 && messages.length%2 === 1)? "bg-stone-300":"bg-white"}`}>
                            <div className="font-semibold">{item.username} : </div>
                            <div>guessed the word</div>
                        </div>
                    ) : (
                        <div key={item.id} className={`flex gap-1 px-2 ${(index%2===0 && messages.length%2 === 0) || (index%2===1 && messages.length%2 === 1)? "bg-stone-300":"bg-white"}`}>
                            <div className="font-semibold">{item.username} : </div>
                            <div>{item.message}</div>
                        </div>
                    )
                })
                }
            </div>
            <input onKeyDown={chatInput} type="text" className="bg-white p-2 outline-0 ring-1 duration-300 focus:ring-3 ring-blue-600 rounded-md px-4 text-xl " placeholder="text here, one word entries will be considered guesses" />
        </div>
      </div>
    </div>
  );
}

export default Room;
