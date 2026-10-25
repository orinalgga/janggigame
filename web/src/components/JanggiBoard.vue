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

/** 장기알은 팔각형이고, 장 > 차·포·마·상 > 사·졸 순으로 크다. */
const RADIUS: Record<string, number> = {
  GENERAL: 28,
  CHARIOT: 24,
  CANNON: 24,
  HORSE: 24,
  ELEPHANT: 24,
  GUARD: 19,
  SOLDIER: 19,
}

/** 중심 (cx, cy), 반지름 r인 팔각형. 위아래가 평평하다. */
function octagon(cx: number, cy: number, r: number) {
  const pts: string[] = []
  for (let k = 0; k < 8; k++) {
    const a = ((22.5 + k * 45) * Math.PI) / 180
    pts.push(`${(cx + r * Math.cos(a)).toFixed(1)},${(cy + r * Math.sin(a)).toFixed(1)}`)
  }
  return pts.join(' ')
}

/** 잡을 수 있는 칸이면 점 대신 상대 알 둘레에 고리를 그린다. */
function occupant(sq: SquareDto) {
  return props.pieces.find((p) => p.x === sq.x && p.y === sq.y)
}

const targets = computed(() => {
  const s = props.selected
  if (!s) return []
  return props.legalMoves
    .filter((m) => m.from && m.to && m.from.x === s.x && m.from.y === s.y)
    .map((m) => m.to as SquareDto)
})

const isSelected = (p: PieceDto) => !!props.selected && props.selected.x === p.x && props.selected.y === p.y

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
    <defs>
      <!-- 나무결: 가로로 길게 늘인 잡음을 판 색에 얹는다. -->
      <filter id="grain" x="0" y="0" width="100%" height="100%">
        <feTurbulence type="fractalNoise" baseFrequency="0.004 0.09" numOctaves="3" seed="7" />
        <feColorMatrix
          type="matrix"
          values="0 0 0 0 0.35  0 0 0 0 0.2  0 0 0 0 0.08  0 0 0 0.55 -0.12"
        />
        <feComposite in2="SourceGraphic" operator="in" />
      </filter>
      <linearGradient id="wood" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="var(--board-light)" />
        <stop offset="1" stop-color="var(--board-dark)" />
      </linearGradient>
      <radialGradient id="face" cx="0.4" cy="0.35" r="0.75">
        <stop offset="0" stop-color="var(--piece-light)" />
        <stop offset="1" stop-color="var(--piece-dark)" />
      </radialGradient>
      <filter id="shadow" x="-30%" y="-30%" width="160%" height="160%">
        <feDropShadow dx="1.5" dy="2.5" stdDeviation="1.8" flood-color="#3a2108" flood-opacity="0.45" />
      </filter>
      <filter id="lift" x="-40%" y="-40%" width="180%" height="180%">
        <feDropShadow dx="2.5" dy="5" stdDeviation="3" flood-color="#3a2108" flood-opacity="0.5" />
      </filter>
    </defs>

    <rect :width="WIDTH" :height="HEIGHT" rx="10" fill="url(#wood)" />
    <rect :width="WIDTH" :height="HEIGHT" rx="10" fill="var(--board-dark)" filter="url(#grain)" />
    <rect x="4" y="4" :width="WIDTH - 8" :height="HEIGHT - 8" rx="8" class="frame" />

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
    <rect :x="px(0) - 6" :y="PAD - 6" :width="CELL * 8 + 12" :height="CELL * 9 + 12" class="border" />

    <rect
      v-for="(sq, i) in lastSquares"
      :key="'last' + i"
      :x="px(sq.x) - 29"
      :y="py(sq.y) - 29"
      width="58"
      height="58"
      rx="8"
      class="last"
      :class="{ to: i === 1 }"
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
      :class="[p.side.toLowerCase(), { selected: isSelected(p) }]"
      :filter="isSelected(p) ? 'url(#lift)' : 'url(#shadow)'"
      @click="clickPoint(p.x, p.y)"
    >
      <polygon :points="octagon(px(p.x), py(p.y) - (isSelected(p) ? 2 : 0), RADIUS[p.type])" class="edge" />
      <polygon
        :points="octagon(px(p.x), py(p.y) - (isSelected(p) ? 2 : 0), RADIUS[p.type] - 2.5)"
        fill="url(#face)"
        class="face"
      />
      <polygon
        :points="octagon(px(p.x), py(p.y) - (isSelected(p) ? 2 : 0), RADIUS[p.type] - 6)"
        class="inlay"
      />
      <text
        :x="px(p.x)"
        :y="py(p.y) - (isSelected(p) ? 2 : 0)"
        :font-size="RADIUS[p.type] * 1.12"
        dy="0.36em"
        text-anchor="middle"
      >
        {{ p.label }}
      </text>
    </g>

    <template v-for="t in targets" :key="`t${t.x},${t.y}`">
      <polygon
        v-if="occupant(t)"
        :points="octagon(px(t.x), py(t.y), RADIUS[occupant(t)!.type] + 4)"
        class="capture"
        @click="clickPoint(t.x, t.y)"
      />
      <circle v-else :cx="px(t.x)" :cy="py(t.y)" r="9" class="target" @click="clickPoint(t.x, t.y)" />
    </template>
  </svg>
</template>

<style scoped>
.board {
  width: 100%;
  max-width: 560px;
  display: block;
  user-select: none;
  border-radius: 10px;
  box-shadow:
    0 10px 24px rgba(58, 33, 8, 0.25),
    0 2px 4px rgba(58, 33, 8, 0.2);
}
.frame {
  fill: none;
  stroke: var(--board-line);
  stroke-opacity: 0.35;
  stroke-width: 2;
}
.line {
  stroke: var(--board-line);
  stroke-width: 1.6;
  stroke-linecap: round;
}
.border {
  fill: none;
  stroke: var(--board-line);
  stroke-width: 3;
}
.hit {
  fill: transparent;
  cursor: pointer;
}
.piece {
  cursor: pointer;
}
.piece .edge {
  fill: var(--piece-edge);
}
.piece .face {
  stroke: rgba(255, 255, 255, 0.6);
  stroke-width: 1;
}
.piece .inlay {
  fill: none;
  stroke-width: 1.4;
  stroke-opacity: 0.55;
}
.piece text {
  font-family: 'Noto Serif KR', 'Nanum Myeongjo', 'AppleMyungjo', serif;
  font-weight: 900;
  pointer-events: none;
  paint-order: stroke;
  stroke: rgba(255, 255, 255, 0.5);
  stroke-width: 0.8;
}
.piece.cho text {
  fill: var(--cho-color);
}
.piece.cho .inlay {
  stroke: var(--cho-color);
}
.piece.han text {
  fill: var(--han-color);
}
.piece.han .inlay {
  stroke: var(--han-color);
}
.piece.selected .edge {
  fill: var(--select);
}
.last {
  fill: var(--last-move);
}
.last.to {
  fill: none;
  stroke: var(--last-move-strong);
  stroke-width: 3;
}
.target {
  fill: var(--highlight);
  stroke: rgba(255, 255, 255, 0.7);
  stroke-width: 1.5;
  cursor: pointer;
}
.capture {
  fill: none;
  stroke: var(--capture);
  stroke-width: 3.5;
  stroke-dasharray: 6 4;
  cursor: pointer;
}
</style>
