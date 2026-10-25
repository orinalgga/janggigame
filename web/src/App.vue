<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import JanggiBoard from './components/JanggiBoard.vue'
import PlayerClocks from './components/PlayerClocks.vue'
import RecentGames from './components/RecentGames.vue'
import ReplayView from './components/ReplayView.vue'
import {
  api,
  auth,
  type EndReason,
  type Formation,
  type GameDto,
  type Level,
  type MatchStatus,
  type Me,
  type Side,
  type SquareDto,
} from './api'
import { Live } from './live'

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

const END_REASON: Record<EndReason, string> = {
  CHECKMATE: '외통',
  BIKJANG: '빅장, 점수 판정',
  RESIGN: '기권',
  TIMEOUT: '시간패',
}

/** HUMAN이면 한 화면에서 둘이 두고, ONLINE이면 매칭된 회원과 두고, 아니면 그 진영을 AI가 둔다. */
const opponent = ref<'HUMAN' | 'ONLINE' | Side>('HAN')
const level = ref<Level>('INTERMEDIATE')
const thinking = ref(false)
/** 수를 보내고 응답을 기다리는 중. 이때 누른 칸은 무시해서 옛 국면 기준으로 알이 골라지지 않게 한다. */
const busy = ref(false)
const replayId = ref<string | null>(null)
const recent = ref<InstanceType<typeof RecentGames> | null>(null)
const me = ref<Me | null>(null)
const providers = ref<string[]>([])

onMounted(async () => {
  auth.takeTokenFromUrl()
  providers.value = (await api.providers().catch(() => ({ providers: [] }))).providers
  if (auth.token()) {
    me.value = await api.me().catch(() => {
      auth.clear()
      return null
    })
    recent.value?.refresh()
  }
})

function logout() {
  leaveOnline()
  live?.close()
  live = null
  auth.clear()
  me.value = null
  if (opponent.value === 'ONLINE') opponent.value = 'HAN'
  recent.value?.refresh()
}
const choFormation = ref<Formation>('MA_SANG_MA_SANG')
const hanFormation = ref<Formation>('MA_SANG_MA_SANG')
const game = ref<GameDto | null>(null)
const selected = ref<SquareDto | null>(null)
const error = ref('')

// 온라인 대전 상태
let live: Live | null = null
let stopMatchSub: (() => void) | null = null
let stopGameSub: (() => void) | null = null
const searching = ref(false)
const confirmResign = ref(false)

onBeforeUnmount(() => live?.close())

/** 온라인 대국에서 내가 두는 진영. */
const mySide = computed<Side | null>(() => {
  const g = game.value
  if (!g?.online || !me.value) return null
  return g.cho?.id === me.value.id ? 'CHO' : g.han?.id === me.value.id ? 'HAN' : null
})

const canAct = computed(() => {
  const g = game.value
  if (!g || g.result !== 'ONGOING' || thinking.value || busy.value) return false
  return g.online ? g.sideToMove === mySide.value : g.sideToMove !== g.aiSide
})

const canPass = computed(() => game.value?.legalMoves.some((m) => m.from === null) ?? false)

const flipped = computed(() => (game.value?.online ? mySide.value === 'HAN' : game.value?.aiSide === 'CHO'))

const status = computed(() => {
  const g = game.value
  if (!g) return ''
  if (thinking.value) return 'AI가 생각하는 중…'
  const turn = g.sideToMove === 'CHO' ? '초' : '한'
  if (g.result !== 'ONGOING') {
    const how = g.endReason ? END_REASON[g.endReason] : ''
    if (g.result === 'DRAW') return `${how}: 무승부입니다.`
    const winner: Side = g.result === 'CHO_WIN' ? 'CHO' : 'HAN'
    if (mySide.value) return `${how}: ${winner === mySide.value ? '이겼습니다!' : '졌습니다.'}`
    return `${how}: ${winner === 'CHO' ? '초' : '한'}가 이겼습니다.`
  }
  const whose = mySide.value ? (g.sideToMove === mySide.value ? '내 차례' : '상대 차례') : `${turn} 차례`
  return g.check ? `장군! ${whose}` : whose
})

async function start() {
  if (opponent.value === 'ONLINE') return findMatch()
  leaveOnline()
  error.value = ''
  selected.value = null
  try {
    const aiSide = opponent.value === 'HUMAN' ? null : opponent.value
    thinking.value = aiSide === 'CHO'
    game.value = await api.newGame(choFormation.value, hanFormation.value, aiSide, level.value)
    replayId.value = null
    recent.value?.refresh()
  } catch (e) {
    error.value = `서버에 연결할 수 없습니다: ${(e as Error).message}`
  } finally {
    thinking.value = false
  }
}

async function findMatch() {
  leaveOnline()
  error.value = ''
  selected.value = null
  game.value = null
  replayId.value = null
  live ??= new Live()
  const connected = await Promise.race([
    live.connected().then(() => true),
    new Promise<boolean>((r) => setTimeout(() => r(false), 5000)),
  ])
  if (!connected) {
    error.value = '실시간 서버에 연결할 수 없습니다.'
    return
  }
  searching.value = true
  stopMatchSub = live.subscribe<MatchStatus>('/user/queue/match', (m) => {
    if (m.gameId) void openOnline(m.gameId)
  })
  try {
    const r = await api.match(choFormation.value)
    if (r.status === 'MATCHED' && r.gameId) await openOnline(r.gameId)
  } catch (e) {
    searching.value = false
    error.value = (e as Error).message
  }
}

