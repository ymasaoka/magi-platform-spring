type RuntimeConfig = Record<string, string | undefined>;

const runtimeConfig: RuntimeConfig =
  typeof window !== "undefined" && (window as any).__APP_CONFIG__
    ? (window as any).__APP_CONFIG__
    : {};

// helper: check runtime first, then Vite env, then undefined
function getVar(name: string): string | undefined {
  const runt = runtimeConfig[name] ?? runtimeConfig["VITE_" + name];
  const vite =
    typeof import.meta !== "undefined" ? (import.meta as any).env?.["VITE_" + name] : undefined;
  return runt ?? vite;
}

function requireVar(name: string): string {
  const v = getVar(name);
  if (!v) {
    throw new Error(`Missing required environment variable: VITE_${name}`);
  }
  return v;
}

export const authConfig = {
  clientId: requireVar("AZURE_CLIENT_ID"),
  authority: requireVar("AZURE_AUTHORITY"),
  // redirectUri は明示しない場合 window.location.origin を使う（クライアント側で安全）
  redirectUri: getVar("REDIRECT_URI") ?? (typeof window !== "undefined" ? window.location.origin : ""),
  apiScope: requireVar("API_SCOPE"),
};
