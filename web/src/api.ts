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
  result: 'ONGOING' | 'CHO_WIN' | 'HAN_WIN' | 'DRAW'
  choScore: number
  hanScore: number
  aiSide: Side | null
  aiLevel: Level | null
  lastMove: MoveDto | null
}

const BASE = import.meta.env.VITE_API_BASE ?? ''

async function request<T>(path: string, body?: unknown): Promise<T> {
  const res = await fetch(BASE + path, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { 'Content-Type': 'application/json' },
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
}
