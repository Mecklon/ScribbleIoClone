import { configureStore } from "@reduxjs/toolkit";
import authSlice from './AuthSlice'
import errorSlice from "./ErrorSlice"

export const store = configureStore({
    reducer:{
        auth: authSlice,
        error: errorSlice
    }
})

