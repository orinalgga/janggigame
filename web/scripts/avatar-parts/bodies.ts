/** 몸 상품: 맨몸(body), 머리(head), 얼굴(face), 동물 꼬리(back). */
import { LINE, capsule, circle, ellipse, mirror, shape, tone, write } from './lib'
import { ARMS, EARS, EYE_L, EYE_R, FACE, FACE_LIGHT, FACE_SHADE, FEET, HANDS, LEGS, NECK, TORSO } from './rig'

interface Person {
  skin: string
  woman: boolean
  elder: boolean
  /** 눈썹 색. */
  brow: string
}

const PEOPLE: Record<string, Person> = {
  'body.man': { skin: '#ffe0c7', woman: false, elder: false, brow: '#3a2a22' },
  'body.woman': { skin: '#ffe6d3', woman: true, elder: false, brow: '#4a3226' },
  'body.elder': { skin: '#fbdcc2', woman: false, elder: true, brow: '#b9b2aa' },
  'body.elder.woman': { skin: '#ffe2cc', woman: true, elder: true, brow: '#c2bbb3' },
}

const shadow = `<ellipse cx="120" cy="272" rx="50" ry="8" fill="#00000024"/>`

/** 맨몸: 그림자, 목, 몸통, 팔, 손, 다리, 발. 옷이 덮지 않는 곳만 보인다. */
function limbs(skin: string, limb = skin) {
  return (
    shadow +
    LEGS.map((d) => shape({ d, fill: limb, box: [100, 222, 40, 40] })).join('') +
    FEET.map((d) => shape({ d, fill: limb, box: [96, 256, 48, 16] })).join('') +
    shape({ d: TORSO, fill: skin, box: [88, 170, 64, 56] }) +
    ARMS.map((d) => shape({ d, fill: limb, box: [70, 176, 100, 46] })).join('') +
    HANDS.map((d) => shape({ d, fill: limb, box: [68, 212, 104, 18] })).join('') +
    shape({ d: NECK, fill: skin, shade: 'M108 154 L132 154 L132 166 Q120 172 108 166 Z' })
  )
}

function head(skin: string) {
  return (
    EARS.map((d) => shape({ d, fill: skin, shade: '' })).join('') +
    shape({ d: FACE, fill: skin, shade: FACE_SHADE, light: FACE_LIGHT })
  )
}

/** 큰 눈: 짙은 눈동자, 아래로 밝아지는 홍채, 반짝임 두 개, 굵은 윗눈썹선. */
function eye([x, y]: readonly [number, number], woman: boolean, iris: string, flip: number) {
  const g = `ir${x}`
  const ry = woman ? 11 : 10
  return (
    `<defs><linearGradient id="${g}" x1="0" y1="0" x2="0" y2="1"><stop offset="0.35" stop-color="#2a1e24"/><stop offset="1" stop-color="${iris}"/></linearGradient></defs>` +
    `<ellipse cx="${x}" cy="${y}" rx="7.4" ry="${ry}" fill="url(#${g})"/>` +
    `<ellipse cx="${x - 2.4}" cy="${y - 4}" rx="2.9" ry="3.6" fill="#fff"/>` +
    `<circle cx="${x + 2.6}" cy="${y + 4.6}" r="1.5" fill="#fff" opacity="0.9"/>` +
    `<path d="M${x - 9} ${y - ry + 3} Q${x} ${y - ry - 4} ${x + 9} ${y - ry + 2}" fill="none" stroke="${LINE}" stroke-width="${woman ? 3.4 : 3}" stroke-linecap="round"/>` +
    (woman
      ? `<path d="M${x + 8.4 * flip} ${y - ry + 2.4} l${4 * flip} -3.6 M${x + 8.8 * flip} ${y - ry + 5.4} l${4.4 * flip} -1.6" stroke="${LINE}" stroke-width="2" stroke-linecap="round"/>`
      : '')
  )
}

function face(p: Person) {
  const eyes = p.elder
    ? [EYE_L, EYE_R]
        .map(([x, y]) => `<path d="M${x - 8} ${y + 1} Q${x} ${y - 8} ${x + 8} ${y + 1}" fill="none" stroke="${LINE}" stroke-width="3" stroke-linecap="round"/>`)
        .join('') +
      `<path d="M84 132 q4 3 8 1 M148 133 q4 2 8 -1" stroke="${tone(p.skin, -0.3)}" stroke-width="1.4" fill="none" stroke-linecap="round"/>`
    : eye(EYE_L, p.woman, p.woman ? '#a0603a' : '#6a5040', -1) + eye(EYE_R, p.woman, p.woman ? '#a0603a' : '#6a5040', 1)
  const brows = `<path d="M90 103 Q100 99 109 102 M131 102 Q140 99 150 103" stroke="${p.brow}" stroke-width="${p.woman ? 2.4 : 3.4}" fill="none" stroke-linecap="round"/>`
  const blush = `<ellipse cx="86" cy="139" rx="9" ry="5" fill="#ff8f9c" opacity="0.4"/><ellipse cx="154" cy="139" rx="9" ry="5" fill="#ff8f9c" opacity="0.4"/>`
  const nose = `<path d="M121 133 q-1.6 2.4 0.4 3" stroke="${tone(p.skin, -0.3)}" stroke-width="1.6" fill="none" stroke-linecap="round"/>`
  const mouth = p.woman
    ? `<path d="M114 146 Q120 152 126 146 Q120 148 114 146 Z" fill="#e0646a" stroke="#a8404a" stroke-width="1.4" stroke-linejoin="round"/>`
    : `<path d="M114 146 Q120 151 126 146" fill="none" stroke="#9a4a44" stroke-width="2.2" stroke-linecap="round"/>`
  return brows + eyes + blush + nose + mouth
}

