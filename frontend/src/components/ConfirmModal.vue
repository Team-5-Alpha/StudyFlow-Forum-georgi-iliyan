<script setup>
const props = defineProps({
  isOpen: Boolean,
  title: { type: String, default: 'Are you sure?' },
  message: { type: String, default: 'This action cannot be undone.' },
  // НОВО: Текст на бутона за потвърждение (по подразбиране 'Confirm')
  confirmText: { type: String, default: 'Confirm' }
});

const emit = defineEmits(['confirm', 'cancel']);
</script>

<template>
  <div v-if="isOpen" class="modal-backdrop" @click.self="$emit('cancel')">
    <div class="modal-card">

      <div class="icon-header">
        <div class="icon-circle">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor" class="trash-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
          </svg>
        </div>
      </div>

      <h3>{{ title }}</h3>
      <p>{{ message }}</p>

      <div class="modal-actions">
        <button @click="$emit('cancel')" class="btn-cancel">Cancel</button>

        <button @click="$emit('confirm')" class="btn-confirm">
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.6); backdrop-filter: blur(4px);
  display: flex; justify-content: center; align-items: center;
  z-index: 9999;
}

.modal-card {
  background: var(--color-white);
  padding: 30px;
  border-radius: 20px;
  width: 100%; max-width: 380px;
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.15);
  text-align: center;
  border: 1px solid rgba(0,0,0,0.05);
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.icon-header { display: flex; justify-content: center; margin-bottom: 15px; }
.icon-circle {
  width: 60px; height: 60px;
  background-color: #fee2e2;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.trash-icon { width: 30px; height: 30px; color: #ef4444; }

h3 { margin-top: 0; color: var(--color-dark); margin-bottom: 8px; font-size: 20px; }
p { color: var(--color-text-muted); font-size: 14px; margin-bottom: 25px; line-height: 1.5; }

.modal-actions { display: flex; gap: 12px; justify-content: center; }

.btn-cancel {
  background: transparent; border: 1px solid #e2e8f0; color: var(--color-text-muted);
  padding: 12px 24px; border-radius: 12px; font-weight: 600; cursor: pointer;
  font-size: 15px; transition: all 0.2s;
}
.btn-cancel:hover { background: #f8fafc; color: var(--color-dark); }

.btn-confirm {
  background: #ef4444; color: white; border: none;
  padding: 12px 24px; border-radius: 12px; font-weight: 600; cursor: pointer;
  font-size: 15px; transition: all 0.2s;
  box-shadow: 0 4px 6px rgba(239, 68, 68, 0.2);
}
.btn-confirm:hover { background: #dc2626; transform: translateY(-1px); }
</style>