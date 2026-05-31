import React, { useEffect, useRef, useState } from "react";
import { PiScribbleDuotone } from "react-icons/pi";
import { useWebSocket } from "./hooks/useWebSocket";
import useWebSocketContext from "./hooks/useWebSocketContext";
import { useNavigate, useParams } from "react-router-dom";
import usePostFetch from "./hooks/usePostFetch";
import useGetFetch from "./hooks/useGetFetch";
import Image from "./hooks/Image";
import avatar from './assets/defaultAvatar.webp'
import { RiWifiOffLine } from "react-icons/ri";
import { useSelector } from "react-redux";
import { button, div } from "motion/react-client";
import { RxDividerVertical } from "react-icons/rx";

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
    const auth = useSelector(store=>store.auth)
    const [words, setWords] = useState([])
    const {error:wordsLoadingError, loading:wordsLoading , fetch:fetchWords} = useGetFetch();
    const {error:wordLoadingError, loading:wordLoading , fetch:fetchWord} = useGetFetch();
    const {error:chooseWordError, loading:chooseWordLoading, fetch: fetchChooseWord} = usePostFetch();
    const {error:chatSendError, loading:sendingChat, fetch: fetchSendWord} = usePostFetch();
    const [events, setEvents] = useState([])
    const [eventIndex, setEventIndex] = useState(null)

    const bufferEvents = useRef(false);
    const eventBuffer = useRef([]);
    const navigate = useNavigate();


    useEffect(() => {
            if (!wsConnected) return;
            if (!roomId) {
                navigate("/");
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
                        let newEvents = [];
                        if("points" in event.data){
                            newEvents.push({
                                type:"POINTS",
                                playerPoints:event.data.points
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
                        setRoomState(prev=>{
                            return {...prev, 
                                status: "DRAWER_SELECTING_WORD",
                                phaseDeadLine: event.data.phaseDeadLine,
                                drawer: event.data.drawer,
                                drawerId: event.data.drawerId,
                                currentHiddenWord:null,
                                currentRound:event.data.newRoundIndex
                            }
                            console.log("round end: ", data.drawer, data.drawerId, auth.id, auth.id === data.drawerId)
                        })
                        let newEvents = [];
                        newEvents.push({
                            type:"NEW_ROUND"
                        })
                        newEvents.push({
                                type:"POINTS",
                                playerPoints:event.data.points
                            })
                        newEvents.push({
                            type:"DRAWER_INTRO",
                        })
                        setEvents(newEvents)
                        setEventIndex(0)
                    }else if(event.type === "GAME_END"){
                        console.log("game ended routing to leaderboard")
                        console.log("leaderboard/"+roomId)
                        navigate("/leaderboard/"+roomId)
                    }
                }
            );
            const join = async () => {
                // remember to account for the the base snapshot changing the slide events
                bufferEvents.current = true;
                let data = await fetch("joinGame/"+roomId)
                bufferEvents.current = false;
                data.chats.reverse();
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
                        if("points" in event.data){
                            newEvents.push({
                                type:"POINTS",
                                playerPoints:event.data.points
                            })
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
                        data = {...data, 
                                status: "DRAWER_SELECTING_WORD",
                                phaseDeadLine: event.data.phaseDeadLine,
                                drawer: event.data.drawer,
                                drawerId: event.data.drawerId,
                                currentHiddenWord:null,
                                currentRound:event.data.newRoundIndex
                            }
                        newEvents = [];
                        newEvents.push({
                            type:"NEW_ROUND"
                        })
                        newEvents.push({
                                type:"POINTS",
                                playerPoints:event.data.points
                            })
                        newEvents.push({
                            type:"DRAWER_INTRO",
                        })
                    }else if(event.type === "GAME_END"){
                        game.status = "ENDED"
                    }   
                }

                if(data.status === "ENDED"){
                    navigate("/leaderboard/"+roomId)
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
                console.log("my turn getting")
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
        console.log(data)
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
   
  return (
    <div className="py-7 px-5 flex flex-col gap-3 h-screen">
      <div className="flex bg-red-600 gap-1 items-center">
        <PiScribbleDuotone className="text-6xl" />
        <div className="text-4xl font-bold">Scribblr</div>
      </div>
      <div className="flex bg-red-600 font-bold text-4xl p-5 gap-1 items-center">
        { roomState.status === "PLAYERS_SWITCHING_TO_GAME" &&`Waiting for players to join..... ${timer!=null ? timer:""}` }
        { roomState.status === "DRAWER_SELECTING_WORD" &&`${roomState.drawer}  is selecting a word..... ${timer!=null ? timer:""}` }
        { roomState.status === "PLAYER_DRAWING" && auth.id !== roomState.drawerId &&`Round:(${roomState.currentRound}/${roomState.rounds})${roomState.drawer} is drawing..... ${timer!=null ? "("+timer+"/"+roomState.timePerRound+")":""}   ${roomState.currentHiddenWord}` }
        { roomState.status === "PLAYER_DRAWING" && auth.id === roomState.drawerId &&`Round:(${roomState.currentRound}/${roomState.rounds}) You are drawing..... ${timer!=null ? "("+timer+"/"+roomState.timePerRound+")":""}   ${roomState.currentWord}` }
      </div>
      <div className="grow w-full flex gap-3 bg-red-600 min-h-0">
        <div className="w-75 bg-red-700 gap-3 shrink-0 flex flex-col h-full min-h-0">
            <div className="grow w-full overflow-auto noScrollBar">
                {
                    roomState.players.map(data=>{
                        return <div key={data.player.id} className="flex p-1 relative">
                                    {!data.connected &&
                                        <RiWifiOffLine className="top-1 left-1 absolute text-2xl"/>
                                    }
                                    <div className="grow text-center flex flex-col justify-center">
                                        <div className="font-semibold text-xl">{data.player.username}</div>
                                        <div className="text-xl">Points: {data.points}</div>
                                    </div>
                                    <Image path={data.player.profile} fallback={avatar} className="aspect-square rounded-full shrink-0 bg-red-950 w-16"></Image>
                                </div>
                    })
                }
            </div>
            <div className="text-center bg-amber-600 font-bold text-2xl p-2 rounded-md">Kick player</div>
        </div>
        <div className="grow relative bg-red-700">
                {
                    eventIndex!=null && events.length!=0 &&
                    <div className="bg-black/50 h-full text-white p-3">
                        {
                            events[eventIndex].type === "POINTS" && 
                            <div className="flex flex-col h-full text-4xl justify-center items-center">
                                {
                                    events[eventIndex].playerPoints.map((player)=>{
                                        return <div key={player.id} className="flex gap-3">
                                            <div>{player.username}</div>
                                            <div>{player.points}</div>
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
                                        return <div onClick={()=>chooseWord(word)} className="bg-black/24 p-2 px-4 rounded-md cursor-pointer duration-300 hover:bg-black/50 hover:scale-110">{word}</div>
                                    })}

                                </div>
                            </div>
                        }
                        {
                            events[eventIndex].type === "NEW_ROUND" &&
                            <div className="text-center text-4xl font-semibold">
                                Round: {roomState.currentRound}
                            </div>
                        }
                    </div>
                }

        </div>
        <div className="w-100 bg-red-700 gap-2 p-2 flex shrink-0 flex-col h-full">
            <div className="bg-red-800 grow min-h-0 overflow-auto noScrollBar flex flex-col-reverse text-xl p-1">
                {
                    messages.map(item => {
                    return item.correct === true ? (
                        <div key={item.id} className="flex gap-1 px-2 text-green-500">
                            <div className="font-semibold">{item.username} : </div>
                            <div>guessed the word</div>
                        </div>
                    ) : (
                        <div key={item.id} className="flex gap-1 px-2">
                            <div className="font-semibold">{item.username} : </div>
                            <div>{item.message}</div>
                        </div>
                    )
                })
                }
            </div>
            <input onKeyDown={chatInput} type="text" className="bg-white p-2 outline-0 ring-0 duration-300 focus:ring-3 ring-blue-600 rounded-md px-4 text-xl " placeholder="text here, one word entries will be considered guesses" />
        </div>
      </div>
    </div>
  );
}

export default Room;
