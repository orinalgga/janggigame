/**
 * 아바타의 모양. id는 서버 shop/ShopItem과 같다.
 * 동그란 패 위에 한자 한 글자. 기본 아바타는 닉네임 첫 글자를 쓴다.
 */
export interface AvatarLook {
  /** 비우면 닉네임 첫 글자. */
  glyph: string | null
  /** 위에서 아래로 칠할 두 색. */
  from: string
  to: string
  fg: string
  ring: string
  /** 대회 보상은 금빛으로 빛난다. */
  glow?: boolean
}

export const DEFAULT_AVATAR = 'avatar.basic'

export const AVATARS: Record<string, AvatarLook> = {
  'avatar.basic': { glyph: null, from: '#a8642a', to: '#7a4116', fg: '#fff', ring: 'transparent' },
  'avatar.pine': { glyph: '松', from: '#3f7a4c', to: '#1b3a24', fg: '#eaf5dd', ring: '#9cc68a' },
  'avatar.plum': { glyph: '梅', from: '#fbe3ea', to: '#e597ad', fg: '#7a1f3d', ring: '#c9587a' },
  'avatar.turtle': { glyph: '龜', from: '#c8eadc', to: '#5ea68d', fg: '#123d2f', ring: '#2f7560' },
  'avatar.moon': { glyph: '月', from: '#2f3a6e', to: '#0f1430', fg: '#ffe7a3', ring: '#c9b06a' },
  'avatar.crane': { glyph: '鶴', from: '#ffffff', to: '#e6e1d6', fg: '#1b1b1b', ring: '#c43a2c' },
  'avatar.tiger': { glyph: '虎', from: '#f9b955', to: '#c8641b', fg: '#2a160a', ring: '#2a160a' },
  'avatar.dragon': { glyph: '龍', from: '#3f82d6', to: '#173a7a', fg: '#eef5ff', ring: '#e4c169' },
  'avatar.phoenix': { glyph: '鳳', from: '#e5503f', to: '#8e1616', fg: '#ffe4a6', ring: '#e4c169' },
  'avatar.crown': { glyph: '冠', from: '#fff3ad', to: '#d39b1c', fg: '#5a3200', ring: '#8a5a00', glow: true },
  'avatar.king': { glyph: '王', from: '#62318f', to: '#2a0d48', fg: '#ffd86b', ring: '#e4c169', glow: true },
  'avatar.master': { glyph: '國', from: '#3a3a3a', to: '#050505', fg: '#f5d77a', ring: '#e4c169', glow: true },
}

/** 운영 대회 우승 보상으로 고를 수 있는 아바타. 서버 ShopItem의 reward 상품과 같다. */
export const REWARD_AVATARS: { id: string; title: string }[] = [
  { id: 'avatar.crown', title: '우승 금관' },
  { id: 'avatar.king', title: '정기 리그 왕' },
  { id: 'avatar.master', title: '국수' },
]

export const rewardTitle = (id: string | null) => REWARD_AVATARS.find((a) => a.id === id)?.title ?? ''

export function avatarLook(id: string | null | undefined): AvatarLook {
  return AVATARS[id ?? DEFAULT_AVATAR] ?? AVATARS[DEFAULT_AVATAR]
}
