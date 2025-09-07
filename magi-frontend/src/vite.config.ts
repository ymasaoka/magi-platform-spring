import { reactRouter } from "@react-router/dev/vite";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig, loadEnv } from "vite";
import tsconfigPaths from "vite-tsconfig-paths";

export default defineConfig(({ mode }) => {
  // .env*, .env.local を読み込む（VITE_ プレフィックスを期待）
  const env = loadEnv(mode, process.cwd());
  const apiTarget = env.VITE_API_PROXY_TARGET;

  if (!apiTarget) {
    throw new Error(
      "VITE_API_PROXY_TARGET is not set. Create .env.local with VITE_API_PROXY_TARGET=<your-backend-url>"
    );
  }

  return {
    plugins: [
      tailwindcss(),
      reactRouter(),
      tsconfigPaths(),
    ],
    server: {
      proxy: {
        "/api": {
          target: apiTarget,
          changeOrigin: true,
          secure: false,
        }
      }
    }
  };
});
