<script setup>
import { ref } from 'vue';

const props = defineProps({
  isOpen: Boolean
});

const emit = defineEmits(['confirm', 'cancel']);

const password = ref('');
const isPasswordVisible = ref(false);

const toggleVisibility = () => {
  isPasswordVisible.value = !isPasswordVisible.value;
};

const handleConfirm = () => {
  if (password.value.length > 0) {
    emit('confirm', password.value);
    password.value = '';
  }
};

const handleCancel = () => {
  emit('cancel');
  password.value = '';
};
</script>

<template>
  <div v-if="isOpen" class="modal-backdrop" @click.self="handleCancel">
    <div class="modal-card">

      <div class="icon-header">
        <div class="icon-circle">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="lock-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
          </svg>
        </div>
      </div>

      <h3>Security Check</h3>
      <p>For security reasons, please enter your current password to confirm these changes.</p>

      <div class="input-wrapper">
        <input
            :type="isPasswordVisible ? 'text' : 'password'"
            v-model="password"
            placeholder="Current Password"
            class="password-input"
            @keyup.enter="handleConfirm"
        />

        <button type="button" @click="toggleVisibility" class="eye-btn">
          <svg v-if="isPasswordVisible" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" /><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" /></svg>
        </button>
      </div>

      <div class="modal-actions">
        <button @click="handleCancel" class="btn-cancel">Cancel</button>
        <button @click="handleConfirm" class="btn-confirm">Confirm</button>
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
  border-radius: 20px; /* По-заоблени ръбове */
  width: 100%; max-width: 420px;
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.15);
  text-align: center;
  border: 1px solid rgba(0,0,0,0.05);
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

/* HEADER ICON STYLES */
.icon-header {
  display: flex;
  justify-content: center;
  margin-bottom: 15px;
}

.icon-circle {
  width: 60px;
  height: 60px;
  background-color: #fdf6e7; /* Много светло златисто/кремаво */
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lock-icon {
  width: 32px;
  height: 32px;
  color: var(--color-accent); /* Златистото от темата */
}

h3 { margin-top: 0; color: var(--color-dark); margin-bottom: 8px; font-size: 20px; }
p { color: var(--color-text-muted); font-size: 14px; margin-bottom: 25px; line-height: 1.5; }

.input-wrapper { position: relative; margin-bottom: 25px; display: flex; align-items: center; }

.password-input {
  width: 100%; padding: 14px 45px 14px 15px;
  border: 1px solid #e2e8f0; border-radius: 12px; font-size: 16px;
  background-color: #f8fafc; outline: none; transition: all 0.2s;
}
.password-input:focus { border-color: var(--color-accent); background-color: #fff; box-shadow: 0 0 0 3px rgba(182, 157, 116, 0.1); }

.eye-btn { position: absolute; right: 12px; background: none; border: none; cursor: pointer; color: #94a3b8; display: flex; align-items: center; }
.eye-btn:hover { color: var(--color-dark); }
.eye-btn svg { width: 22px; height: 22px; }

.modal-actions { display: flex; gap: 12px; justify-content: center; }

.btn-confirm {
  background: var(--color-dark); color: white; border: none;
  padding: 12px 28px; border-radius: 12px; font-weight: 600; cursor: pointer;
  font-size: 15px; transition: transform 0.1s;
}
.btn-confirm:hover { background: var(--color-accent); transform: translateY(-1px); }

.btn-cancel {
  background: transparent; border: 1px solid #e2e8f0; color: var(--color-text-muted);
  padding: 12px 28px; border-radius: 12px; font-weight: 600; cursor: pointer;
  font-size: 15px;
}
.btn-cancel:hover { background: #f8fafc; color: var(--color-dark); }
</style>