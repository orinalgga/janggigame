<script setup lang="ts">
import type { Me } from '../api'
import CharacterAvatar from './CharacterAvatar.vue'

/** 로비 맨 위 내 캐릭터. 꾸미기(상점)와 확성기로 바로 간다. 로그인하지 않았어도 상점은 미리 볼 수 있다. */
defineProps<{ me: Me | null }>()
const emit = defineEmits<{ (e: 'dress'): void; (e: 'megaphone'): void }>()
</script>

<template>
  <section class="my-card">
    <button type="button" class="figure" title="캐릭터 꾸미기" @click="emit('dress')">
      <CharacterAvatar :look="me?.look" class="avatar" />
    </button>
    <div class="info">
      <template v-if="me">
        <p class="name">{{ me.nickname }}</p>
        <p class="muted">{{ me.grade }} · 레이팅 {{ me.rating }}</p>
      </template>
      <template v-else>
        <p class="name">내 캐릭터</p>
        <p class="muted">옷, 머리, 모자, 배경을 골라 꾸며 보세요. 입어 보기는 로그인 없이도 돼요.</p>
      </template>
      <div class="actions">
        <button type="button" class="primary" @click="emit('dress')">✨ 꾸미기</button>
        <button type="button" class="ghost" @click="emit('megaphone')">📢 확성기</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.my-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 4px 0 16px;
  padding: 12px;
  border-radius: 16px;
  background: linear-gradient(135deg, #fff8e6, var(--surface-strong));
  border: 1px solid var(--border);
  box-shadow: 0 2px 8px var(--shadow);
}
.figure {
  flex: none;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
}
.avatar {
  width: 92px;
  transition: transform 0.15s;
}
.figure:hover .avatar {
  transform: translateY(-2px);
}
.info {
  min-width: 0;
  flex: 1;
}
.name {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.muted {
  margin: 2px 0 0;
  font-size: 0.85rem;
  color: var(--muted);
  line-height: 1.45;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.primary,
.ghost {
  font: inherit;
  font-weight: 700;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
}
.primary {
  color: #fff;
  background: var(--accent);
  border: none;
  box-shadow: 0 3px 0 var(--accent-deep);
}
.ghost {
  color: var(--text);
  background: var(--surface-strong);
  border: 1px solid var(--accent-soft);
}
</style>
