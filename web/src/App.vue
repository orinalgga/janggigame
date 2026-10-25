<script setup lang="ts">
import { computed, ref } from 'vue'
import JanggiBoard from './components/JanggiBoard.vue'
import { api, type Formation, type GameDto, type SquareDto } from './api'

const FORMATIONS: { value: Formation; label: string }[] = [
  { value: 'MA_SANG_MA_SANG', label: '마상마상' },
  { value: 'SANG_MA_SANG_MA', label: '상마상마' },
  { value: 'MA_SANG_SANG_MA', label: '마상상마' },
  { value: 'SANG_MA_MA_SANG', label: '상마마상' },
]

const choFormation = ref<Formation>('MA_SANG_MA_SANG')
const hanFormation = ref<Formation>('MA_SANG_MA_SANG')
const game = ref<GameDto | null>(null)
const selected = ref<SquareDto | null>(null)
const error = ref('')

const canPass = computed(() => game.value?.legalMoves.some((m) => m.from === null) ?? false)

const status = computed(() => {
  const g = game.value
  if (!g) return ''
  const turn = g.sideToMove === 'CHO' ? '초' : '한'
  if (g.checkmate) return `외통! ${turn}이 졌습니다.`
  if (g.bikjang) return `빅장입니다. ${turn} 차례`
  if (g.check) return `장군! ${turn} 차례`
  return `${turn} 차례`
})

async function start() {
  error.value = ''
  selected.value = null
  try {
    game.value = await api.newGame(choFormation.value, hanFormation.value)
  } catch (e) {
    error.value = `서버에 연결할 수 없습니다: ${(e as Error).message}`
  }
}

async function onSelect(sq: SquareDto) {
  const g = game.value
  if (!g || g.checkmate) return
  const piece = g.pieces.find((p) => p.x === sq.x && p.y === sq.y)
  if (piece && piece.side === g.sideToMove) {
    selected.value = sq
    return
  }
  const from = selected.value
  if (!from) return
  const legal = g.legalMoves.some(
    (m) => m.from?.x === from.x && m.from?.y === from.y && m.to?.x === sq.x && m.to?.y === sq.y,
  )
  if (!legal) return
  await play({ from, to: sq })
}

async function pass() {
  await play({ from: null, to: null })
}

async function play(move: { from: SquareDto | null; to: SquareDto | null }) {
  if (!game.value) return
  try {
    game.value = await api.move(game.value.id, move)
    selected.value = null
    error.value = ''
  } catch (e) {
    error.value = (e as Error).message
  }
}
</script>

<template>
  <main>
    <h1>장기</h1>
    <section class="controls">
      <label>
        초 상차림
        <select v-model="choFormation">
          <option v-for="f in FORMATIONS" :key="f.value" :value="f.value">{{ f.label }}</option>
        </select>
      </label>
      <label>
        한 상차림
        <select v-model="hanFormation">
          <option v-for="f in FORMATIONS" :key="f.value" :value="f.value">{{ f.label }}</option>
        </select>
      </label>
      <button @click="start">새 대국</button>
    </section>

    <p v-if="error" class="error">{{ error }}</p>

    <template v-if="game">
      <p class="status">
        {{ status }}
        <span class="score">초 {{ game.choScore }} · 한 {{ game.hanScore }}</span>
      </p>
      <JanggiBoard :pieces="game.pieces" :legal-moves="game.legalMoves" :selected="selected" @select="onSelect" />
      <button :disabled="!canPass" @click="pass">한수쉼</button>
    </template>
  </main>
</template>

<style scoped>
main {
  max-width: 560px;
  margin: 0 auto;
  padding: 16px;
}
.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}
.status {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
}
.score {
  font-weight: 400;
}
.error {
  color: var(--han-color);
}
button {
  padding: 6px 14px;
  margin-top: 8px;
}
</style>
