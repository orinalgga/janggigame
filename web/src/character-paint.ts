import type { Look } from './api'

/**
 * 그림체 캐릭터(애니메이션풍·조선 민화풍). 같은 부품 id를 120×150 상반신 SVG로 그린다.
 * 얼굴과 선만 화풍마다 다르고, 옷·머리·모자·배경 그림은 함께 쓴다. 얼굴은 (60, 52) 언저리다.
 */
export type Skin = 'anime' | 'joseon'

const GOLD = '#d4a83a'

type Animal = 'cat' | 'dog' | 'rabbit' | 'bear' | 'fox' | 'tiger' | 'panda'

interface Body {
  skin: string
  shade: string
  hair: string
  woman: boolean
  elder: boolean
  /** 동물이면 skin이 털색, inner가 주둥이·귀 안쪽 색이고 머리카락·수염을 그리지 않는다. */
  animal?: Animal
  inner?: string
}

const BODIES: Record<string, Body> = {
  'body.man': { skin: '#ffe3cc', shade: '#efbfa0', hair: '#2b211d', woman: false, elder: false },
  'body.woman': { skin: '#ffe9d9', shade: '#f2c6aa', hair: '#3a2620', woman: true, elder: false },
  'body.elder': { skin: '#fadcc2', shade: '#e8b896', hair: '#cfc9c2', woman: false, elder: true },
  'body.elder.woman': { skin: '#ffe4d0', shade: '#eebfa2', hair: '#d6cfca', woman: true, elder: true },
  'body.cat': { skin: '#f2b66d', shade: '#d9934a', hair: '#f2b66d', woman: false, elder: false, animal: 'cat', inner: '#fff4e6' },
  'body.dog': { skin: '#e9d2b0', shade: '#c9a57a', hair: '#e9d2b0', woman: false, elder: false, animal: 'dog', inner: '#fffaf2' },
  'body.rabbit': { skin: '#fbf7f4', shade: '#e2d6cf', hair: '#fbf7f4', woman: true, elder: false, animal: 'rabbit', inner: '#f8c6cf' },
  'body.bear': { skin: '#a6774f', shade: '#86593a', hair: '#a6774f', woman: false, elder: false, animal: 'bear', inner: '#e8cba6' },
  'body.fox': { skin: '#ef8a3a', shade: '#c96a24', hair: '#ef8a3a', woman: true, elder: false, animal: 'fox', inner: '#fff8ef' },
  'body.tiger': { skin: '#f4a03c', shade: '#d27f22', hair: '#f4a03c', woman: false, elder: false, animal: 'tiger', inner: '#fff8ea' },
  'body.panda': { skin: '#ffffff', shade: '#dedede', hair: '#ffffff', woman: false, elder: false, animal: 'panda', inner: '#ffffff' },
}

const DARK = '#2a2224'

/** 동물 귀(머리 뒤). */
function animalEars(b: Body): string {
  const f = b.skin
  const i = b.inner!
  switch (b.animal) {
    case 'cat':
    case 'fox': {
      const big = b.animal === 'fox' ? 4 : 0
      return `<path d="M43 44 L${40 - big} ${22 - big} L56 35 Z M77 44 L${80 + big} ${22 - big} L64 35 Z" fill="${f}"/>
        <path d="M45.4 40 L${43 - big / 2} ${27 - big} L52.6 35.6 Z M74.6 40 L${77 + big / 2} ${27 - big} L67.4 35.6 Z" fill="${b.animal === 'fox' ? DARK : '#f7c6c0'}" stroke="none"/>`
    }
    case 'rabbit':
      return `<ellipse cx="51" cy="20" rx="5.6" ry="16" fill="${f}" transform="rotate(-10 51 20)"/><ellipse cx="69" cy="20" rx="5.6" ry="16" fill="${f}" transform="rotate(10 69 20)"/>
        <ellipse cx="51" cy="21" rx="2.6" ry="12" fill="${i}" stroke="none" transform="rotate(-10 51 21)"/><ellipse cx="69" cy="21" rx="2.6" ry="12" fill="${i}" stroke="none" transform="rotate(10 69 21)"/>`
    case 'dog':
      return `<path d="M44 40 Q34 40 34 56 Q36 66 43 62 Z M76 40 Q86 40 86 56 Q84 66 77 62 Z" fill="${b.shade}"/>`
    case 'panda':
      return `<circle cx="45" cy="37" r="6.4" fill="${DARK}"/><circle cx="75" cy="37" r="6.4" fill="${DARK}"/>`
    default:
      return `<circle cx="45" cy="37" r="6.4" fill="${f}"/><circle cx="75" cy="37" r="6.4" fill="${f}"/>
        <circle cx="45" cy="37.6" r="3.4" fill="${i}" stroke="none"/><circle cx="75" cy="37.6" r="3.4" fill="${i}" stroke="none"/>`
  }
}

