<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import ChatPanel from './components/ChatPanel.vue'
import JanggiBoard from './components/JanggiBoard.vue'
import AdminView from './components/AdminView.vue'
import LobbyView from './components/LobbyView.vue'
import NicknameDialog from './components/NicknameDialog.vue'
import NoticePopup from './components/NoticePopup.vue'
import ProfileDialog from './components/ProfileDialog.vue'
import { profileId } from './profile'
import PlayerClocks from './components/PlayerClocks.vue'
import ResultPopup from './components/ResultPopup.vue'
import RankingBoard from './components/RankingBoard.vue'
import RecentGames from './components/RecentGames.vue'
import ReplayView from './components/ReplayView.vue'
import ShopView from './components/ShopView.vue'
import CharacterAvatar from './components/CharacterAvatar.vue'
import MegaphoneBar from './components/MegaphoneBar.vue'
import MyCharacterCard from './components/MyCharacterCard.vue'
import MegaphoneDialog from './components/MegaphoneDialog.vue'
import HostTournamentDialog from './components/HostTournamentDialog.vue'
import InviteDialog from './components/InviteDialog.vue'
import TournamentList from './components/TournamentList.vue'
import TournamentView from './components/TournamentView.vue'
import {
  api,
  auth,
  type EndReason,
  type Formation,
  type GameDto,
  type Invite,
  type InviteEvent,
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
  BIKJANG: '빅장 점수 판정',
  RESIGN: '기권',
  TIMEOUT: '시간패',
  DISCONNECT: '접속 끊김',
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
const megaphoneOpen = ref(false)
/** 처음 로그인해 임시 닉네임이면 저절로 열리고, 머리글의 닉네임을 눌러도 열린다. */
const nicknameOpen = ref(false)

onMounted(async () => {
  auth.takeTokenFromUrl()
  // 앱에서는 브라우저로 로그인한 뒤 앱 주소(com.janggi.app://auth#token=...)로 돌아온다.
  void onLoginReturn((url) => auth.takeToken(url) && void loadMe())
  providers.value = (await api.providers().catch(() => ({ providers: [] }))).providers
  await loadMe()
  void openInviteLink()
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
  if (me.value) {
    watchTournaments()
    watchInvites()
    void rejoinOngoing()
  }
}

/** 창을 닫았거나 새로 고친 뒤 다시 들어오면, 두다 만 온라인 대국으로 바로 돌아간다. */
async function rejoinOngoing() {
  const r = await api.ongoing().catch(() => null)
  if (!r?.gameId || game.value?.id === r.gameId || playingOnline.value) return
  live ??= new Live()
  replayId.value = null
  shopOpen.value = false
  adminOpen.value = false
  await openOnline(r.gameId)
}

// 특정 상대 초대: 내가 보내고 기다리는 초대, 내게 온 초대
const inviting = ref<Invite | null>(null)
const incoming = ref<Invite | null>(null)
const inviteBusy = ref(false)
const inviteError = ref('')
let stopInviteSub: (() => void) | null = null
let invitingTimer: ReturnType<typeof setTimeout> | undefined
const INVITE_KEY = 'janggi.invite'

/** 초대 알림은 어느 화면에 있든 받는다. 내가 두는 온라인 대국 중이면 저절로 거절한다. */
function watchInvites() {
  if (stopInviteSub) return
  live ??= new Live()
  stopInviteSub = live.subscribe<InviteEvent>('/user/queue/invite', (e) => {
    if (e.type === 'INVITED') {
      if (playingOnline.value || incoming.value) {
        void api.declineInvite(e.invite.id).catch(() => undefined)
        return
      }
      inviteError.value = ''
      incoming.value = e.invite
    } else if (e.type === 'CANCELLED') {
      if (incoming.value?.id === e.invite.id) incoming.value = null
    } else if (inviting.value?.id === e.invite.id) {
      stopInviting()
      if (e.type === 'ACCEPTED' && e.gameId) {
        enterInvitedGame(e.gameId)
      } else {
        error.value = `${e.invite.toNickname ?? '상대'} 님이 초대를 거절했어요.`
      }
    }
  })
}

/** 주소의 ?invite=를 열거나, 로그인하러 다녀오기 전에 열어 둔 초대를 다시 띄운다. */
async function openInviteLink() {
  const params = new URLSearchParams(location.search)
  let id = params.get('invite')
  if (id) {
    params.delete('invite')
    const rest = params.toString()
    history.replaceState(null, '', location.pathname + (rest ? `?${rest}` : '') + location.hash)
  } else {
    try {
      id = sessionStorage.getItem(INVITE_KEY)
    } catch {
      id = null
    }
  }
  try {
    sessionStorage.removeItem(INVITE_KEY)
  } catch {
    // 저장소를 못 쓰면 로그인 뒤 링크를 다시 열면 된다.
  }
  if (!id) return
  try {
    const invite = await api.getInvite(id)
    inviteError.value = ''
    incoming.value = invite
    // 로그인하러 나갔다 와도 이 초대로 돌아오게 둔다.
    if (!me.value) sessionStorage.setItem(INVITE_KEY, id)
  } catch (e) {
    error.value = (e as Error).message
  }
}

async function sendInvite(nickname: string | null, formation: Formation) {
  leaveOnline()
  error.value = ''
  live ??= new Live()
  try {
    const invite = await api.invite(nickname, formation)
    inviting.value = invite
    clearTimeout(invitingTimer)
    // 닉네임 초대는 시간이 지나면 저절로 거둔다. 링크 초대는 취소할 때까지 둔다.
    if (invite.toId !== null) {
      invitingTimer = setTimeout(() => {
        if (inviting.value?.id !== invite.id) return
        void cancelInvite()
        error.value = `${invite.toNickname} 님이 답하지 않았어요.`
      }, invite.expiresAt - Date.now())
    }
  } catch (e) {
    error.value = (e as Error).message
  }
}

function stopInviting() {
  inviting.value = null
  clearTimeout(invitingTimer)
}

async function cancelInvite() {
  const invite = inviting.value
  stopInviting()
  if (invite) await api.cancelInvite(invite.id).catch(() => undefined)
}

async function acceptInvite(formation: Formation) {
  const invite = incoming.value
  if (!invite) return
  inviteBusy.value = true
  inviteError.value = ''
  try {
    const { gameId } = await api.acceptInvite(invite.id, formation)
    incoming.value = null
    enterInvitedGame(gameId)
  } catch (e) {
    inviteError.value = (e as Error).message
  } finally {
    inviteBusy.value = false
  }
}

function declineInvite() {
  const invite = incoming.value
  incoming.value = null
  try {
    sessionStorage.removeItem(INVITE_KEY)
  } catch {
    // 무시
  }
  if (invite && me.value && invite.toId === me.value.id) void api.declineInvite(invite.id).catch(() => undefined)
}

/** 초대로 잡힌 대국으로 간다. 다른 화면에 있었어도 대국 화면으로 바꾼다. */
function enterInvitedGame(gameId: string) {
  leaveOnline()
  error.value = ''
  selected.value = null
  replayId.value = null
  shopOpen.value = false
  adminOpen.value = false
  tournamentId.value = null
  live ??= new Live()
  void openOnline(gameId)
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
  void cancelInvite()
  incoming.value = null
  stopTournamentSub = null
  stopInviteSub = null
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
  // 장군은 글자 없이 화면 흔들림으로만 알린다. 빅장은 몇 번째인지 보여 준다.
  return g.bikjang ? `빅장 ${g.bikjangCount}/${g.bikjangLimit} · ${whose}` : whose
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
/** 채팅 말풍선 옆에 보일 온라인 상대의 캐릭터 차림. */
const chatOpponentLook = computed(() => {
  const g = game.value
  if (!g?.online) return null
  return (mySide.value === 'CHO' ? g.han : g.cho)?.look ?? null
})
const chatOpponentId = computed(() => {
  const g = game.value
  if (!g?.online) return null
  return (mySide.value === 'CHO' ? g.han : g.cho)?.id ?? null
})
const ongoing = computed(() => game.value?.result === 'ONGOING')

/** 온라인 대국을 마지막으로 받은 때. 나간 사람의 남은 시간을 여기서부터 줄여 보여 준다. */
const gameAt = ref(Date.now())
const nowTick = ref(Date.now())
setInterval(() => (nowTick.value = Date.now()), 500)
/** 대국 화면을 떠난 사람 안내. 내가 두는 대국이면 상대만, 구경 중이면 양쪽 다 본다. */
const awayNotice = computed(() => {
  const g = game.value
  if (!g?.online || !ongoing.value) return null
  const sides: Side[] = mySide.value ? [mySide.value === 'CHO' ? 'HAN' : 'CHO'] : ['CHO', 'HAN']
  for (const side of sides) {
    const ms = side === 'CHO' ? g.choAwayMs : g.hanAwayMs
    if (ms == null) continue
    const left = Math.max(0, Math.ceil((ms - (nowTick.value - gameAt.value)) / 1000))
    const who = (side === 'CHO' ? g.cho : g.han)?.nickname ?? (side === 'CHO' ? '초' : '한')
    return mySide.value
      ? `${who} 님이 나갔어요. ${left}초 안에 돌아오지 않으면 내가 이겨요.`
      : `${who} 님이 나갔어요. ${left}초 안에 돌아오지 않으면 상대가 이겨요.`
  }
  return null
})

async function start(options: StartOptions) {
  lastOptions.value = options
  resultOpen.value = false
  if (options.mode === 'ONLINE') return findMatch(options.choFormation, options.baseSeconds ?? 600)
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

/** 머리글의 내 얼굴을 누르면 어느 화면에서든(대국 중 빼고) 꾸미기로 간다. */
function openDress() {
  goHome()
  shopOpen.value = true
}

/** 대국을 접고 로비로 돌아간다. 대회 대국이었으면 대회 화면으로 돌아간다. 진행 중인 온라인 대국에서는 쓰지 않는다. */
function toLobby() {
  resultOpen.value = false
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

async function findMatch(formation: Formation, baseSeconds: number) {
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
    const r = await api.match(formation, baseSeconds)
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
    gameAt.value = Date.now()
    game.value = g
    if (g.result !== 'ONGOING') void gameEnded()
  })
  // 연결이 끊긴 사이에 상대가 둔 수나 끝난 결과를 놓치지 않게 다시 받아 온다.
  live!.onReconnect = () => {
    if (game.value?.id !== id) return
    void api.game(id).then((g) => {
      if (game.value?.id !== id) return
      gameAt.value = Date.now()
      game.value = g
      if (g.result !== 'ONGOING') void gameEnded()
    }).catch(() => undefined)
  }
  gameAt.value = Date.now()
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

/** 온라인 대국은 두는 회원이, AI 대국은 사람이 기권할 수 있다. 같은 화면 2인 대국에는 기권이 없다. */
const canResign = computed(() => {
  const g = game.value
  if (!g || !ongoing.value) return false
  return g.online ? !!mySide.value : !!g.aiSide
})

async function resign() {
  confirmResign.value = false
  if (!game.value || busy.value || thinking.value) return
  try {
    game.value = await api.resign(game.value.id)
    void gameEnded()
  } catch (e) {
    error.value = (e as Error).message
  }
}

/** 둘 수 없는 곳을 누르면 판 위에 잠깐 띄우는 안내. */
const hint = ref<{ id: number; text: string } | null>(null)
let hintId = 0
function showHint(text: string) {
  const id = ++hintId
  hint.value = { id, text }
  setTimeout(() => hint.value?.id === id && (hint.value = null), 1800)
}

/** 지금 판을 눌러도 둘 수 없는 이유. 둘 수 있으면 null. */
function whyCannotAct(g: GameDto): string | null {
  if (g.result !== 'ONGOING') return '대국이 끝났어요.'
  if (thinking.value) return 'AI가 생각하는 중이에요.'
  if (busy.value) return null
  if (g.online && !mySide.value) return '구경 중에는 둘 수 없어요.'
  if (g.online ? g.sideToMove !== mySide.value : g.sideToMove === g.aiSide) return '상대 차례예요. 기다려 주세요.'
  return null
}

async function onSelect(sq: SquareDto) {
  const g = game.value
  if (!g) return
  if (!canAct.value) {
    const why = whyCannotAct(g)
    if (why) showHint(why)
    return
  }
  const piece = g.pieces.find((p) => p.x === sq.x && p.y === sq.y)
  if (piece && piece.side === g.sideToMove) {
    const movable = g.legalMoves.some((m) => m.from?.x === sq.x && m.from?.y === sq.y)
    if (!movable) {
      selected.value = null
      showHint(g.check ? '장군이에요! 이 알로는 장군을 막을 수 없어요.' : '이 알은 지금 움직일 수 없어요. 갈 수 있는 자리가 없거나, 움직이면 내 장이 잡혀요.')
      return
    }
    selected.value = sq
    return
  }
  const from = selected.value
  if (!from) {
    showHint(piece ? '상대 알이에요. 내 알을 먼저 골라 주세요.' : '움직일 내 알을 먼저 골라 주세요.')
    return
  }
  const legal = g.legalMoves.some(
    (m) => m.from?.x === from.x && m.from?.y === from.y && m.to?.x === sq.x && m.to?.y === sq.y,
  )
  if (!legal) {
    showHint(g.check ? '장군이에요! 그 자리로 가면 장군을 피할 수 없어요.' : '그 자리로는 갈 수 없어요. 표시된 점으로만 옮길 수 있어요.')
    return
  }
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

/** 승·패·무승부 팝업. 한 대국에 한 번만, 장군 흔들림이 끝난 뒤에 띄운다. */
const resultOpen = ref(false)
let resultShownFor: string | null = null
const againLabel = computed(() => {
  if (tournamentId.value) return '대회로 돌아가기'
  if (!lastOptions.value) return null
  return game.value?.online ? '새 상대 찾기' : '한 판 더'
})

function again() {
  resultOpen.value = false
  if (tournamentId.value) toLobby()
  else if (lastOptions.value) void start(lastOptions.value)
}

function showResult() {
  const id = game.value?.id
  if (!id || resultShownFor === id) return
  resultShownFor = id
  setTimeout(() => {
    if (game.value?.id === id && game.value.result !== 'ONGOING') resultOpen.value = true
  }, 1100)
}

/** 끝난 대국을 목록에 올리고, 온라인 대국이면 바뀐 레이팅과 급수를 머리글에 반영한다. */
async function gameEnded() {
  showResult()
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
        <button type="button" class="shop-link icon-link" aria-label="확성기" title="확성기" @click="megaphoneOpen = true">📢</button>
        <button v-if="me?.admin" type="button" class="shop-link admin-link" @click="adminOpen = true">관리자</button>
      </template>
      <div v-if="me" class="me">
        <button type="button" class="face-button" title="캐릭터 꾸미기" :disabled="!!game" @click="openDress">
          <CharacterAvatar :look="me.look" head class="me-face" />
        </button>
        <button type="button" class="nickname" title="닉네임 바꾸기" @click="nicknameOpen = true">{{ me.nickname }}</button>
        <span class="rating" :title="`레이팅 ${me.rating}`">{{ me.grade }} <small>{{ me.rating }}</small></span>
        <button type="button" class="text-button" @click="logout">로그아웃</button>
      </div>
      <a v-else-if="providers.includes('google')" class="login" :href="auth.loginUrl('google')" @click="auth.openLogin($event, 'google')">구글로 로그인</a>
    </header>

    <NoticePopup :paused="!!game || nicknameOpen" />
    <ProfileDialog v-if="profileId" :id="profileId" :key="profileId" @close="profileId = null" />

    <MegaphoneBar @open="megaphoneOpen = true" />
    <MegaphoneDialog v-if="megaphoneOpen" :me="me" @close="megaphoneOpen = false" />

    <InviteDialog
      v-if="incoming"
      :invite="incoming"
      :me="me"
      :providers="providers"
      :busy="inviteBusy"
      :error="inviteError"
      @accept="acceptInvite"
      @decline="declineInvite"
    />

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

    <ResultPopup
      v-if="game && resultOpen && !replayId"
      :game="game"
      :me="playerSide"
      :again-label="againLabel"
      @again="again"
      @close="resultOpen = false"
    />

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
          <Transition name="hint">
            <div v-if="hint" :key="hint.id" class="move-hint" role="alert">{{ hint.text }}</div>
          </Transition>
          <div v-if="awayNotice" class="away-notice" role="status">{{ awayNotice }}</div>
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
            <template v-if="canResign">
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
          :opponent-look="chatOpponentLook"
          :opponent-id="chatOpponentId"
          :ai-badge="persona?.badge ?? null"
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
      <MyCharacterCard :me="me" @dress="shopOpen = true" @megaphone="megaphoneOpen = true" />
      <LobbyView
        :me="me"
        :providers="providers"
        :searching="searching"
        :error="error"
        :inviting="inviting"
        @start="start"
        @cancel="cancelMatch"
        @invite="sendInvite"
        @cancel-invite="cancelInvite"
      />
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
.away-notice {
  margin: 0 0 8px;
  padding: 8px 12px;
  border-radius: 10px;
  background: #fff4d6;
  border: 1px solid #e8b94a;
  color: #6b4a00;
  font-weight: 600;
  text-align: center;
}
@media (prefers-color-scheme: dark) {
  .away-notice {
    background: #3a2e10;
    border-color: #8a6a1e;
    color: #ffe2a0;
  }
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
.board-col {
  position: relative;
}
.move-hint {
  position: absolute;
  top: 45%;
  left: 50%;
  z-index: 20;
  width: max-content;
  max-width: min(320px, calc(100% - 32px));
  padding: 12px 18px;
  border-radius: 14px;
  color: #fff;
  background: rgba(30, 20, 10, 0.88);
  box-shadow: 0 10px 30px var(--shadow);
  font-weight: 600;
  text-align: center;
  line-height: 1.45;
  transform: translate(-50%, -50%);
  pointer-events: none;
}
.hint-enter-active,
.hint-leave-active {
  transition:
    opacity 0.2s,
    transform 0.2s;
}
.hint-enter-from,
.hint-leave-to {
  opacity: 0;
  transform: translate(-50%, -40%);
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
/* 상점·확성기·관리자는 한데 모아 오른쪽 내 정보(또는 로그인) 앞에 둔다. */
.shop-link + .shop-link {
  margin-left: -6px;
}
.icon-link {
  padding-left: 10px;
  padding-right: 10px;
}
.face-button {
  padding: 0;
  border: none;
  background: none;
  line-height: 0;
  cursor: pointer;
}
.face-button:disabled {
  cursor: default;
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
