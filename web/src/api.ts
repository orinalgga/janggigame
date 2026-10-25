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
}

export type Result = 'ONGOING' | 'CHO_WIN' | 'HAN_WIN' | 'DRAW'

export interface GameSummary {
  id: string
  aiSide: Side | null
  aiLevel: Level | null
  result: Result
  endReason: 'CHECKMATE' | 'BIKJANG' | null
  moveCount: number
  startedAt: string
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

async function request<T>(path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = auth.token()
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(BASE + path, {
    method: body === undefined ? 'GET' : 'POST',
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
}
