import React, { useEffect, useState,useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import { clearAuth, updateProfile } from "../store/AuthSlice";
import { Link, useNavigate } from "react-router-dom";
import avatar from './assets/defaultAvatar.webp'
import Image from "./hooks/Image";
import { IoCheckmarkOutline } from "react-icons/io5";
import { RxCross1 } from "react-icons/rx";
import { FaLessThan } from "react-icons/fa6";
import usePostFetch from "./hooks/usePostFetch";
import useGetFetch from "./hooks/useGetFetch";
import rolling from './assets/rolling.gif'



function menu() {
  const auth = useSelector((store) => store.auth);
  const dispatch = useDispatch();
  const [img, setImg] = useState(null);

  const nameRef = useRef();
  const imageRef = useRef();
  const [showButtons, setShowButtons] = useState(false)

  const navigate = useNavigate();

  
  const handleChange = (e) => {
    if (e.target.files[0]) {
      setImg(URL.createObjectURL(e.target.files[0]));
      setShowButtons(true)
    }
  };

  const handleInput = (e)=>{
    if(e.target.value!=auth.username){
      setShowButtons(true)
    }else if(img===null){
      setShowButtons(false)
    }
  }

  const handleCancel = () => {
    if (img) {
      URL.revokeObjectURL(img);
    }
    setImg(null);
    console.log(nameRef.current)
    nameRef.current.value = auth.username
    setShowButtons(false)
  };

  const{ fetch, loading } = usePostFetch();
  const {fetch: createRoomFetch, loading:roomLoading} = useGetFetch();

  const handleProfileChange = async()=>{
    const formData = new FormData();
    if (imageRef.current.files[0]) {
      formData.append("profile", imageRef.current.files[0]);
    }
    formData.append("username",nameRef.current.value)
    const res = fetch("/updateProfile", formData)

    dispatch(updateProfile({
      username: res.username,
      profile: res.profile 
    }))
    setShowButtons(false)
  }

  useEffect(() => {
    return () => {
      if (img) {
        URL.revokeObjectURL(img);
      }
    };
  }, [img]);

  const createRoom = async()=>{
    const roomId = await createRoomFetch("/createRoom")
    navigate("/roomLobby/"+roomId);
  }

  return (
    <div className="h-screen flex-col flex gap-2 items-center justify-center ">
      <div
        className="fixed top-10 right-10 px-5 py-2 bg-blue-500 text-white text-2xl font-semibold rounded-4xl"
        onClick={() => dispatch(clearAuth())}
      >
        logout
      </div>
      <div className="w-125 p-2 flex relative bg-blue-400/75 flex-col items-center justify-center gap-2 rounded-2xl">
        {
          showButtons &&
          <>
            <RxCross1 onClick={handleCancel} className="absolute left-2 top-2 text-2xl hover:scale-145 duration-300"/>
            <IoCheckmarkOutline onClick={handleProfileChange} className="absolute right-2 top-2 text-3xl hover:scale-145 duration-300"/>
          </>
        }
        <label htmlFor="imageInput">
          <input ref={imageRef} type="file" id="imageInput" onChange={handleChange} className="h-0 w-0" accept="image/*" />
          {img ? (
            <img src={img} className="h-90 w-90 rounded-full" />
          ) : (
            <Image path={auth.profile} className="h-90 w-90 rounded-full" fallback={avatar} /> 
          )}
        </label>
        <input
          ref={nameRef}
          type="text"
          onChange={handleInput}
          className="text-center border-b-2 outline-0 text-2xl text-white"
          defaultValue={auth.username}
        />
      </div>
      <button disabled={roomLoading} onClick={createRoom}  className="flex gap-2 items-center justify-center bg-blue-500 w-125 text-3xl font-bold p-2 rounded-md text-white">
        Create Room
        {roomLoading &&
          <img src={rolling} className="h-10"/>
        }
      </button>
    </div>
  );
}

export default menu;
