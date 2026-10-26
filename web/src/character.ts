import type { Look } from './api'
import { avatarLook } from './avatars'

/**
 * 캐릭터 아바타. 몸(사람·동물), 머리 모양, 옷, 모자를 겹쳐 SVG 하나로 그린다.
 * 부품 id는 서버 shop/ShopItem과 같고, 차림(Look)은 api.ts에 있다. 모르는 id는 기본 부품으로 그린다.
 * 좌표계는 120×140(모자가 위로 14 넘친다)이고, 얼굴은 (60, 54) 언저리, 몸통은 y 84 아래다.
 */
export const DEFAULT_LOOK: Look = {
  background: 'avatar.basic',
  body: 'body.human',
  hair: 'hair.bob',
  outfit: 'outfit.white',
  hat: 'hat.none',
}

interface Body {
  skin: string
  /** 동물은 얼굴 대신 그리는 주둥이·무늬, 머리 위에 귀. 사람이면 null. */
  animal: null | { back: string; front: string }
  /** 동물은 머리 모양을 그리지 않는다. */
  hair: boolean
}

const EYES = `
  <ellipse cx="47" cy="58" rx="4.2" ry="5.2" fill="#2b1d16"/>
  <ellipse cx="73" cy="58" rx="4.2" ry="5.2" fill="#2b1d16"/>
  <circle cx="48.6" cy="56" r="1.6" fill="#fff"/>
  <circle cx="74.6" cy="56" r="1.6" fill="#fff"/>`
const BLUSH = `
  <ellipse cx="39" cy="67" rx="5.5" ry="3.2" fill="#ff8f9c" opacity="0.5"/>
  <ellipse cx="81" cy="67" rx="5.5" ry="3.2" fill="#ff8f9c" opacity="0.5"/>`
const SMILE = `<path d="M55 68 Q60 73 65 68" stroke="#8a3b2e" stroke-width="2" fill="none" stroke-linecap="round"/>`
/** 동물 입: 코 아래로 ω. */
const MUZZLE_MOUTH = (nose: string) => `
  <path d="M56.5 64 L63.5 64 L60 68 Z" fill="${nose}" stroke="${nose}" stroke-width="1.5" stroke-linejoin="round"/>
  <path d="M60 68 Q60 72 56 72 M60 68 Q60 72 64 72" stroke="#5a3428" stroke-width="1.6" fill="none" stroke-linecap="round"/>`

