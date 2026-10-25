<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type GameSummary } from '../api'
import { PERSONAS } from '../options'

defineProps<{ mine: boolean }>()
const emit = defineEmits<{ (e: 'replay', id: string): void; (e: 'resume', id: string): void }>()
const games = ref<GameSummary[]>([])

function opponent(g: GameSummary) {
  if (g.online) return '온라인 대전'
  return g.aiSide && g.aiLevel ? `${PERSONAS[g.aiLevel].name} (AI ${PERSONAS[g.aiLevel].level})` : '같은 화면 2인'
}

/** 끝나지 않은 AI·2인 대국은 이어서 둘 수 있다. 온라인 대국은 시계가 가므로 대국 화면에서만 둔다. */
const resumable = (g: GameSummary) => g.result === 'ONGOING' && !g.online

function result(g: GameSummary) {
  if (g.result === 'ONGOING') return '진행 중'
  const how = { CHECKMATE: '외통', BIKJANG: '빅장', RESIGN: '기권', TIMEOUT: '시간패' }[g.endReason ?? 'CHECKMATE']
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
  <section v-if="games.length" class="recent">
    <h2>{{ mine ? '내 대국' : '최근 대국' }}</h2>
    <ul>
      <li v-for="g in games" :key="g.id">
        <span class="info">
          <b>{{ opponent(g) }}</b>
          <span class="meta">
            {{ new Date(g.startedAt).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' }) }} ·
            {{ g.moveCount }}수 · <span :class="{ live: g.result === 'ONGOING' }">{{ result(g) }}</span>
          </span>
        </span>
        <button v-if="resumable(g)" type="button" class="resume" @click="emit('resume', g.id)">이어 두기</button>
        <button type="button" :disabled="g.moveCount === 0" @click="emit('replay', g.id)">복기</button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
h2 {
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--muted);
  margin: 28px 0 8px;
}
ul {
  list-style: none;
  padding: 0;
  margin: 0;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  overflow: hidden;
}
li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-top: 1px solid var(--border);
}
li:first-child {
  border-top: none;
}
.info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  margin-right: auto;
  font-size: 0.9rem;
}
.meta {
  font-size: 0.78rem;
  color: var(--muted);
}
.live {
  color: var(--cho-color);
  font-weight: 600;
}
button {
  flex: none;
  padding: 6px 12px;
  border-radius: 9px;
  border: 1px solid var(--border);
  background: var(--surface);
  cursor: pointer;
  font-size: 0.85rem;
}
button:disabled {
  opacity: 0.45;
  cursor: default;
}
.resume {
  color: #fff;
  font-weight: 600;
  background: var(--accent);
  border-color: var(--accent);
}
</style>
