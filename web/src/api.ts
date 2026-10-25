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

/** rating은 대국을 시작할 때의 레이팅. */
export interface Player {
  id: number
  nickname: string
  rating: number
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
  rating: number
  /** 장착한 테마의 상점 상품 id. */
  boardTheme: string
  pieceTheme: string
}

export type ShopSlot = 'BOARD' | 'PIECES'

/** 상점 상품. 로그인하지 않았으면 owned, equipped는 무료 상품과 기본 장착 기준이다. */
export interface ShopItem {
  id: string
  slot: ShopSlot
  title: string
  description: string
  /** 원 단위. 0이면 무료. */
  price: number
  owned: boolean
  equipped: boolean
}

export interface ShopOrder {
  orderId: string
  itemId: string
  title: string
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
  loginUrl: (provider: string) => `${AUTH_BASE}/oauth2/authorization/${provider}`,
  /** 로그인 후 돌아온 주소의 #token=을 꺼내 저장하고 주소에서 지운다. */
  takeTokenFromUrl() {
    const m = location.hash.match(/token=([^&]+)/)
    if (m) {
      auth.save(decodeURIComponent(m[1]))
      history.replaceState(null, '', location.pathname + location.search)
    }
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
  providers: () => request<{ providers: string[] }>('/api/auth/providers'),
  replay: (id: string, ply: number) => request<ReplayDto>(`/api/games/${id}/replay?ply=${ply}`),
  game: (id: string) => request<GameDto>(`/api/games/${id}`),
  resign: (id: string) => request<GameDto>(`/api/games/${id}/resign`, {}),
  match: (formation: Formation) => request<MatchStatus>('/api/match', { formation }),
  cancelMatch: () => request<MatchStatus>('/api/match', undefined, 'DELETE'),
  chat: (id: string) => request<ChatMessage[]>(`/api/games/${id}/chat`),
  /** 새로 생긴 말을 돌려준다. AI 대국이면 내 말과 AI의 답이다. */
  say: (id: string, text: string) => request<ChatMessage[]>(`/api/games/${id}/chat`, { text }),
  shop: () => request<ShopItem[]>('/api/shop'),
  order: (itemId: string) => request<ShopOrder>('/api/shop/orders', { itemId }),
  confirmOrder: (orderId: string, paymentKey: string) =>
    request<ShopItem[]>(`/api/shop/orders/${orderId}/confirm`, { paymentKey }),
  equip: (itemId: string) => request<ShopItem[]>('/api/shop/equipped', { itemId }, 'PUT'),
}

/** 실시간 알림(STOMP) 주소. API와 같은 서버의 /ws. */
export function wsUrl(): string {
  const base = BASE ? new URL(BASE) : location
  return `${base.protocol === 'https:' ? 'wss' : 'ws'}://${base.host}/ws`
}
