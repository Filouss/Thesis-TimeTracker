import axios, { type InternalAxiosRequestConfig } from "axios";

axios.defaults.withCredentials = true;

export const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
    withCredentials: true, 
    xsrfCookieName: 'XSRF-TOKEN',
    xsrfHeaderName: 'X-XSRF-TOKEN'
});

let cachedCsrfToken: string | null = null;

http.interceptors.request.use(
    async (config: InternalAxiosRequestConfig) => {
        const method = config.method?.toLowerCase();

        if (method === 'get') {
            return config;
        }

        if (!cachedCsrfToken) {
            try {
                const response = await axios.get<{ token: string }>(
                    `${config.baseURL ?? '/api'}/api/csrf`, 
                    { withCredentials: true }
                );
                cachedCsrfToken = response.data.token;
            } catch (error) {
                console.error("Unable to fetch csrf token", error);
            }
        }

        
        if (cachedCsrfToken) {
            config.headers.set('X-XSRF-TOKEN', cachedCsrfToken);
        }

        return config;
    }, 
    (error) => {
        return Promise.reject(error);
    }
);