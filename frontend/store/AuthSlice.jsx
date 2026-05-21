import { createSlice } from "@reduxjs/toolkit";
import { useEffect } from "react";




const authSlice = createSlice({
    name:"authentication",
    initialState:{
        username:null,
        email:null,
        token: localStorage.getItem("JwtToken") || null,
        profile:null
    },
    reducers:{
        setAuth: (state, action)=>{
            state.username = action.payload.username
            state.email = action.payload.email
            state.token = action.payload.token
            localStorage.setItem("JwtToken", action.payload.token)
            state.profile = action.payload.profile
        },
        updateProfile: (state, action)=>{
            state.username = action.payload.username;
            state.profile = action.payload.profile
        }
        ,
        clearAuth:(state, action)=>{
            state.username = null
            state.email = null
            state.token = null
            localStorage.removeItem("JwtToken")
            state.profile = null
        }
    }
})

export const {setAuth, clearAuth, updateProfile} = authSlice.actions;
export default authSlice.reducer;