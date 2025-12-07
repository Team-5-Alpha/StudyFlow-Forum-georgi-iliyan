<script setup>
import CommentItem from './CommentItem.vue';

defineProps({
  comments: {
    type: Array,
    required: true
  },
  postId: {
    type: Number,
    required: true
  }
});
</script>

<template>
  <div class="comments-list">
    <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        :post-id="postId"
    />
  </div>
</template>

<style scoped>
.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>