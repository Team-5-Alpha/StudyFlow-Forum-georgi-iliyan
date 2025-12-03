<script setup>
import { ref, onMounted } from 'vue';
import tagsService from '../../services/tags.service';

const props = defineProps({
  show: Boolean,
  activeTag: String // Текущо избран таг
});

const emit = defineEmits(['close', 'apply']);

const tags = ref([]);
const selectedTag = ref(props.activeTag);

onMounted(async () => {
  try {
    const res = await tagsService.getAll();
    tags.value = res.data;
  } catch (e) {
    console.error("Failed to load tags", e);
  }
});

const selectTag = (tagName) => {
  // Toggle logic
  if (selectedTag.value === tagName) {
    selectedTag.value = null;
  } else {
    selectedTag.value = tagName;
  }
};

const applyFilters = () => {
  emit('apply', selectedTag.value);
  emit('close');
};

const clearFilters = () => {
  selectedTag.value = null;
  emit('apply', null);
  emit('close');
};
</script>

<template>
  <div v-if="show" class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal-card">

      <div class="modal-header">
        <h3>Filter Discussions</h3>
        <button @click="$emit('close')" class="close-btn">✕</button>
      </div>

      <div class="filter-section">
        <label class="section-label">By Topic</label>
        <div class="tags-grid">
          <button
              v-for="tag in tags"
              :key="tag.id"
              class="tag-chip"
              :class="{ active: selectedTag === tag.name }"
              @click="selectTag(tag.name)"
          >
            #{{ tag.name }}
          </button>
        </div>
      </div>

      <div class="modal-footer">
        <button @click="clearFilters" class="btn-clear">Clear</button>
        <button @click="applyFilters" class="btn-apply">Show Results</button>
      </div>

    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.5); backdrop-filter: blur(4px);
  display: flex; justify-content: center; align-items: center;
  z-index: 1000;
}

.modal-card {
  background: white; padding: 25px; border-radius: 20px;
  width: 100%; max-width: 500px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.2);
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; border-bottom: 1px solid #f1f1f1; padding-bottom: 10px;
}
h3 { margin: 0; color: var(--color-dark); font-size: 18px; }
.close-btn { background: none; border: none; font-size: 22px; color: #94a3b8; cursor: pointer; }
.close-btn:hover { color: var(--color-dark); }

.section-label {
  display: block; font-size: 12px; font-weight: 700;
  color: #94a3b8; text-transform: uppercase; margin-bottom: 10px;
}

.tags-grid {
  display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 25px;
  max-height: 200px; overflow-y: auto;
}

.tag-chip {
  background: #f8fafc; border: 1px solid #e2e8f0;
  padding: 8px 16px; border-radius: 20px;
  color: #64748b; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
}

.tag-chip:hover { border-color: var(--color-dark); color: var(--color-dark); }

.tag-chip.active {
  background-color: var(--color-dark); color: white; border-color: var(--color-dark);
  box-shadow: 0 4px 10px rgba(31, 40, 57, 0.2);
}

.modal-footer {
  display: flex; justify-content: flex-end; gap: 10px;
}

.btn-apply {
  background: var(--color-accent); color: white; border: none;
  padding: 10px 24px; border-radius: 12px; font-weight: 600; cursor: pointer;
}
.btn-apply:hover { filter: brightness(1.1); }

.btn-clear {
  background: transparent; border: none; color: #94a3b8;
  padding: 10px 15px; font-weight: 600; cursor: pointer;
}
.btn-clear:hover { color: var(--color-dark); }
</style>