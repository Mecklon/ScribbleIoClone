import React from 'react'
import { useParams } from 'react-router-dom'

function Leaderboard() {

    const {roomId} = useParams();
    console.log("in leaderboard")
  return (
    <div>
      game ended for room: {roomId}
    </div>
  )
}

export default Leaderboard
