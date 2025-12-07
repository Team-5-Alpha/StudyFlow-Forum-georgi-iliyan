import { ref, computed } from 'vue';
import postsService from '../services/posts.service';
import tagsService from '../services/tags.service';

export function usePostEditing(postRef) {
    const isEditing = ref(false);
    const isSaving = ref(false);
    const availableTags = ref([]);

    const editForm = ref({
        title: '',
        content: '',
        tags: []
    });

    const isEditValid = computed(() =>
        editForm.value.title.length >= 16 &&
        editForm.value.title.length <= 64 &&
        editForm.value.content.length >= 32
    );

    // --- ACTIONS ---
    const startEdit = async () => {
        editForm.value = {
            title: postRef.value.title,
            content: postRef.value.content,
            tags: [...(postRef.value.tags || [])]
        };

        if (availableTags.value.length === 0) {
            try {
                const res = await tagsService.getAll();
                availableTags.value = res.data;
            } catch (err) {
                console.error("Failed to load tags for editing", err);
            }
        }

        isEditing.value = true;
    };

    const cancelEdit = () => {
        isEditing.value = false;
    };

    const saveEdit = async () => {
        if (!isEditValid.value) return;

        isSaving.value = true;
        try {
            const res = await postsService.update(postRef.value.id, editForm.value);

            postRef.value.title = res.data.title;
            postRef.value.content = res.data.content;
            postRef.value.tags = res.data.tags;

            isEditing.value = false;
        } catch (err) {
            const msg = err.response?.data?.message || err.message;
            alert("Update failed: " + msg);
        } finally {
            isSaving.value = false;
        }
    };

    const toggleEditTag = (tagName) => {
        if (editForm.value.tags.includes(tagName)) {
            editForm.value.tags = editForm.value.tags.filter(t => t !== tagName);
        } else {
            editForm.value.tags.push(tagName);
        }
    };

    return {
        isEditing,
        isSaving,
        editForm,
        availableTags,
        isEditValid,
        startEdit,
        cancelEdit,
        saveEdit,
        toggleEditTag
    };
}