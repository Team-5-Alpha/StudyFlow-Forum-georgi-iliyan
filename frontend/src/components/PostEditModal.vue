<script setup>
import { ref, computed, onMounted } from 'vue';
import postsService from '../services/posts.service';
import tagsService from '../services/tags.service';

const props = defineProps({
  show: Boolean,
  post: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['close', 'updated']);

const loading = ref(false);
const error = ref(null);
const availableTags = ref([]);

// Локално състояние на формата (копираме данните от пропса)
const editForm = ref({
  title: props.post.title,
  content: props.post.content,
  tags: [...(props.post.tags || [])]
});

// Зареждаме таговете при отваряне
onMounted(async () => {
  try {
    const res = await tagsService.getAll();
    availableTags.value = res.data;
  } catch (e) {
    console.error("Failed to load tags", e);
  }
});

// Валидация
const isTitleValid = computed(() => editForm.value.title.length >= 16 && editForm.value.title.length <= 64);
const isContentValid = computed(() => editForm.value.content.length >= 32);

// Helper за UI подсказки
const getHintClass = (text, isValid) => {
  if (!text || text.length === 0) return '';
  return isValid ? 'valid-hint' : 'invalid-hint';
};

// Логика за тагове
const toggleTag = (tagName) => {
  if (editForm.value.tags.includes(tagName)) {
    editForm.value.tags = editForm.value.tags.filter(t => t !== tagName);
  } else {
    editForm.value.tags.push(tagName);
  }
};

const handleSave = async () => {
  if (!isTitleValid.value || !isContentValid.value) return;

  loading.value = true;
  error.value = null;

  try {
    const response = await postsService.update(props.post.id, editForm.value);

    // Изпращаме обновения обект към родителя
    emit('updated', response.data);
    emit('close');
  } catch (err) {
    error.value = err.response?.data?.message || "Update failed.";
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div v-if="show" class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-card">

      <!-- HEADER -->
      <div class="modal-header">
        <h3 class="card-title">Edit Discussion</h3>
        <button @click="$emit('close')" class="close-btn">✕</button>
      </div>

      <form @submit.prevent="handleSave">

        <!-- TITLE -->
        <div class="form-group">
          <label class="input-label">Title</label>
          <input
              v-model="editForm.title"
              type="text"
              class="input-field title-input"
              placeholder="Topic Title"
          />
          <p class="input-hint" :class="getHintClass(editForm.title, isTitleValid)">
            Must be 16-64 characters.
          </p>
        </div>

        <!-- CONTENT -->
        <div class="form-group">
          <label class="input-label">Content</label>
          <textarea
              v-model="editForm.content"
              rows="6"
              class="input-field content-input"
              placeholder="Share your thoughts..."
          ></textarea>
          <p class="input-hint" :class="getHintClass(editForm.content, isContentValid)">
            Must be at least 32 characters.
          </p>
        </div>

        <!-- TAGS -->
        <div class="form-group">
          <label class="input-label">Tags</label>
          <div class="tags-wrapper">
            <button
                type="button"
                v-for="tag in availableTags"
                :key="tag.id"
                @click="toggleTag(tag.name)"
                class="tag-chip"
                :class="{ 'selected': editForm.tags.includes(tag.name) }"
            >
              {{ tag.name }}
              <span v-if="editForm.tags.includes(tag.name)" class="check-mark">✓</span>
            </button>
          </div>
        </div>

        <!-- ERROR MESSAGE -->
        <div v-if="error" class="error-msg">{{ error }}</div>

        <!-- FOOTER ACTIONS -->
        <div class="form-footer">
          <button type="button" @click="$emit('close')" class="btn-cancel">Cancel</button>
          <button
              type="submit"
              class="btn-save"
              :disabled="loading || !isTitleValid || !isContentValid"
          >
            {{ loading ? 'Saving...' : 'Save Changes' }}
          </button>
        </div>

      </form>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex; justify-content: center; align-items: center;
  z-index: 1000; backdrop-filter: blur(4px);
}

.modal-card {
  background: var(--color-white); width: 100%; max-width: 600px;
  padding: 30px; border-radius: 16px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  animation: slide-up 0.3s ease-out;
  max-height: 90vh; overflow-y: auto;
}

@keyframes slide-up { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 10px; }
.card-title { margin: 0; font-size: 20px; color: var(--color-dark); }
.close-btn { background: none; border: none; font-size: 24px; color: #999; cursor: pointer; }
.close-btn:hover { color: #333; }

.form-group { margin-bottom: 20px; }
.input-label { display: block; font-size: 12px; font-weight: 700; color: #94a3b8; text-transform: uppercase; margin-bottom: 6px; }

.input-field {
  width: 100%; padding: 12px; border: 1px solid #e2e8f0; border-radius: 8px;
  background-color: #f8fafc; font-family: inherit; font-size: 15px; box-sizing: border-box;
  transition: border-color 0.2s, background-color 0.2s;
}
.input-field:focus { outline: none; background-color: #fff; border-color: var(--color-accent); box-shadow: 0 0 0 3px rgba(182, 157, 116, 0.1); }
.title-input { font-weight: 700; }
.content-edit { resize: vertical; }

/* Hint Styles */
.input-hint { font-size: 12px; margin-top: 5px; color: #94a3b8; font-weight: 500; transition: color 0.2s; }
.valid-hint { color: #1F2839FF; }
.invalid-hint { color: #ef4444; }

/* Tags */
.tags-wrapper { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-chip { background-color: #fff; border: 1px solid #cbd5e1; color: #64748b; padding: 6px 12px; border-radius: 20px; font-size: 13px; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 5px; }
.tag-chip:hover { border-color: var(--color-accent); color: var(--color-dark); }
.tag-chip.selected { background-color: var(--color-accent); color: white; border-color: var(--color-accent); font-weight: 600; }

.error-msg { color: #ef4444; font-size: 13px; margin-bottom: 15px; text-align: center; }

.form-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 10px; border-top: 1px solid #f1f1f1; padding-top: 20px; }

.btn-save {
  background-color: var(--color-dark); color: white; border: none;
  padding: 10px 24px; border-radius: 20px; font-weight: 600; cursor: pointer;
  font-size: 14px; transition: all 0.2s;
}
.btn-save:hover:not(:disabled) { background-color: var(--color-accent); transform: translateY(-1px); }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-cancel {
  background-color: transparent; border: 1px solid #e2e8f0; color: var(--color-text-muted);
  padding: 10px 24px; border-radius: 20px; font-weight: 600; cursor: pointer;
  font-size: 14px; transition: all 0.2s;
}
.btn-cancel:hover { background-color: #f8fafc; color: var(--color-dark); }
</style>