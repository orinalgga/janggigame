import type { Look } from './api'

/**
 * 캐릭터 아바타. 조선 초상화풍으로 몸(남녀·노소), 머리, 수염, 옷, 관모, 배경 그림을 겹쳐 SVG 하나로 그린다.
 * 부품 id는 서버 shop/ShopItem과 같고, 차림(Look)은 api.ts에 있다. 모르는 id는 기본 부품으로 그린다.
 * 좌표계는 120×150이고, 얼굴은 (60, 52) 언저리, 어깨는 y 90 아래다.
 */
export const DEFAULT_LOOK: Look = {
  background: 'avatar.basic',
  body: 'body.man',
  hair: 'hair.sangtu',
  beard: 'beard.none',
  outfit: 'outfit.plain',
  hat: 'hat.none',
}

// ---- 몸과 얼굴 ----

interface Body {
  skin: string
  shade: string
  hair: string
  woman: boolean
  elder: boolean
}

const BODIES: Record<string, Body> = {
  'body.man': { skin: '#efd0ad', shade: '#d9b28c', hair: '#1d1916', woman: false, elder: false },
  'body.woman': { skin: '#f6dcc3', shade: '#e4c0a0', hair: '#1d1916', woman: true, elder: false },
  'body.elder': { skin: '#e8c7a3', shade: '#cfa682', hair: '#cfc9bf', woman: false, elder: true },
  'body.elder.woman': { skin: '#f0d4b9', shade: '#d9b596', hair: '#cfc9bf', woman: true, elder: true },
}

function face(b: Body): string {
  const brow = b.elder ? '#9c968c' : '#2a221d'
  const eyes = b.woman
    ? `<path d="M49.5 50.2 Q53 47.8 56.5 49.8 L57.4 49.2 M70.5 50.2 Q67 47.8 63.5 49.8 L62.6 49.2" stroke="#2f241d" stroke-width="1.3" fill="none" stroke-linecap="round"/>`
    : `<path d="M50 50 Q53 48.2 56 50 M64 50 Q67 48.2 70 50" stroke="#2f241d" stroke-width="1.35" fill="none" stroke-linecap="round"/>`
  const mouth = b.woman
    ? `<path d="M56.2 63.2 Q58 62.1 60 62.8 Q62 62.1 63.8 63.2 Q60 65.8 56.2 63.2 Z" fill="#bf5f55"/>`
    : `<path d="M56 63.6 Q60 64.8 64 63.6" stroke="#93574a" stroke-width="1.3" fill="none" stroke-linecap="round"/>`
  const age = b.elder
    ? `<path d="M53 39.5 Q60 38 67 39.5 M54.5 42 Q60 40.8 65.5 42" stroke="${b.shade}" stroke-width="0.8" fill="none"/>
       <path d="M55 58.5 Q53.8 61.5 55.2 64.5 M65 58.5 Q66.2 61.5 64.8 64.5 M47.6 51.5 L46 52.6 M72.4 51.5 L74 52.6" stroke="${b.shade}" stroke-width="0.9" fill="none" stroke-linecap="round"/>`
    : ''
  const cheek = b.woman ? `<ellipse cx="51.5" cy="58" rx="3.4" ry="1.7" fill="#e3907e" opacity="0.3"/><ellipse cx="68.5" cy="58" rx="3.4" ry="1.7" fill="#e3907e" opacity="0.3"/>` : ''
  return `
    <ellipse cx="43.4" cy="54" rx="3" ry="5" fill="${b.skin}" stroke="${b.shade}" stroke-width="0.6"/>
    <ellipse cx="76.6" cy="54" rx="3" ry="5" fill="${b.skin}" stroke="${b.shade}" stroke-width="0.6"/>
    <ellipse cx="60" cy="52" rx="16.6" ry="20.6" fill="${b.skin}"/>
    <path d="M45 60 Q48 71 60 73 Q72 71 75 60" stroke="${b.shade}" stroke-width="0.8" fill="none" opacity="0.7"/>
    <path d="M49.5 46 Q53 44 56.6 45.4 M63.4 45.4 Q67 44 70.5 46" stroke="${brow}" stroke-width="${b.woman ? 1.2 : 1.7}" fill="none" stroke-linecap="round"/>
    ${eyes}
    <circle cx="53" cy="50.5" r="1.25" fill="#22180f"/><circle cx="67" cy="50.5" r="1.25" fill="#22180f"/>
    <path d="M60 51 Q58.5 56.8 60.4 58.4 M58.2 58.9 Q60 59.9 61.8 58.9" stroke="${b.shade}" stroke-width="1" fill="none" stroke-linecap="round"/>
    ${cheek}${mouth}${age}`
}

// ---- 머리 ----

/** back은 몸 뒤, front는 얼굴 위에 그린다. 색은 몸(나이)이 정한다. */
interface Hair {
  back: (c: string) => string
  front: (c: string) => string
}

