<script setup lang="ts">
import { computed, ref, useId, watch } from 'vue'
import type { MoveDto, PieceDto, Side, SquareDto } from '../api'
import { equipped, themeStyle, type ThemeChoice } from '../themes'

const props = defineProps<{
  pieces: PieceDto[]
  legalMoves: MoveDto[]
  selected: SquareDto | null
  lastMove: MoveDto | null
  /** true면 한이 아래에 보이도록 판을 돌린다. */
  flipped: boolean
  /** 장군을 받은 쪽. 장군이 아니면 null. */
  check?: Side | null
  /** 외통이면 true. */
  mate?: boolean
  /** 상점 미리보기처럼 장착한 것과 다른 테마로 그릴 때. */
  theme?: ThemeChoice
}>()

const style = computed(() => themeStyle(props.theme ?? equipped))
// 한 화면에 판이 여럿이어도 그라데이션과 필터가 섞이지 않게 판마다 id를 따로 쓴다.
const uid = (useId() ?? '').replace(/[^a-zA-Z0-9_-]/g, '')
const defId = (name: string) => `${name}-${uid}`
const url = (name: string) => `url(#${defId(name)})`

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

/**
 * 방금 둔 수. 떠난 자리에 알 모양 발자국, 떠난 곳에서 도착한 곳까지 화살표,
 * 도착한 알 둘레에 고리를 둔 진영 색으로 그린다.
 */
