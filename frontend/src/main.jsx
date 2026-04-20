import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './styles/global.css'
import App from './App.jsx'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')

if (API_BASE_URL) {
  const originalFetch = window.fetch.bind(window)
  window.fetch = (input, init) => {
    if (typeof input === 'string' && input.startsWith('/')) {
      return originalFetch(`${API_BASE_URL}${input}`, init)
    }

    if (input instanceof Request) {
      const requestUrl = new URL(input.url, window.location.origin)
      if (requestUrl.origin === window.location.origin && requestUrl.pathname.startsWith('/')) {
        const nextUrl = `${API_BASE_URL}${requestUrl.pathname}${requestUrl.search}`
        return originalFetch(new Request(nextUrl, input), init)
      }
    }

    return originalFetch(input, init)
  }
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
