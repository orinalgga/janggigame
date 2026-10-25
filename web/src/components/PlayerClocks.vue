<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import type { ClockDto, Player, Side } from '../api'

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

function left(side: Side) {
  const base = side === 'CHO' ? props.clock.choMs : props.clock.hanMs
  return Math.max(0, props.clock.running === side ? base - (now.value - receivedAt.value) : base)
}

function format(ms: number) {
  const s = Math.ceil(ms / 1000)
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
}

const rows = computed(() =>
  ([props.bottom === 'CHO' ? 'HAN' : 'CHO', props.bottom] as Side[]).map((side) => {
    const p = side === 'CHO' ? props.cho : props.han
    const change = props.ratingChange ? (side === 'CHO' ? props.ratingChange.cho : props.ratingChange.han) : null
    const ms = left(side)
    return {
      side,
      name: p ? `${p.nickname} · ${p.grade} (${p.rating})` : '?',
      change: change === null ? '' : change >= 0 ? `+${change}` : `${change}`,
      time: format(ms),
      running: props.clock.running === side,
      low: ms < 30_000,
    }
  }),
)
</script>

<template>
  <div class="clocks">
    <div v-for="r in rows" :key="r.side" class="row" :class="[r.side.toLowerCase(), { running: r.running }]">
      <span class="name">
        <b>{{ r.side === 'CHO' ? '초' : '한' }}</b> {{ r.name }}
        <span v-if="r.change" class="change">{{ r.change }}</span>
      </span>
      <span class="time" :class="{ low: r.low }">{{ r.time }}</span>
    </div>
  </div>
</template>

<style scoped>
.clocks {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.1);
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
</style>