const MANGGEON = `
  <path d="M43.4 42.5 Q60 35.5 76.6 42.5 L76.8 47 Q60 40.2 43.2 47 Z" fill="#26211e" opacity="0.88"/>
  <path d="M46 44.6 Q60 39 74 44.6" stroke="#4a423c" stroke-width="0.5" fill="none" stroke-dasharray="1 1"/>
  <circle cx="44.6" cy="46.6" r="1.7" fill="#d2a43c" stroke="#8a6416" stroke-width="0.5"/>
  <circle cx="75.4" cy="46.6" r="1.7" fill="#d2a43c" stroke="#8a6416" stroke-width="0.5"/>`
const PARTED = (c: string) =>
  `<path d="M42.8 58 Q40.5 30 60 29.5 Q79.5 30 77.2 58 Q76.2 44 70 39.2 Q64.5 35.4 60 37.2 Q55.5 35.4 50 39.2 Q43.8 44 42.8 58 Z" fill="${c}"/>
   <path d="M60 30 L60 37" stroke="#00000033" stroke-width="0.8"/>`
const JJOK_BACK = (c: string) =>
  `<ellipse cx="60" cy="67" rx="15" ry="7" fill="${c}"/>
   <rect x="38" y="65.6" width="44" height="2.6" rx="1.3" fill="#c99a2e"/>
   <circle cx="38.5" cy="66.9" r="2.2" fill="#3d8a6a"/><circle cx="81.5" cy="66.9" r="2.2" fill="#3d8a6a"/>`

const HAIRS: Record<string, Hair> = {
  'hair.sangtu': {
    back: () => '',
    front: (c) => `
      <path d="M43.5 50 Q42 30 60 29 Q78 30 76.5 50 Q74 41 66 39 Q60 38 54 39 Q46 41 43.5 50 Z" fill="${c}"/>
      <ellipse cx="60" cy="26.5" rx="5" ry="5.6" fill="${c}"/>
      <path d="M53.5 26 L66.5 26" stroke="#c99a2e" stroke-width="1.4" stroke-linecap="round"/>
      ${MANGGEON}`,
  },
  'hair.jjok': { back: JJOK_BACK, front: PARTED },
  'hair.loose': {
    back: (c) => `<path d="M70 40 Q84 52 82 74 Q80 92 88 112 Q78 106 74 92 Q70 76 66 60 Z" fill="${c}"/>`,
    front: (c) => `
      <path d="M43.5 51 Q42 30 60 29 Q78 30 76.5 51 Q74 41 66 39 Q60 38 54 39 Q46 41 43.5 51 Z" fill="${c}"/>
      <path d="M43.6 42.6 Q60 36 76.4 42.6 L76.5 45.6 Q60 39.4 43.5 45.6 Z" fill="#8e1f22"/>
      <path d="M76 44 Q84 46 86 54 Q82 50 77 47 Z" fill="#8e1f22"/>`,
  },
  'hair.gache': {
    back: JJOK_BACK,
    front: (c) => `
      ${PARTED(c)}
      <path d="M33 41 Q28 17 60 13 Q92 17 87 41 Q78 30.5 60 31.5 Q42 30.5 33 41 Z" fill="${c}"/>
      ${[0, 1, 2, 3].map((i) => `<path d="M${36 + i * 3} ${38 - i * 5.5} Q60 ${30 - i * 5.5} ${84 - i * 3} ${38 - i * 5.5}" stroke="#ffffff22" stroke-width="1.1" fill="none"/>`).join('')}
      <path d="M44 33 Q60 26 76 33" stroke="#8e1f22" stroke-width="1.6" fill="none"/>`,
  },
}

// ---- 수염 ----

const BEARDS: Record<string, (c: string) => string> = {
  'beard.none': () => '',
  'beard.mustache': (c) => `<path d="M52.5 61.6 Q56.5 59.2 60 60.6 Q63.5 59.2 67.5 61.6 Q63.5 61.4 60 62.3 Q56.5 61.4 52.5 61.6 Z" fill="${c}"/>`,
  'beard.scholar': (c) => `
    <path d="M52.5 61.6 Q56.5 59.6 60 60.8 Q63.5 59.6 67.5 61.6 Q63.5 61.6 60 62.4 Q56.5 61.6 52.5 61.6 Z" fill="${c}"/>
    <path d="M57 66.6 Q60 67.6 63 66.6 Q62.6 77 60 88 Q57.4 77 57 66.6 Z" fill="${c}"/>`,
  'beard.full': (c) => `
    <path d="M43.8 55 Q44.5 72 60 82 Q75.5 72 76.2 55 Q73.5 66 66.5 67.6 Q60 69.2 53.5 67.6 Q46.5 66 43.8 55 Z" fill="${c}"/>
    <path d="M52 61.4 Q56.5 58.6 60 60.2 Q63.5 58.6 68 61.4 Q63.5 62 60 62.8 Q56.5 62 52 61.4 Z" fill="${c}"/>
    <path d="M54 76 Q60 96 66 76 Z" fill="${c}"/>`,
  'beard.sage': (c) => `
    <path d="M49.5 62.5 Q47 92 60 116 Q73 92 70.5 62.5 Q65.5 67.6 60 67.6 Q54.5 67.6 49.5 62.5 Z" fill="${c}"/>
    <path d="M51 61.6 Q56.5 58.4 60 60.2 Q63.5 58.4 69 61.6 Q74 66 76 72 Q68 64 60 63 Q52 64 44 72 Q46 66 51 61.6 Z" fill="${c}"/>
    <path d="M55 74 Q57 96 60 110 M65 74 Q63 96 60 110" stroke="#00000014" stroke-width="0.8" fill="none"/>`,
}