// ---- 동물 ----

interface Beast {
  fur: string
  inner: string
  ears: (fur: string, inner: string) => string
  /** 얼굴 무늬(얼굴 안으로 잘린다). */
  marks?: string
  tail: (fur: string, inner: string) => string
  /** 팔·다리 색(판다). 없으면 털색. */
  paw?: string
  woman?: boolean
}

const pointy = (fur: string, inner: string, tilt = 0) => {
  const d = `M74 80 L70 ${36 - tilt} L104 62 Z`
  return (
    shape({ d, fill: fur, shade: '' }) +
    `<path d="M78 74 L76 ${48 - tilt} L96 64 Z" fill="${inner}"/>` +
    shape({ d: mirror(d), fill: fur, shade: '' }) +
    `<path d="${mirror(`M78 74 L76 ${48 - tilt} L96 64 Z`)}" fill="${inner}"/>`
  )
}
const round = (fur: string, inner: string) =>
  [circle(78, 66, 15), circle(162, 66, 15)].map((d) => shape({ d, fill: fur, shade: '' })).join('') +
  `<circle cx="78" cy="67" r="7.5" fill="${inner}"/><circle cx="162" cy="67" r="7.5" fill="${inner}"/>`

const BEASTS: Record<string, Beast> = {
  'body.cat': {
    fur: '#f6b866',
    inner: '#fff1de',
    ears: (f, i) => pointy(f, i),
    marks: `<path d="M108 58 L112 74 L116 58 Z M120 56 L120 72 M128 58 L124 74" fill="#d98a3c" stroke="#d98a3c" stroke-width="3" stroke-linecap="round"/>`,
    tail: (f) => shape({ d: 'M150 236 Q186 236 190 206 Q194 186 182 180 Q186 200 176 214 Q166 226 148 226 Z', fill: f, shade: '' }),
  },
  'body.dog': {
    fur: '#e8cfa8',
    inner: '#fffaf1',
    ears: (f) =>
      [`M70 74 Q56 74 52 104 Q50 124 62 128 Q74 120 80 84 Z`, mirror('M70 74 Q56 74 52 104 Q50 124 62 128 Q74 120 80 84 Z')]
        .map((d) => shape({ d, fill: tone(f, -0.28), shade: '' }))
        .join(''),
    tail: (f) => shape({ d: 'M150 226 Q176 222 182 198 Q186 188 178 186 Q172 206 150 214 Z', fill: f, shade: '' }),
  },
  'body.rabbit': {
    fur: '#fbf7f5',
    inner: '#f8c8d2',
    woman: true,
    ears: (f, i) =>
      [capsule(96, 70, 88, 10, 12, 11), capsule(144, 70, 152, 10, 12, 11)].map((d) => shape({ d, fill: f, shade: '' })).join('') +
      [capsule(96, 64, 89, 18, 6, 5.5), capsule(144, 64, 151, 18, 6, 5.5)].map((d) => `<path d="${d}" fill="${i}"/>`).join(''),
    tail: (f) => shape({ d: circle(150, 236, 11), fill: f, shade: '' }),
  },
  'body.bear': {
    fur: '#a87a52',
    inner: '#e9cca8',
    ears: round,
    tail: (f) => shape({ d: circle(148, 236, 8), fill: f, shade: '' }),
  },
  'body.fox': {
    fur: '#f08a3c',
    inner: '#fff6ec',
    woman: true,
    ears: (f, i) => pointy(f, i, 8),
    marks: `<path d="M64 120 Q92 128 106 150 Q118 168 120 170 L64 170 Z M176 120 Q148 128 134 150 Q122 168 120 170 L176 170 Z" fill="#fff6ec"/>`,
    tail: (f, i) =>
      shape({ d: 'M148 232 Q196 236 200 196 Q202 170 184 164 Q190 190 176 206 Q164 220 146 220 Z', fill: f, shade: '' }) +
      `<path d="M184 164 Q202 170 200 196 Q190 186 186 176 Z" fill="${i}"/>`,
  },
  'body.panda': {
    fur: '#ffffff',
    inner: '#ffffff',
    paw: '#2a2426',
    ears: () => round('#2a2426', '#2a2426'),
    marks: `<ellipse cx="100" cy="124" rx="14" ry="17" fill="#2a2426" transform="rotate(20 100 124)"/><ellipse cx="140" cy="124" rx="14" ry="17" fill="#2a2426" transform="rotate(-20 140 124)"/>`,
    tail: (f) => shape({ d: circle(148, 236, 8), fill: f, shade: '' }),
  },
  'body.tiger': {
    fur: '#f6a640',
    inner: '#fff7e8',
    ears: round,
    marks:
      `<path d="M112 58 L116 76 L120 58 Z M120 58 L124 76 L128 58 Z" fill="#3a2a22"/>` +
      `<path d="M64 100 L84 106 L64 112 Z M64 124 L80 128 L64 134 Z M176 100 L156 106 L176 112 Z M176 124 L160 128 L176 134 Z" fill="#3a2a22"/>`,
    tail: (f) =>
      shape({ d: 'M150 236 Q186 236 190 206 Q194 186 182 180 Q186 200 176 214 Q166 226 148 226 Z', fill: f, shade: '' }) +
      `<path d="M176 214 l8 6 M184 198 l9 2 M168 222 l4 8" stroke="#3a2a22" stroke-width="4" stroke-linecap="round"/>`,
  },
}

