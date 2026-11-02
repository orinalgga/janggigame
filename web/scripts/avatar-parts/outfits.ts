/** 옷 상품: 옷(outfit)과 등 뒤로 넘어가는 것(back, 후드 모자). 몸 그림(rig) 위에 덮는다. */
import { LINE, capsule, circle, ellipse, shape, tone, write } from './lib'

// ---- 조각 ----

const TOP = 'M89 175 Q120 166 151 175 Q157 179 156 190 L153 230 Q120 235 87 230 L84 190 Q83 179 89 175 Z'
const SHORT_TOP = 'M89 175 Q120 166 151 175 Q157 179 156 190 L154 204 Q120 210 86 204 L84 190 Q83 179 89 175 Z'
const ROBE = 'M89 175 Q120 166 151 175 Q157 179 157 192 L164 262 Q120 270 76 262 L83 192 Q83 179 89 175 Z'
const PANTS = 'M88 220 L152 220 L153 256 Q142 261 130 258 L121 236 L119 236 L110 258 Q98 261 87 256 Z'
const SHORTS = 'M88 220 L152 220 L153 240 Q142 244 128 241 L121 232 L119 232 L112 241 Q98 244 87 240 Z'
const BAJI = 'M86 218 L154 218 Q162 244 147 256 L133 259 L121 238 L119 238 L107 259 L93 256 Q78 244 86 218 Z'
const SKIRT = 'M88 216 L152 216 L161 250 Q120 259 79 250 Z'
const CHIMA = 'M90 194 L150 194 Q170 236 169 268 Q120 278 71 268 Q70 236 90 194 Z'

const sleeve = {
  long: [capsule(91, 180, 79.6, 211, 9.6, 8.2), capsule(149, 180, 160.4, 211, 9.6, 8.2)],
  short: [capsule(91, 180, 86, 195, 9.8, 9.4), capsule(149, 180, 154, 195, 9.8, 9.4)],
  wide: [
    'M90 176 Q76 184 72 206 Q70 220 80 220 L96 218 L99 192 Z',
    'M150 176 Q164 184 168 206 Q170 220 160 220 L144 218 L141 192 Z',
  ],
}

const shoes = (c: string, sole = tone(c, -0.35)) =>
  [ellipse(107, 264, 12.5, 7.6), ellipse(133, 264, 12.5, 7.6)].map((d) => shape({ d, fill: c, shade: '' })).join('') +
  `<path d="M95 266 Q107 272 119.5 266 M120.5 266 Q133 272 145.5 266" stroke="${sole}" stroke-width="2.4" fill="none" stroke-linecap="round"/>`

const top = (c: string, d = TOP) => shape({ d, fill: c, box: [84, 168, 72, 64], light: 'M92 182 Q100 176 112 176 Q98 184 96 200 Z' })
const sleeves = (c: string, kind: keyof typeof sleeve) => sleeve[kind].map((d) => shape({ d, fill: c, box: [68, 176, 104, 46] })).join('')
const lower = (c: string, d: string) => shape({ d, fill: c, box: [80, 216, 80, 56] })

/** 둥근 목둘레 선. */
const crew = (c: string) => `<path d="M106 174 Q120 186 134 174" fill="none" stroke="${c}" stroke-width="4" stroke-linecap="round"/>`
/** 한복 깃: 왼쪽 위에서 오른쪽 아래로 여민 깃과 흰 동정. */
const git = (c: string, len = 204) =>
  `<path d="M133 171 L106 ${len - 4}" stroke="${c}" stroke-width="9" stroke-linecap="round"/>` +
  `<path d="M107 171 L120 188" stroke="${c}" stroke-width="8" stroke-linecap="round"/>` +
  `<path d="M131 170 L109 ${len - 8}" stroke="#ffffff" stroke-width="3" stroke-linecap="round"/>` +
  `<path d="M108 170 L119 185" stroke="#ffffff" stroke-width="3" stroke-linecap="round"/>`