// ---- 옷 ----

interface Outfit {
  coat: string
  collar: string
  /** 동정(깃 위의 흰 띠). */
  trim: string
  /** 여자 몸이면 저고리 아래 치마. */
  skirt: string
  ribbon: string | null
  neck: 'v' | 'round'
  /** 무늬·띠·흉배처럼 몸통 위에 얹는 것. */
  extra?: (id: string) => string
  defs?: (id: string) => string
}

const TORSO = 'M8 150 Q10 106 38 96 L52 89 L68 89 L82 96 Q110 106 112 150 Z'
const GOLD = '#d4a83a'
const goldDots = (id: string, color = GOLD) =>
  `<pattern id="${id}g" width="9" height="9" patternUnits="userSpaceOnUse"><circle cx="2.5" cy="2.5" r="1.2" fill="${color}" opacity="0.85"/><circle cx="7" cy="7" r="0.8" fill="${color}" opacity="0.6"/></pattern>`
const sleeveBands = (colors: string[]) =>
  colors
    .map(
      (c, i) =>
        `<path d="M${10 + i * 0.8} ${126 + i * 5} Q20 ${122 + i * 5} 30 ${121 + i * 5} L30 ${125 + i * 5} Q20 ${126 + i * 5} ${9.6 + i * 0.8} ${130 + i * 5} Z" fill="${c}"/>` +
        `<path d="M${110 - i * 0.8} ${126 + i * 5} Q100 ${122 + i * 5} 90 ${121 + i * 5} L90 ${125 + i * 5} Q100 ${126 + i * 5} ${110.4 - i * 0.8} ${130 + i * 5} Z" fill="${c}"/>`,
    )
    .join('')
const ROUNDEL = (x: number, y: number, r: number) => `
  <circle cx="${x}" cy="${y}" r="${r}" fill="${GOLD}" stroke="#8f6a14" stroke-width="0.8"/>
  <path d="M${x - r * 0.6} ${y + r * 0.2} Q${x - r * 0.2} ${y - r * 0.7} ${x + r * 0.15} ${y - r * 0.05} Q${x + r * 0.45} ${y + r * 0.5} ${x + r * 0.6} ${y - r * 0.4}" stroke="#8a1a1a" stroke-width="${r * 0.14}" fill="none" stroke-linecap="round"/>
  <circle cx="${x + r * 0.6}" cy="${y - r * 0.45}" r="${r * 0.12}" fill="#8a1a1a"/>`

