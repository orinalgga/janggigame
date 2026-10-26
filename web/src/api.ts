import { isNative, openInBrowser } from './native'

export type Side = 'CHO' | 'HAN'
export type Level = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
export type Formation = 'SANG_MA_SANG_MA' | 'MA_SANG_MA_SANG' | 'MA_SANG_SANG_MA' | 'SANG_MA_MA_SANG'

export interface SquareDto {
  x: number
  y: number
}

export interface MoveDto {
  from: SquareDto | null
  to: SquareDto | null
}

export interface PieceDto {
  x: number
  y: number
  side: Side
  type: string
  label: string
}

export interface GameDto {
  id: string
  pieces: PieceDto[]
  sideToMove: Side
  legalMoves: MoveDto[]
  check: boolean
  checkmate: boolean
  bikjang: boolean
  result: Result
  choScore: number
  hanScore: number
  aiSide: Side | null
  aiLevel: Level | null
  lastMove: MoveDto | null
  endReason: EndReason | null
  online: boolean
  cho: Player | null
  han: Player | null
  clock: ClockDto | null
  ratingChange: { cho: number; han: number } | null
  /** AI 대국에서 이번 수에 AI가 한 말. 없으면 null. */
  aiComment: ChatMessage | null
}

/** 대국 채팅 한 줄. id는 대국 안에서 늘어나는 번호라 같은 말을 두 번 받으면 걸러 낸다. */
export interface ChatMessage {
  id: number
  side: Side
  nickname: string
  ai: boolean
  text: string
  at: number
}

export type EndReason = 'CHECKMATE' | 'BIKJANG' | 'RESIGN' | 'TIMEOUT'

/** rating과 grade는 대국을 시작할 때의 레이팅과 급수. */
/** 캐릭터 차림. 모두 상점 상품 id이고 background는 배경 패다. */
export interface Look {
  /** 화풍: 인형·애니메이션·민화. */
  style: string
  background: string
  body: string
  hair: string
  beard: string
  outfit: string
  hat: string
}

export interface Player {
  id: number
  nickname: string
  rating: number
  grade: string
  look: Look
}

/** 서버가 보낸 시점의 남은 시간. running 쪽은 받은 뒤로도 계속 줄어든다. */
export interface ClockDto {
  choMs: number
  hanMs: number
  running: Side | null
}

export interface MatchStatus {
  status: 'WAITING' | 'MATCHED' | 'CANCELLED'
  gameId: string | null
}

export type Result = 'ONGOING' | 'CHO_WIN' | 'HAN_WIN' | 'DRAW'

export interface GameSummary {
  id: string
  aiSide: Side | null
  aiLevel: Level | null
  result: Result
  endReason: EndReason | null
  moveCount: number
  startedAt: string
  online: boolean
}

export interface ReplayDto {
  id: string
  ply: number
  totalPlies: number
  pieces: PieceDto[]
  lastMove: MoveDto | null
  check: boolean
  result: Result
  aiSide: Side | null
}

export interface Me {
  id: number
  nickname: string
  /** false면 가입할 때 받은 임시 닉네임(장기꾼+번호). */
  nicknameChosen: boolean
  rating: number
  /** 레이팅에 따른 급수: 18급~1급, 초단~9단. */
  grade: string
  /** 장착한 테마의 상점 상품 id. */
  boardTheme: string
  pieceTheme: string
  /** 캐릭터 차림. */
  look: Look
  /** 관리자 페이지를 쓸 수 있는 회원. */
  admin: boolean
  /** 정지된 회원은 자기 정보 말고는 아무것도 할 수 없다. */
  banned: boolean
  banReason: string | null
}

export interface AdminStats {
  users: number
  usersToday: number
  gamesToday: number
  onlinePlaying: number
  waiting: number
  paidOrders: number
  revenueTotal: number
  revenueToday: number
}

export interface AdminUser {
  id: number
  nickname: string
  nicknameChosen: boolean
  rating: number
  grade: string
  createdAt: string
  emails: string[]
  banned: boolean
  banReason: string | null
  /** 산 테마(무료 제외)의 상품 id. */
  items: string[]
  admin: boolean
  /** 엽전 잔액(전체). */
  coins: number
  /** 그중 운영자가 준 엽전. 운영자는 이것만 뺄 수 있다. */
  freeCoins: number
}

export interface AdminUserPage {
  users: AdminUser[]
  page: number
  totalPages: number
  total: number
}

