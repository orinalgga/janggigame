<script setup lang="ts">
import { computed } from 'vue'
import type { MoveDto, PieceDto, SquareDto } from '../api'

const props = defineProps<{
  pieces: PieceDto[]
  legalMoves: MoveDto[]
  selected: SquareDto | null
  lastMove: MoveDto | null
  /** true면 한이 아래에 보이도록 판을 돌린다. */
  flipped: boolean
}>()

const emit = defineEmits<{
  (e: 'select', square: SquareDto): void
}>()

const CELL = 60
const PAD = 40
const WIDTH = PAD * 2 + CELL * 8
const HEIGHT = PAD * 2 + CELL * 9

// 기본은 한이 위, 초가 아래. flipped면 180도 돌린다.
const px = (x: number) => PAD + (props.flipped ? 8 - x : x) * CELL
const py = (y: number) => PAD + (props.flipped ? y : 9 - y) * CELL

const lastSquares = computed(() => {
  const m = props.lastMove
  return m && m.from && m.to ? [m.from, m.to] : []
})

const RADIUS: Record<string, number> = {
  GENERAL: 27,
  CHARIOT: 23,
  CANNON: 23,
  HORSE: 23,
  ELEPHANT: 23,
  GUARD: 18,
  SOLDIER: 18,
}

const targets = computed(() => {
  const s = props.selected
  if (!s) return []
  return props.legalMoves
    .filter((m) => m.from && m.to && m.from.x === s.x && m.from.y === s.y)
    .map((m) => m.to as SquareDto)
})

const palaceDiagonals = [
  [3, 0, 5, 2],
  [5, 0, 3, 2],
  [3, 7, 5, 9],
  [5, 7, 3, 9],
]

function clickPoint(x: number, y: number) {
  emit('select', { x, y })
}
</script>

<template>
  <svg :viewBox="`0 0 ${WIDTH} ${HEIGHT}`" class="board" role="img" aria-label="장기판">
    <rect :width="WIDTH" :height="HEIGHT" rx="8" class="bg" />
    <line v-for="y in 10" :key="'h' + y" :x1="px(0)" :x2="px(8)" :y1="py(y - 1)" :y2="py(y - 1)" class="line" />
    <line v-for="x in 9" :key="'v' + x" :x1="px(x - 1)" :x2="px(x - 1)" :y1="py(0)" :y2="py(9)" class="line" />
    <line
      v-for="(d, i) in palaceDiagonals"
      :key="'p' + i"
      :x1="px(d[0])"
      :y1="py(d[1])"
      :x2="px(d[2])"
      :y2="py(d[3])"
      class="line"
    />

    <rect
      v-for="(sq, i) in lastSquares"
      :key="'last' + i"
      :x="px(sq.x) - 29"
      :y="py(sq.y) - 29"
      width="58"
      height="58"
      rx="6"
      class="last"
    />

    <template v-for="y in 10" :key="'row' + y">
      <circle
        v-for="x in 9"
        :key="x"
        :cx="px(x - 1)"
        :cy="py(y - 1)"
        r="28"
        class="hit"
        @click="clickPoint(x - 1, y - 1)"
      />
    </template>

    <g
      v-for="p in pieces"
      :key="`${p.x},${p.y}`"
      class="piece"
      :class="[p.side.toLowerCase(), { selected: selected && selected.x === p.x && selected.y === p.y }]"
      @click="clickPoint(p.x, p.y)"
    >
      <circle :cx="px(p.x)" :cy="py(p.y)" :r="RADIUS[p.type]" />
      <text :x="px(p.x)" :y="py(p.y)" :font-size="RADIUS[p.type]" dy="0.35em" text-anchor="middle">{{ p.label }}</text>
    </g>

    <circle
      v-for="t in targets"
      :key="`t${t.x},${t.y}`"
      :cx="px(t.x)"
      :cy="py(t.y)"
      r="10"
      class="target"
      @click="clickPoint(t.x, t.y)"
    />
  </svg>
</template>

<style scoped>
.board {
  width: 100%;
  max-width: 560px;
  display: block;
  user-select: none;
}
.bg {
  fill: var(--board-bg);
}
.line {
  stroke: var(--board-line);
  stroke-width: 1.5;
}
.hit {
  fill: transparent;
  cursor: pointer;
}
.piece {
  cursor: pointer;
}
.piece circle {
  fill: var(--piece-bg);
  stroke-width: 2.5;
}
.piece text {
  font-weight: 700;
  font-family: 'Noto Serif KR', serif;
  pointer-events: none;
}
.piece.cho circle {
  stroke: var(--cho-color);
}
.piece.cho text {
  fill: var(--cho-color);
}
.piece.han circle {
  stroke: var(--han-color);
}
.piece.han text {
  fill: var(--han-color);
}
.piece.selected circle {
  stroke-width: 5;
}
.last {
  fill: var(--last-move);
}
.target {
  fill: var(--highlight);
  cursor: pointer;
}
</style>