async function openOnline(id: string) {
  searching.value = false
  stopMatchSub?.()
  stopMatchSub = null
  stopGameSub?.()
  stopGameSub = live!.subscribe<GameDto>(`/topic/games/${id}`, (g) => {
    game.value = g
    if (g.result !== 'ONGOING') void gameEnded()
  })
  game.value = await api.game(id)
  recent.value?.refresh()
}

async function cancelMatch() {
  searching.value = false
  stopMatchSub?.()
  stopMatchSub = null
  await api.cancelMatch().catch(() => undefined)
}

function leaveOnline() {
  if (searching.value) void cancelMatch()
  stopGameSub?.()
  stopGameSub = null
  confirmResign.value = false
}

async function resign() {
  confirmResign.value = false
  if (!game.value) return
  try {
    game.value = await api.resign(game.value.id)
    void gameEnded()
  } catch (e) {
    error.value = (e as Error).message
  }
}

async function onSelect(sq: SquareDto) {
  const g = game.value
  if (!g || !canAct.value) return
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
  busy.value = true
  try {
    game.value = await api.move(game.value.id, move)
    error.value = ''
    if (game.value.result !== 'ONGOING') void gameEnded()
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    thinking.value = false
    busy.value = false
  }
}

/** 끝난 대국을 목록에 올리고, 온라인 대국이면 바뀐 레이팅을 머리글에 반영한다. */
async function gameEnded() {
  recent.value?.refresh()
  if (game.value?.online && auth.token()) me.value = await api.me().catch(() => me.value)
}
</script>

<template>
  <main>
    <header>
      <h1>장기</h1>
      <div v-if="me" class="me">
        {{ me.nickname }} · {{ me.rating }}
        <button @click="logout">로그아웃</button>
      </div>
      <a v-else-if="providers.includes('google')" class="login" :href="auth.loginUrl('google')">구글로 로그인</a>
    </header>
    <section class="controls">
      <label>
        상대
        <select v-model="opponent">
          <option value="HAN">AI (내가 초)</option>
          <option value="CHO">AI (내가 한)</option>
          <option value="HUMAN">같은 화면 2인</option>
          <option v-if="me" value="ONLINE">온라인 대전</option>
        </select>
      </label>
      <label v-if="opponent === 'CHO' || opponent === 'HAN'">
        난이도
        <select v-model="level">
          <option v-for="l in LEVELS" :key="l.value" :value="l.value">{{ l.label }}</option>
        </select>
      </label>
      <label v-if="opponent === 'ONLINE'">
        내 상차림
        <select v-model="choFormation">
          <option v-for="f in FORMATIONS" :key="f.value" :value="f.value">{{ f.label }}</option>
        </select>
      </label>
      <label v-if="opponent !== 'ONLINE'">
        초 상차림
        <select v-model="choFormation">
          <option v-for="f in FORMATIONS" :key="f.value" :value="f.value">{{ f.label }}</option>
        </select>
      </label>
      <label v-if="opponent !== 'ONLINE'">
        한 상차림
        <select v-model="hanFormation">
          <option v-for="f in FORMATIONS" :key="f.value" :value="f.value">{{ f.label }}</option>
        </select>
      </label>
      <button v-if="!searching" @click="start">{{ opponent === 'ONLINE' ? '상대 찾기' : '새 대국' }}</button>
    </section>
    <p v-if="!me && providers.length" class="hint">로그인하면 다른 사람과 온라인 대전을 할 수 있습니다.</p>

    <p v-if="searching" class="searching">
      상대를 찾는 중…
      <button @click="cancelMatch">취소</button>
    </p>

    <p v-if="error" class="error">{{ error }}</p>

    <ReplayView v-if="replayId" :key="replayId" :game-id="replayId" @close="replayId = null" />

    <template v-else-if="game">
      <PlayerClocks
        v-if="game.online && game.clock"
        :clock="game.clock"
        :cho="game.cho"
        :han="game.han"
        :bottom="mySide ?? 'CHO'"
        :rating-change="game.ratingChange"
      />
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
      <div class="actions">
        <button :disabled="!canPass || !canAct" @click="pass">한수쉼</button>
        <template v-if="game.online && mySide && game.result === 'ONGOING'">
          <button v-if="!confirmResign" @click="confirmResign = true">기권</button>
          <span v-else class="confirm">
            정말 기권할까요?
            <button class="danger" @click="resign">기권</button>
            <button @click="confirmResign = false">계속 두기</button>
          </span>
        </template>
      </div>
    </template>

    <RecentGames ref="recent" :mine="me !== null" @replay="(id) => (replayId = id)" />
  </main>
</template>

<style scoped>
header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.me,
.login {
  font-size: 0.9rem;
}
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
.hint,
.searching {
  font-size: 0.9rem;
}
.searching {
  font-weight: 600;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.confirm {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
}
.danger {
  color: var(--han-color);
}
button {
  padding: 6px 14px;
  margin-top: 8px;
}
</style>
