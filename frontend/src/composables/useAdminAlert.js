import { inject, provide } from 'vue';
import { addAlert as globalAddAlert } from '../components/admin/AdminAlerts.vue';

export const useAdminAlert = () => {
    const addAlert = (message, type = 'success', duration = 3000) => {
        globalAddAlert(message, type, duration);
    };

    return { addAlert };
};
