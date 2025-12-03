import axios from 'axios';
import { getAuth } from 'firebase/auth';

const API_URL = 'http://localhost:8080/api';

const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api', // Adjust if your port is different
    headers: {
        'Content-Type': 'application/json'
    }
});

apiClient.interceptors.request.use(
    async (config) => {
        const auth = getAuth();
        const user = auth.currentUser;

        if (user) {
            try {
                const token = await user.getIdToken();

                config.headers['Authorization'] = `Bearer ${token}`;

            } catch (e) {
                console.error("Error fetching Firebase token", e);
            }
        } else {
            console.warn("No Firebase user found in interceptor - sending anonymous request");
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            console.error("Unauthorized! Token might be invalid or user missing in DB.");
        }
        return Promise.reject(error);
    }
);
apiClient.interceptors.request.use(config => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
        try {
            const user = JSON.parse(userStr);
            if (user && user.id) {
                config.headers['X-User-Id'] = user.id;
            }
        } catch (e) {
            console.error("Error parsing user from local storage", e);
        }
    }
    return config;
}, error => {
    return Promise.reject(error);
});

export default apiClient;