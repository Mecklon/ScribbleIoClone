import { useState, useRef,useEffect } from "react"
import api from "../api/api"
const usePostFetch = (initialValue=null)=>{
    const [state,setState] = useState(initialValue)
    const [error, setError] = useState(null)
    const [loading, setLoading] = useState(false)

    const controllerRef = useRef()

    const fetch = async(query, body, skipAuth = false, headers = {})=>{
        if(controllerRef.current) controllerRef.current.abort();
        controllerRef.current = new AbortController()

        setLoading(true)
        setError(null)

        try{
            const res = await api.post(query,body, {skipAuth: skipAuth, signal: controllerRef.current.signal, headers})
            setState(res.data)
            return res.data
        }catch(error){
            if(error.response){
                console.log("server error")
                console.log(error.response)
                setError(error.response.data)
            }else if(error.request){
                console.log("Network error")
                setError("Network error")
            }else{
                console.log("something went wrong")
                setError("Something went wrong")
            }
        }finally{
            setLoading(false)
        }
    }


    useEffect(()=>{
        return ()=>{
            if(controllerRef.current)controllerRef.current.abort()
        }
    },[])

    return {state,setState, error, loading, fetch};
}

export default usePostFetch