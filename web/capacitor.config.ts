import type { CapacitorConfig } from '@capacitor/cli'

/**
 * 안드로이드·iOS 앱. 웹 빌드(dist)를 그대로 앱 안에 넣는다.
 * 앱은 서버와 다른 주소에서 뜨므로 VITE_API_BASE에 서버 주소를 넣고 빌드해야 한다(README '모바일 앱').
 */
const config: CapacitorConfig = {
  appId: 'com.janggi.app',
  appName: '장기',
  webDir: 'dist',
  android: {
    // 개발 중에는 PC 서버(http://192.168.x.x:8080)에 붙으므로 https 앱 화면에서 http 요청을 허용한다.
    allowMixedContent: true,
  },
  server: {
    androidScheme: 'https',
    cleartext: true,
  },
}

export default config
