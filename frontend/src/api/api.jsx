import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:9090/",
});

api.interceptors.request.use((config) => {
  if(config.skipAuth)  return config
  const token = localStorage.getItem("JwtToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});


export default api