const BODIES: Record<string, Body> = {
  'body.human': { skin: '#ffe2cc', animal: null, hair: true },
  'body.human.tan': { skin: '#e9b38b', animal: null, hair: true },
  'body.rabbit': {
    skin: '#fffaf5',
    hair: false,
    animal: {
      back: `
        <g transform="rotate(-10 44 30)"><ellipse cx="44" cy="12" rx="9" ry="24" fill="#fffaf5" stroke="#eadfd6" stroke-width="1.5"/><ellipse cx="44" cy="14" rx="4.5" ry="17" fill="#ffc6d2"/></g>
        <g transform="rotate(10 76 30)"><ellipse cx="76" cy="12" rx="9" ry="24" fill="#fffaf5" stroke="#eadfd6" stroke-width="1.5"/><ellipse cx="76" cy="14" rx="4.5" ry="17" fill="#ffc6d2"/></g>`,
      front: MUZZLE_MOUTH('#ff8fa6'),
    },
  },
  'body.cat': {
    skin: '#d9d4cf',
    hair: false,
    animal: {
      back: `
        <path d="M30 44 L32 16 L52 30 Z" fill="#d9d4cf"/><path d="M34 38 L35 23 L46 31 Z" fill="#ffc6d2"/>
        <path d="M90 44 L88 16 L68 30 Z" fill="#d9d4cf"/><path d="M86 38 L85 23 L74 31 Z" fill="#ffc6d2"/>`,
      front: `
        <path d="M52 28 Q56 36 54 42 M60 26 L60 40 M68 28 Q64 36 66 42" stroke="#9c948d" stroke-width="2.4" fill="none" stroke-linecap="round"/>
        ${MUZZLE_MOUTH('#f28b9b')}
        <path d="M30 62 L42 64 M30 68 L42 67 M90 62 L78 64 M90 68 L78 67" stroke="#8d8680" stroke-width="1.2" stroke-linecap="round"/>`,
    },
  },
  'body.bear': {
    skin: '#a9754e',
    hair: false,
    animal: {
      back: `
        <circle cx="34" cy="30" r="11" fill="#a9754e"/><circle cx="34" cy="30" r="6" fill="#d9a983"/>
        <circle cx="86" cy="30" r="11" fill="#a9754e"/><circle cx="86" cy="30" r="6" fill="#d9a983"/>`,
      front: `<ellipse cx="60" cy="68" rx="12" ry="9" fill="#ead2b6"/>${MUZZLE_MOUTH('#3b2418')}`,
    },
  },
  'body.fox': {
    skin: '#f38b3c',
    hair: false,
    animal: {
      back: `
        <path d="M28 46 L30 8 L54 30 Z" fill="#f38b3c"/><path d="M31 30 L31 14 L40 22 Z" fill="#3a2a22"/>
        <path d="M92 46 L90 8 L66 30 Z" fill="#f38b3c"/><path d="M89 30 L89 14 L80 22 Z" fill="#3a2a22"/>`,
      front: `
        <path d="M29 58 Q36 78 60 80 Q84 78 91 58 Q80 70 60 66 Q40 70 29 58 Z" fill="#fff6ee"/>
        ${MUZZLE_MOUTH('#3a2a22')}`,
    },
  },
  'body.tiger': {
    skin: '#f7a33b',
    hair: false,
    animal: {
      back: `
        <circle cx="33" cy="30" r="11" fill="#f7a33b"/><circle cx="33" cy="30" r="6" fill="#fff6ee"/>
        <circle cx="87" cy="30" r="11" fill="#f7a33b"/><circle cx="87" cy="30" r="6" fill="#fff6ee"/>`,
      front: `
        <ellipse cx="60" cy="69" rx="15" ry="10" fill="#fff6ee"/>
        <path d="M52 30 L68 30 M53 35 L67 35 M60 30 L60 41 M52 41 L68 41" stroke="#2b1d16" stroke-width="2.6" stroke-linecap="round"/>
        <path d="M29 50 L38 52 M29 57 L37 57 M91 50 L82 52 M91 57 L83 57" stroke="#2b1d16" stroke-width="2.6" stroke-linecap="round"/>
        ${MUZZLE_MOUTH('#e0606e')}`,
    },
  },
  'body.dragon': {
    skin: '#6cc7ab',
    hair: false,
    animal: {
      back: `
        <path d="M40 30 Q34 14 40 4 Q44 16 50 26 Z" fill="#f2c94c" stroke="#c99a1e" stroke-width="1.5"/>
        <path d="M80 30 Q86 14 80 4 Q76 16 70 26 Z" fill="#f2c94c" stroke="#c99a1e" stroke-width="1.5"/>
        <path d="M30 40 Q16 36 12 44 Q22 46 30 50 Z" fill="#4fae92"/><path d="M90 40 Q104 36 108 44 Q98 46 90 50 Z" fill="#4fae92"/>`,
      front: `
        <ellipse cx="60" cy="69" rx="14" ry="9" fill="#e9f6d8"/>
        <circle cx="55" cy="66" r="1.4" fill="#2f5d50"/><circle cx="65" cy="66" r="1.4" fill="#2f5d50"/>
        <path d="M54 72 Q60 76 66 72" stroke="#2f5d50" stroke-width="1.8" fill="none" stroke-linecap="round"/>
        <path d="M46 70 Q32 72 26 82 M74 70 Q88 72 94 82" stroke="#f2c94c" stroke-width="1.8" fill="none" stroke-linecap="round"/>
        <path d="M54 26 L60 20 L66 26 Z" fill="#4fae92"/>`,
    },
  },
}

/** 머리 모양: 얼굴 뒤(back)와 앞머리(front). */
interface Hair {
  color: string
  back: string
  front: string
}

const BLACK = '#2d2420'

const BANGS_FLAT = 'M28 50 Q29 19 60 18 Q91 19 92 50 Q84 40 75 43 Q67 34 59 42 Q49 35 41 44 Q34 42 28 50 Z'
const BANGS_PARTED = 'M28 54 Q29 18 60 17 Q91 18 92 54 Q86 32 62 30 L60 24 L58 30 Q34 32 28 54 Z'

