<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, type GameSummary } from '../api'
import { PERSONAS } from '../options'
import { MARK, outcome, result } from '../records'

defineProps<{ mine: boolean }>()
const emit = defineEmits<{ (e: 'replay', id: string): void; (e: 'resume', id: string): void }>()
const games = ref<GameSummary[]>([])

function opponent(g: GameSummary) {
  if (g.online) return g.opponentNickname ? `${g.opponentNickname} (온라인)` : '온라인 대전'
  return g.aiSide && g.aiLevel ? `${PERSONAS[g.aiLevel].name} (AI ${PERSONAS[g.aiLevel].level})` : '같은 화면 2인'
}

/** 끝나지 않은 AI·2인 대국은 이어서 둘 수 있다. 온라인 대국은 시계가 가므로 대국 화면에서만 둔다. */
const resumable = (g: GameSummary) => g.result === 'ONGOING' && !g.online

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
      <li v-for="g in games" :key="g.id" :class="outcome(g)">
        <span v-if="outcome(g)" class="mark" :aria-label="`내 ${MARK[outcome(g)!]}`">{{ MARK[outcome(g)!] }}</span>
        <span class="info">
          <b>{{ opponent(g) }}</b>
          <span class="meta">
            {{ new Date(g.startedAt).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' }) }} ·
            {{ g.moveCount }}수 · <span :class="['res', { live: g.result === 'ONGOING' }]">{{ result(g) }}</span>
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
.mark {
  flex: none;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  font-family: 'Noto Serif KR', serif;
  font-size: 1.05rem;
  font-weight: 900;
  color: #fff;
}
.win .mark {
  background: #2f6fd6;
}
.loss .mark {
  background: #d64545;
}
.draw .mark {
  background: #8a8a8a;
}
.win {
  box-shadow: inset 4px 0 0 #2f6fd6;
}
.loss {
  box-shadow: inset 4px 0 0 #d64545;
}
.draw {
  box-shadow: inset 4px 0 0 #8a8a8a;
}
.win .res {
  color: #2f6fd6;
  font-weight: 700;
}
.loss .res {
  color: #d64545;
  font-weight: 700;
}
.draw .res {
  font-weight: 700;
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
