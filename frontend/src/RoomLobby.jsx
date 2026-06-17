import React, { useEffect, useRef, useState } from "react";
import useWebSocketContext from "./hooks/useWebSocketContext";
import { useNavigate, useParams } from "react-router-dom";
import useGetFetch from "./hooks/useGetFetch";
import Image from "./hooks/Image";
import { useSelector } from "react-redux";
import rolling from './assets/rolling.gif';
import usePostFetch from "./hooks/usePostFetch";
import { BiDoorOpen } from "react-icons/bi";
import { motion } from "motion/react";
import avatar from './assets/defaultAvatar.webp'



function RoomLobby() {
    console.log("in lobby")
    
    const auth= useSelector(store=>store.auth)
    
    const {roomId} = useParams();
    console.log(roomId)

    const {loading, error, fetch} = useGetFetch();
    const {loading: startingGame, error: gameStartError, fetch:startGameFetch} = usePostFetch();
    const {fetch: exitFetch, loading:exiting} = useGetFetch();
    
    const navigate = useNavigate();

    const [players, setPlayers] = useState([])

    const { client, wsConnected } = useWebSocketContext();

    const [host , setHost] = useState(null);

    const [rounds, setRounds] = useState(3);
    const [timePerRound, setTimePerRound] = useState(40);
    const [hostUsername, setHostUsername] = useState("")

    const timeInputRef = useRef();
    const roundInputRef = useRef();

    const customWordsInputRef = useRef();
    const checkBoxRef = useRef();

    const [customWordsError,setCustomWordError] = useState(null);

    const [showComfirmationBox, setShowConfirmationBox] = useState(false);

    const joinGameRef = useRef(false);

    


    useEffect(() => {
        if (!wsConnected) return;
        if (!roomId) {
            console.log("no room id")
            navigate("/",{replace:true});
            return;
        }

        const sub = client.subscribe(
            "/topic/room/" + roomId,(payload) => {

                const event = JSON.parse(payload.body)
                if(event.type === "NEW_MEMBER_JOINED"){
                    if (event.initiator.id === auth.id) return;
                    setPlayers(prev => [
                        ...prev,
                        event.initiator
                    ]);    
                }else if(event.type === "SETTINGS_CHANGED" && host !== auth.id){
                    if(event.data.rounds!=rounds){
                        setRounds(event.data.rounds)
                    }
                    (event.data.timePerRound!=timePerRound)
                    if(event.data.timePerRound!=timePerRound){
                        setTimePerRound(event.data.timePerRound)
                    }
                }else if(event.type === "PLAYERS_SWITCHING_TO_GAME" && host!==auth.id){
                    joinGameRef.current = true;
                    console.log("switching to game")
                    navigate("/room/"+roomId,{replace:true});
                }else if(event.type === "PLAYER_EXIT"){
                    setPlayers(prev=>{return prev.filter(item=> item.id!==event.initiator.id)})
                }
            }
        );
        const join = async () => {
            const data =
            await fetch("/joinRoom/" + roomId);
            setPlayers(data.players);
            setHost(data.host);
            setHostUsername(data.hostUsername)
        };
        join();
        return () => sub.unsubscribe();
    }, [wsConnected, roomId]);
   

    const handleMouseOver = ()=>{
        linkRef.current.innerText = "http://localhost:5173/roomLobby/"+roomId
    }

    const handleMouseLeave = ()=>{
        linkRef.current.innerText = "hover your mouse to get the link"
    }
    const linkRef = useRef()

    const handleSettingsChange = ()=>{
        client.publish({
            destination: "/app/settingsChange",
            body: JSON.stringify({
                rounds: roundInputRef.current.value,
                timePerRound: timeInputRef.current.value,
                roomId
            }),
        });
    }

    const startGame = async()=>{

        setCustomWordError(null)
        const validLetterTest = /^[a-zA-Z ]+$/;

        if (customWordsInputRef.current.value.trim()!=0) {
            const words = customWordsInputRef.current.value
                .toLowerCase()
                .split(",")
                .map(word => word.trim())
                .filter(word => word.length > 0);

            
            for (let i = 0; i < words.length; i++) {
                if ( words[i].length > 20) {
                    setCustomWordError(words[i]+" greater then 20 characters in length")
                    return
                }
                if (words[i].length < 3 ) {
                    setCustomWordError(words[i]+" is smaller than 3 characters in length")
                    return
                }
                if(!validLetterTest.test(words[i])){
                    setCustomWordError("\""+words[i]+"\" contains special character, not allowed")
                    return
                }
            }

            if(checkBoxRef.current.checked && words.length  < 10){
                setCustomWordError("You need to enter atleast 10 words for custom words only mode");
                return;
            }
        }  
        
        if(players.length<=1){
            setCustomWordError("You need atleast two members to start the game");
            return;
        }
        
        await startGameFetch("/startGame",{
            roomId,
            timePerRound: timeInputRef.current.value,
            rounds: roundInputRef.current.value,
            customWords: customWordsInputRef.current.value.toUpperCase().trim(),
            onlyCustomWords: checkBoxRef.current.checked
        })
        console.log("gamestart exit")
        navigate("/room/"+roomId,{replace:true});
    }


    const exitLobby = async () => {
        //edge case: another race condition the game is switched from lobby to game, grace time is given to players to join the game , which now if exactly at the same time the player exits but his frontend also got the join game message and also hit the join game route , now the player is in race with himself
        console.log("self exit")
        await exitFetch("/exitLobby");
        navigate("/", { replace: true });
    };



    useEffect(()=>{
        return ()=>{
            if(joinGameRef.current = false){
                exitLobby()
            }
        }
    },[])
    
  return (
    <div className="p-5 flex flex-col gap-5 h-screen">
        {
            showComfirmationBox &&
            <motion.div onClick={()=>{setShowConfirmationBox(false)}} 
            initial={{ backdropFilter: "blur(0px)" }}
            animate={{ backdropFilter: "blur(8px)" }}
            exit={{ backdropFilter: "blur(0px)" }}
            transition={{ duration: 0.3 }}
            className="fixed inset-0 flex items-center justify-center">
                <motion.div
                initial={{ y: 150,opacity: 0,}}
                animate={{ y: 0,opacity: 1, }}
                exit={{ y: 150,opacity: 0,}}
                transition={{ duration: 0.3,ease: [0.16, 1, 0.3, 1] }}
                 onClick={e=>e.stopPropagation()} className="bg-white p-7  border border-gray-600 shadow-2xl rounded-3xl text-3xl font-semibold">
                    Are you sure you want to exit this room
                    <div className="flex justify-around mt-15 text-white">
                        <button onClick={(e)=>{
                            exitLobby()
                        }} className="p-3 rounded-2xl px-6 duration-300 hover:scale-110 bg-red-500 flex gap-2 items-center">Yes {exiting ? <img src={rolling} className="h-12"></img>:""}</button>
                        <button onClick={()=>setShowConfirmationBox(false)} className="p-3 rounded-2xl px-6 duration-300 hover:scale-110 bg-gray-700">No</button>
                    </div>
                </motion.div>
            </motion.div>
        }
        <BiDoorOpen onClick={()=>setShowConfirmationBox(true)} className="fixed right-6 top-6 text-5xl duration-300 hover:scale-110 text-white"/>
        {
            auth.id !== host &&
            <div className="text-4xl font-bold text-white">
                Rounds: {rounds}&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Time Per Round: {timePerRound}
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                Host: {hostUsername}
            </div>
        }
      <div className="flex gap-5 h-[80%] w-full">
        {
            auth.id === host && <div className="w-160 shrink-0">
            <div className="text-white text-5xl font-semibold text-center mb-4">Settings</div>
            <div className="bg-white p-2 rounded-lg">
                <div className="text-center text-3xl border-b pb-2">Lobby</div>
                <div className="text-2xl font-semibold py-1">Rounds</div>
                <select ref={roundInputRef} onChange={handleSettingsChange} defaultValue={3} name="" className="border w-full p-2 px-3 text-2xl border-stone-700" id="">
                    <option value="1">1</option>
                    <option value="2">2</option>
                    <option value="3">3</option>
                    <option value="4">4</option>
                    <option value="5">5</option>
                    <option value="6">6</option>
                </select>
                <div className="text-2xl font-semibold py-1">Draw time in seconds</div>
                 <select ref={timeInputRef} onChange={handleSettingsChange} defaultValue={40} name="" className="border w-full p-2 px-3 text-2xl border-stone-700" id="">
                    <option value="10">10</option>
                    <option value="20">20</option>
                    <option value="30">30</option>
                    <option value="40">40</option>
                    <option value="50">50</option>
                    <option value="60">60</option>
                    <option value="70">70</option>
                    <option value="80">80</option>
                    <option value="90">90</option>
                    <option value="100">100</option>
                    <option value="110">110</option>
                    <option value="120">120</option>
                    <option value="10000">10000</option>
                </select>
                <div className="text-2xl font-semibold py-1">Custom words</div>
                <textarea name="" ref={customWordsInputRef} placeholder="type in you custom words seperated by columns (minimum length of 3 characters and maximum of 20 words)" className="border border-stone-700 p-2 w-full text-xl resize-none h-70" id="">
                    
                </textarea>
                {
                    customWordsError &&
                    <div className="text-red-600 text-md">{customWordsError}</div>
                }
                <label className="gap-2 mb-2 flex items-center font-semibold" htmlFor="check">
                    Use custom words only
                    <input ref={checkBoxRef} id="check" type="checkbox" />
                </label>
                <button disabled={startingGame} onClick={startGame} className="text-center flex justify-center items-center gap-3 bg-green-600 text-white text-4xl rounded-md p-2 px-4 w-full">
                    Start Game
                    {
                        startingGame &&
                        <img src={rolling} className="h-10" alt="" />
                    }
                </button>
            </div>
        </div>
        }
        <div className="grow flex flex-col ">
            <div className="text-white text-5xl font-semibold text-center mb-4">Players</div>
            <div className="grow w-full overflow-auto flex  gap-3 flex-wrap p-3 items-baseline">
                {
                    players.map(item=>{
                        return <div key={item.email} className="flex flex-col p-2 w-60 items-center justify-center  ">
                                    <Image fallback={avatar} className="w-[80%] bg-blac  rounded-full aspect-square" path={item.profile}></Image>
                                    <div className="text-2xl text-white font-semibold text-center">{item.username}</div>
                                </div>
                    })
                }

            </div>
        </div>
      </div>
        <div className="w-full  grow">
            <div className="text-white text-center text-5xl font-semibold">Invite your friends!</div>
            <div onMouseLeave={handleMouseLeave} onMouseOver={handleMouseOver} className="flex mt-6">
                <div ref={linkRef} className="grow bg-white text-center p-2 px-3 text-stone-800 text-3xl
                ">hover your mouse to get the link</div>
                <div onClick={()=>{
                    navigator.clipboard.writeText("http://localhost:5173/roomLobby/"+roomId);
                }} className="bg-yellow-600 p-2 px-3 text-3xl text-white cursor-pointer">copy</div>
            </div>
        </div>
   
    </div>
  );
}

export default RoomLobby;
