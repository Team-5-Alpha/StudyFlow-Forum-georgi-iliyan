import axios from 'axios';

const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api', // Adjust if your port is different
    headers: {
        'Content-Type': 'application/json'
    }
});

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