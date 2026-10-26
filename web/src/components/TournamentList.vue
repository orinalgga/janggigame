<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { api, type Me, type TournamentSummary } from '../api'
import { DIVISION, countdown, kindLabel, startLabel, timeControl } from '../tournaments'

defineProps<{ me: Me | null }>()
const emit = defineEmits<{ (e: 'open', id: number): void; (e: 'host'): void }>()

const list = ref<TournamentSummary[]>([])
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | undefined
let poll: ReturnType<typeof setInterval> | undefined

async function refresh() {
  list.value = await api.tournaments().catch(() => list.value)
}

onMounted(() => {
  void refresh()
  timer = setInterval(() => (now.value = Date.now()), 1000)
  poll = setInterval(refresh, 30_000)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  clearInterval(poll)
})
defineExpose({ refresh })

function state(t: TournamentSummary) {
  switch (t.status) {
    case 'SCHEDULED':
      return { label: `${countdown(t.startsAt, now.value)} 뒤 시작`, cls: 'soon' }
    case 'RUNNING':
      return { label: '진행 중', cls: 'live' }
    case 'FINISHED':
      return { label: t.winner ? `우승 ${t.winner.nickname}` : '끝남', cls: 'done' }
    default:
      return { label: '취소됨', cls: 'done' }
  }
}
</script>

<template>
  <section class="tournaments">
    <div class="head">
      <h2>대회</h2>
      <button v-if="me" type="button" class="host" @click="emit('host')">대회 열기</button>
    </div>
    <p v-if="!list.length" class="empty">열려 있는 대회가 없어요.<template v-if="me"> 직접 대회를 열어 보세요.</template></p>
    <ul v-else>
      <li v-for="t in list" :key="t.id">
        <button type="button" class="card" @click="emit('open', t.id)">
          <span class="top-line">
            <span class="badge" :class="{ regular: t.kind === 'REGULAR' && !t.host, hosted: !!t.host }">{{ kindLabel(t) }}</span>
            <span v-if="t.division !== 'ALL'" class="badge div">{{ DIVISION[t.division] }}</span>
            <span class="state" :class="state(t).cls">{{ state(t).label }}</span>
          </span>
          <b class="title">{{ t.title }}</b>
          <span class="meta">
            {{ startLabel(t.startsAt) }} · {{ timeControl(t) }} 속기 · {{ t.players }}/{{ t.maxPlayers }}명
            <template v-if="t.host"> · {{ t.host.nickname }}님이 엶</template>
          </span>
          <span class="money">
            <span>참가비 {{ t.entryFee }}닢</span>
            <span v-if="t.prizePool" class="prize">우승 상금 {{ t.prizePool.toLocaleString('ko-KR') }}닢</span>
            <span v-if="t.joined" class="joined">참가 중</span>
          </span>
        </button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 28px 0 8px;
}
h2 {
  margin: 0;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--muted);
}
.host {
  font: inherit;
  font-size: 0.82rem;
  font-weight: 700;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  color: var(--accent-deep);
  cursor: pointer;
}
.empty {
  margin: 0;
  padding: 14px;
  border-radius: 14px;
  border: 1px dashed var(--border);
  font-size: 0.88rem;
  color: var(--muted);
}
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  padding: 12px 14px;
  font: inherit;
  text-align: left;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  cursor: pointer;
}
.card:hover {
  border-color: var(--accent);
}
.top-line {
  display: flex;
  align-items: center;
  gap: 6px;
}
.badge {
  font-size: 0.7rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
  color: var(--accent);
  background: var(--accent-bg);
}
.badge.regular {
  color: #fff;
  background: var(--accent);
}
.badge.hosted {
  color: var(--cho-color);
  background: color-mix(in srgb, var(--cho-color) 12%, transparent);
}
.badge.div {
  color: var(--text);
  background: var(--surface);
  border: 1px solid var(--border);
}
.state {
  margin-left: auto;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}
.state.live {
  color: var(--han-color);
}
.state.soon {
  color: var(--accent-deep);
}
.title {
  font-size: 1rem;
}
.meta {
  font-size: 0.8rem;
  color: var(--muted);
}
.money {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
  font-size: 0.82rem;
}
.prize {
  font-weight: 700;
  color: #9a7412;
}
.joined {
  margin-left: auto;
  font-weight: 700;
  color: var(--cho-color);
}
</style>