const OUTFITS: Record<string, Outfit> = {
  'outfit.plain': { coat: '#ebe3d2', collar: '#c9bb9c', trim: '#faf7f0', skirt: '#cfc4ae', ribbon: '#b9a883', neck: 'v' },
  'outfit.durumagi': { coat: '#5c2846', collar: '#4a1f38', trim: '#f4efe6', skirt: '#3e2a4f', ribbon: '#4a1f38', neck: 'v' },
  'outfit.dopo': {
    coat: '#d6e5e2',
    collar: '#bcd2ce',
    trim: '#ffffff',
    skirt: '#9fb9c6',
    ribbon: null,
    neck: 'v',
    extra: () => `
      <path d="M22 128 Q60 134 98 128" stroke="#2c4a86" stroke-width="2.6" fill="none"/>
      <path d="M68 131 Q66 140 64 150 M71 131 Q72 140 74 150" stroke="#2c4a86" stroke-width="1.8" stroke-linecap="round"/>
      <path d="M62 146 L66 150 L62 150 Z M72 146 L76 150 L72 150 Z" fill="#2c4a86"/>`,
  },
  'outfit.cheollik': {
    coat: '#272a31',
    collar: '#7e1c1f',
    trim: '#e9e2d6',
    skirt: '#3a2a2a',
    ribbon: null,
    neck: 'v',
    extra: () => `
      <rect x="18" y="122" width="84" height="5" fill="#7e1c1f"/>
      ${[0, 1, 2, 3, 4, 5, 6].map((i) => `<path d="M${22 + i * 12} 128 L${20 + i * 12} 150" stroke="#1b1d22" stroke-width="1"/>`).join('')}
      <path d="M8 150 Q10 132 18 124 L22 150 Z M112 150 Q110 132 102 124 L98 150 Z" fill="#7e1c1f"/>`,
  },
  'outfit.dangui': {
    coat: '#2f6a4e',
    collar: '#7b2d6a',
    trim: '#fbf7ee',
    skirt: '#8e1b2c',
    ribbon: '#7b2d6a',
    neck: 'v',
    defs: (id) => goldDots(id),
    extra: (id) => `<path d="${TORSO}" fill="url(#${id}g)" opacity="0.9"/>`,
  },
  'outfit.gwanbok': {
    coat: '#a2232c',
    collar: '#7d141c',
    trim: '#f6efe2',
    skirt: '#7d141c',
    ribbon: null,
    neck: 'round',
    extra: () => `
      <rect x="45" y="104" width="30" height="24" rx="1.5" fill="#1d2d5a" stroke="${GOLD}" stroke-width="1.4"/>
      <path d="M49 112 Q54 106 60 111 Q66 106 71 112 M60 111 L60 121 M55 121 Q60 124 65 121" stroke="#f3efe6" stroke-width="1.6" fill="none" stroke-linecap="round"/>
      <circle cx="60" cy="109.6" r="1.3" fill="#d42a2a"/>
      <path d="M48 124 Q52 118 56 124 M64 124 Q68 118 72 124" stroke="#7fb0d6" stroke-width="1" fill="none"/>
      <rect x="20" y="134" width="80" height="5.5" rx="2.5" fill="#1b1b1f"/>
      ${[30, 44, 58, 72, 86].map((x) => `<rect x="${x - 3}" y="134.8" width="6" height="4" rx="1" fill="${GOLD}"/>`).join('')}`,
  },
  'outfit.armor': {
    coat: '#8a1d22',
    collar: '#1c2a4f',
    trim: GOLD,
    skirt: '#1c2a4f',
    ribbon: null,
    neck: 'v',
    extra: () => `
      ${Array.from({ length: 6 }, (_, r) => Array.from({ length: 10 }, (_, c) => `<circle cx="${17 + c * 9.6 + (r % 2) * 4.8}" cy="${104 + r * 8}" r="1.3" fill="${GOLD}"/>`).join('')).join('')}
      <path d="M6 116 Q8 96 38 94 L40 112 Q20 108 6 116 Z" fill="#3c4250" stroke="${GOLD}" stroke-width="1.6"/>
      <path d="M114 116 Q112 96 82 94 L80 112 Q100 108 114 116 Z" fill="#3c4250" stroke="${GOLD}" stroke-width="1.6"/>
      <circle cx="60" cy="118" r="9" fill="#c7cfd8" stroke="${GOLD}" stroke-width="1.8"/>
      <path d="M55 115 Q60 111 65 115" stroke="#ffffff" stroke-width="1.4" fill="none" opacity="0.8"/>
      <rect x="18" y="136" width="84" height="5" fill="#1c2a4f"/>`,
  },
  'outfit.wonsam': {
    coat: '#1f6046',
    collar: '#8e1b2c',
    trim: '#fbf7ee',
    skirt: '#8e1b2c',
    ribbon: '#8e1b2c',
    neck: 'v',
    defs: (id) => goldDots(id),
    extra: (id) => `<path d="${TORSO}" fill="url(#${id}g)"/>${sleeveBands(['#c8222f', '#f2c447', '#2b5ea8', '#ffffff'])}`,
  },
  'outfit.gonryongpo': {
    coat: '#b0141c',
    collar: '#7d0d14',
    trim: '#f6efe2',
    skirt: '#7d0d14',
    ribbon: null,
    neck: 'round',
    extra: () => `
      ${ROUNDEL(60, 116, 11)}${ROUNDEL(24, 106, 7)}${ROUNDEL(96, 106, 7)}
      <rect x="18" y="134" width="84" height="6" rx="3" fill="#121217"/>
      ${[28, 42, 56, 70, 84, 96].map((x) => `<rect x="${x - 3}" y="135" width="6" height="4" rx="1" fill="#9fd8c0"/>`).join('')}`,
  },
}

// ---- 관모·장신구 ----

