import apiClient from './axios-config';

export default {
    getAll(filterOptions) {
        // params: { title, keyword, authorId, tagName, isDeleted, sortBy, sortOrder, page, size }
        return apiClient.get('/posts', { params: filterOptions });
    },

    getById(id) {
        return apiClient.get(`/posts/${id}`);
    },

    getLatest(limit = 10) {
        return apiClient.get('/posts/latest', { params: { limit } });
    },

    getTopCommented(limit = 10) {
        return apiClient.get('/posts/top-commented', { params: { limit } });
    },

    getComments(postId, page = 0, size = 10) {
        return apiClient.get(`/posts/${postId}/comments`, { params: { page, size } });
    },

    create(postCreateDTO) {
        return apiClient.post('/posts', postCreateDTO);
    },

    update(id, postUpdateDTO) {
        return apiClient.put(`/posts/${id}`, postUpdateDTO);
    },

    delete(id) {
        return apiClient.delete(`/posts/${id}`);
    },

    like(id) {
        return apiClient.post(`/posts/${id}/likes`);
    },

    unlike(id) {
        return apiClient.delete(`/posts/${id}/likes`);
    },

    addComment(postId, commentCreateDTO) {
        return apiClient.post(`/posts/${postId}/comments`, commentCreateDTO);
    }
};