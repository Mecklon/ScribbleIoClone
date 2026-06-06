import React, { useRef, useState } from "react";
import { PiScribbleDuotone } from "react-icons/pi";
import { Link, useNavigate } from "react-router-dom";
import usePostFetch from "./hooks/usePostFetch";
import rolling from './assets/rolling.gif'
import { useDispatch } from "react-redux";
import { setAuth } from "../store/AuthSlice";

function Login() {

    const {fetch, loading, error:serverError} = usePostFetch();
    const dispatch = useDispatch();
    const navigate = useNavigate();

    const handleLogin = async()=>{

        const email = emailRef.current.value.trim();
        const password = passwordRef.current.value.trim();
        let hasError = false;
        setEmailError(null);
        setPasswordError(null);
        
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if(!emailRegex.test(email)){
            setEmailError({message:"Enter a proper email"})
            hasError = true;
        }
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%#*?&]{8,}$/;
        if(!passwordRegex.test(password)){
            setPasswordError({message: "atleast one small letter, one capital letter, one digit, one special character and minimum of 8 characters"})
            hasError = true;
        }
        if(email===null || email===""){
            setEmailError({message:"Enter a email"})
            hasError = true;
        }
        if(password===null || password===""){
            setPasswordError({ message:"Enter a password"})
            hasError = true;
        }
        if(hasError)return;
        const data = await fetch("/auth/login",{
            email,
            password
        },true);
        if(data){
            dispatch(setAuth(data))
            navigate("/",{replace: true})
        }
    }
    const [emailError, setEmailError] = useState(null);
    const [passwordError, setPasswordError] = useState(null);
    const emailRef = useRef();
    const passwordRef = useRef();


  return (
    <div className="flex items-center justify-center h-screen">
      <div className="aspect-7/8 border border-gray-500 bg-blue-500/50 rounded-md shadow-2xl p-5">
        <div className="flex gap-1 items-center">
          <PiScribbleDuotone className="text-5xl" />
          <div className="text-3xl font-bold">Scribblr</div>
        </div>
            <div className="text-3xl font-semibold mt-7 relative">Email
                {emailError &&
                    <div className="text-red-700 text-xs absolute top-24 left-1">{emailError.message}</div>
                }
            </div>
          <input ref={emailRef} type="text" className={`w-full bg-white mt-3 text-2xl p-1 outline-0 rounded-sm ${(emailError)? "border-red-700":"border-blue-600"} border-4`}/>
            <div className="text-3xl font-semibold mt-7 relative">Password
                {passwordError &&
                    <div className="text-red-700 text-xs absolute top-24 left-1">{passwordError.message}</div>
                }
            </div>
          <input ref={passwordRef} type="password" className={`w-full bg-white mt-3 text-2xl p-1 outline-0 rounded-sm ${(passwordError)? "border-red-700":"border-blue-600"} border-4`}/>
          <button onClick={handleLogin} className="relative bg-blue-900 text-white text-center font-semibold w-full mt-10 p-2 text-3xl rounded-sm">Login
                {
                    loading && <img className="absolute h-10 top-2 left-[63%]" src={rolling} alt="" />
                }
          </button>
          {
            serverError &&
            <div className="p-2 rounded-md border border-red-600 bg-red-200 text-xl text-red-600 mt-2 text-center">
                {serverError.message}
            </div>
            }
          <div className="text-center mt-4">
            Dont have an account? 
          <Link className="text-blue-950 ml-1 underline " to="/signup">Signup</Link>
          </div>
      </div>
    </div>
  );
}

export default Login;
