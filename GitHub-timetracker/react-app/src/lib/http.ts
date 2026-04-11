import axios from "axios";

axios.defaults.withCredentials = true;

export const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
    withCredentials: true, 
    xsrfCookieName: 'XSRF-TOKEN',
    xsrfHeaderName: 'X-XSRF-TOKEN'
});
