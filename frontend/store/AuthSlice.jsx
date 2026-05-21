import { createSlice } from "@reduxjs/toolkit";
import { useEffect } from "react";




const authSlice = createSlice({
    name:"authentication",
    initialState:{
        username:null,
        email:null,
        token: localStorage.getItem("JwtToken") || null
    },
    reducers:{
        setAuth: (state, action)=>{
            state.username = action.payload.username
            state.email = action.payload.email
            state.token = action.payload.token
            localStorage.setItem("JwtToken", action.payload.token)
        },
        clearAuth:(state, action)=>{
            state.username = null,
            state.email = null,
            state.token = null,
            localStorage.removeItem("JwtToken")
        }
    }
})

export const {setAuth, clearAuth} = authSlice.actions;
export default authSlice.reducer;