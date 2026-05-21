import { useEffect, useState } from "react";
import reactLogo from "./assets/react.svg";
import viteLogo from "./assets/vite.svg";
import heroImg from "./assets/hero.png";
import "./App.css";
import { useDispatch, useSelector } from "react-redux";
import { Routes, BrowserRouter, Route, Navigate, useFetcher } from "react-router-dom";
import Login from "./Login";
import Menu from "./Menu";
import Signup from "./Signup";
import usePostFetch from "./hooks/usePostFetch";
import { setAuth } from "../store/AuthSlice";
import RoomLobby from "./RoomLobby";

function App() {
  const [count, setCount] = useState(0);
  const auth = useSelector((store) => store.auth);
  const {fetch} = usePostFetch();
  const dispatch = useDispatch();
  useEffect(()=>{
    const autoLogin = async()=>{
      if(auth.token===null)return
      const data = await fetch("/autoLogin")
      data.token = auth.token;
      dispatch(setAuth(data))
    }
    autoLogin()
  },[auth])

  console.log(auth)
  return (
    <BrowserRouter>
      {auth.username !== null ? (
        <Routes>
          <Route path="/" element={<Menu />}></Route> 
          <Route path="/roomLobby" element={<RoomLobby />}></Route> 
          <Route path="*" element={<Navigate to="/" />}></Route> 
        </Routes>
      ) : (
        <Routes>
          <Route path="/login" element={<Login />}></Route>
          <Route path="/signup" element={<Signup />}></Route>
          <Route path="*" element={<Navigate to="/login" />}></Route>
        </Routes>
      )}
    </BrowserRouter>
  );
}

export default App;
