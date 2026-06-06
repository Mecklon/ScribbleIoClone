import React, { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import useGetFetch from './hooks/useGetFetch';
import avatar from './assets/defaultAvatar.webp'
import Image from './hooks/Image';
import { IoHomeSharp } from "react-icons/io5";

function Leaderboard() {

    const {roomId} = useParams();
    const {loading, error, fetch} = useGetFetch();
    const [leaderboard,setLeaderBoard]  = useState([])
    const navigate = useNavigate();

    useEffect(()=>{
      const getLeaderboard = async ()=>{
        const data = await fetch("/leaderboard/"+roomId)
        setLeaderBoard(data)
      }
      getLeaderboard()
    },[])


    if(loading){
      return ''
    }

    if(leaderboard.length===0){
      return <div className='text-5xl font-black text-center'>Sorry this room does not exist</div>
    }

  return (
    <div className=' flex items-center justify-center'>
      <div onClick={()=>navigate('/',{replace:true})} className='fixed left-3 top-3 p-3 rounded-lg text-white text-2xl flex gap-2 items-center justify-center bg-blue-500/50'>
        <IoHomeSharp />
        return to home
      </div>
      <table className='text-4xl border-separate border-spacing-x-6 border-spacing-y-4 rounded-lg bg-blue-500/75 text-white mt-20'>
        <tbody>
        {
          leaderboard.map(player =>{
            return <tr key={player.id}>
                    <td>
                      <Image path={player.profile} className='h-20 w-20 bg-red-900 rounded-full' fallback={avatar} /> 
                    </td>
                    <td>{player.username}</td>
                    <td>{player.points}</td>
                  </tr>
          })
        }
          
          
        </tbody>
        
      </table>
    </div>
  )
}

export default Leaderboard
