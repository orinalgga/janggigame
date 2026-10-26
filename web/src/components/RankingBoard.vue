<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, type Ranking, type RankingPeriod } from '../api'
import UserAvatar from './UserAvatar.vue'

const props = defineProps<{
  /** 내 회원 번호. 순위표에서 내 줄을 표시한다. */
  myId: number | null
}>()

const PERIODS: { value: RankingPeriod; label: string; now: string; prev: string }[] = [
  { value: 'WEEK', label: '주간', now: '이번 주', prev: '지난주' },
  { value: 'MONTH', label: '월간', now: '이번 달', prev: '지난달' },
  { value: 'YEAR', label: '연간', now: '올해', prev: '작년' },
]

const period = ref<RankingPeriod>('WEEK')
const offset = ref(0)
const ranking = ref<Ranking | null>(null)
const loading = ref(false)
const failed = ref(false)

async function load() {
  loading.value = true
  failed.value = false
  try {
    ranking.value = await api.rankings(period.value, offset.value)
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

function pick(p: RankingPeriod) {
  period.value = p
  offset.value = 0
  void load()
}

function shift(o: number) {
  offset.value = o
  void load()
}

onMounted(load)
defineExpose({ refresh: load })

const info = computed(() => PERIODS.find((p) => p.value === period.value)!)

/** 기간: to는 다음 기간 첫날이라 하루 빼서 보여 준다. */
const range = computed(() => {
  const r = ranking.value
  if (!r) return ''
  const day = (s: string, plus = 0) => {
    const [y, m, d] = s.split('-').map(Number)
    const t = new Date(y, m - 1, d + plus)
    return `${t.getMonth() + 1}.${t.getDate()}`
  }
  return `${day(r.from)} ~ ${day(r.to, -1)}`
})

const meOutside = computed(() => {
  const me = ranking.value?.me
  return me && !ranking.value!.rows.some((r) => r.userId === me.userId) ? me : null
})

const signed = (n: number) => (n > 0 ? `+${n}` : `${n}`)
const record = (r: { wins: number; draws: number; losses: number }) =>
  `${r.wins}승 ${r.draws ? `${r.draws}무 ` : ''}${r.losses}패`
</script>

<template>
  <section class="ranking">
    <div class="head">
      <h2>순위</h2>
      <div class="periods" role="tablist" aria-label="순위 기간">
        <button
          v-for="p in PERIODS"
          :key="p.value"
          type="button"
          role="tab"
          :aria-selected="period === p.value"
          :class="{ active: period === p.value }"
          @click="pick(p.value)"
        >
          {{ p.label }}
        </button>
      </div>
    </div>

    <div class="board">
      <div class="bar">
        <div class="when">
          <button type="button" :class="{ active: offset === 0 }" @click="shift(0)">{{ info.now }}</button>
          <button type="button" :class="{ active: offset === 1 }" @click="shift(1)">{{ info.prev }}</button>
        </div>
        <span class="range">{{ range }}</span>
      </div>

      <p v-if="failed" class="empty">순위를 불러오지 못했어요.</p>
      <p v-else-if="ranking && !ranking.rows.length" class="empty">
        아직 순위에 오른 사람이 없어요. 온라인 대국을 {{ ranking.minGames }}판 이상 두면 순위에 올라요.
      </p>
      <ol v-else-if="ranking" :class="{ dim: loading }">
        <li v-for="r in ranking.rows" :key="r.userId" :class="{ mine: r.userId === props.myId, podium: r.rank! <= 3 }">
          <span class="rank" :class="`r${r.rank}`">{{ r.rank }}</span>
          <UserAvatar :id="r.avatar" :name="r.nickname" :size="30" />
          <span class="who">
            <b>{{ r.nickname }}</b>
            <small>{{ r.grade }} · {{ record(r) }}</small>
          </span>
          <span class="points" :class="{ down: r.points < 0 }">{{ signed(r.points) }}</span>
        </li>
      </ol>

      <div v-if="meOutside" class="me-row">
        <span class="rank">{{ meOutside.rank ?? '-' }}</span>
        <span class="who">
          <b>나</b>
          <small v-if="meOutside.rank === null">
            {{ ranking!.minGames - meOutside.games }}판 더 두면 순위에 올라요 · {{ record(meOutside) }}
          </small>
          <small v-else>{{ record(meOutside) }}</small>
        </span>
        <span class="points" :class="{ down: meOutside.points < 0 }">{{ signed(meOutside.points) }}</span>
      </div>

      <p v-if="ranking" class="note">
        {{ info.label }} 순위는 이 기간 온라인 대국에서 오르내린 점수의 합이에요.
        {{ ranking.minGames }}판 이상 둔 사람만 올라요.
      </p>
    </div>
  </section>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 28px 0 8px;
}
h2 {
  margin: 0;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--muted);
}
.periods {
  display: flex;
  gap: 2px;
  padding: 3px;
  border-radius: 10px;
  background: var(--accent-bg);
}
.periods button {
  font: inherit;
  font-size: 0.82rem;
  font-weight: 700;
  padding: 5px 13px;
  border: none;
  border-radius: 8px;
  background: none;
  color: var(--muted);
  cursor: pointer;
}
.periods button.active {
  background: var(--surface-strong);
  color: var(--text);
  box-shadow: 0 1px 3px var(--shadow);
}
.board {
  border-radius: 14px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  overflow: hidden;
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border);
}
.when {
  display: flex;
  gap: 4px;
}
.when button {
  font: inherit;
  font-size: 0.8rem;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: none;
  color: var(--muted);
  cursor: pointer;
}
.when button.active {
  color: var(--accent-deep);
  border-color: var(--border);
  background: var(--surface);
  font-weight: 700;
}
.range {
  font-size: 0.78rem;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}
ol {
  list-style: none;
  margin: 0;
  padding: 0;
  transition: opacity 0.15s;
}
ol.dim {
  opacity: 0.5;
}
li,
.me-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 14px;
  border-top: 1px solid var(--border);
}
li:first-child {
  border-top: none;
}
li.mine {
  background: var(--accent-bg);
}
.me-row {
  background: var(--accent-bg);
  border-top: 1px dashed var(--border);
}
.rank {
  flex: none;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  font-weight: 800;
  font-size: 0.9rem;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}
.podium .rank {
  color: #fff;
  font-family: 'Noto Serif KR', serif;
  clip-path: polygon(30% 0, 70% 0, 100% 30%, 100% 70%, 70% 100%, 30% 100%, 0 70%, 0 30%);
}
.rank.r1 {
  background: #c9a227;
}
.rank.r2 {
  background: #9aa3ad;
}
.rank.r3 {
  background: #b0764a;
}
.who {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
  font-size: 0.92rem;
}
.who b {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.who small {
  font-size: 0.76rem;
  color: var(--muted);
}
.points {
  flex: none;
  font-weight: 800;
  color: var(--cho-color);
  font-variant-numeric: tabular-nums;
}
.points.down {
  color: var(--han-color);
}
.empty,
.note {
  margin: 0;
  padding: 14px;
  font-size: 0.85rem;
  color: var(--muted);
  line-height: 1.5;
}
.note {
  padding: 8px 14px 10px;
  font-size: 0.75rem;
  border-top: 1px solid var(--border);
}
</style>
