import type { GameSummary } from './api'

/** 대국 기록 한 줄을 내 기준 승·패·무로 읽는 도우미. 로비 기록과 프로필 창이 같이 쓴다. */
const HOW: Record<string, string> = { CHECKMATE: '외통', BIKJANG: '빅장', RESIGN: '기권', TIMEOUT: '시간', DISCONNECT: '끊김' }

/** 내가 둔 쪽이 있으면 내 기준 승·패·무, 없으면(2인·비회원) null. */
export function outcome(g: GameSummary): 'win' | 'loss' | 'draw' | null {
  if (!g.mySide || g.result === 'ONGOING') return null
  if (g.result === 'DRAW') return 'draw'
  return (g.result === 'CHO_WIN') === (g.mySide === 'CHO') ? 'win' : 'loss'
}
export const MARK = { win: '승', loss: '패', draw: '무' }

export function result(g: GameSummary) {
  if (g.result === 'ONGOING') return '진행 중'
  const how = HOW[g.endReason ?? 'CHECKMATE']
  const o = outcome(g)
  if (o) return `${how}${o === 'draw' ? ' 무승부' : o === 'win' ? '승' : '패'}`
  if (g.result === 'DRAW') return `${how} 무승부`
  return `${how} ${g.result === 'CHO_WIN' ? '초' : '한'} 승`
}
