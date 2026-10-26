<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import ChatPanel from './components/ChatPanel.vue'
import JanggiBoard from './components/JanggiBoard.vue'
import AdminView from './components/AdminView.vue'
import LobbyView from './components/LobbyView.vue'
import NicknameDialog from './components/NicknameDialog.vue'
import NoticePopup from './components/NoticePopup.vue'
import PlayerClocks from './components/PlayerClocks.vue'
import RankingBoard from './components/RankingBoard.vue'
import RecentGames from './components/RecentGames.vue'
import ReplayView from './components/ReplayView.vue'
import ShopView from './components/ShopView.vue'
import CharacterAvatar from './components/CharacterAvatar.vue'
import HostTournamentDialog from './components/HostTournamentDialog.vue'
import TournamentList from './components/TournamentList.vue'
import TournamentView from './components/TournamentView.vue'
import {
  api,
  auth,
  type EndReason,
  type Formation,
  type GameDto,
  type MatchStatus,
  type Me,
  type Side,
  type SquareDto,
  type TournamentPaired,
} from './api'
import { Live } from './live'
import { PERSONAS, type StartOptions } from './options'
import { onLoginReturn } from './native'
import { setEquipped } from './themes'

const END_REASON: Record<EndReason, string> = {
  CHECKMATE: '외통',
  BIKJANG: '빅장, 점수 판정',
  RESIGN: '기권',
  TIMEOUT: '시간패',
}

/** 로비에서 마지막으로 고른 설정. 대국이 끝나면 '한 판 더'에 쓴다. */
const lastOptions = ref<StartOptions | null>(null)
const thinking = ref(false)
/** 수를 보내고 응답을 기다리는 중. 이때 누른 칸은 무시해서 옛 국면 기준으로 알이 골라지지 않게 한다. */
const busy = ref(false)
const replayId = ref<string | null>(null)
const recent = ref<InstanceType<typeof RecentGames> | null>(null)
const chat = ref<InstanceType<typeof ChatPanel> | null>(null)
const me = ref<Me | null>(null)
const providers = ref<string[]>([])
const shopOpen = ref(false)
const adminOpen = ref(false)
/** 처음 로그인해 임시 닉네임이면 저절로 열리고, 머리글의 닉네임을 눌러도 열린다. */
const nicknameOpen = ref(false)

onMounted(async () => {
  auth.takeTokenFromUrl()
  // 앱에서는 브라우저로 로그인한 뒤 앱 주소(com.janggi.app://auth#token=...)로 돌아온다.
  void onLoginReturn((url) => auth.takeToken(url) && void loadMe())
  providers.value = (await api.providers().catch(() => ({ providers: [] }))).providers
  await loadMe()
})

async function loadMe() {
  if (!auth.token()) return
  me.value = await api.me().catch(() => {
    auth.clear()
    return null
  })
  setEquipped(me.value?.boardTheme, me.value?.pieceTheme)
  nicknameOpen.value = me.value?.nicknameChosen === false
  recent.value?.refresh()
  if (me.value) watchTournaments()
}

// 대회 상태: 보고 있는 대회, 개인전 만들기 창, 대회 목록
const tournamentId = ref<number | null>(null)
const hostOpen = ref(false)
const tournaments = ref<InstanceType<typeof TournamentList> | null>(null)
let stopTournamentSub: (() => void) | null = null

/** 대회에서 다음 상대가 정해지면 서버가 알려 준다. 어느 화면에 있든 바로 그 대국으로 간다. */
function watchTournaments() {
  if (stopTournamentSub) return
  live ??= new Live()
  stopTournamentSub = live.subscribe<TournamentPaired>('/user/queue/tournament', (p) => {
    if (game.value?.online && game.value.result === 'ONGOING' && mySide.value) return
    replayId.value = null
    shopOpen.value = false
    adminOpen.value = false
    tournamentId.value = p.tournamentId
    void openOnline(p.gameId)
  })
}

function openTournament(id: number) {
  hostOpen.value = false
  tournamentId.value = id
}

/** 대회 화면에서 대국을 고르면 두거나 구경한다. */
function tournamentGame(id: string) {
  live ??= new Live()
  error.value = ''
  selected.value = null
  void openOnline(id)
}

function logout() {
  leaveOnline()
  stopTournamentSub = null
  live?.close()
  live = null
  auth.clear()
  me.value = null
  adminOpen.value = false
  setEquipped(undefined, undefined)
  if (game.value?.online) game.value = null
  recent.value?.refresh()
}
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

