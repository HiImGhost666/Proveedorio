/// <reference types="vitest" />
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080', // Usa la IP, no 'localhost'
        changeOrigin: true,
        secure: false,
      },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      src: path.resolve(__dirname, './src'),
      'assets': path.resolve(__dirname, './src/assets'),
      'components': path.resolve(__dirname, './src/components'),
      'features': path.resolve(__dirname, './src/features'),
      'hooks': path.resolve(__dirname, './src/hooks'),
      'pages': path.resolve(__dirname, './src/pages'),
      'router': path.resolve(__dirname, './src/router'),
      'services': path.resolve(__dirname, './src/services'),
      'store': path.resolve(__dirname, './src/store'),
      'utils': path.resolve(__dirname, './src/utils'),
    }
  }
})