/** 동물 털무늬와 주둥이(선을 두르지 않는 것, 눈보다 먼저). */
function animalMarks(b: Body): string {
  const muzzle = `<ellipse cx="60" cy="61.4" rx="8.4" ry="6" fill="${b.inner}"/>`
  const nose = `<path d="M57.6 58.2 Q60 57 62.4 58.2 Q61.6 60.4 60 60.8 Q58.4 60.4 57.6 58.2 Z" fill="${DARK}"/>
    <path d="M60 60.8 L60 62.6 M56.6 62.6 Q58.4 64.4 60 62.6 Q61.6 64.4 63.4 62.6" stroke="${DARK}" stroke-width="0.9" fill="none" stroke-linecap="round"/>`
  let marks = ''
  if (b.animal === 'tiger')
    marks = `<path d="M60 36 L60 42 M55 37 L56 41 M65 37 L64 41 M43.4 52 L48 53 M43.6 57 L47.6 57 M76.6 52 L72 53 M76.4 57 L72.4 57" stroke="${DARK}" stroke-width="1.6" stroke-linecap="round"/>`
  if (b.animal === 'panda')
    marks = `<ellipse cx="51.6" cy="53" rx="5" ry="6" fill="${DARK}" transform="rotate(20 51.6 53)"/><ellipse cx="68.4" cy="53" rx="5" ry="6" fill="${DARK}" transform="rotate(-20 68.4 53)"/>`
  if (b.animal === 'fox') marks = `<path d="M43 52 Q50 64 60 66 Q70 64 77 52 Q76 70 60 72 Q44 70 43 52 Z" fill="${b.inner}"/>`
  const whiskers = b.animal === 'cat' || b.animal === 'tiger'
    ? `<path d="M50 61 L42 60 M50 63 L42.6 64 M70 61 L78 60 M70 63 L77.4 64" stroke="${DARK}" stroke-width="0.6" opacity="0.7"/>`
    : ''
  return marks + muzzle + nose + whiskers
}

/** 얼굴 모양(선을 두를 것). */
function faceShape(b: Body, skin: Skin): string {
  if (b.animal) return `${animalEars(b)}<ellipse cx="60" cy="54" rx="19.4" ry="17.6" fill="${b.skin}"/>`
  const ears = `<ellipse cx="43.6" cy="54" rx="2.8" ry="4.4" fill="${b.skin}"/><ellipse cx="76.4" cy="54" rx="2.8" ry="4.4" fill="${b.skin}"/>`
  return skin === 'anime'
    ? `${ears}<path d="M43.6 46 Q43.4 61 50 68.4 Q55 73.4 60 74 Q65 73.4 70 68.4 Q76.6 61 76.4 46 Q76 30 60 30 Q44 30 43.6 46 Z" fill="${b.skin}"/>`
    : `${ears}<ellipse cx="60" cy="52.6" rx="17" ry="19.6" fill="${b.skin}"/>`
}