export interface AdminLog {
  at: string
  adminId: number
  admin: string
  action:
    | 'RENAME'
    | 'BAN'
    | 'UNBAN'
    | 'GRANT'
    | 'COINS'
    | 'NOTICE_CREATE'
    | 'NOTICE_UPDATE'
    | 'NOTICE_DELETE'
    | 'TOURNAMENT_CREATE'
    | 'TOURNAMENT_UPDATE'
    | 'TOURNAMENT_CANCEL'
  targetId: number | null
  target: string | null
  detail: string | null
}

export interface AdminOrder {
  orderId: string
  userId: number
  nickname: string
  itemId: string
  itemTitle: string
  amount: number
  provider: string
  status: 'PENDING' | 'PAID' | 'FAILED'
  createdAt: string
  paidAt: string | null
}

/** 공지. 본문은 글자 그대로(줄바꿈 포함) 보여 준다. */
export interface Notice {
  id: number
  title: string
  body: string
  startsAt: string
  /** null이면 끄기 전까지 보인다. */
  endsAt: string | null
  active: boolean
  updatedAt: string
}

export interface NoticeInput {
  title: string
  body: string
  /** null이면 지금부터. */
  startsAt: string | null
  endsAt: string | null
  active: boolean
}

export type RankingPeriod = 'WEEK' | 'MONTH' | 'YEAR'

export interface RankingRow {
  /** 판 수가 모자라 순위에 오르지 못했으면 null. */
  rank: number | null
  userId: number
  nickname: string
  look: Look
  grade: string
  rating: number
  /** 기간 동안 오르내린 레이팅의 합. */
  points: number
  games: number
  wins: number
  draws: number
  losses: number
}

export interface Ranking {
  period: RankingPeriod
  offset: number
  /** 기간 첫날과 다음 기간 첫날(한국 날짜, 끝날은 빠진다). */
  from: string
  to: string
  minGames: number
  rows: RankingRow[]
  /** 로그인했고 이 기간에 둔 판이 있을 때만. */
  me: RankingRow | null
}

export type TournamentStatus = 'SCHEDULED' | 'RUNNING' | 'FINISHED' | 'CANCELLED'
/** 누구나, 유단자(초단 이상), 급. */
export type Division = 'ALL' | 'DAN' | 'KYU'
/** 정기 리그는 repeatDays마다 다음 회차가 저절로 생긴다. */
export type TournamentKind = 'REGULAR' | 'SPECIAL'

export interface TournamentPlayer {
  id: number
  nickname: string
  look: Look
  grade: string
  rating: number
  seed: number | null
}

export interface TournamentSummary {
  id: number
  title: string
  startsAt: string
  status: TournamentStatus
  division: Division
  kind: TournamentKind
  repeatDays: number
  /** 회원이 연 개인 대회면 연 사람. */
  host: TournamentPlayer | null
  /** 운영자나 연 사람이 미리 건 우승 상금. */
  bonusPrize: number
  baseSeconds: number
  incrementSeconds: number
  entryFee: number
  maxPlayers: number
  players: number
  /** 우승 상금. 참가비는 소모되고 상금에 들어가지 않는다. */
  prizePool: number
  currentRound: number
  totalRounds: number
  winner: TournamentPlayer | null
  joined: boolean
  /** 운영 대회 우승자에게 주는 보상 아바타(상점 상품 id). */
  prizeItem: string | null
}

/** 한쪽이 비면 부전승. */
export interface TournamentMatch {
  round: number
  slot: number
  cho: TournamentPlayer | null
  han: TournamentPlayer | null
  gameId: string | null
  winnerId: number | null
}

export interface TournamentEntry {
  player: TournamentPlayer
  /** 탈락한 라운드. */
  out: number | null
  /** 끝난 대회의 순위. */
  rank: number | null
}

export interface TournamentDetail {
  summary: TournamentSummary
  description: string | null
  entries: TournamentEntry[]
  matches: TournamentMatch[]
  nextRoundAt: string | null
  /** 내가 지금 둬야 하는 대회 대국. */
  myGameId: string | null
  eliminated: boolean
}

export interface TournamentInput {
  title: string
  description: string | null
  startsAt: string
  baseSeconds: number
  incrementSeconds: number
  entryFee: number
  maxPlayers: number
  bonusPrize: number
  division?: Division
  both?: boolean
  kind?: TournamentKind
  repeatDays?: number
  prizeItem?: string | null
}

/** 다음 대회 대국이 짝지어졌다는 알림(/user/queue/tournament). */
export interface TournamentPaired {
  tournamentId: number
  round: number
  gameId: string
}

