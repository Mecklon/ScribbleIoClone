import React from "react";

function RoomLobby() {
  return (
    <div className="p-5 flex flex-col gap-5 h-screen bg-red-500">
      <div className="flex gap-5 h-[80%] w-full">
        <div className="w-160 shrink-0 bg-red-800">
            <div className="text-white text-5xl font-semibold text-center mb-4">Settings</div>
            <div className="bg-white p-2">
                <div className="text-center text-3xl border-b pb-2">Lobby</div>
                <div className="text-2xl font-semibold py-1">Rounds</div>
                <select name="" className="border w-full p-2 px-3 text-2xl border-stone-700" id="">
                    <option value="1">1</option>
                    <option value="2">2</option>
                    <option value="3">3</option>
                    <option value="4">4</option>
                    <option value="5">5</option>
                    <option value="6">6</option>
                </select>
                <div className="text-2xl font-semibold py-1">Draw time in seconds</div>
                 <select name="" className="border w-full p-2 px-3 text-2xl border-stone-700" id="">
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
        <div className="grow flex flex-col  bg-red-800">
            <div className="text-white text-5xl font-semibold text-center mb-4">Players</div>
            <div className="bg-white grow w-full overflow-auto flex  gap-3 flex-wrap p-3 items-baseline">
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
                <div className="flex flex-col p-2 w-60 items-center justify-center bg-red-500 ">
                    <img src="" className="w-[80%] bg-black rounded-full aspect-square" alt="" />
                    <div className="text-2xl text-center">Mecklon Fernandes</div>
                </div>
               
            </div>
        </div>
      </div>
        <div className="w-full bg-red-800 grow">
            <div className="text-white text-center text-5xl font-semibold">Invite your friends!</div>
            <div className="flex mt-6">
                <div className="grow bg-white text-center p-2 px-3 text-stone-800 text-3xl
                ">Hover over me to see the link</div>
                <div className="bg-yellow-600 p-2 px-3 text-3xl text-white">copy</div>
            </div>
        </div>
   
    </div>
  );
}

export default RoomLobby;