const HATS: Record<string, string> = {
  'hat.none': '',
  'hat.gat': `
    <path d="M39 35 Q38 76 55 98 M81 35 Q82 76 65 98" stroke="#b77a26" stroke-width="2.3" fill="none" stroke-linecap="round" stroke-dasharray="0.1 3.4"/>
    <ellipse cx="60" cy="33" rx="38" ry="5.6" fill="#111" opacity="0.74"/>
    <path d="M48.4 33 L49.8 12 Q60 9.6 70.2 12 L71.6 33 Z" fill="#111" opacity="0.86"/>
    <rect x="48.6" y="28.4" width="22.8" height="3.6" fill="#2b2b2b"/>`,
  'hat.jeongjagwan': `
    <path d="M45 42 L45 25 L75 25 L75 42 Q60 37 45 42 Z" fill="#141414"/>
    <path d="M46.5 25 L48 15 L54 19 L60 9 L66 19 L72 15 L73.5 25 Z" fill="#1c1c1c"/>
    <path d="M48 15 L48 24 M60 9 L60 24 M72 15 L72 24 M46 33 L74 33" stroke="#3a3a3a" stroke-width="0.8"/>`,
  'hat.samo': `
    <ellipse cx="33" cy="33" rx="12" ry="4.6" fill="#141414" transform="rotate(-6 33 33)"/>
    <ellipse cx="87" cy="33" rx="12" ry="4.6" fill="#141414" transform="rotate(6 87 33)"/>
    <path d="M44.6 42 Q44.6 25 60 23.5 Q75.4 25 75.4 42 Q60 37.5 44.6 42 Z" fill="#141414"/>
    <path d="M50 25 Q60 13 70 25 Z" fill="#1d1d1d"/>`,
  'hat.jeonrip': `
    <path d="M71 20 Q86 6 98 14 Q88 14 80 22 Z" fill="#2e5a8a"/><circle cx="92" cy="12.6" r="2.4" fill="#d9b23a"/><circle cx="92" cy="12.6" r="1.1" fill="#1f3f6a"/>
    <ellipse cx="60" cy="35" rx="31" ry="5" fill="#141414"/>
    <path d="M47 35 Q48 18 60 17 Q72 18 73 35 Z" fill="#1a1a1a"/>
    <path d="M60 17 Q51 8 55 2 Q60 9 65 2 Q69 8 60 17 Z" fill="#c0262d"/>
    <circle cx="60" cy="17" r="2.2" fill="${GOLD}"/>`,
  'hat.ikseon': `
    <path d="M50 27 Q42 20 46 14 Q53 15 55.5 24 Z" fill="#141414"/>
    <path d="M70 27 Q78 20 74 14 Q67 15 64.5 24 Z" fill="#141414"/>
    <path d="M44 43 Q44 23 60 21 Q76 23 76 43 Q60 38 44 43 Z" fill="#141414"/>
    <path d="M50 24 Q60 13 70 24 Z" fill="#1d1d1d"/>
    <path d="M46 36 Q60 31 74 36" stroke="${GOLD}" stroke-width="1" fill="none"/>`,
  'hat.binyeo': `
    <path d="M70 38 L90 28" stroke="${GOLD}" stroke-width="2" stroke-linecap="round"/>
    <path d="M86 30 Q92 22 98 25 Q94 27 95 31 Q100 30 101 35 Q95 33 90 34 Z" fill="${GOLD}" stroke="#8f6a14" stroke-width="0.6"/>
    <circle cx="92" cy="27.5" r="1" fill="#c0262d"/>
    <path d="M90 34 Q96 40 104 40" stroke="${GOLD}" stroke-width="1.2" fill="none"/>
    <circle cx="74" cy="36" r="2.2" fill="#3d8a6a"/>`,
  'hat.jokduri': `
    <path d="M51 35 L53 23 L67 23 L69 35 Q60 32.5 51 35 Z" fill="#17171f"/>
    <path d="M53 23 L60 17 L67 23" stroke="${GOLD}" stroke-width="1.4" fill="none"/>
    <circle cx="60" cy="16.5" r="2.4" fill="#c0262d"/><circle cx="53.5" cy="23" r="1.8" fill="#3d8a6a"/><circle cx="66.5" cy="23" r="1.8" fill="#2b5ea8"/>
    <circle cx="60" cy="28.5" r="2.3" fill="${GOLD}"/>`,
  'hat.tteoljam': [
    [60, 22, '#c0262d'],
    [45, 28, '#2b5ea8'],
    [75, 28, '#3d8a6a'],
  ]
    .map(
      ([x, y, c]) => `
      <path d="M${x} ${Number(y) + 4} L${x} ${Number(y) + 9}" stroke="${GOLD}" stroke-width="0.9"/>
      ${[0, 60, 120, 180, 240, 300].map((a) => `<ellipse cx="${x}" cy="${Number(y) - 4.2}" rx="1.8" ry="3" fill="${GOLD}" transform="rotate(${a} ${x} ${y})"/>`).join('')}
      <circle cx="${x}" cy="${y}" r="2.6" fill="${c}" stroke="${GOLD}" stroke-width="0.8"/>`,
    )
    .join(''),
}

// ---- 배경 그림 ----

/** 배경 그림. 사람 뒤 120×150 판 전체에 그린다. glyph가 있으면 붉은 낙관으로 찍는다. */
interface Backdrop {
  paint: (id: string) => string
  seal?: string
}

const plain = (top: string, bottom: string) => (id: string) =>
  `<defs><linearGradient id="${id}bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${top}"/><stop offset="1" stop-color="${bottom}"/></linearGradient></defs>
   <rect width="120" height="150" fill="url(#${id}bg)"/>`
const PINE = (x: number, y: number, s = 1) => `
  <path d="M${x} ${y} q${14 * s} -4 ${26 * s} 2 q${8 * s} 3 ${18 * s} 0" stroke="#5b3b22" stroke-width="${2.6 * s}" fill="none" stroke-linecap="round"/>
  ${[0, 1, 2].map((i) => `<ellipse cx="${x + (8 + i * 14) * s}" cy="${y - 5 * s}" rx="${8 * s}" ry="${3.6 * s}" fill="#2f5a3c"/><ellipse cx="${x + (8 + i * 14) * s}" cy="${y - 6.5 * s}" rx="${6 * s}" ry="${2.2 * s}" fill="#3f7350"/>`).join('')}`
const CRANE = (x: number, y: number) => `
  <path d="M${x} ${y} q8 -8 18 -4 q-6 1 -9 5 z" fill="#ffffff" stroke="#9a9a9a" stroke-width="0.4"/>
  <path d="M${x + 16} ${y - 3} q8 -10 14 -8 q-6 4 -10 10 z" fill="#ffffff" stroke="#9a9a9a" stroke-width="0.4"/>
  <path d="M${x + 7} ${y - 3} q-6 -6 -4 -12" stroke="#222" stroke-width="1" fill="none"/>
  <circle cx="${x + 3}" cy="${y - 15}" r="1.2" fill="#c0262d"/>
  <path d="M${x - 2} ${y + 1} l-6 2" stroke="#222" stroke-width="1.6"/>`

