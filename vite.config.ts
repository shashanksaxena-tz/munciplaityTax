import path from 'path';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, '.', '');
    // Default to localhost:8080 for gateway service, can be overridden with VITE_API_BASE_URL
    const apiBaseUrl = env.VITE_API_BASE_URL || 'http://localhost:8080';
    
    return {
      server: {
        port: 3000,
        host: '0.0.0.0',
        proxy: {
          // Proxy API requests to the gateway service
          '/api': {
            target: apiBaseUrl,
            changeOrigin: true,
            secure: false,
            configure: (proxy, _options) => {
              proxy.on('error', (err, _req, _res) => {
                console.log('Proxy error:', err);
              });
              proxy.on('proxyReq', (proxyReq, req, _res) => {
                console.log('Proxying:', req.method, req.url, '→', apiBaseUrl);
              });
            }
          },
          // Proxy auth requests
          '/auth': {
            target: apiBaseUrl,
            changeOrigin: true,
            secure: false
          },
          // Proxy extraction requests (for SSE streaming)
          '/extraction': {
            target: apiBaseUrl,
            changeOrigin: true,
            secure: false
          },
          // Proxy submission requests
          '/submissions': {
            target: apiBaseUrl,
            changeOrigin: true,
            secure: false
          }
        }
      },
      plugins: [react()],
      define: {
        'process.env.API_KEY': JSON.stringify(env.GEMINI_API_KEY),
        'process.env.GEMINI_API_KEY': JSON.stringify(env.GEMINI_API_KEY)
      },
      resolve: {
        alias: {
          '@': path.resolve(__dirname, '.'),
        }
      }
    };
});
