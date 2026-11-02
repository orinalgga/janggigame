/** 수염, 모자·소품, 배경. */
import { BACKDROPS } from '../../src/character-paint'
import { LINE, circle, ellipse, mirror, shape, tone, write } from './lib'

const s = (d: string, c: string, sw?: number) => shape({ d, fill: c, shade: '', sw })

// ---- 수염 ----

const MUSTACHE = 'M119 140 Q108 136 100 142 Q96 146 92 144 Q98 152 110 148 Q116 146 119 143 Z'
const mustache = (c: string) => s(MUSTACHE, c, 1.8) + s(mirror(MUSTACHE), c, 1.8)

const BEARDS: Record<string, (c: string) => string> = {
  'beard.mustache': mustache,
  'beard.scholar': (c) => mustache(c) + s('M114 158 Q120 162 126 158 L124 196 Q120 204 116 196 Z', c, 1.8),
  'beard.full': (c) =>
    s('M64 112 Q64 160 120 178 Q176 160 176 112 L168 112 Q166 146 140 154 Q128 150 120 152 Q112 150 100 154 Q74 146 72 112 Z', c, 2) + mustache(c),
  'beard.sage': (c) => s('M96 148 Q92 196 120 230 Q148 196 144 148 Q132 158 120 156 Q108 158 96 148 Z', c, 2) + mustache(c),
}

// ---- 모자·소품 ----

const BLACK = '#26211f'