/** 눈·코·입·볼(선을 두르지 않는 것). */
function features(b: Body, skin: Skin, id: string): string {
  const line = '#2a1a14'
  if (b.animal) {
    const human = features({ ...b, animal: undefined, woman: false, elder: false, hair: b.shade }, skin, id)
    // 사람 얼굴에서 눈만 쓰고(코·입·볼 빼고), 털무늬·주둥이 위에 얹는다.
    const eyes = human.slice(0, human.indexOf('<!--nose-->'))
    return animalMarks(b) + eyes + `<ellipse cx="48" cy="60" rx="3.4" ry="1.7" fill="#f59a9a" opacity="0.4"/><ellipse cx="72" cy="60" rx="3.4" ry="1.7" fill="#f59a9a" opacity="0.4"/>`
  }
  if (skin === 'anime') {
    const iris = b.woman ? ['#5a2e1c', '#c27a3e'] : ['#232838', '#5f7396']
    const eye = (x: number, flip: number) =>
      b.elder
        ? `<path d="M${x - 3.8} 54 Q${x} 49.6 ${x + 3.8} 54" stroke="${line}" stroke-width="1.7" fill="none" stroke-linecap="round"/>`
        : `<ellipse cx="${x}" cy="53" rx="3.9" ry="4.7" fill="#ffffff"/>
           <ellipse cx="${x}" cy="53.6" rx="3.2" ry="4.1" fill="url(#${id}iris)"/>
           <ellipse cx="${x}" cy="54" rx="1.4" ry="2.1" fill="#120c0a"/>
           <circle cx="${x - 1.3}" cy="51.6" r="1.15" fill="#ffffff"/><circle cx="${x + 1.2}" cy="55.6" r="0.55" fill="#ffffff"/>
           <path d="M${x - 4.6} 50.4 Q${x} 46.6 ${x + 4.6} 50" stroke="${line}" stroke-width="${b.woman ? 2 : 1.7}" fill="none" stroke-linecap="round"/>
           ${b.woman ? `<path d="M${x + 4.4 * flip} ${flip > 0 ? 50 : 50.4} l${1.8 * flip} -1.4" stroke="${line}" stroke-width="1.2" stroke-linecap="round"/>` : ''}`
    const brow = b.elder ? '#a29a90' : b.hair
    return `<defs><linearGradient id="${id}iris" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${iris[0]}"/><stop offset="1" stop-color="${iris[1]}"/></linearGradient></defs>
      <path d="M48.6 45.4 Q52.6 43.6 56.4 44.8 M63.6 44.8 Q67.4 43.6 71.4 45.4" stroke="${brow}" stroke-width="1.3" fill="none" stroke-linecap="round"/>
      ${eye(52.4, -1)}${eye(67.6, 1)}<!--nose-->
      <path d="M60.2 58.4 L59.4 60" stroke="${b.shade}" stroke-width="0.9" stroke-linecap="round"/>
      <ellipse cx="49.4" cy="60.6" rx="3.6" ry="1.8" fill="#f59a9a" opacity="0.38"/><ellipse cx="70.6" cy="60.6" rx="3.6" ry="1.8" fill="#f59a9a" opacity="0.38"/>
      ${b.woman ? `<path d="M57.8 64.4 Q60 66.6 62.2 64.4 Z" fill="#d2675e"/>` : `<path d="M57.6 64.6 Q60 65.8 62.4 64.6" stroke="#a0524a" stroke-width="1.1" fill="none" stroke-linecap="round"/>`}`
  }
  // 민화풍: 먹으로 찍은 작은 눈과 동그란 볼
  const eye = (x: number) =>
    b.elder
      ? `<path d="M${x - 2.8} 53 Q${x} 50.4 ${x + 2.8} 53" stroke="${line}" stroke-width="1.5" fill="none" stroke-linecap="round"/>`
      : `<ellipse cx="${x}" cy="52.6" rx="1.5" ry="1.9" fill="${line}"/>${b.woman ? `<path d="M${x - 2.6} 50.8 Q${x} 49.6 ${x + 2.6} 50.8" stroke="${line}" stroke-width="0.8" fill="none"/>` : ''}`
  const brow = b.elder ? '#d8d2c8' : line
  return `<path d="M49.4 46.6 Q53 45 56.4 46.4 M63.6 46.4 Q67 45 70.6 46.6" stroke="${brow}" stroke-width="${b.woman ? 0.9 : 1.6}" fill="none" stroke-linecap="round"/>
    ${eye(53)}${eye(67)}<!--nose-->
    <path d="M60 55.6 Q58.8 58.6 60.6 59.2" stroke="${b.shade}" stroke-width="1" fill="none" stroke-linecap="round"/>
    <circle cx="49.6" cy="60" r="3.4" fill="#f08a7a" opacity="0.42"/><circle cx="70.4" cy="60" r="3.4" fill="#f08a7a" opacity="0.42"/>
    ${b.woman ? `<ellipse cx="60" cy="64.6" rx="2" ry="1.3" fill="#c8443a"/>` : `<path d="M57.8 64.4 Q60 65.6 62.2 64.4" stroke="#9a4a3e" stroke-width="1.2" fill="none" stroke-linecap="round"/>`}`
}

// ---- 머리 ----

/** back은 몸 뒤, front는 얼굴 위에 그린다. 색은 몸(나이)이 정한다. */
interface Hair {
  back: (c: string) => string
  front: (c: string) => string
}

