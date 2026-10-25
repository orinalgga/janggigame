import { Client, type StompSubscription } from '@stomp/stompjs'
import { auth, wsUrl } from './api'

/**
 * 온라인 대전 실시간 연결. 로그인 토큰으로 접속하고, 끊기면 다시 붙어서 구독도 되살린다.
 * 대국 화면은 /topic/games/{id}를, 매칭을 기다릴 때는 /user/queue/match를 구독한다.
 */
export class Live {
  private client: Client
  private subs = new Map<string, { handler: (body: unknown) => void; sub?: StompSubscription }>()
  private ready: Promise<void>

  constructor() {
    let resolve!: () => void
    this.ready = new Promise((r) => (resolve = r))
    this.client = new Client({
      brokerURL: wsUrl(),
      connectHeaders: { Authorization: `Bearer ${auth.token() ?? ''}` },
      reconnectDelay: 2000,
      onConnect: () => {
        for (const [dest, entry] of this.subs) entry.sub = this.attach(dest, entry.handler)
        resolve()
      },
    })
    this.client.activate()
  }

  /** 접속이 끝날 때까지 기다린다. 매칭 요청 전에 불러야 매칭 알림을 놓치지 않는다. */
  connected(): Promise<void> {
    return this.ready
  }

  subscribe<T>(dest: string, handler: (body: T) => void): () => void {
    const h = handler as (body: unknown) => void
    const entry: { handler: (body: unknown) => void; sub?: StompSubscription } = { handler: h }
    this.subs.set(dest, entry)
    if (this.client.connected) entry.sub = this.attach(dest, h)
    return () => {
      entry.sub?.unsubscribe()
      this.subs.delete(dest)
    }
  }

  close() {
    this.subs.clear()
    void this.client.deactivate()
  }

  private attach(dest: string, handler: (body: unknown) => void) {
    return this.client.subscribe(dest, (msg) => handler(JSON.parse(msg.body)))
  }
}
