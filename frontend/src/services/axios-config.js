import axios from 'axios';
import { useAuthStore } from '../stores/auth.store';

const API_URL = 'http://localhost:8080/api';

const apiClient = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

apiClient.interceptors.request.use(
    (config) => {
        try {
            const authStore = useAuthStore();
            const userId = authStore.currentUserId;

            if (userId) {
                config.headers['X-User-Id'] = userId;
            }
        } catch (e) {
            const storedUser = JSON.parse(localStorage.getItem('user'));
            if (storedUser?.id) {
                config.headers['X-User-Id'] = storedUser.id;
            }
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
        return Promise.reject(error);
    }
);

export default apiClient;