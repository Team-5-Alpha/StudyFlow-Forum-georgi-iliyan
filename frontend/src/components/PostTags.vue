<script setup>
defineProps({
  tags: {
    type: Array,
    default: () => []
  }
});
</script>

<template>
  <div class="tags-container" v-if="tags && tags.length > 0">
    <span v-for="tag in tags" :key="tag" class="tag">
      #{{ tag }}
    </span>
  </div>
</template>

<style scoped>
.tags-container {
  margin-bottom: 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  color: var(--color-accent);
  font-weight: 600;
  font-size: 13px;
  background-color: #f9f5f0;
  padding: 2px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.tag:hover {
  background-color: #eee8dc;
  text-decoration: underline;
}
</style>