const HAIRS: Record<string, Hair> = {
  'hair.bob': {
    color: BLACK,
    back: 'M24 56 Q23 14 60 13 Q97 14 96 56 L96 78 Q90 82 84 78 L84 60 L36 60 L36 78 Q30 82 24 78 Z',
    front: BANGS_FLAT,
  },
  'hair.daenggi': {
    color: BLACK,
    back: `M25 56 Q24 15 60 14 Q96 15 95 56 L95 74 L25 74 Z
           M86 74 Q98 84 92 96 Q100 104 92 114 Q98 122 92 128 L86 128 Q90 120 84 114 Q90 104 84 96 Q90 86 80 76 Z`,
    front: BANGS_PARTED,
  },
  'hair.sangtu': {
    color: BLACK,
    back: 'M27 54 Q26 18 60 17 Q94 18 93 54 L93 62 L27 62 Z',
    front: 'M28 48 Q31 18 60 17 Q89 18 92 48 Q78 34 60 34 Q42 34 28 48 Z',
  },
  'hair.jjok': {
    color: BLACK,
    back: 'M25 56 Q24 15 60 14 Q96 15 95 56 L95 70 L25 70 Z M40 80 Q40 70 60 70 Q80 70 80 80 Q80 90 60 90 Q40 90 40 80 Z',
    front: BANGS_PARTED,
  },
  'hair.twin': {
    color: BLACK,
    back: `M24 56 Q23 14 60 13 Q97 14 96 56 L96 70 L24 70 Z
           M16 62 Q8 82 14 104 Q20 112 26 104 Q30 84 26 62 Z M104 62 Q112 82 106 104 Q100 112 94 104 Q90 84 94 62 Z`,
    front: BANGS_FLAT,
  },
  'hair.messy': {
    color: '#6b4a2f',
    back: 'M25 58 Q22 14 60 12 Q98 14 95 58 L95 66 L25 66 Z',
    front: 'M27 52 L30 30 L36 40 L40 20 L48 34 L56 16 L62 32 L70 16 L74 34 L82 22 L84 40 L90 30 L93 52 Q86 42 78 46 Q70 38 60 44 Q50 38 42 46 Q34 42 27 52 Z',
  },
  'hair.long': {
    color: BLACK,
    back: 'M24 56 Q23 13 60 12 Q97 13 96 56 L102 130 Q92 136 84 128 L84 70 L36 70 L36 128 Q28 136 18 130 Z',
    front: 'M28 50 Q27 18 60 17 Q93 18 92 50 Q60 41 28 50 Z',
  },
  'hair.silver': {
    color: '#e3e6f0',
    back: 'M24 56 Q23 13 60 12 Q97 13 96 56 L102 130 Q92 136 84 128 L84 70 L36 70 L36 128 Q28 136 18 130 Z',
    front: BANGS_PARTED,
  },
}

/** 머리 장식(댕기 끈, 비녀, 양갈래 리본, 상투)은 머리색과 따로 칠한다. */
const HAIR_EXTRAS: Record<string, string> = {
  'hair.daenggi': `<path d="M86 126 L80 138 L88 134 L94 140 L94 128 Z" fill="#d6283a"/>`,
  'hair.sangtu': `<ellipse cx="60" cy="12" rx="9" ry="8" fill="${BLACK}"/><rect x="53" y="16" width="14" height="4" rx="2" fill="#c9a24a"/>`,
  'hair.jjok': `<rect x="16" y="77" width="88" height="4.5" rx="2.2" fill="#d4a62a"/><circle cx="17" cy="79" r="4" fill="#3f9a72"/><circle cx="103" cy="79" r="4" fill="#3f9a72"/>`,
  'hair.twin': `
    <path d="M14 60 L6 54 L8 66 Z M28 60 L36 54 L34 66 Z" fill="#e8455f"/><circle cx="21" cy="60" r="4" fill="#e8455f"/>
    <path d="M92 60 L84 54 L86 66 Z M106 60 L114 54 L112 66 Z" fill="#e8455f"/><circle cx="99" cy="60" r="4" fill="#e8455f"/>`,
}

/** 옷: 몸통 색과 깃, 고름, 치마. 갑옷처럼 무늬가 있으면 extra. */
interface Outfit {
  jacket: string
  collar: string
  ribbon: string | null
  skirt: string | null
  /** 깃 모양: 한복 V깃, 둥근 단령. */
  neck: 'v' | 'round'
  extra?: string
}

const TORSO = 'M20 140 Q20 98 42 88 L78 88 Q100 98 100 140 Z'
const SLEEVE_STRIPES = (colors: string[]) =>
  colors
    .map(
      (c, i) =>
        `<path d="M${21 + i * 0.6} ${108 + i * 6} L38 ${106 + i * 6} L38 ${112 + i * 6} L${20.5 + i * 0.6} ${114 + i * 6} Z" fill="${c}"/>` +
        `<path d="M${99 - i * 0.6} ${108 + i * 6} L82 ${106 + i * 6} L82 ${112 + i * 6} L${99.5 - i * 0.6} ${114 + i * 6} Z" fill="${c}"/>`,
    )
    .join('')

