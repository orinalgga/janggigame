<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import type { ClockDto, Player, Side } from '../api'
import CharacterAvatar from './CharacterAvatar.vue'
import { openProfile } from '../profile'

const props = defineProps<{
  clock: ClockDto
  cho: Player | null
  han: Player | null
  /** 아래(내 쪽)에 보일 진영. 위에는 상대가 나온다. */
  bottom: Side
  ratingChange: { cho: number; han: number } | null
}>()

// 서버가 보낸 남은 시간을 받은 시각부터 running 쪽만 직접 줄여 보여 준다.
const receivedAt = ref(Date.now())
const now = ref(Date.now())
watch(
  () => props.clock,
  () => {
    receivedAt.value = Date.now()
    now.value = receivedAt.value
  },
)
const timer = setInterval(() => (now.value = Date.now()), 200)
onBeforeUnmount(() => clearInterval(timer))

/** 지금 남은 기본 시간, 초읽기 횟수, 이번 초읽기에서 남은 시간. 서버 GameClock과 같은 셈이다. */
function left(side: Side): { main: number; periods: number; period: number } {
  const c = props.clock
  const main = side === 'CHO' ? c.choMs : c.hanMs
  const periods = side === 'CHO' ? c.choPeriods : c.hanPeriods
  if (c.running !== side) return { main, periods, period: c.periodMs }
  const elapsed = now.value - receivedAt.value
  if (elapsed < main) return { main: main - elapsed, periods, period: c.periodMs }
  if (c.periodMs <= 0 || periods <= 0) return { main: 0, periods: 0, period: 0 }
  // 받은 시점에 이미 초읽기 중이었으면 그 초읽기에서 쓴 시간부터 이어서 센다.
  const used = (main > 0 ? 0 : c.periodMs - c.periodLeftMs) + elapsed - main
  const lost = Math.floor(used / c.periodMs)
  if (lost >= periods) return { main: 0, periods: 0, period: 0 }
  return { main: 0, periods: periods - lost, period: c.periodMs - (used % c.periodMs) }
}

/** 초읽기 횟수 점. 초읽기 없이 만든 옛 대국은 보이지 않는다. */
const totalPeriods = computed(() => (props.clock.periodMs > 0 ? 3 : 0))

function format(ms: number) {
  const s = Math.ceil(ms / 1000)
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
}

const rows = computed(() =>
  ([props.bottom === 'CHO' ? 'HAN' : 'CHO', props.bottom] as Side[]).map((side) => {
    const p = side === 'CHO' ? props.cho : props.han
    const change = props.ratingChange ? (side === 'CHO' ? props.ratingChange.cho : props.ratingChange.han) : null
    const t = left(side)
    const byoyomi = t.main <= 0 && t.periods > 0
    return {
      side,
      player: p,
      nickname: p?.nickname ?? '?',
      detail: p ? `${p.grade} · ${p.rating}` : '',
      change: change === null ? '' : change >= 0 ? `+${change}` : `${change}`,
      time: format(byoyomi ? t.period : t.main),
      byoyomi,
      periods: t.periods,
      running: props.clock.running === side,
      low: byoyomi ? t.period < 10_000 : t.main < 30_000 && t.periods === 0,
    }
  }),
)
</script>

<template>
  <div class="clocks">
    <div v-for="r in rows" :key="r.side" class="row" :class="[r.side.toLowerCase(), { running: r.running }]">
      <button
        type="button"
        class="open"
        :disabled="!r.player"
        :aria-label="`${r.nickname} 전적 보기`"
        @click="openProfile(r.player?.id)"
      >
        <CharacterAvatar :look="r.player?.look" class="figure" />
      </button>
      <span class="name">
        <span class="who">
          <b class="side-mark">{{ r.side === 'CHO' ? '초' : '한' }}</b>
          <b class="nick">{{ r.nickname }}</b>
        </span>
        <small>
          {{ r.detail }}
          <span v-if="r.change" class="change">{{ r.change }}</span>
        </small>
      </span>
      <span class="time" :class="{ low: r.low, byoyomi: r.byoyomi }">
        <small v-if="r.byoyomi" class="byo-label">초읽기</small>
        {{ r.time }}
        <span class="periods" :aria-label="`초읽기 ${r.periods}회 남음`" :title="`초읽기 ${r.periods}회 남음`">
          <i v-for="n in totalPeriods" :key="n" :class="{ used: n > r.periods }" />
        </span>
      </span>
    </div>
  </div>
</template>

<style scoped>
.open {
  flex: none;
  padding: 0;
  border: none;
  background: none;
  line-height: 0;
  cursor: pointer;
}
.open:disabled {
  cursor: default;
}
.clocks {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}
.row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 12px 5px 6px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: var(--surface, rgba(255, 255, 255, 0.5));
}
.figure {
  width: 46px;
  border-radius: 9px;
  overflow: hidden;
}
.who {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.side-mark {
  flex: none;
  display: inline-grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 0.75rem;
  color: #fff;
}
.cho .side-mark {
  background: var(--cho-color);
}
.han .side-mark {
  background: var(--han-color);
}
.nick {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
small {
  color: var(--muted);
  font-weight: 400;
}
.row.running {
  border-color: currentColor;
  font-weight: 600;
}
.cho {
  color: var(--cho-color);
}
.han {
  color: var(--han-color);
}
.name {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  color: var(--text);
}
.change {
  margin-left: 6px;
  font-weight: 600;
}
.time {
  font-variant-numeric: tabular-nums;
  font-size: 1.1rem;
}
.time.low {
  color: var(--han-color);
}
.time {
  display: flex;
  align-items: center;
  gap: 6px;
}
.byo-label {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--han-color);
}
.periods {
  display: inline-flex;
  gap: 3px;
}
.periods i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}
.periods i.used {
  opacity: 0.2;
}
</style>
