<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '../stores/auth.store';
import postsService from '../services/posts.service';
import tagsService from '../services/tags.service';

const emit = defineEmits(['post-created', 'close']);

const authStore = useAuthStore();
const loading = ref(false);
const error = ref(null);

// State
const availableTags = ref([]);
const selectedTags = ref([]);

const formData = ref({
  title: '',
  content: ''
});

onMounted(async () => {
  try {
    const response = await tagsService.getAll();
    availableTags.value = response.data;
  } catch (err) {
    console.error("Failed to fetch tags", err);
  }
});

const toggleTag = (tagName) => {
  if (selectedTags.value.includes(tagName)) {
    selectedTags.value = selectedTags.value.filter(t => t !== tagName);
  } else {
    selectedTags.value.push(tagName);
  }
};

const isTitleValid = computed(() => formData.value.title.length >= 16 && formData.value.title.length <= 64);
const isContentValid = computed(() => formData.value.content.length >= 32);

const getHintClass = (text, isValid) => {
  if (text.length === 0) return '';
  return isValid ? 'valid' : 'invalid';
};

const handleCreatePost = async () => {
  error.value = null;

  if (!isTitleValid.value || !isContentValid.value) {
    error.value = "Please check the input requirements.";
    return;
  }

  loading.value = true;

  try {
    await postsService.create({
      title: formData.value.title,
      content: formData.value.content,
      tags: selectedTags.value
    });

    formData.value = { title: '', content: '' };
    selectedTags.value = [];
    emit('post-created');

  } catch (err) {
    console.error(err);
    error.value = err.response?.data?.message || "Failed to create post.";
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="modal-backdrop" @click.self="$emit('close')">

    <div class="create-post-card">
      <div class="modal-header">
        <h3 class="card-title">Create Discussion</h3>
        <button @click="$emit('close')" class="close-btn">✕</button>
      </div>

      <div v-if="!authStore.user" class="guest-msg">
        <p>You must be logged in to post.</p>
        <div class="guest-buttons">
          <router-link to="/login" class="btn-primary">Log In</router-link>
        </div>
      </div>

      <form v-else @submit.prevent="handleCreatePost">

        <!-- TITLE -->
        <div class="form-group">
          <label class="input-label">Title</label>
          <input
              v-model="formData.title"
              type="text"
              placeholder="Enter topic title..."
              class="input-field title-input"
          />
          <!-- TEXT WITH VALIDATIONS -->
          <p class="input-hint" :class="getHintClass(formData.title, isTitleValid)">
            Must be between 16 and 64 characters.
          </p>
        </div>

        <!-- CONTENT -->
        <div class="form-group">
          <label class="input-label">Content</label>
          <textarea
              v-model="formData.content"
              placeholder="Share your thoughts..."
              rows="5"
              class="input-field content-input"
          ></textarea>
          <!-- TEXT WITH VALIDATIONS -->
          <p class="input-hint" :class="getHintClass(formData.content, isContentValid)">
            Must be at least 32 characters.
          </p>
        </div>

        <!-- TAGS -->
        <div class="form-group">
          <label class="input-label">Select Tags</label>
          <div class="tags-wrapper">
            <div v-if="availableTags.length === 0" class="no-tags">Loading tags...</div>

            <button
                type="button"
                v-for="tag in availableTags"
                :key="tag.id"
                @click="toggleTag(tag.name)"
                class="tag-chip"
                :class="{ 'selected': selectedTags.includes(tag.name) }"
            >
              {{ tag.name }}
              <span v-if="selectedTags.includes(tag.name)" class="check-mark">✓</span>
            </button>
          </div>
          <div class="selected-summary" v-if="selectedTags.length > 0">
            Selected: <span class="accent-text">{{ selectedTags.join(', ') }}</span>
          </div>
        </div>

        <!-- FOOTER -->
        <div class="form-footer">
          <span v-if="error" class="error-msg">{{ error }}</span>
          <button
              type="submit"
              class="btn-submit"
              :disabled="loading || !isTitleValid || !isContentValid"
          >
            {{ loading ? 'Posting...' : 'Post Discussion' }}
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

.create-post-card {
  background: var(--color-white); width: 100%; max-width: 600px;
  padding: 30px; border-radius: 16px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  animation: slide-up 0.3s ease-out; max-height: 90vh; overflow-y: auto;
}

@keyframes slide-up { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 10px; }
.card-title { margin: 0; font-size: 20px; color: var(--color-dark); }
.close-btn { background: none; border: none; font-size: 24px; color: #999; cursor: pointer; }
.close-btn:hover { color: #333; }

.form-group { margin-bottom: 20px; }
.input-label { display: block; font-size: 14px; font-weight: 600; color: var(--color-dark); margin-bottom: 8px; }

.input-field {
  width: 100%; padding: 12px; border: 1px solid #e2e8f0; border-radius: 8px;
  background-color: #f8fafc; font-family: inherit; font-size: 15px; box-sizing: border-box;
}
.input-field:focus { outline: none; background-color: #fff; border-color: var(--color-accent); box-shadow: 0 0 0 3px rgba(182, 157, 116, 0.1); }
.title-input { font-weight: 700; }

/* HINT TEXT STYLES */
.input-hint {
  font-size: 12px;
  margin-top: 5px;
  color: #94a3b8;
  font-weight: 500;
  transition: color 0.2s;
}

.input-hint.valid {
  color: var(--color-dark);
  opacity: 0.8;
}

.input-hint.invalid {
  color: #ef4444;
}

/* TAGS */
.tags-wrapper { display: flex; flex-wrap: wrap; gap: 10px; padding: 10px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; max-height: 150px; overflow-y: auto; }
.tag-chip { background-color: #fff; border: 1px solid #cbd5e1; color: var(--color-text-muted); padding: 6px 12px; border-radius: 20px; font-size: 13px; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 5px; }
.tag-chip:hover { border-color: var(--color-accent); color: var(--color-dark); }
.tag-chip.selected { background-color: var(--color-accent); color: white; border-color: var(--color-accent); font-weight: 600; }
.selected-summary { margin-top: 8px; font-size: 13px; color: var(--color-text-muted); }
.accent-text { color: var(--color-accent); font-weight: 700; }

.form-footer { display: flex; justify-content: flex-end; align-items: center; gap: 15px; margin-top: 10px; }
.btn-submit { background-color: var(--color-dark); color: var(--color-white); border: none; padding: 10px 30px; border-radius: 20px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-submit:hover:not(:disabled) { background-color: var(--color-accent); transform: translateY(-1px); }
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }
.error-msg { color: #ef4444; font-size: 13px; }
.guest-msg { text-align: center; }
.btn-primary { background: var(--color-dark); color: white; padding: 8px 16px; border-radius: 20px; text-decoration: none; }
</style>