const OUTFITS: Record<string, Outfit> = {
  'outfit.white': { jacket: '#f6f1e6', collar: '#d8ccb8', ribbon: '#cdbb9f', skirt: null, neck: 'v' },
  'outfit.saekdong': {
    jacket: '#ffd85c',
    collar: '#e8455f',
    ribbon: '#e8455f',
    skirt: '#d93a4a',
    neck: 'v',
    extra: SLEEVE_STRIPES(['#e8455f', '#ffd85c', '#3fae6a', '#3d7fd6', '#ff8fb1']),
  },
  'outfit.pink': { jacket: '#ffd6e2', collar: '#9b5fc0', ribbon: '#d93d74', skirt: '#ff8fb3', neck: 'v' },
  'outfit.dopo': {
    jacket: '#dfe7ef',
    collar: '#ffffff',
    ribbon: null,
    skirt: null,
    neck: 'v',
    extra: `<rect x="24" y="118" width="72" height="4" rx="2" fill="#3c5a99"/><path d="M66 120 L64 138 M70 120 L72 138" stroke="#3c5a99" stroke-width="2.2" stroke-linecap="round"/>`,
  },
  'outfit.hwarot': {
    jacket: '#c8102e',
    collar: '#f2c94c',
    ribbon: '#f2c94c',
    skirt: '#1f4fa8',
    neck: 'v',
    extra:
      SLEEVE_STRIPES(['#f2c94c', '#3fae6a', '#ffffff', '#3d7fd6']) +
      [[44, 98], [76, 98], [50, 114], [70, 114], [30, 126], [90, 126]]
        .map(([x, y]) => `<circle cx="${x}" cy="${y}" r="3.2" fill="#f2c94c"/><circle cx="${x}" cy="${y}" r="1.3" fill="#c8102e"/>`)
        .join(''),
  },
  'outfit.armor': {
    jacket: '#7a1c1c',
    collar: '#d4a62a',
    ribbon: null,
    skirt: null,
    neck: 'v',
    extra:
      Array.from({ length: 4 }, (_, row) =>
        Array.from({ length: 7 }, (_, col) => {
          const x = 30 + col * 10 + (row % 2) * 5
          const y = 100 + row * 9
          return `<path d="M${x - 5} ${y} Q${x} ${y + 7} ${x + 5} ${y}" stroke="#d4a62a" stroke-width="1.4" fill="none"/>`
        }).join(''),
      ).join('') +
      `<path d="M14 104 Q16 86 40 86 L42 102 Q28 98 14 104 Z" fill="#4a4f5a" stroke="#d4a62a" stroke-width="2"/>
       <path d="M106 104 Q104 86 80 86 L78 102 Q92 98 106 104 Z" fill="#4a4f5a" stroke="#d4a62a" stroke-width="2"/>
       <circle cx="60" cy="112" r="8" fill="#cfd6df" stroke="#d4a62a" stroke-width="2"/><circle cx="58" cy="110" r="2.5" fill="#fff" opacity="0.8"/>`,
  },
  'outfit.gonryongpo': {
    jacket: '#b3141c',
    collar: '#8a0f15',
    ribbon: null,
    skirt: null,
    neck: 'round',
    extra: `
      <circle cx="60" cy="108" r="11" fill="#e4b73c" stroke="#a87b12" stroke-width="1.5"/>
      <path d="M53 110 Q56 102 60 106 Q64 110 67 104 M54 113 Q60 116 66 112" stroke="#8a0f15" stroke-width="1.6" fill="none" stroke-linecap="round"/>
      <circle cx="28" cy="104" r="6" fill="#e4b73c"/><circle cx="92" cy="104" r="6" fill="#e4b73c"/>
      <rect x="22" y="124" width="76" height="6" rx="3" fill="#1d1d24"/>
      ${[32, 44, 56, 68, 80, 92].map((x) => `<rect x="${x - 2.5}" y="125" width="5" height="4" rx="1" fill="#9be0c4"/>`).join('')}`,
  },
  'outfit.black': {
    jacket: '#25252e',
    collar: '#c22a2a',
    ribbon: null,
    skirt: null,
    neck: 'v',
    extra: `<rect x="22" y="118" width="76" height="5" rx="2.5" fill="#c22a2a"/>
      <rect x="20" y="124" width="14" height="14" rx="3" fill="#4a4f5a"/><rect x="86" y="124" width="14" height="14" rx="3" fill="#4a4f5a"/>`,
  },
}

