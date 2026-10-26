import type { Look } from './api'
import { dollSvg } from './character-doll'
import { paintSvg } from './character-paint'

/**
 * 캐릭터 아바타. 화풍(style)에 따라 입체감 있는 인형(기본) 또는 애니메이션풍으로 그린다.
 * 부품 id는 서버 shop/ShopItem과 같고, 차림(Look)은 api.ts에 있다. 모르는 id는 기본 부품으로 그린다.
 */
export const DEFAULT_LOOK: Look = {
  style: 'style.doll',
  background: 'avatar.basic',
  body: 'body.man',
  hair: 'hair.short',
  beard: 'beard.none',
  outfit: 'outfit.tee',
  hat: 'hat.none',
}

/** 캐릭터 한 장. head면 얼굴만 둥글게 잘라 작은 자리(머리글 등)에 쓴다. */
export function characterSvg(look: Partial<Look> | null | undefined, opts: { head?: boolean } = {}): string {
  const l = { ...DEFAULT_LOOK, ...(look ?? {}) }
  if (l.style === 'style.anime') return paintSvg(l, opts)
  return dollSvg(l, opts)
}

export const LOOK_KEY = {
  STYLE: 'style',
  BODY: 'body',
  HAIR: 'hair',
  BEARD: 'beard',
  OUTFIT: 'outfit',
  HAT: 'hat',
  AVATAR: 'background',
} as const
export type LookSlot = keyof typeof LOOK_KEY

/** 동물 몸에는 머리카락을 그리지 않는다. */
export const isAnimal = (body: string) => !['body.man', 'body.woman', 'body.elder', 'body.elder.woman'].includes(body)

/** 여자와 동물에는 수염을 그리지 않는다. */
export const showsBeard = (body: string) => body === 'body.man' || body === 'body.elder'

export function withPart(look: Look, slot: LookSlot, id: string): Look {
  return { ...look, [LOOK_KEY[slot]]: id }
}