export type ShopSlot = 'BOARD' | 'PIECES' | 'STYLE' | 'AVATAR' | 'BODY' | 'HAIR' | 'BEARD' | 'OUTFIT' | 'HAT'

/** 상점 상품. 로그인하지 않았으면 owned, equipped는 무료 상품과 기본 장착 기준이다. */
export interface ShopItem {
  id: string
  slot: ShopSlot
  title: string
  description: string
  /** 엽전 가격. 0이면 무료(reward가 아니면). */
  price: number
  /** 팔지 않고 운영 대회 우승으로만 얻는 보상. */
  reward: boolean
  owned: boolean
  equipped: boolean
}

/** 엽전 충전 상품. 100원에 1닢. */
export interface CoinPack {
  id: string
  title: string
  coins: number
  /** 원. */
  price: number
}

export interface ShopState {
  /** 엽전 잔액. 로그인하지 않았으면 null. */
  coins: number | null
  /** 그중 운영자가 준 엽전. 살 때 이것부터 쓴다. */
  freeCoins: number | null
  items: ShopItem[]
  bundles: ShopBundle[]
  packs: CoinPack[]
}

/** 캐릭터 세트. price는 깎은 값(로그인했으면 아직 없는 부품만 센 값), fullPrice는 깎기 전 값. */
export interface ShopBundle {
  id: string
  title: string
  description: string
  items: string[]
  fullPrice: number
  price: number
  /** 부품을 다 가진 세트. */
  owned: boolean
}

export interface CoinLedgerRow {
  id: number
  delta: number
  /** delta 중 운영자 엽전 몫. */
  freeDelta: number
  balance: number
  kind: 'CHARGE' | 'SPEND' | 'ADMIN'
  ref: string | null
  memo: string | null
  at: string
}

/** 엽전 충전 주문. amount는 원. */
export interface ShopOrder {
  orderId: string
  packId: string
  title: string
  coins: number
  amount: number
  /** 결제사 이름. test면 돈이 오가지 않는 테스트 결제. */
  provider: string
}

const BASE = import.meta.env.VITE_API_BASE ?? ''
// 소셜 로그인은 Vite 프록시를 거치지 않고 서버로 바로 가야 구글에 등록한 리디렉션 주소와 맞는다.
const AUTH_BASE = BASE || (import.meta.env.DEV ? 'http://localhost:8080' : '')
const TOKEN_KEY = 'janggi.token'

export const auth = {
  token(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  save(token: string) {
    try {
      localStorage.setItem(TOKEN_KEY, token)
    } catch {
      // 저장소를 못 쓰는 브라우저에서는 이번 방문 동안만 로그인 상태가 유지되지 않는다.
    }
  },
  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY)
    } catch {
      // 무시
    }
  },
  /** 앱이면 ?app=1을 붙여, 로그인이 끝난 뒤 서버가 웹 대신 앱으로 돌려보내게 한다. */
  loginUrl: (provider: string) => `${AUTH_BASE}/oauth2/authorization/${provider}${isNative ? '?app=1' : ''}`,
  /** 로그인 링크를 누를 때. 앱에서는 기기 브라우저로 연다. */
  openLogin(e: MouseEvent, provider: string) {
    if (!isNative) return
    e.preventDefault()
    void openInBrowser(auth.loginUrl(provider))
  },
  /** 로그인 후 돌아온 주소의 #token=을 꺼내 저장한다. 찾았으면 true. */
  takeToken(url: string): boolean {
    const m = url.match(/token=([^&]+)/)
    if (m) auth.save(decodeURIComponent(m[1]))
    return m !== null
  },
  /** 웹에서 로그인 후 돌아온 주소의 토큰을 저장하고 주소에서 지운다. */
  takeTokenFromUrl() {
    if (auth.takeToken(location.hash)) history.replaceState(null, '', location.pathname + location.search)
  },
}

async function request<T>(path: string, body?: unknown, method?: string): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = auth.token()
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(BASE + path, {
    method: method ?? (body === undefined ? 'GET' : 'POST'),
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }))
    throw new Error(err.error ?? res.statusText)
  }
  return res.json()
}

