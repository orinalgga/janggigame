import { Capacitor } from '@capacitor/core'

/** 안드로이드·iOS 앱 안에서 돌고 있으면 true. 웹 브라우저에서는 false. */
export const isNative = Capacitor.isNativePlatform()

/**
 * 구글은 앱 안 화면(WebView)에서 로그인을 막으므로, 앱에서는 기기 브라우저로 로그인 페이지를 연다.
 * 로그인이 끝나면 서버가 com.janggi.app://auth#token=... 으로 돌려보내고 onLoginReturn이 받는다.
 */
export async function openInBrowser(url: string) {
  const { Browser } = await import('@capacitor/browser')
  await Browser.open({ url })
}

export async function onLoginReturn(handle: (url: string) => void) {
  if (!isNative) return
  const [{ App }, { Browser }] = await Promise.all([import('@capacitor/app'), import('@capacitor/browser')])
  await App.addListener('appUrlOpen', ({ url }) => {
    handle(url)
    Browser.close().catch(() => undefined)
  })
}
