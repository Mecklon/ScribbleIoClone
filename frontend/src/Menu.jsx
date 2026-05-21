import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { clearAuth } from "../store/AuthSlice";
import { Link } from "react-router-dom";

function menu() {
  const dispatch = useDispatch();

  const auth = useSelector((store) => store.auth);
  return (
    <div className="h-screen flex-col flex gap-2 items-center justify-center ">
      <div
        className="fixed top-10 right-10 px-5 py-2 bg-blue-500 text-white text-2xl font-semibold rounded-4xl"
        onClick={() => dispatch(clearAuth())}
      >
        logout
      </div>
      <div className="w-125 p-2 flex flex-col items-center justify-center gap-2 border">
        <label htmlFor="imageInput">
          <input type="file" id="imageInput" className="h-0 w-0" accept="/image/*" />
          <img src="" className="bg-red-600 h-90 w-90 rounded-full" alt="" />
        </label>
        <input
          type="text"
          className="text-center border-b-2 outline-0 text-2xl"
          defaultValue={auth.username}
        />
      </div>
      <Link to="/roomLobby" className="text-center bg-blue-500 w-125 text-3xl font-bold p-2 rounded-md text-white">
        Create Room
      </Link>
    </div>
  );
}

export default menu;
