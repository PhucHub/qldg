/**
 * Cloudflare Worker - QLDG Reverse Proxy & Static Asset Handler
 * 
 * - Chuyển tiếp (Reverse Proxy) mọi request /api/* sang máy chủ Spring Boot Backend.
 * - Phục vụ toàn bộ tài nguyên tĩnh (HTML, CSS, JS, Images) cho Vue 3 SPA.
 */

export default {
  async fetch(request, env) {
    const url = new URL(request.url)

    // 1. Xử lý preflight CORS (nếu có)
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        status: 204,
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
          'Access-Control-Allow-Headers': 'Content-Type, Authorization',
          'Access-Control-Max-Age': '86400',
        },
      })
    }

    // 2. Chuyển tiếp API sang Backend Spring Boot
    if (url.pathname.startsWith('/api')) {
      const backendBase = env.BACKEND_API_URL || env.API_HOST || ''

      if (backendBase) {
        // Chuẩn hóa URL backend (bỏ dấu / ở cuối nếu có)
        const targetOrigin = backendBase.replace(/\/+$/, '')
        const targetUrl = new URL(url.pathname + url.search, targetOrigin)

        // Sao chép headers
        const headers = new Headers(request.headers)
        headers.set('X-Forwarded-Host', url.host)
        headers.set('X-Forwarded-Proto', url.protocol.replace(':', ''))

        const init = {
          method: request.method,
          headers,
          redirect: 'follow',
        }

        // Đính kèm body nếu method không phải GET / HEAD
        if (request.method !== 'GET' && request.method !== 'HEAD') {
          init.body = request.body
        }

        try {
          const response = await fetch(targetUrl.toString(), init)
          const newHeaders = new Headers(response.headers)
          newHeaders.set('Access-Control-Allow-Origin', '*')
          return new Response(response.body, {
            status: response.status,
            statusText: response.statusText,
            headers: newHeaders,
          })
        } catch (err) {
          return new Response(JSON.stringify({ error: 'Không thể kết nối đến máy chủ Backend', details: err.message }), {
            status: 502,
            headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' },
          })
        }
      } else {
        // Chưa cấu hình BACKEND_API_URL -> Trả về 503 JSON để SPA Frontend chuyển sang Mock Data Mode
        return new Response(JSON.stringify({ 
          error: 'BACKEND_NOT_CONFIGURED',
          message: 'Máy chủ Backend chưa được kết nối. Ứng dụng tự động chuyển sang Chế độ Dữ liệu mẫu (Mock Demo Mode).' 
        }), {
          status: 503,
          headers: { 
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
          },
        })
      }
    }

    // 3. Phục vụ Static Assets (Vue 3 SPA)
    if (env.ASSETS) {
      return env.ASSETS.fetch(request)
    }

    return new Response('Assets binding not found', { status: 404 })
  },
}
