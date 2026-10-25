<script setup lang="ts">
import { onMounted, ref } from 'vue'
import JanggiBoard from './JanggiBoard.vue'
import { api, type ReplayDto } from '../api'

const props = defineProps<{ gameId: string }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const frame = ref<ReplayDto | null>(null)
const error = ref('')

async function go(ply: number) {
  try {
    frame.value = await api.replay(props.gameId, ply)
  } catch (e) {
    error.value = (e as Error).message
  }
}

onMounted(() => go(0))
</script>

<template>
  <section>
    <p class="status">
      <span>복기 {{ frame ? `${frame.ply} / ${frame.totalPlies}수` : '' }}</span>
      <button @click="emit('close')">닫기</button>
    </p>
    <p v-if="error" class="error">{{ error }}</p>
    <template v-if="frame">
      <JanggiBoard
        :pieces="frame.pieces"
        :legal-moves="[]"
        :selected="null"
        :last-move="frame.lastMove"
        :flipped="frame.aiSide === 'CHO'"
      />
      <div class="nav">
        <button :disabled="frame.ply === 0" @click="go(0)">처음</button>
        <button :disabled="frame.ply === 0" @click="go(frame.ply - 1)">이전</button>
        <button :disabled="frame.ply === frame.totalPlies" @click="go(frame.ply + 1)">다음</button>
        <button :disabled="frame.ply === frame.totalPlies" @click="go(frame.totalPlies)">끝</button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.nav {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.error {
  color: var(--han-color);
}
</style>