const MODERN_HAIRS: Record<string, Hair> = {
  'hair.sidepart': {
    back: () => '',
    front: (c) => `<path d="M42.4 54 Q40 27 60 26.4 Q80 27 77.6 54 L76.4 47 Q73 41 66 40.4 Q56 40 48 45 Q45.6 48 44.6 52 Z" fill="${c}"/>
      <path d="M52 28 Q50 34 51 40" stroke="#00000040" stroke-width="0.9" fill="none"/>`,
  },
  'hair.slick': {
    back: () => '',
    front: (c) => `<path d="M43 52 Q41 25.6 60 25 Q79 25.6 77 52 Q76.4 39 69 35.6 Q60 33.6 51 35.6 Q43.6 39 43 52 Z" fill="${c}"/>
      ${[50, 56, 62, 68].map((x) => `<path d="M${x} 27 Q${x + 2} 31 ${x + 1} 35" stroke="#ffffff40" stroke-width="0.9" fill="none"/>`).join('')}`,
  },
  'hair.buzz': {
    back: () => '',
    front: (c) => `<path d="M43.4 48 Q42.6 28 60 27.6 Q77.4 28 76.6 48 Q75 38.6 66 37.2 Q60 36.6 54 37.2 Q45 38.6 43.4 48 Z" fill="${c}" opacity="0.9"/>`,
  },
  'hair.ponytail': {
    back: (c) => `<path d="M70 38 Q92 44 88 78 Q86 98 92 108 Q76 104 76 84 Q76 62 66 46 Z" fill="${c}"/>`,
    front: (c) => `<path d="M42.6 56 Q40 27 60 26.6 Q80 27 77.4 52 Q76 44 70 41 L65 45 L61 40 L55 45.6 L49.6 42.4 Q45 47 44.6 58 Z" fill="${c}"/>
      <circle cx="74.6" cy="39" r="2.6" fill="#d9483b"/>`,
  },
  'hair.wave': {
    back: (c) => `<path d="M37 60 Q34 28 60 27 Q86 28 83 60 Q90 74 84 88 Q92 102 86 118 Q74 128 60 128 Q46 128 34 118 Q28 102 36 88 Q30 74 37 60 Z" fill="${c}"/>`,
    front: (c) => `<path d="M39.4 96 Q35 80 38.6 66 Q35 46 42 36 Q48 27 60 27 Q72 27 78 36 Q85 46 81.4 66 Q85 80 80.6 96 L77 96 Q80 82 76.6 70 Q79 56 75 48 Q68 42 60 41 Q56 46 50 49 Q44 52 43.4 66 Q40 80 43.4 96 Z" fill="${c}"/>`,
  },
  'hair.bun': {
    back: () => '',
    front: (c) => `<circle cx="60" cy="21" r="8.6" fill="${c}"/><path d="M53 27.6 Q60 30.4 67 27.6" stroke="#d9483b" stroke-width="2.2" fill="none"/>
      <path d="M40 62 Q37 29 60 28 Q83 29 80 62 L76.4 62 Q77 52 75.4 46 Q66 44 60 45.6 Q54 44 44.6 46 Q43 52 43.6 62 Z" fill="${c}"/>`,
  },
  'hair.short': {
    back: () => '',
    front: (c) => `<path d="M42.4 54 Q40 27 60 26.4 Q80 27 77.6 54 L75 45 L71 48 L68 41 L63 46.6 L58 40.6 L53 46 L49 41.4 L46 47 Z" fill="${c}"/>`,
  },
  'hair.bob': {
    back: (c) => `<path d="M37 72 Q34 30 60 28 Q86 30 83 72 Q72 76 60 75 Q48 76 37 72 Z" fill="${c}"/>`,
    front: (c) => `<path d="M38.6 70 Q37 28 60 27 Q83 28 81.4 70 L77 70 Q77.6 54 75.6 46 L44.4 46 Q42.4 54 43 70 Z" fill="${c}"/>`,
  },
  'hair.perm': {
    back: (c) => [
      [40, 44, 8], [80, 44, 8], [38, 58, 7.4], [82, 58, 7.4], [41, 70, 6.6], [79, 70, 6.6], [46, 30, 8], [74, 30, 8], [60, 25, 9],
    ].map(([x, y, r]) => `<circle cx="${x}" cy="${y}" r="${r}" fill="${c}"/>`).join(''),
    front: (c) => `<path d="M43 48 Q42 30 60 29 Q78 30 77 48 Z" fill="${c}"/>
      ${[[46, 46], [52.4, 44.4], [59, 43.6], [65.6, 44.4], [72, 46]].map(([x, y]) => `<circle cx="${x}" cy="${y}" r="3.6" fill="${c}"/>`).join('')}`,
  },
  'hair.long': {
    back: (c) => `<path d="M38 60 Q34 28 60 27 Q86 28 82 60 L86 128 Q60 136 34 128 Z" fill="${c}"/>`,
    front: (c) => `<path d="M39.4 92 Q36 28 60 27 Q84 28 80.6 92 L77 92 Q78 60 76 50 Q70 44 60 42 Q54 46 50 52 Q46 46 45 50 Q42 62 43.4 92 Z" fill="${c}"/>`,
  },
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
  ...MODERN_HAIRS,
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
  /** v·round는 한복 깃, crew는 티셔츠, shirt는 셔츠 깃, suit는 양복 깃. */
  neck: 'v' | 'round' | 'crew' | 'shirt' | 'suit'
  /** 요즘 옷은 여자도 치마를 따로 두르지 않는다. */
  modern?: boolean
  /** 몸 뒤에 그리는 것(후드 모자 등). */
  back?: string
  /** 무늬·띠·흉배처럼 몸통 위에 얹는 것. */
  extra?: (id: string) => string
  defs?: (id: string) => string
}

