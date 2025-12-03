import apiClient from './axios-config';

export default {
    getAll(filterOptions) {
        return apiClient.get('/comments', { params: filterOptions });
    },

    getById(id) {
        return apiClient.get(`/comments/${id}`);
    },

    getReplies(id) {
        return apiClient.get(`/comments/${id}/replies`);
    },

    replyToComment(id, commentCreateDTO) {
        return apiClient.post(`/comments/${id}`, commentCreateDTO);
    },

    update(id, commentUpdateDTO) {
        return apiClient.put(`/comments/${id}`, commentUpdateDTO);
    },

    delete(id) {
        return apiClient.delete(`/comments/${id}`);
    },

    likeComment(id) {
        return apiClient.post(`/comments/${id}/likes`);
    },

    unlikeComment(id) {
        return apiClient.delete(`/comments/${id}/likes`);
    }
};