import axios from "axios";

const baseURL =
  import.meta.env.VITE_USE_RENDER === "true"
    ? import.meta.env.VITE_RENDER_API_URL
    : import.meta.env.VITE_API_URL;


const api = axios.create({
  baseURL,
});

api.interceptors.request.use((config) => {
  if (config.skipAuth) return config;
  const token = localStorage.getItem("JwtToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
