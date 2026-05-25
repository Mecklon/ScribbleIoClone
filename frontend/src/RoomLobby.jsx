import React, { useEffect, useRef, useState } from "react";
import useWebSocketContext from "./hooks/useWebSocketContext";
import { useNavigate, useParams } from "react-router-dom";
import useGetFetch from "./hooks/useGetFetch";
import Image from "./hooks/Image";
import { useSelector } from "react-redux";
function RoomLobby() {


    const auth= useSelector(store=>store.auth)
    console.log(auth)

    const {roomId} = useParams();

    const {loading, error, fetch} = useGetFetch();
    
    const navigate = useNavigate();

    const [players, setPlayers] = useState([])

    const { client, wsConnected } = useWebSocketContext();

    const [host , setHost] = useState(null);

    const [rounds, setRounds] = useState(3);
    const [timePerRound, setTimePerRound] = useState(40);
    const [hostUsername, setHostUsername] = useState("")

    const timeInputRef = useRef();
    const roundInputRef = useRef();

    useEffect(() => {
        if (!wsConnected) return;
        if (!roomId) {
            navigate("/");
            return;
        }

        const sub = client.subscribe(
            "/topic/room/" + roomId,(payload) => {

                const event = JSON.parse(payload.body)
                console.log(event)
                if(event.type === "NEW_MEMBER_JOINED"){
                    if (event.initiator === auth.username) return;
                    setPlayers(prev => [
                        ...prev,
                        event.initiator
                    ]);
                }else if(event.type === "SETTINGS_CHANGED" && host !== auth.id){
                    if(event.data.rounds!=rounds){
                        setRounds(event.data.rounds)
                    }
                    console.log("time",event.data.timePerRound)
                    console.log(event.data.timePerRound!=timePerRound)
                    if(event.data.timePerRound!=timePerRound){
                        console.log("setting to",event.data.timePerRound)
                        setTimePerRound(event.data.timePerRound)
                    }
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
    console.log(host)
    console.log("sj:",timePerRound)

    const handleMouseOver = ()=>{
        linkRef.current.innerText = "http://localhost:5173/roomLobby/"+roomId
    }
    console.log(players)

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
    
  return (
    <div className="p-5 flex flex-col gap-5 h-screen bg-red-500">
        {
            auth.id !== host &&
            <div className="text-4xl font-bold">
                Rounds: {rounds}&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Time Per Round: {timePerRound}
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                Host: {hostUsername}
            </div>
        }
      <div className="flex gap-5 h-[80%] w-full">
        {
            auth.id === host && <div className="w-160 shrink-0 bg-red-800">
            <div className="text-white text-5xl font-semibold text-center mb-4">Settings</div>
            <div className="bg-white p-2">
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
                </select>
                <div className="text-2xl font-semibold py-1">Custom words</div>
                <textarea name="" placeholder="type in you custom words seperated by columns (minimum length of 3 characters and maximum of 20 words)" className="border border-stone-700 p-2 w-full text-xl resize-none h-70" id="">
                    
                </textarea>
                <label className="gap-2 mb-2 flex items-center font-semibold" htmlFor="check">
                    Use custom words only
                    <input id="check" type="checkbox" />
                </label>
                <button className="text-center bg-green-600 text-white text-4xl rounded-md p-2 px-4 w-full">
                    Start Game
                </button>
            </div>
        </div>
        }
        <div className="grow flex flex-col  bg-red-800">
            <div className="text-white text-5xl font-semibold text-center mb-4">Players</div>
            <div className="bg-white grow w-full overflow-auto flex  gap-3 flex-wrap p-3 items-baseline">
                {
                    players.map(item=>{
                        return <div key={item.email} className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                                    <Image className="w-[80%] bg-black rounded-full aspect-square" path={item.profile}></Image>
                                    <div className="text-2xl text-center">{item.username}</div>
                                </div>
                    })
                }

            </div>
        </div>
      </div>
        <div className="w-full bg-red-800 grow">
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
