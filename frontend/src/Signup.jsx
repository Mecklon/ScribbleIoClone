import React, { useRef, useState } from 'react'
import { PiScribbleDuotone } from "react-icons/pi";
import { Link } from 'react-router-dom';
import usePostFetch from './hooks/usePostFetch';
import { useDispatch } from 'react-redux';
import { setAuth } from '../store/AuthSlice';
import rolling from './assets/rolling.gif'

function Signup() {

    const emailRef = useRef()
    const usernameRef = useRef()
    const passwordRef = useRef()

    const [usernameError, setUsernameError] = useState(null);
    const [passwordError, setPasswordError] = useState(null);
    const [emailError, setEmailError] = useState(null);

    const {loading, fetch, error: serverError} = usePostFetch();

    const dispatch = useDispatch();

    const handleSignup = async ()=>{
        const email = emailRef.current.value.trim();
        const password = passwordRef.current.value.trim();
        const username = usernameRef.current.value.trim();
        let hasError = false;
        setEmailError(null)
        setPasswordError(null)
        setUsernameError(null)
        
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if(!emailRegex.test(email)){
            setEmailError({message:"Enter a proper email"})
            hasError = true;
        }
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%#*?&])[A-Za-z\d@$!#%*?&]{8,}$/;
        if(!passwordRegex.test(password)){
            setPasswordError({message: "atleast one small letter, one capital letter, one digit, one special character and minimum of 8 characters"})
            hasError = true;
        }
        const usernameRegex = /^[A-Za-z0-9_ ]{3,30}$/;
        
        if (!usernameRegex.test(username)) {
            setUsernameError({message:"Username must be 3-30 characters and contain only letters, numbers, spaces, and underscores"});
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
        if(username===null || username===""){
            setUsernameError({message :"Enter your username"})
            hasError = true;
        }
        
        if(hasError)return;

        const data = await fetch("auth/signup",{
            username,
            password,
            email
        }, true)
        if(data){
            dispatch(setAuth(data))
        }
    }

   return (
    <div className="flex items-center justify-center h-screen">
      <div className="aspect-7/8 border bg-blue-500/50 border-gray-500  rounded-md shadow-2xl p-5">
        <div className="flex gap-1 items-center">
          <PiScribbleDuotone className="text-5xl" />
          <div className="text-3xl font-bold">Scribblr</div>
        </div>
            <div className="text-3xl font-semibold mt-7 relative">Email
                {emailError &&
                    <div className="text-red-700 text-xs absolute top-24 left-1">{emailError.message}</div>
                }
            </div>
          <input ref={emailRef} type="text" className={`w-full  mt-3 text-2xl p-1 outline-0 rounded-sm ${(emailError)? "border-red-700":"border-blue-600"} border-4`}/>
            <div className="text-3xl font-semibold mt-7 relative">Username
                {usernameError &&
                    <div className="text-red-700 text-xs absolute top-24 left-1">{usernameError.message}</div>
                }
            </div>
          <input ref={usernameRef} type="text" className={`w-full  mt-3 text-2xl p-1 outline-0 rounded-sm ${(usernameError)? "border-red-700":"border-blue-600"} border-4`}/>
            <div className="text-3xl font-semibold mt-7 relative">Password
                {passwordError &&
                    <div className="text-red-700 text-xs absolute top-24 left-1">{passwordError.message}</div>
                }
            </div>
          <input ref={passwordRef} type="text" className={`w-full  mt-3 text-2xl p-1 outline-0 rounded-sm ${(passwordError)? "border-red-700":"border-blue-600"} border-4`}/>
          <button onClick={handleSignup} className="bg-blue-900 relative text-white text-center font-semibold w-full mt-10 p-2 text-3xl rounded-sm">Signup
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
            Already have an account? 
          <Link className="text-blue-600 underline ml-1" to="/loging">Login</Link>
          </div>
      </div>
    </div>
  );
}

export default Signup