/** 내가 두는 진영. 온라인은 매칭된 쪽, AI 대국은 AI의 반대쪽, 같은 화면 2인은 없다. */
const playerSide = computed<Side | null>(() => {
  const g = game.value
  if (!g) return null
  if (g.online) return mySide.value
  return g.aiSide ? (g.aiSide === 'CHO' ? 'HAN' : 'CHO') : null
})

const status = computed(() => {
  const g = game.value
  if (!g) return ''
  if (thinking.value) return 'AI가 생각하는 중…'
  const turn = g.sideToMove === 'CHO' ? '초' : '한'
  const me = playerSide.value
  if (g.result !== 'ONGOING') {
    const how = g.endReason ? END_REASON[g.endReason] : ''
    if (g.result === 'DRAW') return `${how}: 무승부입니다.`
    const winner: Side = g.result === 'CHO_WIN' ? 'CHO' : 'HAN'
    if (me) return `${how}: ${winner === me ? '이겼습니다!' : '졌습니다.'}`
    return `${how}: ${winner === 'CHO' ? '초' : '한'}가 이겼습니다.`
  }
  const whose = me ? (g.sideToMove === me ? '내 차례' : g.online ? '상대 차례' : 'AI 차례') : `${turn} 차례`
  return g.check ? `장군! ${whose}` : whose
})

/** 채팅 상대. 같은 화면 2인 대국에는 채팅이 없다. */
const chatKind = computed<'ai' | 'online' | null>(() =>
  game.value?.online ? (mySide.value ? 'online' : null) : game.value?.aiSide ? 'ai' : null,
)
/** 내가 두고 있는 온라인 대국. 구경할 때는 언제든 나갈 수 있다. */
const playingOnline = computed(() => !!game.value?.online && ongoing.value && mySide.value !== null)
const persona = computed(() => (game.value?.aiLevel ? PERSONAS[game.value.aiLevel] : null))
const chatTitle = computed(() => {
  const g = game.value
  if (!g) return ''
  if (!g.online) return persona.value?.name ?? 'AI'
  const other = mySide.value === 'CHO' ? g.han : g.cho
  return other?.nickname ?? '상대'
})
const ongoing = computed(() => game.value?.result === 'ONGOING')

async function start(options: StartOptions) {
  lastOptions.value = options
  if (options.mode === 'ONLINE') return findMatch(options.choFormation)
  leaveOnline()
  error.value = ''
  selected.value = null
  try {
    const aiSide: Side | null = options.mode === 'HUMAN' ? null : options.mySide === 'CHO' ? 'HAN' : 'CHO'
    thinking.value = aiSide === 'CHO'
    game.value = await api.newGame(options.choFormation, options.hanFormation, aiSide, options.level)
    replayId.value = null
    shopOpen.value = false
    adminOpen.value = false
    tournamentId.value = null
    recent.value?.refresh()
  } catch (e) {
    error.value = `서버에 연결할 수 없습니다: ${(e as Error).message}`
  } finally {
    thinking.value = false
  }
}

/** 머리글의 '장기'를 누르면 어느 화면에서든 로비로 간다. */
function goHome() {
  if (game.value) toLobby()
  replayId.value = null
  shopOpen.value = false
  adminOpen.value = false
  tournamentId.value = null
}

/** 대국을 접고 로비로 돌아간다. 대회 대국이었으면 대회 화면으로 돌아간다. 진행 중인 온라인 대국에서는 쓰지 않는다. */
function toLobby() {
  leaveOnline()
  game.value = null
  selected.value = null
  error.value = ''
  recent.value?.refresh()
}

/** 최근 대국 목록에서 끝나지 않은 AI·2인 대국을 이어서 둔다. */
async function resume(id: string) {
  leaveOnline()
  error.value = ''
  selected.value = null
  try {
    game.value = await api.game(id)
  } catch (e) {
    error.value = (e as Error).message
  }
}