/** 고름: 가슴께에 맨 리본과 늘어진 끈. */
const goreum = (c: string, y = 196, len = 24) =>
  shape({ d: `M122 ${y} Q112 ${y - 7} 108 ${y + 1} Q114 ${y + 7} 122 ${y + 2} Z`, fill: c, shade: '', sw: 1.6 }) +
  shape({ d: `M122 ${y + 1} L116 ${y + len} L121 ${y + len} L125 ${y + 2} Z M124 ${y + 1} L130 ${y + len - 4} L135 ${y + len - 6} L127 ${y} Z`, fill: c, shade: '', sw: 1.6 })
/** 단령(둥근 깃). */
const danryeong = (c: string) =>
  `<path d="M104 173 Q120 192 136 173" fill="none" stroke="${c}" stroke-width="7" stroke-linecap="round"/>` +
  `<path d="M107 172 Q120 186 133 172" fill="none" stroke="#ffffff" stroke-width="2.4" stroke-linecap="round"/>`
const belt = (c: string, y = 208, h = 7) => shape({ d: `M84 ${y} L156 ${y} L156.4 ${y + h} L83.6 ${y + h} Z`, fill: c, shade: '', sw: 2 })
/** 소매 끝 띠: 소매 끝 한 마디를 다른 색으로. */
const cuffs = (c: string, kind: 'long' | 'wide' | 'short') => {
  if (kind === 'wide') return [[78, 216], [162, 216]].map(([x, y]) => `<path d="M${x - 9} ${y - 1} L${x + 9} ${y + 1}" stroke="${c}" stroke-width="5" stroke-linecap="round"/>`).join('')
  const [a, b] = kind === 'long' ? [[81.4, 206, 79.6, 211, 8.4, 8.2], [158.6, 206, 160.4, 211, 8.4, 8.2]] : [[87, 191, 86, 195, 9.5, 9.4], [153, 191, 154, 195, 9.5, 9.4]]
  return [a, b].map(([x1, y1, x2, y2, r1, r2]) => shape({ d: capsule(x1, y1, x2, y2, r1, r2), fill: c, shade: '', sw: 2 })).join('')
}
/** 흉배: 가슴의 네모난 수 장식. */
const badge = (bg: string, motif: string) =>
  shape({ d: 'M107 188 L133 188 L133 210 L107 210 Z', fill: bg, shade: '', sw: 1.8 }) + motif
const studs = (c: string, area: [number, number, number, number], step = 9) => {
  const [x0, y0, x1, y1] = area
  let out = ''
  for (let y = y0; y <= y1; y += step) for (let x = x0 + ((y / step) % 2 ? step / 2 : 0); x <= x1; x += step) out += `<circle cx="${x}" cy="${y}" r="1.7" fill="${c}"/>`
  return out
}
const clip = (id: string, d: string, inner: string) => `<clipPath id="${id}"><path d="${d}"/></clipPath><g clip-path="url(#${id})">${inner}</g>`

// ---- 상품 ----

type Maker = () => string

