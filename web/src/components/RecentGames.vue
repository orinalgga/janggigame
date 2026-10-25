<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type GameSummary } from '../api'

defineProps<{ mine: boolean }>()
const emit = defineEmits<{ (e: 'replay', id: string): void }>()
const games = ref<GameSummary[]>([])

const LEVEL: Record<string, string> = { BEGINNER: '초급', INTERMEDIATE: '중급', ADVANCED: '상급' }

function opponent(g: GameSummary) {
  return g.aiSide && g.aiLevel ? `AI ${LEVEL[g.aiLevel]} (${g.aiSide === 'CHO' ? '초' : '한'})` : '2인 대국'
}

function result(g: GameSummary) {
  if (g.result === 'ONGOING') return '진행 중'
  const how = g.endReason === 'BIKJANG' ? '빅장' : '외통'
  if (g.result === 'DRAW') return `${how} 무승부`
  return `${how} ${g.result === 'CHO_WIN' ? '초' : '한'} 승`
}

async function refresh() {
  games.value = await api.recent().catch(() => [])
}

onMounted(refresh)
defineExpose({ refresh })
</script>

<template>
  <section v-if="games.length">
    <h2>{{ mine ? '내 대국' : '최근 대국' }}</h2>
    <ul>
      <li v-for="g in games" :key="g.id">
        <span>{{ new Date(g.startedAt).toLocaleString('ko-KR') }} · {{ opponent(g) }} · {{ g.moveCount }}수 · {{ result(g) }}</span>
        <button :disabled="g.moveCount === 0" @click="emit('replay', g.id)">복기</button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
h2 {
  font-size: 1.1rem;
  margin-top: 24px;
}
ul {
  list-style: none;
  padding: 0;
}
li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  font-size: 0.9rem;
}
</style>