const HATS: Record<string, { hat: () => string; back?: () => string }> = {
  'hat.cap': {
    hat: () =>
      s('M60 96 C58 54 86 36 120 36 C154 36 182 54 180 96 Z', '#2f62b4') +
      `<path d="M120 36 L120 96 M90 42 Q98 70 100 96 M150 42 Q142 70 140 96" stroke="#244c8e" stroke-width="2" fill="none"/>` +
      s('M56 94 Q120 82 184 94 Q192 102 178 106 Q120 96 62 106 Q48 102 56 94 Z', '#244c8e') +
      s(circle(120, 36, 5), '#244c8e') +
      s(circle(120, 66, 8), '#ffffff', 1.6),
  },
  'hat.beanie': {
    hat: () =>
      s('M60 102 C58 56 86 38 120 38 C154 38 182 56 180 102 Z', '#d8423e') +
      s('M58 88 L182 88 L182 104 L58 104 Z', '#b8302e') +
      [72, 86, 100, 114, 128, 142, 156, 170].map((x) => `<path d="M${x} 90 L${x} 102" stroke="#962624" stroke-width="2"/>`).join('') +
      s(circle(120, 32, 12), '#ffffff'),
  },
  'hat.glasses': {
    hat: () =>
      [[100, 122], [140, 122]].map(([x, y]) => `<rect x="${x - 14}" y="${y - 11}" width="28" height="22" rx="7" fill="#ffffff22" stroke="${BLACK}" stroke-width="3.4"/>`).join('') +
      `<path d="M114 120 Q120 116 126 120 M86 118 L66 114 M154 118 L174 114" stroke="${BLACK}" stroke-width="3" fill="none" stroke-linecap="round"/>`,
  },
  'hat.sunglasses': {
    hat: () =>
      [[100, 122], [140, 122]].map(([x, y]) => `<path d="M${x - 15} ${y - 9} L${x + 15} ${y - 9} Q${x + 15} ${y + 11} ${x} ${y + 11} Q${x - 15} ${y + 11} ${x - 15} ${y - 9} Z" fill="#1e2228" stroke="${BLACK}" stroke-width="2.6" stroke-linejoin="round"/><path d="M${x - 10} ${y - 5} L${x - 4} ${y - 5}" stroke="#ffffff88" stroke-width="2.4" stroke-linecap="round"/>`).join('') +
      `<path d="M115 115 L125 115 M85 114 L66 112 M155 114 L174 112" stroke="${BLACK}" stroke-width="3" stroke-linecap="round"/>`,
  },
  'hat.gat': {
    hat: () =>
      `<ellipse cx="120" cy="78" rx="74" ry="14" fill="#1f1a1aDD" stroke="${BLACK}" stroke-width="2.4"/>` +
      s('M96 78 L99 36 Q120 30 141 36 L144 78 Q120 84 96 78 Z', BLACK) +
      `<path d="M100 70 Q120 74 140 70" stroke="#5a4a40" stroke-width="3" fill="none"/>` +
      Array.from({ length: 9 }, (_, i) => {
        const t = i / 8
        return `<circle cx="${(72 + 22 * t).toFixed(1)}" cy="${(86 + 90 * t).toFixed(1)}" r="2.6" fill="#d9a441"/><circle cx="${(168 - 22 * t).toFixed(1)}" cy="${(86 + 90 * t).toFixed(1)}" r="2.6" fill="#d9a441"/>`
      }).join(''),
  },
  'hat.binyeo': {
    hat: () =>
      `<path d="M150 58 L196 82" stroke="#e8b83f" stroke-width="5" stroke-linecap="round"/>` +
      s('M140 50 Q150 40 158 50 Q164 44 168 52 Q160 62 148 60 Z', '#e8b83f', 1.8) +
      `<circle cx="152" cy="52" r="2.2" fill="#d8323c"/>`,
  },
  'hat.jeongjagwan': {
    hat: () =>
      s('M82 84 L86 40 L102 28 L120 40 L138 28 L154 40 L158 84 Q120 92 82 84 Z', BLACK) +
      `<path d="M86 60 L154 60 M102 28 L104 60 M138 28 L136 60 M120 40 L120 86" stroke="#4a403a" stroke-width="2.2"/>`,
  },
  'hat.jokduri': {
    hat: () =>
      s('M98 62 L102 34 L138 34 L142 62 Q120 68 98 62 Z', BLACK) +
      [[110, 40, '#d8323c'], [120, 34, '#2b5ea8'], [130, 40, '#3f8a52'], [120, 50, '#e8b83f']].map(([x, y, c]) => s(circle(x as number, y as number, 4.4), c as string, 1.4)).join(''),
  },
  'hat.samo': {
    back: () => [ellipse(58, 66, 24, 8), ellipse(182, 66, 24, 8)].map((d) => s(d, BLACK)).join(''),
    hat: () =>
      s('M70 96 C68 62 92 52 120 52 C148 52 172 62 170 96 Q120 88 70 96 Z', BLACK) +
      s('M94 56 C94 30 146 30 146 56 Z', BLACK) +
      `<path d="M74 88 Q120 80 166 88" stroke="#4a403a" stroke-width="2.4" fill="none"/>`,
  },
  'hat.jeonrip': {
    hat: () =>
      `<ellipse cx="120" cy="80" rx="66" ry="13" fill="${BLACK}" stroke="${LINE}" stroke-width="2.4"/>` +
      s('M92 80 C90 40 150 40 148 80 Z', BLACK) +
      s('M120 40 Q114 24 120 14 Q130 26 120 40 Z', '#d8323c') +
      `<path d="M140 50 Q170 28 192 34" stroke="#2f7a6a" stroke-width="4" fill="none" stroke-linecap="round"/><circle cx="192" cy="34" r="6" fill="#2b5ea8" stroke="#e8b83f" stroke-width="2.4"/>` +
      `<path d="M96 72 Q120 76 144 72" stroke="#e8b83f" stroke-width="3" fill="none"/>`,
  },
  'hat.tteoljam': {
    hat: () =>
      [[120, 46, 9], [86, 60, 7], [154, 60, 7]]
        .map(([x, y, r]) =>
          s(circle(x, y, r), '#e8b83f', 1.8) +
          s(circle(x, y, r * 0.45), '#2b5ea8', 1.2) +
          `<path d="M${x - r * 0.6} ${y + r} L${x - r * 0.8} ${y + r + 10} M${x + r * 0.6} ${y + r} L${x + r * 0.8} ${y + r + 10}" stroke="#e8b83f" stroke-width="1.6"/><circle cx="${x - r * 0.8}" cy="${y + r + 12}" r="2.2" fill="#d8323c"/><circle cx="${x + r * 0.8}" cy="${y + r + 12}" r="2.2" fill="#3f8a52"/>`,
        )
        .join(''),
  },
  'hat.ikseon': {
    back: () => [ellipse(94, 34, 13, 18), ellipse(146, 34, 13, 18)].map((d) => s(d, BLACK)).join(''),
    hat: () =>
      s('M74 98 C72 58 94 44 120 44 C146 44 168 58 166 98 Q120 90 74 98 Z', BLACK) +
      s('M100 50 C100 28 140 28 140 50 Z', BLACK) +
      `<path d="M78 90 Q120 82 162 90" stroke="#e8b83f" stroke-width="2.6" fill="none"/>`,
  },
}

export function extras() {
  for (const [id, f] of Object.entries(BEARDS)) {
    write(id, 'beard', f('#3a2a24'))
    write(id, 'beard@elder', f('#eeeae2'))
  }
  for (const [id, h] of Object.entries(HATS)) {
    write(id, 'hat', h.hat())
    if (h.back) write(id, 'back', h.back())
  }
  for (const [id, b] of Object.entries(BACKDROPS)) write(id, 'bg', `<g transform="scale(2)">${b.paint('bg')}</g>`)
}


