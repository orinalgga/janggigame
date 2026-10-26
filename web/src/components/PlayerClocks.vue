<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import type { ClockDto, Player, Side } from '../api'
import CharacterAvatar from './CharacterAvatar.vue'

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
      player: p,
      nickname: p?.nickname ?? '?',
      detail: p ? `${p.grade} · ${p.rating}` : '',
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
      <CharacterAvatar :look="r.player?.look" class="figure" />
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
</style>
