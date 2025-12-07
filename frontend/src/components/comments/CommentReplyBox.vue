<script setup>
import { ref, watch, nextTick } from 'vue';

const props = defineProps({
  placeholder: { type: String, default: 'Write a comment...' },
  initialValue: { type: String, default: '' },
  btnText: { type: String, default: 'Post' },
  autoFocus: Boolean,
  showCancel: Boolean
});

const emit = defineEmits(['submit', 'cancel']);

const content = ref(props.initialValue);
const inputRef = ref(null);

watch(() => props.initialValue, (val) => {
  content.value = val;
});

const focus = () => {
  nextTick(() => inputRef.value?.focus());
};

const handleSubmit = () => {
  if (!content.value.trim() || content.value.length < 2) return;
  emit('submit', content.value);
  content.value = ''; // Изчистваме след успех
};

defineExpose({ focus });
</script>

<template>
  <div class="reply-box">
    <input
        ref="inputRef"
        v-model="content"
        type="text"
        :placeholder="placeholder"
        class="reply-input"
        :autofocus="autoFocus"
        @keyup.enter="handleSubmit"
    />

    <div class="actions">
      <button
          @click="handleSubmit"
          class="btn-submit"
          :disabled="content.length < 2"
      >
        {{ btnText }}
      </button>

      <button
          v-if="showCancel"
          @click="$emit('cancel')"
          class="btn-cancel"
          title="Cancel"
      >
        ✕
      </button>
    </div>
  </div>
</template>

<style scoped>
.reply-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 6px 12px;
  transition: border-color 0.2s, box-shadow 0.2s;
  width: 100%;
  box-sizing: border-box;
}

.reply-box:focus-within {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 2px rgba(182, 157, 116, 0.1);
}

.reply-input {
  flex: 1;
  border: none;
  font-size: 14px;
  outline: none;
  color: #334155;
  background: transparent;
  padding: 2px 0;
}

.reply-input::placeholder {
  color: #94a3b8;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-submit {
  background: none;
  border: none;
  color: var(--color-accent);
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  transition: opacity 0.2s;
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-cancel {
  background: none;
  border: none;
  color: #94a3b8;
  font-weight: 700;
  cursor: pointer;
  font-size: 12px;
  padding: 4px;
  line-height: 1;
}

.btn-cancel:hover {
  color: #ef4444;
}
</style>