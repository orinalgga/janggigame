import type { Formation, Level, Side } from './api'

export const FORMATIONS: { value: Formation; label: string; hanja: string }[] = [
  { value: 'MA_SANG_MA_SANG', label: '마상마상', hanja: '馬象馬象' },
  { value: 'SANG_MA_SANG_MA', label: '상마상마', hanja: '象馬象馬' },
  { value: 'MA_SANG_SANG_MA', label: '마상상마', hanja: '馬象象馬' },
  { value: 'SANG_MA_MA_SANG', label: '상마마상', hanja: '象馬馬象' },
]

/** AI 상대의 캐릭터. 서버 chat/Persona와 이름을 맞춘다. */
export const PERSONAS: Record<Level, { level: string; name: string; badge: string; desc: string }> = {
  BEGINNER: { level: '초급', name: '새내기 장돌이', badge: '卒', desc: '장기를 막 배운 해맑은 초보' },
  INTERMEDIATE: { level: '중급', name: '동네 고수 박씨', badge: '馬', desc: '공원 정자의 넉살 좋은 아저씨' },
  ADVANCED: { level: '상급', name: '장기 도사', badge: '車', desc: '말수 적고 침착한 고수' },
}

export const LEVELS: Level[] = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED']

export type Mode = 'AI' | 'ONLINE' | 'HUMAN'

export interface StartOptions {
  mode: Mode
  /** AI 대국에서 내가 두는 진영. */
  mySide: Side
  level: Level
  choFormation: Formation
  hanFormation: Formation
}