const HATS: Record<string, string> = {
  'hat.none': '',
  'hat.flower': `
    <g transform="translate(86 30)">
      ${[0, 72, 144, 216, 288].map((a) => `<ellipse cx="0" cy="-6" rx="4.5" ry="6.5" fill="#ff8fb3" transform="rotate(${a})"/>`).join('')}
      <circle r="3.5" fill="#ffd85c"/>
    </g>`,
  'hat.gat': `
    <path d="M24 92 Q40 98 52 82 M96 92 Q80 98 68 82" stroke="#2d2420" stroke-width="1.2" fill="none" stroke-dasharray="2 2.5"/>
    <ellipse cx="60" cy="22" rx="48" ry="8" fill="#1b1b1b" opacity="0.85"/>
    <path d="M45 22 L47 -2 Q60 -6 73 -2 L75 22 Z" fill="#1b1b1b" opacity="0.92"/>
    <rect x="45" y="17" width="30" height="4" fill="#3a3a3a"/>`,
  'hat.jokduri': `
    <path d="M46 20 L50 6 L70 6 L74 20 Z" fill="#1d1d2a"/>
    <path d="M50 6 L60 0 L70 6" stroke="#d4a62a" stroke-width="2" fill="none"/>
    <circle cx="60" cy="1" r="3" fill="#e8455f"/><circle cx="52" cy="6" r="2.4" fill="#3fae6a"/><circle cx="68" cy="6" r="2.4" fill="#3d7fd6"/>
    <circle cx="60" cy="13" r="3" fill="#d4a62a"/>`,
  'hat.hwagwan': [
    [30, 30, '#ff8fb3'],
    [40, 21, '#ffd85c'],
    [51, 16, '#e8455f'],
    [60, 14, '#ffffff'],
    [69, 16, '#9b5fc0'],
    [80, 21, '#ff8fb3'],
    [90, 30, '#ffd85c'],
  ]
    .map(
      ([x, y, c]) =>
        `<g transform="translate(${x} ${y})">${[0, 72, 144, 216, 288]
          .map((a) => `<ellipse cy="-4" rx="3.2" ry="4.6" fill="${c}" transform="rotate(${a})"/>`)
          .join('')}<circle r="2.4" fill="#f2a93b"/></g>`,
    )
    .join(''),
  'hat.helmet': `
    <path d="M24 48 Q24 10 60 8 Q96 10 96 48 Z" fill="#5a606b"/>
    <path d="M24 48 L18 70 L30 66 Z M96 48 L102 70 L90 66 Z" fill="#4a4f5a"/>
    <rect x="22" y="42" width="76" height="8" rx="3" fill="#d4a62a"/>
    <path d="M60 8 L60 -4" stroke="#d4a62a" stroke-width="3"/>
    <path d="M60 -4 Q50 -14 44 -6 Q54 -8 60 -2 Q66 -8 76 -6 Q70 -14 60 -4 Z" fill="#d6283a"/>
    <path d="M40 22 Q60 12 80 22" stroke="#8b919c" stroke-width="2" fill="none"/>`,
  'hat.ikseon': `
    <ellipse cx="36" cy="10" rx="9" ry="7" fill="#1b1b1b" transform="rotate(-25 36 10)"/>
    <ellipse cx="84" cy="10" rx="9" ry="7" fill="#1b1b1b" transform="rotate(25 84 10)"/>
    <path d="M30 34 Q30 6 60 4 Q90 6 90 34 Z" fill="#1b1b1b"/>
    <rect x="28" y="30" width="64" height="7" rx="3" fill="#2c2c2c"/>
    <path d="M44 18 Q60 10 76 18" stroke="#d4a62a" stroke-width="1.6" fill="none"/>`,
}

let uid = 0