const TORSO = 'M8 150 Q10 106 38 96 L52 89 L68 89 L82 96 Q110 106 112 150 Z'
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
  'outfit.tee': {
    coat: '#fbfaf5', collar: '#dcd8cc', trim: '#fbfaf5', skirt: '#4f73a8', ribbon: null, neck: 'crew', modern: true,
    extra: () => `<circle cx="74" cy="112" r="4" fill="#d9483b"/><path d="M71.6 112 L76.4 112 M74 109.6 L74 114.4" stroke="#ffffff" stroke-width="1"/>`,
  },
  'outfit.hoodie': {
    coat: '#8fa3bd', collar: '#7d90a8', trim: '#8fa3bd', skirt: '#3c414d', ribbon: null, neck: 'crew', modern: true,
    back: `<path d="M30 104 Q30 80 60 80 Q90 80 90 104 Z" fill="#7d90a8"/>`,
    extra: () => `
      <path d="M55 96 L53.6 114 M65 96 L66.4 114" stroke="#ffffff" stroke-width="1.6" stroke-linecap="round"/>
      <circle cx="53.6" cy="115" r="1.4" fill="#ffffff"/><circle cx="66.4" cy="115" r="1.4" fill="#ffffff"/>
      <path d="M38 134 L82 134 L86 150 L34 150 Z" fill="#7d90a8"/>`,
  },
  'outfit.knit': {
    coat: '#dcc59f', collar: '#ffffff', trim: '#dcc59f', skirt: '#5c5149', ribbon: null, neck: 'shirt', modern: true,
    extra: () => `${[18, 26, 34, 42, 50, 58, 66, 74, 82, 90, 98].map((x) => `<path d="M${x} 116 L${x} 150" stroke="#c9b089" stroke-width="1.2"/>`).join('')}
      <path d="M12 140 L108 140" stroke="#c9b089" stroke-width="3"/>`,
  },
  'outfit.suit': {
    coat: '#2e3648', collar: '#222838', trim: '#ffffff', skirt: '#2e3648', ribbon: null, neck: 'suit', modern: true,
    extra: () => `<path d="M80 112 L88 112 L87 115 L81 115 Z" fill="#ffffff"/><circle cx="62" cy="128" r="1.4" fill="#151a26"/><circle cx="62" cy="140" r="1.4" fill="#151a26"/>`,
  },
  'outfit.dress': {
    coat: '#e2808f', collar: '#ffffff', trim: '#e2808f', skirt: '#e2808f', ribbon: null, neck: 'round', modern: true,
    extra: () => `${[[28, 118], [44, 134], [76, 120], [92, 136], [60, 144], [20, 142], [100, 112]].map(([x, y]) => `<g fill="#fbe3e8">${[0, 72, 144, 216, 288].map((a) => `<circle cx="${x}" cy="${y - 2}" r="1.6" transform="rotate(${a} ${x} ${y})"/>`).join('')}</g><circle cx="${x}" cy="${y}" r="0.9" fill="#f2c84b"/>`).join('')}`,
  },
  'outfit.golf': {
    coat: '#ffffff', collar: '#2c4a7c', trim: '#ffffff', skirt: '#e6d8b8', ribbon: null, neck: 'shirt', modern: true,
    extra: () => `<rect x="9" y="116" width="102" height="4" fill="#2c4a7c"/><rect x="9" y="122" width="102" height="2.4" fill="#d9483b"/>
      <path d="M58 92 L58 104 M62 92 L62 104" stroke="#2c4a7c" stroke-width="0.8"/><circle cx="60" cy="98" r="1" fill="#2c4a7c"/><circle cx="60" cy="102" r="1" fill="#2c4a7c"/>`,
  },
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

// ---- 모자·소품 ----