function beastFace(b: Beast) {
  const iris = '#5a3e30'
  return (
    `<clipPath id="fm"><path d="${FACE}"/></clipPath><g clip-path="url(#fm)">${b.marks ?? ''}</g>` +
    eye([100, 120], !!b.woman, iris, -1) +
    eye([140, 120], !!b.woman, iris, 1) +
    `<ellipse cx="120" cy="146" rx="21" ry="15" fill="${b.inner}"/>` +
    `<path d="M112 138 Q120 134 128 138 Q126 145 120 146 Q114 145 112 138 Z" fill="#3a2a2a"/>` +
    `<path d="M120 146 L120 151 M112 152 Q116 156 120 151 Q124 156 128 152" fill="none" stroke="#3a2a2a" stroke-width="2" stroke-linecap="round"/>` +
    `<ellipse cx="84" cy="140" rx="8" ry="4.5" fill="#ff8f9c" opacity="0.45"/><ellipse cx="156" cy="140" rx="8" ry="4.5" fill="#ff8f9c" opacity="0.45"/>`
  )
}

export function bodies() {
  for (const [id, p] of Object.entries(PEOPLE)) {
    write(id, 'body', limbs(p.skin))
    write(id, 'head', head(p.skin))
    write(id, 'face', face(p))
  }
  for (const [id, b] of Object.entries(BEASTS)) {
    write(id, 'back', b.tail(b.fur, b.inner))
    write(id, 'body', limbs(b.fur, b.paw ?? b.fur))
    write(id, 'head', b.ears(b.fur, b.inner) + shape({ d: FACE, fill: b.fur, shade: FACE_SHADE, light: FACE_LIGHT }))
    write(id, 'face', beastFace(b))
  }
}

/** 작가용 뼈대 안내 그림(docs/avatar-rig.svg): 기본 남자 몸 위에 기준선을 그린다. */
export function rigGuide(): string {
  const p = PEOPLE['body.man']
  const guide = (d: string, c = '#e0457b') => `<path d="${d}" stroke="${c}" stroke-width="1" stroke-dasharray="4 3" fill="none"/>`
  const label = (x: number, y: number, t: string) => `<text x="${x}" y="${y}" font-size="8" fill="#e0457b" font-family="sans-serif">${t}</text>`
  return (
    `<rect width="240" height="300" fill="#fbf6ed"/>` +
    limbs(p.skin) +
    head(p.skin) +
    face(p) +
    guide('M0 270 L240 270') + label(4, 266, '발 끝 y 270') +
    guide('M120 0 L120 300') + label(124, 10, '가운데 x 120') +
    guide('M0 112 L240 112') + label(4, 108, '얼굴 가운데 y 112') +
    guide('M64 56 L176 56 L176 166 L64 166 Z', '#3b82f6') + label(178, 60, '얼굴 64~176 × 56~166') +
    guide('M0 122 L240 122', '#22a06b') + label(178, 132, '눈 y 122 (x 100, 140)') +
    guide('M0 176 L240 176', '#22a06b') + label(178, 186, '어깨 y 176') +
    guide('M0 224 L240 224', '#22a06b') + label(178, 234, '허리 y 224') +
    `<circle cx="120" cy="110" r="70" stroke="#f59e0b" stroke-width="1" stroke-dasharray="4 3" fill="none"/>` + label(4, 46, '머리글 얼굴 원 (120,110) r 70')
  )
}