export const api = {
  newGame: (choFormation: Formation, hanFormation: Formation, aiSide: Side | null, aiLevel: Level) =>
    request<GameDto>('/api/games', { choFormation, hanFormation, aiSide, aiLevel }),
  move: (id: string, move: MoveDto) => request<GameDto>(`/api/games/${id}/moves`, move),
  recent: () => request<GameSummary[]>('/api/games'),
  me: () => request<Me>('/api/me'),
  setNickname: (nickname: string) => request<Me>('/api/me/nickname', { nickname }, 'PUT'),
  providers: () => request<{ providers: string[] }>('/api/auth/providers'),
  replay: (id: string, ply: number) => request<ReplayDto>(`/api/games/${id}/replay?ply=${ply}`),
  game: (id: string) => request<GameDto>(`/api/games/${id}`),
  resign: (id: string) => request<GameDto>(`/api/games/${id}/resign`, {}),
  match: (formation: Formation) => request<MatchStatus>('/api/match', { formation }),
  cancelMatch: () => request<MatchStatus>('/api/match', undefined, 'DELETE'),
  chat: (id: string) => request<ChatMessage[]>(`/api/games/${id}/chat`),
  /** 새로 생긴 말을 돌려준다. AI 대국이면 내 말과 AI의 답이다. */
  say: (id: string, text: string) => request<ChatMessage[]>(`/api/games/${id}/chat`, { text }),
  shop: () => request<ShopState>('/api/shop'),
  order: (packId: string) => request<ShopOrder>('/api/shop/orders', { packId }),
  confirmOrder: (orderId: string, paymentKey: string) =>
    request<ShopState>(`/api/shop/orders/${orderId}/confirm`, { paymentKey }),
  buy: (itemId: string) => request<ShopState>(`/api/shop/items/${itemId}/buy`, {}),
  buyBundle: (bundleId: string) => request<ShopState>(`/api/shop/bundles/${bundleId}/buy`, {}),
  equip: (itemId: string) => request<ShopState>('/api/shop/equipped', { itemId }, 'PUT'),
  coinHistory: () => request<CoinLedgerRow[]>('/api/shop/coins/history'),
  notices: () => request<Notice[]>('/api/notices'),
  tournaments: () => request<TournamentSummary[]>('/api/tournaments'),
  tournament: (id: number) => request<TournamentDetail>(`/api/tournaments/${id}`),
  joinTournament: (id: number) => request<TournamentDetail>(`/api/tournaments/${id}/join`, {}),
  leaveTournament: (id: number) => request<TournamentDetail>(`/api/tournaments/${id}/leave`, {}),
  hostTournament: (t: TournamentInput) => request<TournamentDetail>('/api/tournaments', t),
  cancelHosted: (id: number) => request<TournamentDetail>(`/api/tournaments/${id}/cancel`, {}),
  rankings: (period: RankingPeriod, offset = 0) =>
    request<Ranking>(`/api/rankings?period=${period.toLowerCase()}&offset=${offset}`),
  admin: {
    stats: () => request<AdminStats>('/api/admin/stats'),
    users: (q: string, page: number) =>
      request<AdminUserPage>(`/api/admin/users?q=${encodeURIComponent(q)}&page=${page}`),
    rename: (id: number, nickname: string) => request<AdminUser>(`/api/admin/users/${id}/nickname`, { nickname }, 'PUT'),
    ban: (id: number, reason: string) => request<AdminUser>(`/api/admin/users/${id}/ban`, { reason }),
    unban: (id: number) => request<AdminUser>(`/api/admin/users/${id}/ban`, undefined, 'DELETE'),
    grant: (id: number, itemId: string) => request<AdminUser>(`/api/admin/users/${id}/items`, { itemId }),
    coins: (id: number, amount: number, memo: string) =>
      request<AdminUser>(`/api/admin/users/${id}/coins`, { amount, memo }),
    orders: () => request<AdminOrder[]>('/api/admin/orders'),
    logs: () => request<AdminLog[]>('/api/admin/logs'),
    notices: () => request<Notice[]>('/api/admin/notices'),
    tournaments: () => request<TournamentSummary[]>('/api/admin/tournaments'),
    createTournament: (t: TournamentInput) => request<TournamentSummary[]>('/api/admin/tournaments', t),
    cancelTournament: (id: number) => request<TournamentSummary>(`/api/admin/tournaments/${id}/cancel`, {}),
    createNotice: (n: NoticeInput) => request<Notice>('/api/admin/notices', n),
    updateNotice: (id: number, n: NoticeInput) => request<Notice>(`/api/admin/notices/${id}`, n, 'PUT'),
    deleteNotice: (id: number) => request<{ deleted: boolean }>(`/api/admin/notices/${id}`, undefined, 'DELETE'),
  },
}

/** 실시간 알림(STOMP) 주소. API와 같은 서버의 /ws. */
export function wsUrl(): string {
  const base = BASE ? new URL(BASE) : location
  return `${base.protocol === 'https:' ? 'wss' : 'ws'}://${base.host}/ws`
}