const BACKDROPS: Record<string, Backdrop> = {
  'avatar.basic': {
    paint: (id) => `${plain('#f3ead8', '#e4d3b4')(id)}
      <path d="M0 132 Q30 124 60 130 T120 126 L120 150 L0 150 Z" fill="#d9c49f" opacity="0.5"/>`,
  },
  'avatar.plum': {
    paint: (id) => `${plain('#f6efe6', '#eadbd0')(id)}
      <path d="M-2 40 Q22 34 30 18 M18 36 Q30 46 46 40 M100 8 Q108 30 122 34" stroke="#3a2a22" stroke-width="2.4" fill="none" stroke-linecap="round"/>
      ${[[24, 22], [32, 16], [40, 42], [12, 38], [106, 22], [114, 32], [48, 38]].map(([x, y]) => `${[0, 72, 144, 216, 288].map((a) => `<circle cx="${x}" cy="${y - 2.4}" r="1.8" fill="#f5c9cf" transform="rotate(${a} ${x} ${y})"/>`).join('')}<circle cx="${x}" cy="${y}" r="0.9" fill="#d4475a"/>`).join('')}`,
  },
  'avatar.moon': {
    paint: (id) => `${plain('#1c2546', '#3a3f62')(id)}
      <circle cx="88" cy="28" r="15" fill="#f6efd8"/><circle cx="88" cy="28" r="15" fill="#e8dcb4" opacity="0.4"/>
      <path d="M0 112 L18 92 L32 104 L50 84 L70 102 L88 88 L120 108 L120 150 L0 150 Z" fill="#141a33"/>
      <path d="M0 124 L24 108 L46 120 L72 104 L96 118 L120 112 L120 150 L0 150 Z" fill="#0d1226"/>`,
  },
  'avatar.turtle': {
    paint: (id) => `${plain('#f2ece0', '#ddd3c2')(id)}
      <path d="M0 96 Q10 56 22 64 Q30 30 42 58 Q48 72 56 70 L56 150 L0 150 Z" fill="#8f938f" opacity="0.55"/>
      <path d="M64 80 Q76 36 88 52 Q96 20 108 46 Q114 60 120 58 L120 150 L64 150 Z" fill="#6f746f" opacity="0.55"/>
      <path d="M0 116 Q30 96 60 112 T120 104 L120 150 L0 150 Z" fill="#4c514d" opacity="0.5"/>
      <path d="M10 90 L40 90 M76 72 L110 72" stroke="#ffffff" stroke-width="3" opacity="0.6" stroke-linecap="round"/>`,
    seal: '山',
  },
  'avatar.pine': {
    paint: (id) => `${plain('#efe6d2', '#dccdae')(id)}${PINE(-4, 30, 1.1)}${CRANE(84, 22)}
      <path d="M0 140 Q40 130 120 138 L120 150 L0 150 Z" fill="#c6b48f" opacity="0.6"/>`,
    seal: '松',
  },
  'avatar.crane': {
    paint: (id) => `${plain('#bfd9cf', '#e9e0c8')(id)}
      <circle cx="96" cy="20" r="9" fill="#d23a2e"/>
      <path d="M0 76 Q14 38 28 56 Q36 26 52 50 L52 150 L0 150 Z" fill="#3f8a6d"/><path d="M14 46 Q20 40 26 46 M34 36 Q40 30 46 36" stroke="#ffffff" stroke-width="2" fill="none"/>
      <path d="M78 150 L78 70 Q92 40 106 64 Q114 48 120 56 L120 150 Z" fill="#2f7a5d"/>
      <path d="M30 18 Q40 10 54 16 Q46 22 30 18 Z M58 24 Q66 18 76 22" fill="#ffffff" stroke="#ffffff" opacity="0.85"/>
      ${CRANE(60, 40)}`,
    seal: '壽',
  },
  'avatar.tiger': {
    paint: (id) => `${plain('#f1e4c4', '#e2cd9f')(id)}${PINE(66, 18, 0.9)}
      <path d="M96 10 q4 -6 10 -2 q-3 1 -4 4 z" fill="#1d2b3a"/><ellipse cx="98" cy="12" rx="4" ry="2.6" fill="#1d2b3a"/><path d="M98 13 q2 2 6 2" stroke="#ffffff" stroke-width="1.2"/>
      <g transform="translate(4 66)">
        <path d="M0 40 Q2 8 20 4 Q38 8 38 40 Z" fill="#e9962e"/>
        <path d="M6 14 L12 18 M4 22 L11 24 M30 14 L24 18 M32 22 L25 24 M17 6 L17 13 M22 6 L22 13" stroke="#2a1a10" stroke-width="2.4" stroke-linecap="round"/>
        <circle cx="12" cy="22" r="4" fill="#fff8e0"/><circle cx="27" cy="22" r="4" fill="#fff8e0"/>
        <circle cx="12" cy="22" r="2.2" fill="#2a1a10"/><circle cx="27" cy="22" r="2.2" fill="#2a1a10"/>
        <ellipse cx="19.5" cy="31" rx="7" ry="4.5" fill="#fff8e0"/><path d="M17 29 L22 29 L19.5 32 Z" fill="#2a1a10"/>
        <circle cx="5" cy="5" r="5" fill="#e9962e"/><circle cx="34" cy="5" r="5" fill="#e9962e"/>
      </g>`,
    seal: '虎',
  },
  'avatar.dragon': {
    paint: (id) => `${plain('#1f2f4a', '#2f4d5e')(id)}
      ${[[18, 30], [92, 22], [30, 110], [100, 96], [64, 14]].map(([x, y]) => `<path d="M${x - 14} ${y} q4 -8 10 -4 q4 -8 10 0 q6 -2 8 4 z" fill="#d8e2e6" opacity="0.65"/>`).join('')}
      <path d="M-4 70 Q20 40 40 60 Q60 80 80 52 Q96 30 124 44" stroke="#3f8f6c" stroke-width="9" fill="none" stroke-linecap="round"/>
      <path d="M-4 70 Q20 40 40 60 Q60 80 80 52 Q96 30 124 44" stroke="${GOLD}" stroke-width="2" fill="none" stroke-dasharray="2 4"/>
      <circle cx="100" cy="72" r="5" fill="#f2e2a0"/>`,
    seal: '龍',
  },
  'avatar.phoenix': {
    paint: (id) => `${plain('#f4e0bf', '#e5b98a')(id)}
      <circle cx="22" cy="22" r="11" fill="#d23a2e"/>
      <path d="M70 34 Q84 16 100 22 Q92 26 90 32 Q104 34 112 46 Q98 42 88 44 Q100 56 98 74 Q86 58 76 52 Q70 46 70 34 Z" fill="#c8312b"/>
      <path d="M88 44 Q104 70 122 74 M90 48 Q100 80 116 92 M86 50 Q92 84 102 104" stroke="#2b6aa8" stroke-width="2.6" fill="none" stroke-linecap="round"/>
      <path d="M88 44 Q104 70 122 74 M90 48 Q100 80 116 92 M86 50 Q92 84 102 104" stroke="${GOLD}" stroke-width="1" fill="none" stroke-dasharray="1 3"/>
      <circle cx="96" cy="23" r="1.2" fill="#111"/>`,
    seal: '鳳',
  },
  'avatar.irworobong': {
    paint: (id) => `${plain('#1d3a6e', '#2a5a8a')(id)}
      <circle cx="20" cy="24" r="9" fill="#d8312a"/><circle cx="100" cy="24" r="9" fill="#f4f1e6"/>
      <path d="M-2 96 L14 56 L26 80 L42 40 L60 82 L78 40 L94 80 L106 56 L122 96 L122 150 L-2 150 Z" fill="#2f7a4f"/>
      <path d="M14 56 L10 66 L18 66 Z M42 40 L36 54 L48 54 Z M78 40 L72 54 L84 54 Z M106 56 L102 66 L110 66 Z" fill="#f4f1e6"/>
      <path d="M-2 120 Q10 112 22 120 T46 120 T70 120 T94 120 T122 120 L122 150 L-2 150 Z" fill="#3c76b8"/>
      <path d="M-2 128 Q10 120 22 128 T46 128 T70 128 T94 128 T122 128" stroke="#e8f1f8" stroke-width="1.6" fill="none"/>
      <path d="M4 150 L6 98 M114 150 L112 98" stroke="#a03a24" stroke-width="3"/>
      <ellipse cx="6" cy="96" rx="7" ry="3" fill="#2f5a3c"/><ellipse cx="112" cy="96" rx="7" ry="3" fill="#2f5a3c"/>`,
  },
  'avatar.crown': {
    paint: (id) => `${plain('#f2d98a', '#c99a32')(id)}
      ${[30, 60, 90].map((x) => `<path d="M${x} 0 L${x} 150" stroke="#a87b1c" stroke-width="0.8" opacity="0.6"/>`).join('')}
      ${[0, 1, 2, 3, 4, 5].map((i) => `<rect x="${(i % 3) * 40 + 6}" y="${Math.floor(i / 3) * 70 + 8}" width="${14 + (i % 2) * 8}" height="${12 + (i % 3) * 4}" fill="#fff3c4" opacity="0.35"/>`).join('')}`,
    seal: '冠',
  },
  'avatar.king': {
    paint: (id) => `${plain('#3a1d4f', '#1d0e2c')(id)}
      ${[0, 1, 2, 3, 4].map((i) => `<circle cx="${12 + i * 24}" cy="${14 + (i % 2) * 6}" r="1.6" fill="${GOLD}"/>`).join('')}
      <path d="M0 140 Q60 120 120 140" stroke="${GOLD}" stroke-width="2" fill="none" opacity="0.6"/>`,
    seal: '王',
  },
  'avatar.master': {
    paint: (id) => `${plain('#2a2723', '#0e0d0c')(id)}
      <path d="M-4 120 Q30 84 70 104 Q96 116 124 92" stroke="#ffffff" stroke-width="10" fill="none" opacity="0.06" stroke-linecap="round"/>
      <path d="M8 20 Q30 14 46 24" stroke="${GOLD}" stroke-width="1.4" fill="none"/>`,
    seal: '國',
  },
}

