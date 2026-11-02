/**
 * 임시 아바타 그림을 src/avatar/parts/ 에 만든다(npm run avatar:parts). 작가 그림(png, webp)이 있는 층은 건드리지 않는다.
 * 그림을 손보면 여기서 고치고 다시 돌린다. 규격은 docs/avatar-art.md.
 */
import fs from 'node:fs'
import { bodies, rigGuide } from './bodies'
import { extras } from './extras'
import { hairs } from './hairs'
import { svg } from './lib'
import { outfits } from './outfits'

bodies()
outfits()
hairs()
extras()
fs.writeFileSync('../docs/avatar-rig.svg', svg(rigGuide()))
console.log('아바타 임시 그림을 만들었어요')