const last = computed(() => {
  const m = props.lastMove
  if (!m?.from || !m.to) return null
  const mover = props.pieces.find((p) => p.x === m.to!.x && p.y === m.to!.y)
  if (!mover) return null
  const r = RADIUS[mover.type]
  const x1 = px(m.from.x)
  const y1 = py(m.from.y)
  const x2 = px(m.to.x)
  const y2 = py(m.to.y)
  const len = Math.hypot(x2 - x1, y2 - y1)
  const ux = (x2 - x1) / len
  const uy = (y2 - y1) / len
  // 화살표는 발자국 안쪽에서 시작해 도착한 알 테두리 바로 앞에서 끝난다.
  const sx = x1 + ux * 10
  const sy = y1 + uy * 10
  const tx = x2 - ux * (r + 7)
  const ty = y2 - uy * (r + 7)
  const bx = tx - ux * 14
  const by = ty - uy * 14
  const head = [
    [tx, ty],
    [bx - uy * 8, by + ux * 8],
    [bx + uy * 8, by - ux * 8],
  ]
  return {
    side: mover.side.toLowerCase(),
    from: { x: x1, y: y1 },
    to: { x: x2, y: y2 },
    r,
    shaft: { x1: sx, y1: sy, x2: bx, y2: by },
    head: head.map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`).join(' '),
  }
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

/** 수를 둘 때의 연출: 알이 미끄러져 가고, 지나간 자리에 잔상이 남고, 잡힌 알은 터지듯 사라진다. */
const MOVE_MS = 320
const svg = ref<SVGSVGElement | null>(null)

type EffectBody =
  | { kind: 'trail'; from: SquareDto; to: SquareDto; piece: PieceDto }
  | { kind: 'captured'; piece: PieceDto }
  | { kind: 'ripple'; at: SquareDto }
type Effect = EffectBody & { id: number }
const effects = ref<Effect[]>([])
const captures = computed(() =>
  effects.value.filter((e): e is Extract<Effect, { kind: 'captured' }> => e.kind === 'captured'),
)
let effectId = 0

function addEffect(e: EffectBody, ms: number) {
  const id = ++effectId
  effects.value.push({ ...e, id })
  setTimeout(() => (effects.value = effects.value.filter((x) => x.id !== id)), ms)
}

const reduceMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

const at = (list: PieceDto[], sq: SquareDto) => list.find((p) => p.x === sq.x && p.y === sq.y)

watch(
  () => props.pieces,
  (now, before) => {
    const m = props.lastMove
    if (!before || !m?.from || !m.to || reduceMotion()) return
    const moved = at(now, m.to)
    const origin = at(before, m.from)
    // 이 수가 방금 판에 반영된 경우에만 움직인다(같은 국면을 다시 받거나 복기에서 건너뛸 때는 그냥 그린다).
    if (!moved || !origin || at(now, m.from) || origin.side !== moved.side || origin.type !== moved.type) return
    const captured = at(before, m.to)
    if (captured && captured.side === moved.side) return

    const dx = px(m.from.x) - px(m.to.x)
    const dy = py(m.from.y) - py(m.to.y)
    const el = svg.value?.querySelector<SVGGElement>(`g.piece[data-sq="${m.to.x},${m.to.y}"]`)
    el?.animate(
      [
        { transform: `translate(${dx}px, ${dy}px) scale(1)` },
        { transform: `translate(${dx * 0.5}px, ${dy * 0.5}px) scale(1.14)`, offset: 0.5 },
        { transform: 'translate(0, 0) scale(0.96)', offset: 0.85 },
        { transform: 'translate(0, 0) scale(1)' },
      ],
      { duration: MOVE_MS, easing: 'cubic-bezier(0.3, 0.7, 0.3, 1)' },
    )
    addEffect({ kind: 'trail', from: m.from, to: m.to, piece: moved }, 700)
    if (captured) addEffect({ kind: 'captured', piece: captured }, MOVE_MS + 450)
    setTimeout(() => addEffect({ kind: 'ripple', at: m.to! }, 600), MOVE_MS * 0.8)
  },
  { flush: 'post' },
)

/** 장군을 받은 장. 둘레에 붉은 고리가 맥박치듯 뛴다. */
const checkedGeneral = computed(() =>
  props.check ? props.pieces.find((p) => p.type === 'GENERAL' && p.side === props.check) : undefined,
)

/** 만화의 깜짝 폭발 말풍선. 뾰족한 끝의 길이를 조금씩 달리해 손으로 그린 느낌을 낸다. */
function burst(spikes: number, outer: number, inner: number) {
  const pts: string[] = []
  for (let k = 0; k < spikes * 2; k++) {
    const jitter = k % 2 === 0 ? [1, 0.86, 0.95, 0.8, 1.04, 0.9][(k / 2) % 6] : 1
    const r = (k % 2 === 0 ? outer : inner) * jitter
    const a = (k * Math.PI) / spikes - Math.PI / 2
    pts.push(`${(r * Math.cos(a)).toFixed(1)},${(r * Math.sin(a)).toFixed(1)}`)
  }
  return pts.join(' ')
}
const BURST = burst(14, 98, 66)
const BURST_INNER = burst(14, 82, 56)

/** 장군이 걸리면 알이 도착한 뒤 화면 가운데에 빨간 폭발 말풍선이 터지고 화면이 흔들린다. 외통이면 더 크게. */
const wrap = ref<HTMLDivElement | null>(null)
const stamp = ref<{ id: number; mate: boolean } | null>(null)
let stampId = 0

watch(
  () => props.pieces,
  (_now, before) => {
    if (!before || !props.check) return
    const mate = !!props.mate
    const id = ++stampId
    setTimeout(() => {
      stamp.value = { id, mate }
      navigator.vibrate?.(mate ? [80, 60, 220] : 120)
      // 판만이 아니라 화면 전체(#app)가 흔들린다. 터지는 말풍선은 body에 따로 있어 함께 흔들리지 않는다.
      // 장군은 꼭 알아차려야 하므로 OS의 '애니메이션 줄이기' 설정과 상관없이 흔든다.
      const k = mate ? 1.6 : 1
      ;(document.getElementById('app') ?? wrap.value)?.animate(
        [
          { transform: 'translate(0, 0)' },
          { transform: `translate(${-14 * k}px, ${5 * k}px) rotate(${-0.8 * k}deg)` },
          { transform: `translate(${12 * k}px, ${-6 * k}px) rotate(${0.7 * k}deg)` },
          { transform: `translate(${-10 * k}px, ${3 * k}px) rotate(${-0.4 * k}deg)` },
          { transform: `translate(${8 * k}px, ${-3 * k}px)` },
          { transform: `translate(${-5 * k}px, ${2 * k}px)` },
          { transform: `translate(${3 * k}px, 0)` },
          { transform: 'translate(0, 0)' },
        ],
        { duration: mate ? 750 : 520, delay: 80, easing: 'ease-out' },
      )
    }, MOVE_MS * 0.8)
    setTimeout(() => stamp.value?.id === id && (stamp.value = null), MOVE_MS * 0.8 + (mate ? 2600 : 1400))
  },
  { flush: 'post' },
)
</script>

<template>
  <div ref="wrap" class="wrap" :style="style">
    <svg ref="svg" :viewBox="`0 0 ${WIDTH} ${HEIGHT}`" class="board" role="img" aria-label="장기판">
      <defs>
        <!-- 나무결: 가로로 길게 늘인 잡음을 판 색에 얹는다. -->
        <filter :id="defId('grain')" x="0" y="0" width="100%" height="100%">
          <feTurbulence type="fractalNoise" baseFrequency="0.004 0.09" numOctaves="3" seed="7" />
          <feColorMatrix
            type="matrix"
            values="0 0 0 0 0.35  0 0 0 0 0.2  0 0 0 0 0.08  0 0 0 0.55 -0.12"
          />
          <feComposite in2="SourceGraphic" operator="in" />
        </filter>
        <linearGradient :id="defId('wood')" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="var(--board-light)" />
          <stop offset="1" stop-color="var(--board-dark)" />
        </linearGradient>
        <radialGradient :id="defId('face')" cx="0.4" cy="0.35" r="0.75">
          <stop offset="0" stop-color="var(--piece-light)" />
          <stop offset="1" stop-color="var(--piece-dark)" />
        </radialGradient>
        <filter :id="defId('shadow')" x="-30%" y="-30%" width="160%" height="160%">
          <feDropShadow dx="1.5" dy="2.5" stdDeviation="1.8" flood-color="#3a2108" flood-opacity="0.45" />
        </filter>
        <filter :id="defId('lift')" x="-40%" y="-40%" width="180%" height="180%">
          <feDropShadow dx="2.5" dy="5" stdDeviation="3" flood-color="#3a2108" flood-opacity="0.5" />
        </filter>
      </defs>

      <rect :width="WIDTH" :height="HEIGHT" rx="10" :fill="url('wood')" />
      <rect :width="WIDTH" :height="HEIGHT" rx="10" fill="var(--board-dark)" :filter="url('grain')" class="grain" />
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

      <g v-if="last" :key="`${last.from.x},${last.from.y}-${last.to.x},${last.to.y}`" class="last" :class="last.side">
        <rect :x="last.to.x - 29" :y="last.to.y - 29" width="58" height="58" rx="8" class="spot" />
        <polygon :points="octagon(last.from.x, last.from.y, last.r - 2)" class="footprint" />
        <line v-bind="last.shaft" class="arrow" />
        <polygon :points="last.head" class="arrow-head" />
      </g>

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

      <template v-for="e in effects" :key="'fx' + e.id">
        <g v-if="e.kind === 'trail'" class="trail" :class="e.piece.side.toLowerCase()">
          <line :x1="px(e.from.x)" :y1="py(e.from.y)" :x2="px(e.to.x)" :y2="py(e.to.y)" class="streak" />
          <polygon
            v-for="k in 3"
            :key="k"
            :points="
              octagon(
                px(e.from.x) + (px(e.to.x) - px(e.from.x)) * (k - 1) * 0.28,
                py(e.from.y) + (py(e.to.y) - py(e.from.y)) * (k - 1) * 0.28,
                RADIUS[e.piece.type],
              )
            "
            class="ghost"
            :style="{ animationDelay: `${(k - 1) * 70}ms` }"
          />
        </g>
        <polygon
          v-else-if="e.kind === 'ripple'"
          :points="octagon(px(e.at.x), py(e.at.y), 30)"
          class="ripple"
        />
      </template>

      <g
        v-for="e in captures"
        :key="'cap' + e.id"
        class="piece captured"
        :class="e.piece.side.toLowerCase()"
        :filter="url('shadow')"
      >
        <polygon :points="octagon(px(e.piece.x), py(e.piece.y), RADIUS[e.piece.type])" class="edge" />
        <polygon
          :points="octagon(px(e.piece.x), py(e.piece.y), RADIUS[e.piece.type] - 2.5)"
          :fill="url('face')"
          class="face"
        />
        <text
          :x="px(e.piece.x)"
          :y="py(e.piece.y)"
          :font-size="RADIUS[e.piece.type] * 1.12"
          dy="0.36em"
          text-anchor="middle"
        >
          {{ e.piece.label }}
        </text>
      </g>

      <g
        v-for="p in pieces"
        :key="`${p.x},${p.y}`"
        :data-sq="`${p.x},${p.y}`"
        class="piece"
        :class="[p.side.toLowerCase(), { selected: isSelected(p) }]"
        :filter="isSelected(p) ? url('lift') : url('shadow')"
        @click="clickPoint(p.x, p.y)"
      >
        <polygon :points="octagon(px(p.x), py(p.y) - (isSelected(p) ? 2 : 0), RADIUS[p.type])" class="edge" />
        <polygon
          :points="octagon(px(p.x), py(p.y) - (isSelected(p) ? 2 : 0), RADIUS[p.type] - 2.5)"
          :fill="url('face')"
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

      <polygon v-if="last" :points="octagon(last.to.x, last.to.y, last.r + 4)" class="last-ring" :class="last.side" />

      <g v-if="checkedGeneral" class="check-ring" :class="{ mate }">
        <polygon :points="octagon(px(checkedGeneral.x), py(checkedGeneral.y), RADIUS.GENERAL + 7)" class="glow" />
        <polygon :points="octagon(px(checkedGeneral.x), py(checkedGeneral.y), RADIUS.GENERAL + 7)" />
        <polygon :points="octagon(px(checkedGeneral.x), py(checkedGeneral.y), RADIUS.GENERAL + 7)" class="echo" />
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
    <Teleport to="body">
      <div v-if="stamp" :key="stamp.id" class="shock" :class="{ mate: stamp.mate }" role="status">
        <div class="speed-lines" aria-hidden="true" />
        <div class="burst">
          <svg viewBox="-110 -110 220 220" aria-hidden="true">
            <defs>
              <radialGradient :id="defId('burst-fill')" cx="0" cy="0" r="100" gradientUnits="userSpaceOnUse">
                <stop offset="0" stop-color="#ff5a3c" />
                <stop offset="0.6" stop-color="#e3101a" />
                <stop offset="1" stop-color="#a1000a" />
              </radialGradient>
            </defs>
            <polygon :points="BURST" class="burst-shadow" transform="translate(6 8)" />
            <polygon :points="BURST" :fill="url('burst-fill')" class="burst-edge" />
            <polygon :points="BURST_INNER" class="burst-inner" />
          </svg>
          <span class="shout">{{ stamp.mate ? '외통!!' : '장군!!' }}</span>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* 나무결 짙기. 한지나 옥돌처럼 결이 옅은 테마는 줄인다. */
.grain {
  opacity: var(--grain, 1);
}
.wrap {
  position: relative;
  width: 100%;
  max-width: 560px;
}
.board {
  width: 100%;
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
.piece {
  transform-box: fill-box;
  transform-origin: center;
  transition: filter 0.15s;
}
.piece.selected {
  animation: breathe 1.2s ease-in-out infinite;
}
.piece.captured {
  pointer-events: none;
  animation: shatter 0.5s ease-in 0.24s both;
}
.trail {
  pointer-events: none;
}
.trail .streak {
  stroke-width: 10;
  stroke-linecap: round;
  stroke-opacity: 0.35;
  animation: fade 0.6s ease-out both;
}
.trail .ghost {
  fill: var(--piece-light);
  stroke-width: 2;
  animation: ghost 0.5s ease-out both;
}
.trail.cho .streak,
.trail.cho .ghost {
  stroke: var(--cho-color);
}
.trail.han .streak,
.trail.han .ghost {
  stroke: var(--han-color);
}
.ripple {
  fill: none;
  stroke: var(--last-move-strong);
  stroke-width: 3;
  pointer-events: none;
  transform-box: fill-box;
  transform-origin: center;
  animation: ripple 0.55s ease-out both;
}
@keyframes breathe {
  50% {
    transform: scale(1.06);
  }
}
@keyframes shatter {
  0% {
    opacity: 1;
    transform: scale(1);
  }
  30% {
    transform: scale(1.2) rotate(-8deg);
  }
  100% {
    opacity: 0;
    transform: scale(0.4) rotate(25deg);
  }
}
@keyframes fade {
  to {
    stroke-opacity: 0;
  }
}
@keyframes ghost {
  from {
    opacity: 0.45;
  }
  to {
    opacity: 0;
  }
}
@keyframes ripple {
  from {
    opacity: 0.9;
    transform: scale(0.8);
  }
  to {
    opacity: 0;
    transform: scale(1.6);
  }
}
@media (prefers-reduced-motion: reduce) {
  .piece.selected {
    animation: none;
  }
}
.target {
  animation: pop-in 0.18s ease-out both;
  transform-box: fill-box;
  transform-origin: center;
}
@keyframes pop-in {
  from {
    opacity: 0;
    transform: scale(0.3);
  }
}
.check-ring polygon {
  fill: none;
  stroke: var(--han-color);
  stroke-width: 4;
  pointer-events: none;
  transform-box: fill-box;
  transform-origin: center;
  animation: alarm 0.9s ease-in-out infinite;
}
.check-ring .glow {
  stroke-width: 12;
  stroke-opacity: 0.25;
  stroke-linejoin: round;
}
.check-ring .echo {
  stroke-width: 2;
  animation: echo 0.9s ease-out infinite;
}
.check-ring.mate polygon {
  stroke-width: 5;
  animation-duration: 0.6s;
}
@keyframes alarm {
  50% {
    stroke-opacity: 0.45;
    transform: scale(1.06);
  }
}
@keyframes echo {
  from {
    opacity: 0.8;
    transform: scale(1);
  }
  to {
    opacity: 0;
    transform: scale(1.7);
  }
}
.shock {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  pointer-events: none;
  animation: shock-out 0.35s ease-in 1.05s forwards;
}
.shock.mate {
  animation-delay: 2.25s;
}
.speed-lines {
  position: absolute;
  inset: -20%;
  background: repeating-conic-gradient(from 0deg, rgba(190, 0, 10, 0.55) 0deg 1.6deg, transparent 1.6deg 7deg);
  -webkit-mask-image: radial-gradient(circle, transparent 18%, #000 60%);
  mask-image: radial-gradient(circle, transparent 18%, #000 60%);
  animation: lines-in 0.25s ease-out both;
}
.mate .speed-lines {
  background:
    radial-gradient(circle, transparent 30%, rgba(40, 0, 0, 0.5) 100%),
    repeating-conic-gradient(from 0deg, rgba(150, 0, 10, 0.7) 0deg 2deg, transparent 2deg 6deg);
}
.burst {
  position: relative;
  display: grid;
  place-items: center;
  width: min(78vw, 420px);
  aspect-ratio: 1;
  animation: burst-pop 0.5s cubic-bezier(0.2, 1.8, 0.4, 1) both;
}
.mate .burst {
  width: min(90vw, 520px);
}
.burst svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
}
.burst-shadow {
  fill: rgba(40, 0, 0, 0.35);
}
.burst-edge {
  stroke: #3d0004;
  stroke-width: 6;
  stroke-linejoin: round;
}
.burst-inner {
  fill: none;
  stroke: rgba(255, 236, 200, 0.85);
  stroke-width: 3;
  stroke-linejoin: round;
}
.shout {
  position: relative;
  color: #fff;
  font-weight: 900;
  font-size: min(15vw, 5.2rem);
  letter-spacing: -0.04em;
  transform: rotate(-8deg);
  -webkit-text-stroke: 3px #3d0004;
  paint-order: stroke fill;
  text-shadow: 0 5px 0 #3d0004;
  animation: shout-shake 0.5s 0.35s ease-in-out 2;
}
.mate .shout {
  font-size: min(18vw, 6.4rem);
}
@keyframes burst-pop {
  from {
    opacity: 0;
    transform: scale(0.1) rotate(-30deg);
  }
  to {
    opacity: 1;
    transform: scale(1) rotate(0);
  }
}
@keyframes lines-in {
  from {
    opacity: 0;
    transform: scale(1.4);
  }
}
@keyframes shout-shake {
  25% {
    transform: rotate(-11deg) translate(-3px, 1px);
  }
  75% {
    transform: rotate(-5deg) translate(3px, -1px);
  }
}
@keyframes shock-out {
  to {
    opacity: 0;
    transform: scale(1.15);
  }
}
@media (prefers-reduced-motion: reduce) {
  .check-ring polygon {
    animation: none;
  }
}
.last {
  pointer-events: none;
  --mover: var(--han-color);
}
.last.cho,
.last-ring.cho {
  --mover: var(--cho-color);
}
.last {
  animation: last-in 0.3s 0.22s ease-out both;
}
@keyframes last-in {
  from {
    opacity: 0;
  }
}
.last .spot {
  fill: var(--last-move);
}
.last .footprint {
  fill: var(--mover);
  fill-opacity: 0.12;
  stroke: var(--mover);
  stroke-width: 2.5;
  stroke-dasharray: 6 5;
  stroke-opacity: 0.85;
}
.last .arrow {
  stroke: var(--mover);
  stroke-width: 6;
  stroke-linecap: round;
  stroke-opacity: 0.55;
}
.last .arrow-head {
  fill: var(--mover);
  fill-opacity: 0.75;
}
.last-ring {
  --mover: var(--han-color);
  fill: none;
  stroke: var(--mover);
  stroke-width: 3;
  stroke-opacity: 0.9;
  pointer-events: none;
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
