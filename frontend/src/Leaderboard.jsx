import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import useGetFetch from './hooks/useGetFetch';
import avatar from './assets/defaultAvatar.webp'
import Image from './hooks/Image';

function Leaderboard() {

    const {roomId} = useParams();
    const {loading, error, fetch} = useGetFetch();
    const [leaderboard,setLeaderBoard]  = useState([])

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
