import type { Division, TournamentInput, TournamentSummary } from './api'

export const DIVISION: Record<Division, string> = { ALL: '누구나', DAN: '유단자', KYU: '급' }

/** 정기·특별·개인 중 하나. */
export function kindLabel(t: TournamentSummary): string {
  if (t.host) return '개인'
  return t.kind === 'REGULAR' ? (t.repeatDays === 1 ? '매일 정기' : '매주 정기') : '특별'
}

/** 1분+10초 같은 시간 규칙. */
export function timeControl(t: { baseSeconds: number; incrementSeconds: number }): string {
  const base = t.baseSeconds % 60 === 0 ? `${t.baseSeconds / 60}분` : `${t.baseSeconds}초`
  return t.incrementSeconds ? `${base}+${t.incrementSeconds}초` : base
}

/** 16강, 8강, 4강, 결승. */
export function roundName(round: number, total: number): string {
  const left = 2 ** (total - round + 1)
  return left === 2 ? '결승' : `${left}강`
}

export function startLabel(iso: string): string {
  return new Date(iso).toLocaleString('ko-KR', { month: 'numeric', day: 'numeric', weekday: 'short', hour: 'numeric', minute: '2-digit' })
}

/** 남은 시간을 "2시간 5분", "4분 30초"처럼. */
export function countdown(iso: string, now: number): string {
  const s = Math.max(0, Math.round((new Date(iso).getTime() - now) / 1000))
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (d) return `${d}일 ${h}시간`
  if (h) return `${h}시간 ${m}분`
  if (m) return `${m}분 ${s % 60}초`
  return `${s}초`
}

/** datetime-local 칸 값(내 시간대)을 ISO로. */
export const fromLocal = (v: string) => new Date(v).toISOString()

/** 지금부터 minutes분 뒤를 datetime-local 칸 값으로(분 단위). */
export function localIn(minutes: number): string {
  const d = new Date(Date.now() + minutes * 60_000)
  d.setSeconds(0, 0)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function emptyInput(): TournamentInput {
  return { title: '', description: null, startsAt: '', baseSeconds: 60, incrementSeconds: 10, entryFee: 1, maxPlayers: 16, bonusPrize: 10 }
}