let uid = 0

/** 캐릭터 한 장. head면 얼굴만 둥글게 잘라 작은 자리(머리글 등)에 쓴다. */
export function characterSvg(look: Partial<Look> | null | undefined, opts: { head?: boolean } = {}): string {
  const l = { ...DEFAULT_LOOK, ...(look ?? {}) }
  const body = BODIES[l.body] ?? BODIES[DEFAULT_LOOK.body]
  const outfit = OUTFITS[l.outfit] ?? OUTFITS[DEFAULT_LOOK.outfit]
  const hair = HAIRS[l.hair] ?? HAIRS[DEFAULT_LOOK.hair]
  const beard = body.woman ? '' : (BEARDS[l.beard] ?? BEARDS[DEFAULT_LOOK.beard])(body.elder ? '#e4e0d8' : body.hair)
  const hat = HATS[l.hat] ?? ''
  const bg = BACKDROPS[l.background] ?? BACKDROPS[DEFAULT_LOOK.background]
  const id = `ch${++uid}`

  const neckline =
    outfit.neck === 'round'
      ? `<path d="M46 90 Q60 104 74 90" stroke="${outfit.trim}" stroke-width="2.4" fill="none"/>
         <path d="M45 89 Q60 106 75 89" stroke="${outfit.collar}" stroke-width="3.4" fill="none" transform="translate(0 2.4)"/>`
      : `<path d="M52 89 L60 104 L68 89 Z" fill="#f7f2e8"/>
         <path d="M68 89 L57.6 104.6" stroke="${outfit.collar}" stroke-width="6" stroke-linecap="square"/>
         <path d="M52 89 L73 124" stroke="${outfit.collar}" stroke-width="6.4" stroke-linecap="square"/>
         <path d="M51 90.5 L69.6 121.6" stroke="${outfit.trim}" stroke-width="2.2"/>`
  const short = body.woman
  const skirt = short ? `<path d="M12 150 Q14 128 26 123 L94 123 Q106 128 108 150 Z" fill="${outfit.skirt}"/>` : ''
  const ribbon = outfit.ribbon
    ? `<path d="M66 113 Q58.5 108.5 57 113.5 Q59 118 66 115 Z M66 113 Q73.5 108.5 75 113.5 Q73 118 66 115 Z" fill="${outfit.ribbon}"/>
       <path d="M65 115 L60.5 ${short ? 140 : 146} L64 ${short ? 140 : 146} L67 115.6 Z M67 115 L73 ${short ? 138 : 144} L76.5 ${short ? 137 : 143} L68.6 114.8 Z" fill="${outfit.ribbon}"/>`
    : ''
  // 여자 저고리는 짧아서 치마가 가슴께부터 보인다.
  const coat = short ? `<path d="M8 150 Q10 106 38 96 L52 89 L68 89 L82 96 Q110 106 112 150 L112 126 L8 126 Z" fill="${outfit.coat}"/>` : `<path d="${TORSO}" fill="${outfit.coat}"/>`
  const fold = `<path d="M30 112 Q28 130 30 150 M90 112 Q92 130 90 150" stroke="#00000018" stroke-width="1.2" fill="none"/>`

  const parts = [
    `<defs>${outfit.defs?.(id) ?? ''}<clipPath id="${id}c">${opts.head ? '<circle cx="60" cy="50" r="27"/>' : '<rect width="120" height="150" rx="14"/>'}</clipPath></defs>`,
    `<g clip-path="url(#${id}c)">`,
    bg.paint(id),
    hair.back(body.hair),
    coat,
    short ? skirt : '',
    outfit.extra?.(id) ?? '',
    fold,
    `<path d="M54 66 L54 92 L66 92 L66 66 Z" fill="${body.skin}"/><path d="M54 74 Q60 79 66 74 L66 80 Q60 84 54 80 Z" fill="${body.shade}" opacity="0.6"/>`,
    neckline,
    ribbon,
    face(body),
    beard,
    hair.front(body.hair),
    hat,
    bg.seal && !opts.head
      ? `<g transform="translate(101 128)"><rect x="-8" y="-8" width="16" height="16" rx="2" fill="#b8231f" opacity="0.92"/><text y="5" text-anchor="middle" font-size="12" font-family="'Noto Serif KR', serif" font-weight="900" fill="#f8e9d8">${bg.seal}</text></g>`
      : '',
    `</g>`,
  ]
  const frame = opts.head
    ? `<circle cx="60" cy="50" r="26.4" fill="none" stroke="#00000022" stroke-width="1.2"/>`
    : `<rect x="0.6" y="0.6" width="118.8" height="148.8" rx="13.6" fill="none" stroke="#00000026" stroke-width="1.2"/>`
  const view = opts.head ? '33 23 54 54' : '0 0 120 150'
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${view}" role="img">${parts.join('')}${frame}</svg>`
}

export const LOOK_KEY = { BODY: 'body', HAIR: 'hair', BEARD: 'beard', OUTFIT: 'outfit', HAT: 'hat', AVATAR: 'background' } as const
export type LookSlot = keyof typeof LOOK_KEY

/** 여자 몸에는 수염을 그리지 않는다. */
export const showsBeard = (body: string) => !(BODIES[body]?.woman ?? false)

export function withPart(look: Look, slot: LookSlot, id: string): Look {
  return { ...look, [LOOK_KEY[slot]]: id }
}
