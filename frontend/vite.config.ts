import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// Dev-only proxy: lets the browser call relative /api paths so the
// backend's CORS config never has to be touched locally.
// In production, set VITE_API_BASE_URL to the deployed backend's origin.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_BACKEND_ORIGIN || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