/** 캐릭터 한 장. head면 얼굴만 둥글게 잘라 작은 자리(머리글 등)에 쓴다. */
export function characterSvg(look: Partial<Look> | null | undefined, opts: { head?: boolean } = {}): string {
  const l = { ...DEFAULT_LOOK, ...(look ?? {}) }
  const body = BODIES[l.body] ?? BODIES[DEFAULT_LOOK.body]
  const outfit = OUTFITS[l.outfit] ?? OUTFITS[DEFAULT_LOOK.outfit]
  const hair = body.hair ? HAIRS[l.hair] ?? HAIRS[DEFAULT_LOOK.hair] : null
  const hairExtra = body.hair ? HAIR_EXTRAS[l.hair] ?? '' : ''
  const hat = HATS[l.hat] ?? ''
  const bg = avatarLook(l.background)
  const id = `ch${++uid}`

  const neckline =
    outfit.neck === 'round'
      ? `<path d="M46 88 Q60 100 74 88" stroke="${outfit.collar}" stroke-width="6" fill="none"/>`
      : `<path d="M46 88 L60 108 L74 88 Z" fill="#fbf8f2"/>
         <path d="M46 88 L64 112 M74 88 L58 104" stroke="${outfit.collar}" stroke-width="5.5" stroke-linecap="round"/>
         <path d="M47 89 L62 109" stroke="#ffffff" stroke-width="1.6" stroke-linecap="round"/>`
  const ribbon = outfit.ribbon
    ? `<path d="M63 108 Q56 103 54 108 Q56 113 63 110 Z M63 108 Q70 103 72 108 Q70 113 63 110 Z" fill="${outfit.ribbon}"/>
       <path d="M62 110 L58 136 L62 136 L64 112 Z M64 110 L70 134 L74 133 L66 110 Z" fill="${outfit.ribbon}"/>`
    : ''
  const skirt = outfit.skirt ? `<path d="M22 140 Q26 118 38 114 L82 114 Q94 118 98 140 Z" fill="${outfit.skirt}"/>` : ''

  const parts = [
    // 배경 패: 아래로 짙어지는 색, 큰 한자를 흐리게.
    `<defs><linearGradient id="${id}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${bg.from}"/><stop offset="1" stop-color="${bg.to}"/></linearGradient></defs>`,
    opts.head
      ? `<circle cx="60" cy="56" r="56" fill="url(#${id})"/>`
      : `<rect x="0" y="-14" width="120" height="154" rx="18" fill="url(#${id})"/>`,
    bg.glyph && !opts.head
      ? `<text x="60" y="100" text-anchor="middle" font-size="92" font-family="'Noto Serif KR', serif" font-weight="900" fill="${bg.fg}" opacity="0.18">${bg.glyph}</text>`
      : '',
    hair ? `<path d="${hair.back}" fill="${hair.color}"/>` : '',
    body.animal?.back ?? '',
    // 몸통과 옷
    `<path d="${TORSO}" fill="${outfit.jacket}"/>`,
    skirt,
    outfit.extra ?? '',
    `<rect x="53" y="76" width="14" height="14" fill="${body.skin}"/>`,
    neckline,
    ribbon,
    // 얼굴
    body.animal ? '' : `<circle cx="29" cy="58" r="6" fill="${body.skin}"/><circle cx="91" cy="58" r="6" fill="${body.skin}"/>`,
    `<ellipse cx="60" cy="54" rx="31" ry="29" fill="${body.skin}"/>`,
    body.animal ? '' : `<path d="M29 48 Q60 40 91 48" fill="none"/>`,
    EYES,
    BLUSH,
    body.animal?.front ?? SMILE,
    hair ? `<path d="${hair.front}" fill="${hair.color}"/>` : '',
    hairExtra,
    hat,
  ]
  const ring = opts.head && bg.ring !== 'transparent' ? `<circle cx="60" cy="56" r="55" fill="none" stroke="${bg.ring}" stroke-width="4"/>` : ''
  const view = opts.head ? '4 0 112 112' : '0 -14 120 154'
  const clip = opts.head ? `<clipPath id="${id}c"><circle cx="60" cy="56" r="56"/></clipPath>` : ''
  const content = parts.join('')
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${view}" role="img">${clip}<g${opts.head ? ` clip-path="url(#${id}c)"` : ''}>${content}</g>${ring}</svg>`
}

/** 상점 칸에서 부품 하나만 바꿔 입혀 볼 때. */
export const LOOK_KEY = { BODY: 'body', HAIR: 'hair', OUTFIT: 'outfit', HAT: 'hat', AVATAR: 'background' } as const
export type LookSlot = keyof typeof LOOK_KEY

/** 동물 몸은 머리 모양을 그리지 않는다. */
export const showsHair = (body: string) => BODIES[body]?.hair ?? true

export function withPart(look: Look, slot: LookSlot, id: string): Look {
  return { ...look, [LOOK_KEY[slot]]: id }
}