const HATS: Record<string, string> = {
  'hat.cap': `
    <path d="M42 44 Q41 23 60 22 Q79 23 78 44 Z" fill="#2f4f8f"/>
    <path d="M60 22 L60 44" stroke="#233c6e" stroke-width="0.8"/>
    <path d="M38 44 Q60 39 84 44 Q81 50 60 48 Q42 49 38 44 Z" fill="#233c6e"/>
    <circle cx="60" cy="22.6" r="1.6" fill="#233c6e"/>
    <path d="M55 32 L65 32 L60 38 Z" fill="#ffffff"/>`,
  'hat.beanie': `
    <path d="M41 45 Q40 21 60 20 Q80 21 79 45 Z" fill="#c7473f"/>
    <rect x="39.5" y="38" width="41" height="9" rx="3.5" fill="#a8382f"/>
    ${[44, 49, 54, 59, 64, 69, 74].map((x) => `<path d="M${x + 1} 39 L${x + 1} 46" stroke="#8f2f28" stroke-width="1"/>`).join('')}
    <circle cx="60" cy="18" r="5.4" fill="#ffffff"/>`,
  'hat.glasses': `
    <rect x="47" y="47.6" width="11" height="9" rx="3" fill="#ffffff22" stroke="#2a2220" stroke-width="1.5"/>
    <rect x="62" y="47.6" width="11" height="9" rx="3" fill="#ffffff22" stroke="#2a2220" stroke-width="1.5"/>
    <path d="M58 51 Q60 49.6 62 51 M47 50 L43.4 49 M73 50 L76.6 49" stroke="#2a2220" stroke-width="1.4" fill="none"/>`,
  'hat.sunglasses': `
    <path d="M46.4 48 L58.2 48 Q58.6 57 52.4 57 Q46.2 57 46.4 48 Z" fill="#1c1a1e"/>
    <path d="M73.6 48 L61.8 48 Q61.4 57 67.6 57 Q73.8 57 73.6 48 Z" fill="#1c1a1e"/>
    <path d="M58.2 49 Q60 47.8 61.8 49 M46.4 48.6 L43 47.6 M73.6 48.6 L77 47.6" stroke="#1c1a1e" stroke-width="1.5" fill="none"/>
    <path d="M48.6 50 L51 50 M64 50 L66.4 50" stroke="#ffffff" stroke-width="1.2" opacity="0.6" stroke-linecap="round"/>`,
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
    <path d="M49 29 Q44.6 25 46.6 20.6 Q51.6 21.4 53.6 26 Z" fill="#141414"/>
    <path d="M71 29 Q75.4 25 73.4 20.6 Q68.4 21.4 66.4 26 Z" fill="#141414"/>
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

// ---- 배경 ----

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
  'avatar.cafe': {
    paint: (id) => `${plain('#ead7c0', '#dcc3a6')(id)}
      <defs><pattern id="${id}brick" width="16" height="8" patternUnits="userSpaceOnUse"><path d="M0 7.5 L16 7.5 M8 0 L8 4 M0 4 L16 4 M0 0 L0 4" stroke="#cfb08f" stroke-width="1"/></pattern></defs>
      <rect width="120" height="130" fill="url(#${id}brick)"/>
      ${[24, 96].map((x) => `<path d="M${x} 0 L${x} 14" stroke="#3a2a24" stroke-width="1"/><path d="M${x - 7} 22 L${x - 4} 14 L${x + 4} 14 L${x + 7} 22 Z" fill="#3a2a24"/><circle cx="${x}" cy="24" r="8" fill="#ffe08a" opacity="0.35"/><ellipse cx="${x}" cy="22.6" rx="4" ry="1.6" fill="#ffe08a"/>`).join('')}
      <rect x="0" y="130" width="120" height="20" fill="#7a5640"/>
      <rect x="92" y="104" width="10" height="9" rx="2" fill="#ffffff"/><path d="M102 106 Q106 108 102 111" stroke="#ffffff" stroke-width="1.4" fill="none"/><rect x="86" y="113" width="30" height="3" fill="#5a3a2a"/>`,
  },
  'avatar.beach': {
    paint: (id) => `${plain('#7fcbed', '#d6f1fb')(id)}
      <circle cx="96" cy="22" r="10" fill="#ffe066"/>
      <rect x="0" y="96" width="120" height="20" fill="#2f8fcf"/>
      <path d="M0 102 Q10 99 20 102 T40 102 T60 102 T80 102 T100 102 T120 102" stroke="#bfe9ff" stroke-width="1.4" fill="none"/>
      <path d="M0 116 Q60 108 120 116 L120 150 L0 150 Z" fill="#f3dca8"/>
      <path d="M14 150 Q12 110 20 70" stroke="#8a5a3a" stroke-width="4" fill="none"/>
      ${[-40, -10, 20, 50, 160, 200].map((a) => `<path d="M20 70 Q${20 + 22 * Math.cos((a * Math.PI) / 180)} ${66 + 22 * Math.sin((a * Math.PI) / 180) - 8} ${20 + 30 * Math.cos((a * Math.PI) / 180)} ${70 + 30 * Math.sin((a * Math.PI) / 180)}" stroke="#3f9a4f" stroke-width="5" fill="none" stroke-linecap="round"/>`).join('')}`,
  },
  'avatar.golf': {
    paint: (id) => `${plain('#a9d9f2', '#e2f3fb')(id)}
      ${[0, 18, 36, 54, 72, 90, 108].map((x) => `<ellipse cx="${x + 6}" cy="94" rx="12" ry="9" fill="#3f8a4a"/>`).join('')}
      <rect x="0" y="96" width="120" height="54" fill="#79b85a"/>
      <ellipse cx="80" cy="122" rx="40" ry="12" fill="#9bd27a"/>
      <path d="M98 122 L98 76" stroke="#ffffff" stroke-width="1.6"/><path d="M98 76 L112 81 L98 86 Z" fill="#d9483b"/>
      <ellipse cx="98" cy="122" rx="3" ry="1.2" fill="#2a3a2a"/>
      <path d="M14 22 Q20 14 30 18 Q38 12 44 20 Q50 22 44 26 L16 26 Q10 24 14 22 Z" fill="#ffffff"/>`,
  },
  'avatar.city': {
    paint: (id) => `${plain('#1d1838', '#3a2f6a')(id)}
      <circle cx="94" cy="20" r="7" fill="#fff3b0"/><circle cx="97" cy="18" r="6" fill="#1d1838" opacity="0.9"/>
      ${[[0, 20, 64], [20, 16, 44], [36, 22, 80], [58, 14, 36], [72, 20, 58], [92, 28, 70]].map(([x, w, top]) => `<rect x="${x}" y="${top}" width="${w}" height="${150 - top}" fill="#191530"/>${Array.from({ length: Math.floor((130 - top) / 8) }, (_, r) => Array.from({ length: Math.floor((w - 4) / 6) }, (_, c) => ((x + r * 3 + c * 5) % 7 < 4 ? `<rect x="${x + 3 + c * 6}" y="${top + 5 + r * 8}" width="3" height="4" fill="#ffd86b"/>` : '')).join('')).join('')}`).join('')}
      <rect x="0" y="136" width="120" height="14" fill="#2b2450"/>`,
  },
  'avatar.basic': {
    paint: (id) => `${plain('#fbf3e4', '#f1e4cc')(id)}
      <rect x="76" y="16" width="34" height="34" rx="2" fill="#ffffff"/><rect x="79" y="19" width="28" height="28" fill="#bfe3f5"/>
      <path d="M93 19 L93 47 M79 33 L107 33" stroke="#ffffff" stroke-width="2"/>
      <rect x="0" y="128" width="120" height="22" fill="#dcb98c"/><path d="M0 128 L120 128 M0 139 L120 139 M30 128 L30 139 M78 139 L78 150" stroke="#c9a57a" stroke-width="1"/>
      <path d="M6 128 L8 108 L22 108 L24 128 Z" fill="#c77b4c"/>
      <ellipse cx="11" cy="100" rx="6" ry="9" fill="#5e9c5a" transform="rotate(-20 11 100)"/><ellipse cx="20" cy="98" rx="6" ry="10" fill="#4f8c4c" transform="rotate(18 20 98)"/>`,
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

const OUTLINE: Record<Skin, string> = {
  anime: 'stroke="#5a3a36" stroke-width="0.75" stroke-opacity="0.8" stroke-linejoin="round"',
  joseon: 'stroke="#3a2a20" stroke-width="1.15" stroke-linejoin="round"',
}

let uid = 0

/** 그림체 캐릭터 한 장. head면 얼굴만 둥글게 잘라 작은 자리(머리글 등)에 쓴다. */
export function paintSvg(l: Look, skin: Skin, opts: { head?: boolean } = {}): string {
  const body = BODIES[l.body] ?? BODIES['body.man']
  const outfit = OUTFITS[l.outfit] ?? OUTFITS['outfit.tee']
  const hair = HAIRS[l.hair] ?? HAIRS['hair.short']
  const hairColor = body.hair
  const beard = body.woman || body.animal ? '' : (BEARDS[l.beard] ?? BEARDS['beard.none'])(body.elder ? '#eeeae2' : hairColor)
  const hat = HATS[l.hat] ?? ''
  const bg = BACKDROPS[l.background] ?? BACKDROPS['avatar.basic']
  const id = `pt${++uid}`

  const neck =
    outfit.neck === 'round'
      ? `<path d="M46 90 Q60 104 74 90" stroke="${outfit.trim}" stroke-width="2.4" fill="none"/>
         <path d="M45 89 Q60 106 75 89" stroke="${outfit.collar}" stroke-width="3.4" fill="none" transform="translate(0 2.4)"/>`
      : outfit.neck === 'crew'
        ? `<path d="M50 89.6 Q60 99 70 89.6" stroke="${outfit.collar}" stroke-width="3" fill="none"/>`
        : outfit.neck === 'shirt'
          ? `<path d="M50.6 89 L58.6 98 L51 100 Z M69.4 89 L61.4 98 L69 100 Z" fill="${outfit.collar}"/>`
          : outfit.neck === 'suit'
            ? `<path d="M52 89 L60 110 L68 89 Z" fill="#ffffff"/>
               <path d="M58.4 92 L61.6 92 L62.6 108 L60 112 L57.4 108 Z" fill="#b8323a"/><path d="M58 90 L62 90 L61.6 93 L58.4 93 Z" fill="#8f2229"/>
               <path d="M52 89 L60 114 L50 104 L44 93 Z M68 89 L60 114 L70 104 L76 93 Z" fill="${outfit.collar}"/>`
            : `<path d="M52 89 L60 104 L68 89 Z" fill="#f7f2e8"/>
               <path d="M68 89 L57.6 104.6" stroke="${outfit.collar}" stroke-width="6" stroke-linecap="square"/>
               <path d="M52 89 L73 124" stroke="${outfit.collar}" stroke-width="6.4" stroke-linecap="square"/>
               <path d="M51 90.5 L69.6 121.6" stroke="${outfit.trim}" stroke-width="2.2"/>`
  const short = body.woman && !outfit.modern
  const skirt = short ? `<path d="M12 150 Q14 128 26 123 L94 123 Q106 128 108 150 Z" fill="${outfit.skirt}"/>` : ''
  const ribbon = outfit.ribbon
    ? `<path d="M66 113 Q58.5 108.5 57 113.5 Q59 118 66 115 Z M66 113 Q73.5 108.5 75 113.5 Q73 118 66 115 Z" fill="${outfit.ribbon}"/>
       <path d="M65 115 L60.5 ${short ? 140 : 146} L64 ${short ? 140 : 146} L67 115.6 Z M67 115 L73 ${short ? 138 : 144} L76.5 ${short ? 137 : 143} L68.6 114.8 Z" fill="${outfit.ribbon}"/>`
    : ''
  const coat = short
    ? `<path d="M8 150 Q10 106 38 96 L52 89 L68 89 L82 96 Q110 106 112 150 L112 126 L8 126 Z" fill="${outfit.coat}"/>`
    : `<path d="${TORSO}" fill="${outfit.coat}"/>`
  const shade = skin === 'anime' ? `<path d="M54 70 Q60 78 66 70 L66 78 Q60 82 54 78 Z" fill="${body.shade}"/>` : ''
  const hairShine =
    skin === 'anime' && !body.animal ? `<path d="M47 36 Q60 30 73 36" stroke="#ffffff" stroke-width="2.2" fill="none" opacity="0.28" stroke-linecap="round"/>` : ''

  const parts = [
    `<defs>${outfit.defs?.(id) ?? ''}<clipPath id="${id}c">${opts.head ? '<circle cx="60" cy="50" r="27"/>' : '<rect width="120" height="150" rx="14"/>'}</clipPath></defs>`,
    `<g clip-path="url(#${id}c)">`,
    bg.paint(id),
    skin === 'joseon' ? `<rect width="120" height="150" fill="#f3e2bd" opacity="0.14"/>` : '',
    `<g ${OUTLINE[skin]}>`,
    body.animal ? '' : hair.back(hairColor),
    // 애니메이션풍은 어깨를 좁히고 몸을 올려 목을 짧게 한다.
    skin === 'anime' ? `<g transform="translate(60 -4) scale(0.8 1) translate(-60 0)">` : '<g>',
    outfit.back ?? '',
    coat,
    skirt,
    `<g stroke="none">${outfit.extra?.(id) ?? ''}</g>`,
    `<path d="M54 64 L54 92 L66 92 L66 64 Z" fill="${body.skin}"/>`,
    `<g stroke="none">${shade}${neck}${ribbon}</g>`,
    skin === 'anime' ? `<path d="M30 112 Q28 130 29 150 M90 112 Q92 130 91 150" stroke="#00000024" stroke-width="1.4" fill="none"/>` : '',
    `</g>`,
    faceShape(body, skin),
    `</g>`,
    features(body, skin, id),
    `<g ${OUTLINE[skin]}>`,
    beard,
    body.animal ? '' : hair.front(hairColor),
    `</g>`,
    hairShine,
    `<g ${OUTLINE[skin]}>${hat}</g>`,
    bg.seal && skin === 'joseon' && !opts.head
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
