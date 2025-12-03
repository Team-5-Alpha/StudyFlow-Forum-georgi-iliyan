import { ref, computed } from 'vue';
import postsService from '../services/posts.service';
import tagsService from '../services/tags.service';

/**
 * Логика за редактиране на пост
 * @param {Ref<Object>} postRef - Реактивна референция към обекта на поста (за да го обновим при успех)
 */
export function usePostEditing(postRef) {
    const isEditing = ref(false);
    const isSaving = ref(false);
    const availableTags = ref([]);

    // Формата за редакция (копие на данните)
    const editForm = ref({
        title: '',
        content: '',
        tags: []
    });

    // Валидация (същата логика като в CreatePost)
    const isEditValid = computed(() =>
        editForm.value.title.length >= 16 &&
        editForm.value.title.length <= 64 &&
        editForm.value.content.length >= 32
    );

    // --- ACTIONS ---

    const startEdit = async () => {
        // 1. Попълваме формата с текущите данни от поста
        editForm.value = {
            title: postRef.value.title,
            content: postRef.value.content,
            tags: [...(postRef.value.tags || [])] // Копираме масива, за да не мутираме директно
        };

        // 2. Зареждаме възможните тагове от бекенда (ако още не са заредени)
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
        // Можем да изчистим формата, но не е задължително, тъй като startEdit я презаписва
    };

    const saveEdit = async () => {
        if (!isEditValid.value) return;

        isSaving.value = true;
        try {
            const res = await postsService.update(postRef.value.id, editForm.value);

            // 3. Обновяваме локалния обект (postRef) с новите данни от сървъра
            // Това веднага се отразява в UI-а на PostCard
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