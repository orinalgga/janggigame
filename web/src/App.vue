<script setup lang="ts">
import { computed, ref } from 'vue'
import JanggiBoard from './components/JanggiBoard.vue'
import { api, type Formation, type GameDto, type Level, type Side, type SquareDto } from './api'

const FORMATIONS: { value: Formation; label: string }[] = [
  { value: 'MA_SANG_MA_SANG', label: '마상마상' },
  { value: 'SANG_MA_SANG_MA', label: '상마상마' },
  { value: 'MA_SANG_SANG_MA', label: '마상상마' },
  { value: 'SANG_MA_MA_SANG', label: '상마마상' },
]

const LEVELS: { value: Level; label: string }[] = [
  { value: 'BEGINNER', label: '초급' },
  { value: 'INTERMEDIATE', label: '중급' },
  { value: 'ADVANCED', label: '상급' },
]

/** HUMAN이면 한 화면에서 둘이 두고, 아니면 그 진영을 AI가 둔다. */
const opponent = ref<'HUMAN' | Side>('HAN')
const level = ref<Level>('INTERMEDIATE')
const thinking = ref(false)
const choFormation = ref<Formation>('MA_SANG_MA_SANG')
const hanFormation = ref<Formation>('MA_SANG_MA_SANG')
const game = ref<GameDto | null>(null)
const selected = ref<SquareDto | null>(null)
const error = ref('')

const canPass = computed(() => game.value?.legalMoves.some((m) => m.from === null) ?? false)

const flipped = computed(() => game.value?.aiSide === 'CHO')

const status = computed(() => {
  const g = game.value
  if (!g) return ''
  if (thinking.value) return 'AI가 생각하는 중…'
  const turn = g.sideToMove === 'CHO' ? '초' : '한'
  if (g.result !== 'ONGOING') {
    const how = g.checkmate ? '외통' : '빅장, 점수 판정'
    if (g.result === 'DRAW') return `${how}: 무승부입니다.`
    return `${how}: ${g.result === 'CHO_WIN' ? '초' : '한'}가 이겼습니다.`
  }
  if (g.check) return `장군! ${turn} 차례`
  return `${turn} 차례`
})

async function start() {
  error.value = ''
  selected.value = null
  try {
    const aiSide = opponent.value === 'HUMAN' ? null : opponent.value
    thinking.value = aiSide === 'CHO'
    game.value = await api.newGame(choFormation.value, hanFormation.value, aiSide, level.value)
  } catch (e) {
    error.value = `서버에 연결할 수 없습니다: ${(e as Error).message}`
  } finally {
    thinking.value = false
  }
}

async function onSelect(sq: SquareDto) {
  const g = game.value
  if (!g || g.result !== 'ONGOING' || thinking.value || g.sideToMove === g.aiSide) return
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
  selected.value = null
  thinking.value = game.value.aiSide !== null
  try {
    game.value = await api.move(game.value.id, move)
    error.value = ''
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    thinking.value = false
  }
}
</script>

<template>
  <main>
    <h1>장기</h1>
    <section class="controls">
      <label>
        상대
        <select v-model="opponent">
          <option value="HAN">AI (내가 초)</option>
          <option value="CHO">AI (내가 한)</option>
          <option value="HUMAN">같은 화면 2인</option>
        </select>
      </label>
      <label v-if="opponent !== 'HUMAN'">
        난이도
        <select v-model="level">
          <option v-for="l in LEVELS" :key="l.value" :value="l.value">{{ l.label }}</option>
        </select>
      </label>
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
      <JanggiBoard
        :pieces="game.pieces"
        :legal-moves="game.legalMoves"
        :selected="selected"
        :last-move="game.lastMove"
        :flipped="flipped"
        @select="onSelect"
      />
      <button :disabled="!canPass || thinking || game.sideToMove === game.aiSide" @click="pass">한수쉼</button>
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
