/// <reference types="vitest" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Vite dev server config.
// The /uploads proxy forwards browser requests to the Spring Boot gateway on
// :8080, avoiding CORS in development. In production the frontend should be
// served from the same origin as the gateway (or via a reverse proxy).
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.ts'],
  },
  server: {
    port: 5173,
    proxy: {
      "/uploads": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      // Story 1.3: SSE stream of extraction-failure events from the gateway.
      "/sse": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      // Story 2.2a: PDF byte-stream endpoint and lineage API.
      "/documents": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
