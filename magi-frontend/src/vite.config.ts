import { reactRouter } from "@react-router/dev/vite";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig, loadEnv } from "vite";
import tsconfigPaths from "vite-tsconfig-paths";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  const apiProxyTarget = env.VITE_API_PROXY_TARGET || process.env.VITE_API_PROXY_TARGET;
  const apiBaseForClient = env.VITE_API_BASE || process.env.VITE_API_BASE;

  if (mode === "development" && !apiProxyTarget) {
    throw new Error(
      "VITE_API_PROXY_TARGET is not set for development. Create .env.local with VITE_API_PROXY_TARGET=http://localhost:8080"
    );
  }

  if (mode !== "development" && !apiBaseForClient) {
    console.warn(
      "VITE_API_BASE is not set for non-development. Build will embed an empty API base. " +
      "Pass --build-arg VITE_API_BASE=... during build if client needs an absolute URL."
    );
  }

  return {
    plugins: [
      tailwindcss(),
      reactRouter(),
      tsconfigPaths(),
    ],
    server: mode === "development" ? {
      proxy: {
        "/api": {
          target: apiProxyTarget,
          changeOrigin: true,
          secure: false,
        }
      }
    } : {}
  };
});