async function findMatch(formation: Formation) {
  leaveOnline()
  error.value = ''
  selected.value = null
  game.value = null
  replayId.value = null
  tournamentId.value = null
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
    const r = await api.match(formation)
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

/** 서버 응답 전에 보여 줄 국면: 알 하나를 옮기고 잡힌 알을 뺀다. 둘 수 있는 수는 비운다. */
function withMove(g: GameDto, from: SquareDto, to: SquareDto): GameDto {
  const pieces = g.pieces
    .filter((p) => !(p.x === to.x && p.y === to.y))
    .map((p) => (p.x === from.x && p.y === from.y ? { ...p, x: to.x, y: to.y } : p))
  // 장군 여부는 서버 응답이 와야 알 수 있으므로 잠시 끈다.
  return { ...g, pieces, legalMoves: [], lastMove: { from, to }, check: false, checkmate: false }
}

async function play(move: { from: SquareDto | null; to: SquareDto | null }) {
  if (!game.value) return
  const before = game.value
  selected.value = null
  thinking.value = game.value.aiSide !== null
  busy.value = true
  try {
    const sent = api.move(game.value.id, move)
    // AI 대국은 응답에 AI의 수까지 실려 오므로, 내 수를 먼저 판에 올려 움직임이 보이게 한다.
    if (game.value.aiSide !== null && move.from && move.to) {
      game.value = withMove(game.value, move.from, move.to)
      await new Promise((r) => setTimeout(r, 350))
    }
    game.value = await sent
    if (game.value.aiComment) chat.value?.push(game.value.aiComment)
    error.value = ''
    if (game.value.result !== 'ONGOING') void gameEnded()
  } catch (e) {
    game.value = before
    error.value = (e as Error).message
  } finally {
    thinking.value = false
    busy.value = false
  }
}

/** 온라인 대국 뒤 급수가 바뀌면 잠깐 띄우는 알림. */
const gradeNotice = ref<{ up: boolean; grade: string } | null>(null)
let gradeTimer: ReturnType<typeof setTimeout> | undefined

/** 끝난 대국을 목록에 올리고, 온라인 대국이면 바뀐 레이팅과 급수를 머리글에 반영한다. */
async function gameEnded() {
  recent.value?.refresh()
  if (!game.value?.online || !auth.token()) return
  const before = me.value
  me.value = await api.me().catch(() => me.value)
  if (before && me.value && me.value.grade !== before.grade) {
    gradeNotice.value = { up: me.value.rating > before.rating, grade: me.value.grade }
    clearTimeout(gradeTimer)
    gradeTimer = setTimeout(() => (gradeNotice.value = null), 5000)
  }
}
</script>

<template>
  <main :class="{ wide: (game && chatKind) || ((shopOpen || adminOpen || tournamentId) && !game && !replayId) }">
    <header class="top">
      <button v-if="game || replayId || shopOpen || adminOpen || tournamentId" type="button" class="brand link" :disabled="playingOnline" @click="goHome">
        장기
      </button>
      <span v-else class="brand">장기</span>
      <template v-if="!game && !replayId && !shopOpen && !adminOpen && !tournamentId">
        <button type="button" class="shop-link" @click="shopOpen = true">상점</button>
        <button v-if="me?.admin" type="button" class="shop-link admin-link" @click="adminOpen = true">관리자</button>
      </template>
      <div v-if="me" class="me">
        <CharacterAvatar :look="me.look" head class="me-face" />
        <button type="button" class="nickname" title="닉네임 바꾸기" @click="nicknameOpen = true">{{ me.nickname }}</button>
        <span class="rating" :title="`레이팅 ${me.rating}`">{{ me.grade }} <small>{{ me.rating }}</small></span>
        <button type="button" class="text-button" @click="logout">로그아웃</button>
      </div>
      <a v-else-if="providers.includes('google')" class="login" :href="auth.loginUrl('google')" @click="auth.openLogin($event, 'google')">구글로 로그인</a>
    </header>

    <NoticePopup :paused="!!game || nicknameOpen" />

    <HostTournamentDialog v-if="me && hostOpen" :me="me" @created="openTournament" @close="hostOpen = false" />

    <NicknameDialog
      v-if="me && nicknameOpen"
      :me="me"
      @saved="(m) => ((me = m), (nicknameOpen = false))"
      @close="nicknameOpen = false"
    />

    <Transition name="grade">
      <div v-if="gradeNotice" class="grade-notice" :class="{ down: !gradeNotice.up }" role="status">
        <span class="grade-mark" aria-hidden="true">{{ gradeNotice.up ? '昇' : '降' }}</span>
        <span>
          <b>{{ gradeNotice.up ? '승급!' : '강등' }}</b>
          {{ gradeNotice.grade }}{{ gradeNotice.up ? '이 되었어요' : '(으)로 내려갔어요' }}
        </span>
      </div>
    </Transition>

    <p v-if="me?.banned" class="banned" role="alert">
      이용이 정지된 계정이에요. 사유: {{ me.banReason }}<br />
      잘못된 정지라고 생각되면 운영자에게 문의해 주세요.
    </p>

    <ReplayView v-if="replayId" :key="replayId" :game-id="replayId" @close="replayId = null" />

    <section v-else-if="game" class="game-screen">
      <div class="topbar">
        <button v-if="!playingOnline" type="button" class="back" @click="toLobby">{{ tournamentId ? '‹ 대회' : '‹ 로비' }}</button>
        <span v-if="game.online && !mySide" class="watching">구경 중</span>
        <div v-if="!game.online" class="opponent">
          <template v-if="persona">
            <span class="badge han" aria-hidden="true">{{ persona.badge }}</span>
            <span class="who">
              <b>{{ persona.name }}</b>
              <span class="level">AI {{ persona.level }}</span>
            </span>
          </template>
          <span v-else class="who"><b>같은 화면 2인</b></span>
        </div>
        <span class="pill" :class="{ check: game.check && ongoing, over: !ongoing, thinking }">{{ status }}</span>
      </div>

      <div class="layout">
        <div class="board-col">
          <PlayerClocks
            v-if="game.online && game.clock"
            :clock="game.clock"
            :cho="game.cho"
            :han="game.han"
            :bottom="mySide ?? 'CHO'"
            :rating-change="game.ratingChange"
          />
          <JanggiBoard
            :pieces="game.pieces"
            :legal-moves="game.legalMoves"
            :selected="selected"
            :last-move="game.lastMove"
            :flipped="flipped"
            :check="game.check ? game.sideToMove : null"
            :mate="game.checkmate"
            @select="onSelect"
          />
          <div class="actions">
            <span class="score"><b class="cho-text">초</b> {{ game.choScore }} · <b class="han-text">한</b> {{ game.hanScore }}</span>
            <button type="button" :disabled="!canPass || !canAct" @click="pass">한수쉼</button>
            <template v-if="game.online && mySide && ongoing">
              <button v-if="!confirmResign" type="button" @click="confirmResign = true">기권</button>
              <span v-else class="confirm">
                정말 기권할까요?
                <button type="button" class="danger" @click="resign">기권</button>
                <button type="button" @click="confirmResign = false">계속 두기</button>
              </span>
            </template>
            <button v-if="!ongoing && tournamentId" type="button" class="again" @click="toLobby">대회로 돌아가기</button>
            <button v-else-if="!ongoing && lastOptions" type="button" class="again" @click="start(lastOptions)">
              {{ game.online ? '새 상대 찾기' : '한 판 더' }}
            </button>
          </div>
          <p v-if="error" class="error">{{ error }}</p>
        </div>
        <ChatPanel
          v-if="chatKind"
          ref="chat"
          :key="game.id"
          class="chat-col"
          :game-id="game.id"
          :kind="chatKind"
          :my-side="playerSide ?? 'CHO'"
          :title="chatTitle"
          :live="live"
        />
      </div>
    </section>

    <TournamentView
      v-else-if="tournamentId"
      :id="tournamentId"
      :key="tournamentId"
      :me="me"
      :providers="providers"
      @close="tournamentId = null"
      @game="tournamentGame"
    />

    <AdminView v-else-if="adminOpen && me?.admin" @close="adminOpen = false" />

    <ShopView v-else-if="shopOpen" :me="me" :providers="providers" @close="shopOpen = false" @look="(look) => me && (me = { ...me, look })" />

    <template v-else>
      <LobbyView :me="me" :providers="providers" :searching="searching" :error="error" @start="start" @cancel="cancelMatch" />
      <TournamentList ref="tournaments" :me="me" @open="openTournament" @host="hostOpen = true" />
      <RecentGames ref="recent" :mine="me !== null" @replay="(id) => (replayId = id)" @resume="resume" />
      <RankingBoard :key="me?.id ?? 0" :my-id="me?.id ?? null" />
    </template>
  </main>
</template>

<style scoped>
main {
  max-width: 720px;
  margin: 0 auto;
  padding: 12px 16px 32px;
}
main.wide {
  max-width: 940px;
}
.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding: 4px 0 16px;
}
.brand,
.shop-link,
.nickname,
.rating,
.text-button,
.login {
  white-space: nowrap;
}
.brand {
  font-family: 'Noto Serif KR', serif;
  font-size: 1.3rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--accent-deep);
}
.brand.link {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
}
.brand.link:disabled {
  cursor: default;
}
.shop-link {
  margin-left: auto;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  color: var(--accent-deep);
  font-weight: 700;
  font-size: 0.88rem;
  cursor: pointer;
}
.admin-link {
  margin-left: 0;
  color: #fff;
  background: var(--accent-deep);
  border-color: var(--accent-deep);
}
.rating small {
  font-weight: 500;
  opacity: 0.75;
}
.grade-notice {
  position: fixed;
  top: calc(16px + env(safe-area-inset-top));
  left: 50%;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 18px 10px 10px;
  border-radius: 999px;
  color: #fff;
  background: var(--cho-color);
  box-shadow: 0 10px 30px var(--shadow);
  transform: translateX(-50%);
  white-space: nowrap;
}
.grade-notice.down {
  background: var(--muted);
}
.grade-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  font-family: 'Noto Serif KR', serif;
  font-weight: 900;
  color: var(--cho-color);
  background: #fff;
}
.grade-notice.down .grade-mark {
  color: var(--muted);
}
.grade-enter-active,
.grade-leave-active {
  transition:
    opacity 0.3s,
    transform 0.3s;
}
.grade-enter-from,
.grade-leave-to {
  opacity: 0;
  transform: translate(-50%, -20px);
}
.banned {
  margin: 0 0 14px;
  padding: 12px 14px;
  border-radius: 12px;
  color: #fff;
  background: var(--han-color);
  font-weight: 600;
  line-height: 1.5;
}
.me-face {
  width: 30px;
}
.me {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
}
.nickname {
  font: inherit;
  font-weight: 600;
  padding: 2px 4px;
  border: none;
  border-bottom: 1px dashed var(--muted);
  background: none;
  cursor: pointer;
}
.rating {
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--accent-bg);
  color: var(--accent);
  font-weight: 700;
  font-size: 0.8rem;
}
.text-button {
  background: none;
  border: none;
  color: var(--muted);
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
}
.login {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--accent);
}
.topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.watching {
  font-size: 0.8rem;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--line, rgba(0, 0, 0, 0.08));
}