const OUTFITS: Record<string, { outfit: Maker; woman?: Maker; back?: Maker }> = {
  'outfit.tee': {
    outfit: () => lower('#4f7bb8', PANTS) + `<path d="M120 222 L120 236" stroke="${tone('#4f7bb8', -0.3)}" stroke-width="1.6"/>` + shoes('#ffffff', '#c9c9c9') + sleeves('#ffffff', 'short') + top('#ffffff') + crew('#d8d8d8'),
  },
  'outfit.hoodie': {
    back: () => shape({ d: 'M86 182 Q86 156 120 152 Q154 156 154 182 Z', fill: '#7d93b4', shade: '' }),
    outfit: () =>
      lower('#3c4150', PANTS) +
      `<path d="M98 252 L120 252 M120 252 L142 252" stroke="#2c303a" stroke-width="4"/>` +
      shoes('#f2f2f2', '#9aa0aa') +
      sleeves('#8aa1c2', 'long') + cuffs('#7a90b0', 'long') + top('#8aa1c2') +
      shape({ d: 'M100 208 L140 208 L143 226 L97 226 Z', fill: '#7d93b4', shade: '', sw: 1.8 }) +
      `<path d="M106 174 Q120 184 134 174" fill="none" stroke="#6c82a4" stroke-width="5" stroke-linecap="round"/>` +
      `<path d="M114 180 L113 196 M126 180 L127 196" stroke="#ffffff" stroke-width="2" stroke-linecap="round"/>`,
  },
  'outfit.knit': {
    outfit: () =>
      lower('#7a6a5c', PANTS) +
      shoes('#5b3b2a') +
      sleeves('#e8d3ae', 'long') + cuffs('#d6bd92', 'long') + top('#e8d3ae') +
      clip('kn', TOP, [96, 104, 112, 128, 136, 144].map((x) => `<path d="M${x} 186 L${x} 228" stroke="#d6bd92" stroke-width="2"/>`).join('')) +
      `<path d="M108 171 L120 182 L132 171 L128 186 L120 182 L112 186 Z" fill="#ffffff" stroke="${LINE}" stroke-width="1.6" stroke-linejoin="round"/>`,
  },
  'outfit.suit': {
    outfit: () =>
      lower('#2b3a5c', PANTS) +
      `<path d="M120 222 L120 238" stroke="#1d2840" stroke-width="1.6"/>` +
      shoes('#2a2222', '#111') +
      sleeves('#33456b', 'long') + cuffs('#ffffff', 'long') + top('#33456b') +
      `<path d="M108 172 L120 204 L132 172 Z" fill="#ffffff"/>` +
      shape({ d: 'M117.5 178 L122.5 178 L124 198 L120 205 L116 198 Z', fill: '#c4343c', shade: '', sw: 1.6 }) +
      shape({ d: 'M116 172 L124 172 L123 179 L117 179 Z', fill: '#a3262e', shade: '', sw: 1.6 }) +
      shape({ d: 'M106 172 L120 210 L104 196 L98 180 Z', fill: '#283858', shade: '', sw: 1.8 }) +
      shape({ d: 'M134 172 L120 210 L136 196 L142 180 Z', fill: '#283858', shade: '', sw: 1.8 }) +
      `<circle cx="120" cy="216" r="2" fill="#1b2438"/><path d="M132 188 L142 188" stroke="#ffffff" stroke-width="2.4" stroke-linecap="round"/>`,
  },
  'outfit.dress': {
    outfit: () =>
      shoes('#f07c98') +
      shape({ d: 'M86 208 L154 208 L165 252 Q120 262 75 252 Z', fill: '#f4a3b8', box: [75, 208, 90, 50] }) +
      clip('fl', 'M86 208 L154 208 L165 252 Q120 262 75 252 Z', [[90, 222], [112, 236], [136, 226], [150, 244], [100, 248], [126, 250]].map(([x, y]) => `<g fill="#ffffff" opacity="0.9">${[0, 72, 144, 216, 288].map((a) => `<circle cx="${(x + 3.2 * Math.cos((a * Math.PI) / 180)).toFixed(1)}" cy="${(y + 3.2 * Math.sin((a * Math.PI) / 180)).toFixed(1)}" r="2.4"/>`).join('')}</g><circle cx="${x}" cy="${y}" r="1.6" fill="#f7d35a"/>`).join('')) +
      sleeves('#f4a3b8', 'short') + top('#f4a3b8', SHORT_TOP.replace('L154 204 Q120 210 86 204', 'L154 212 Q120 216 86 212')) +
      `<path d="M104 173 Q120 190 136 173" fill="#ffffff" stroke="${LINE}" stroke-width="1.8"/>` +
      shape({ d: 'M86 206 L154 206 L154 212 L86 212 Z', fill: '#e07b96', shade: '', sw: 1.6 }),
  },
  'outfit.golf': {
    outfit: () =>
      lower('#d9c49a', PANTS) +
      shoes('#ffffff', '#b8b8b8') +
      sleeves('#ffffff', 'short') +
      cuffs('#2d3f73', 'short') + top('#ffffff') +
      clip('gf', TOP, `<path d="M80 196 L160 196" stroke="#2d3f73" stroke-width="5"/><path d="M80 204 L160 204" stroke="#c8323a" stroke-width="3"/>`) +
      `<path d="M106 171 L120 180 L134 171 L131 182 L120 180 L109 182 Z" fill="#ffffff" stroke="${LINE}" stroke-width="1.6" stroke-linejoin="round"/>` +
      `<path d="M120 180 L120 192" stroke="#c9c9c9" stroke-width="1.6"/><circle cx="120" cy="186" r="1.2" fill="#999"/>`,
  },
  'outfit.plain': {
    outfit: () => lower('#efe6d2', BAJI) + `<path d="M93 254 L107 254 M133 254 L147 254" stroke="#c9bb9c" stroke-width="3"/>` + shoes('#f4efe4', '#bfb39a') + sleeves('#f6f0e2', 'long') + top('#f6f0e2') + git('#c9bb9c') + goreum('#efe6d2', 200),
    woman: () => shoes('#f4efe4', '#bfb39a') + lower('#e2414a', CHIMA) + sleeves('#f7d34c', 'long') + cuffs('#e2414a', 'long') + top('#f7d34c', SHORT_TOP) + git('#a7343a', 200) + goreum('#e2414a', 192, 30),
  },
  'outfit.durumagi': {
    outfit: () => shoes('#f4efe4', '#bfb39a') + lower('#7a3a6a', ROBE) + sleeves('#7a3a6a', 'wide') + git('#5a2a4e', 226) + goreum('#5a2a4e', 204, 26),
  },
  'outfit.dopo': {
    outfit: () =>
      shoes('#2e2a28') +
      lower('#cfe5e2', ROBE) +
      sleeves('#cfe5e2', 'wide') +
      git('#2e3c50', 226) +
      belt('#3d5a8a', 210, 4) +
      `<path d="M126 214 L124 244 M130 214 L131 240" stroke="#3d5a8a" stroke-width="3" stroke-linecap="round"/><circle cx="124" cy="246" r="3" fill="#3d5a8a"/><circle cx="131" cy="242" r="3" fill="#3d5a8a"/>`,
  },
  'outfit.cheollik': {
    outfit: () => shoes('#2e2a28') + lower('#2e3c5c', ROBE) + clip('ch', ROBE, studs('#3a4a6c', [80, 222, 160, 266], 6)) + sleeves('#2e3c5c', 'wide') + git('#1d2840', 220) + belt('#c8323a', 206, 8),
  },
  'outfit.dangui': {
    outfit: () =>
      shoes('#f4efe4', '#bfb39a') +
      lower('#d8323c', CHIMA) +
      clip('dc', CHIMA, `<path d="M70 256 Q120 264 170 256" stroke="#e8b83f" stroke-width="5" fill="none"/>`) +
      sleeves('#3f8a52', 'long') +
      cuffs('#ffffff', 'long') + top('#3f8a52', 'M89 175 Q120 166 151 175 Q157 179 156 190 L160 232 Q140 226 120 236 Q100 226 80 232 L84 190 Q83 179 89 175 Z') +
      clip('dg', 'M89 175 Q120 166 151 175 Q157 179 156 190 L160 232 Q140 226 120 236 Q100 226 80 232 L84 190 Q83 179 89 175 Z', studs('#e8c25a', [86, 190, 156, 230], 10)) +
      git('#7a2a46', 204) +
      goreum('#7a2a46', 196, 28),
  },
  'outfit.gwanbok': {
    outfit: () =>
      shoes('#2e2a28') +
      lower('#b8323a', ROBE) +
      sleeves('#b8323a', 'wide') +
      danryeong('#8e2228') +
      badge('#2d3f73', `<path d="M112 202 Q116 194 120 198 Q124 194 128 202 M114 196 L110 192 M126 196 L130 192" stroke="#ffffff" stroke-width="2" fill="none" stroke-linecap="round"/><circle cx="120" cy="198" r="1.6" fill="#e8b83f"/>`) +
      belt('#e8b83f', 214, 6) +
      `<circle cx="100" cy="217" r="2" fill="#b8323a"/><circle cx="140" cy="217" r="2" fill="#b8323a"/>`,
  },
  'outfit.armor': {
    outfit: () =>
      shoes('#2e2a28') +
      lower('#4a3a6a', ROBE) +
      clip('am', ROBE, studs('#e8c25a', [82, 186, 158, 262], 8)) +
      sleeves('#4a3a6a', 'wide') +
      clip('as1', sleeve.wide[0], studs('#e8c25a', [72, 186, 98, 220], 8)) +
      clip('as2', sleeve.wide[1], studs('#e8c25a', [142, 186, 168, 220], 8)) +
      danryeong('#e8b83f') +
      belt('#2a2226', 210, 8) +
      `<circle cx="120" cy="214" r="5" fill="#e8b83f" stroke="${LINE}" stroke-width="1.6"/>`,
  },
  'outfit.wonsam': {
    outfit: () =>
      shoes('#f4efe4', '#bfb39a') +
      lower('#d8323c', CHIMA) +
      lower('#2f7a4a', 'M89 175 Q120 166 151 175 Q157 179 157 192 L162 250 Q120 256 78 250 L83 192 Q83 179 89 175 Z') +
      clip('ws', 'M89 175 Q120 166 151 175 Q157 179 157 192 L162 250 Q120 256 78 250 L83 192 Q83 179 89 175 Z', studs('#e8c25a', [84, 196, 158, 248], 11)) +
      sleeves('#2f7a4a', 'wide') +
      [0, 1].map((i) => clip(`wc${i}`, sleeve.wide[i], ['#d8323c', '#f7d34c', '#2b5ea8', '#ffffff', '#e86aa0'].map((c, k) => `<path d="M60 ${200 + k * 4} L180 ${200 + k * 4}" stroke="${c}" stroke-width="4"/>`).join(''))).join('') +
      sleeve.wide.map((d) => `<path d="${d}" fill="none" stroke="${LINE}" stroke-width="2.4" stroke-linejoin="round"/>`).join('') +
      git('#7a2a46', 204) +
      belt('#d8323c', 212, 7),
  },
  'outfit.gonryongpo': {
    outfit: () =>
      shoes('#2e2a28') +
      lower('#c8323a', ROBE) +
      sleeves('#c8323a', 'wide') +
      danryeong('#9a2228') +
      [[120, 198, 11], [94, 182, 7], [146, 182, 7]]
        .map(([x, y, r]) => shape({ d: circle(x, y, r), fill: '#e8b83f', shade: '', sw: 1.8 }) + `<path d="M${x - r * 0.5} ${y + r * 0.1} Q${x} ${y - r * 0.6} ${x + r * 0.5} ${y + r * 0.1} Q${x} ${y + r * 0.5} ${x - r * 0.5} ${y + r * 0.1}" fill="none" stroke="#2d6a4a" stroke-width="1.8"/>`)
        .join('') +
      belt('#e8b83f', 214, 6) +
      studs('#ffffff', [92, 217, 148, 217], 12),
  },
}

export function outfits() {
  for (const [id, o] of Object.entries(OUTFITS)) {
    write(id, 'outfit', o.outfit())
    if (o.woman) write(id, 'outfit@woman', o.woman())
    if (o.back) write(id, 'back', o.back())
  }
}
