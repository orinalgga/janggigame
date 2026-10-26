import type { Look } from '../api'
import { spriteSvg } from './sprite'

/** 그림 확인 페이지(avatar-gallery.html). 칸마다 모든 부품을 기본 캐릭터에 입혀 늘어놓는다. 배포 빌드에는 들어가지 않는다. */
const BASE: Look = {
  style: 'style.chibi',
  background: 'avatar.basic',
  body: 'body.man',
  hair: 'hair.short',
  beard: 'beard.none',
  outfit: 'outfit.tee',
  hat: 'hat.none',
}
const WOMAN: Partial<Look> = { body: 'body.woman', hair: 'hair.bob' }

const ids = (prefix: string) =>
  [...new Set(Object.keys(import.meta.glob('./parts/*/*')).map((p) => p.split('/')[2]))].filter((id) => id.startsWith(prefix))

const GROUPS: [string, string, (id: string) => Partial<Look>[]][] = [
  ['body', '몸', (id) => [{ body: id, hair: id.includes('woman') ? 'hair.bob' : 'hair.short' }]],
  ['outfit', '옷', (id) => [{ outfit: id }, { ...WOMAN, outfit: id }]],
  ['hair', '머리', (id) => [{ hair: id }, { body: 'body.woman', hair: id }, { body: 'body.elder', hair: id }]],
  ['beard', '수염', (id) => [{ beard: id }, { body: 'body.elder', beard: id }]],
  ['hat', '모자·소품', (id) => [{ hat: id }, { ...WOMAN, hat: id }, { body: 'body.cat', hat: id }]],
  ['avatar', '배경', (id) => [{ background: id }]],
]

const only = new URLSearchParams(location.search).get('only')
const out = document.getElementById('out')!
for (const [slot, title, looks] of GROUPS) {
  if (only && only !== slot) continue
  const list = ids(slot + '.')
  out.insertAdjacentHTML('beforeend', `<h2>${title} (${list.length})</h2>`)
  const grid = document.createElement('div')
  grid.className = 'grid'
  for (const id of list)
    for (const l of looks(id)) grid.insertAdjacentHTML('beforeend', `<figure>${spriteSvg({ ...BASE, ...l })}<figcaption>${id}</figcaption></figure>`)
  out.append(grid)
}
const heads = document.createElement('div')
heads.className = 'grid heads'
for (const id of ids('body.')) heads.insertAdjacentHTML('beforeend', `<figure>${spriteSvg({ ...BASE, body: id }, { head: true })}</figure>`)
out.insertAdjacentHTML('beforeend', '<h2>머리글 얼굴</h2>')
out.append(heads)