.back {
  padding: 7px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  cursor: pointer;
  font-weight: 600;
}
.opponent {
  display: flex;
  align-items: center;
  gap: 10px;
}
.who {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.level {
  font-size: 0.75rem;
  color: var(--muted);
}
.badge {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  font-family: 'Noto Serif KR', serif;
  font-weight: 900;
  background: var(--piece-light);
  clip-path: polygon(30% 0, 70% 0, 100% 30%, 100% 70%, 70% 100%, 30% 100%, 0 70%, 0 30%);
  box-shadow: inset 0 0 0 3px var(--piece-edge);
}
.badge.han {
  color: var(--han-color);
}
.pill {
  margin-left: auto;
  padding: 6px 12px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 0.88rem;
  background: var(--surface-strong);
  border: 1px solid var(--border);
}
.pill.check {
  color: #fff;
  background: var(--han-color);
  border-color: var(--han-color);
}
.pill.over {
  color: #fff;
  background: var(--accent);
  border-color: var(--accent);
}
.pill.thinking {
  color: var(--muted);
}
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
main.wide .layout {
  grid-template-columns: minmax(0, 1fr) 340px;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}
.actions button {
  padding: 7px 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--surface-strong);
  cursor: pointer;
}
.actions button:disabled {
  opacity: 0.5;
  cursor: default;
}
.actions .again {
  color: #fff;
  font-weight: 700;
  background: var(--accent);
  border-color: var(--accent);
}
.score {
  margin-right: auto;
  font-size: 0.9rem;
  color: var(--muted);
}
.cho-text {
  color: var(--cho-color);
}
.han-text {
  color: var(--han-color);
}
.confirm {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.actions .danger {
  color: var(--han-color);
  border-color: var(--han-color);
}
.error {
  color: var(--han-color);
}
@media (max-width: 480px) {
  .top {
    gap: 8px;
  }
  .me {
    gap: 6px;
    min-width: 0;
  }
  .nickname {
    max-width: 7.5em;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
@media (max-width: 860px) {
  main.wide {
    max-width: 720px;
  }
  main.wide .layout {
    grid-template-columns: minmax(0, 1fr);
  }
  /* 채팅 바가 화면 아래를 덮으므로 그만큼 띄운다. */
  main.wide {
    padding-bottom: 90px;
  }
}
</style>
