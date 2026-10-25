import { reactive } from 'vue'
import type { PieceDto, Side } from './api'

/**
 * 상점 테마의 모양. id는 서버 shop/ShopItem과 같다.
 * 장기판에만 CSS 변수를 덮어써서, 채팅이나 버튼 색은 그대로 둔다.
 */
export const BOARD_THEMES: Record<string, Record<string, string>> = {
  'board.classic': { '--board-light': '#f0cf8e', '--board-dark': '#d19a55', '--board-line': '#4a2c10' },
  'board.walnut': { '--board-light': '#9a6a40', '--board-dark': '#5b3518', '--board-line': '#f6e2c0' },
  'board.hanji': { '--board-light': '#fbf8f0', '--board-dark': '#e6dac2', '--board-line': '#1f1a14', '--grain': '0.35' },
  'board.jade': { '--board-light': '#b3dcc2', '--board-dark': '#5f9e7d', '--board-line': '#123a28', '--grain': '0.4' },
  'board.night': { '--board-light': '#34437a', '--board-dark': '#141a38', '--board-line': '#e8c873', '--grain': '0.3' },
}

export const PIECE_THEMES: Record<string, Record<string, string>> = {
  'pieces.classic': {
    '--piece-light': '#fffaf0',
    '--piece-dark': '#ecd9b4',
    '--piece-edge': '#8a6236',
    '--cho-color': '#0e5a3c',
    '--han-color': '#a8161b',
  },
  'pieces.ivory': {
    '--piece-light': '#fff9e8',
    '--piece-dark': '#e6d2a4',
    '--piece-edge': '#b39762',
    '--cho-color': '#1d3b2c',
    '--han-color': '#6e1414',
  },
  'pieces.celadon': {
    '--piece-light': '#dff1e7',
    '--piece-dark': '#94c4ad',
    '--piece-edge': '#4a7b66',
    '--cho-color': '#0f3528',
    '--han-color': '#8a1d24',
  },
  'pieces.ink': {
    '--piece-light': '#45403c',
    '--piece-dark': '#141211',
    '--piece-edge': '#c9a86a',
    '--cho-color': '#74e3ab',
    '--han-color': '#ff7f73',
  },
  'pieces.gold': {
    '--piece-light': '#fff3be',
    '--piece-dark': '#dfae3f',
    '--piece-edge': '#7d5108',
    '--cho-color': '#0d4a31',
    '--han-color': '#9a1010',
  },
}

export const DEFAULT_BOARD = 'board.classic'
export const DEFAULT_PIECES = 'pieces.classic'

export interface ThemeChoice {
  board: string
  pieces: string
}

/** 지금 장착한 테마. 로그인하면 서버에 저장된 값으로 바뀐다. */
export const equipped = reactive<ThemeChoice>({ board: DEFAULT_BOARD, pieces: DEFAULT_PIECES })

export function setEquipped(board: string | undefined, pieces: string | undefined) {
  equipped.board = board && BOARD_THEMES[board] ? board : DEFAULT_BOARD
  equipped.pieces = pieces && PIECE_THEMES[pieces] ? pieces : DEFAULT_PIECES
}

export function themeStyle(t: ThemeChoice): Record<string, string> {
  return { ...(BOARD_THEMES[t.board] ?? {}), ...(PIECE_THEMES[t.pieces] ?? {}) }
}

/** 상점 미리보기용 첫 배치(마상마상). */
export function startingPieces(): PieceDto[] {
  const back: [string, string][] = [
    ['CHARIOT', '車'],
    ['HORSE', '馬'],
    ['ELEPHANT', '象'],
    ['GUARD', '士'],
    ['', ''],
    ['GUARD', '士'],
    ['HORSE', '馬'],
    ['ELEPHANT', '象'],
    ['CHARIOT', '車'],
  ]
  const pieces: PieceDto[] = []
  for (const side of ['CHO', 'HAN'] as Side[]) {
    const row = (y: number) => (side === 'CHO' ? y : 9 - y)
    back.forEach(([type, label], x) => type && pieces.push({ x, y: row(0), side, type, label }))
    pieces.push({ x: 4, y: row(1), side, type: 'GENERAL', label: side === 'CHO' ? '楚' : '漢' })
    for (const x of [1, 7]) pieces.push({ x, y: row(2), side, type: 'CANNON', label: '包' })
    for (const x of [0, 2, 4, 6, 8]) {
      pieces.push({ x, y: row(3), side, type: 'SOLDIER', label: side === 'CHO' ? '卒' : '兵' })
    }
  }
  return pieces
}
