import apiClient from './axios-config';

export default {
    getAll() {
        return apiClient.get('/tags');
    },

    getById(id) {
        return apiClient.get(`/tags/${id}`);
    },

    create(tag) {
        return apiClient.post('/tags', tag);
    },

    update(id, tagUpdateDTO) {
        return apiClient.put(`/tags/${id}`, tagUpdateDTO);
    },

    delete(id) {
        return apiClient.delete(`/tags/${id}`);
    }
};