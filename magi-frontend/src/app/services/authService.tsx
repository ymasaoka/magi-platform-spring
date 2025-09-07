import { PublicClientApplication } from "@azure/msal-browser";
import type { AccountInfo, AuthenticationResult } from "@azure/msal-browser";
import { authConfig } from "../config/authConfig";

// 型チェックでコンパイルエラーを抑えるために宣言（ランタイムでは undefined になることがある）
declare const process: any;

let msalInstance: PublicClientApplication | null = null;
let msalInitPromise: Promise<PublicClientApplication> | null = null;

function getEnv(name: string, fallback?: string): string | undefined {
  // process が未定義の場合を安全に扱う
  if (typeof process !== "undefined" && process && process.env && process.env[name]) {
    return process.env[name];
  }
  return fallback;
}

function createMsalConfig() {
  return {
    auth: {
      clientId: authConfig.clientId,
      authority: authConfig.authority,
      redirectUri: authConfig.redirectUri,
    },
    cache: {
      cacheLocation: "sessionStorage",
      storeAuthStateInCookie: false,
    },
  };
}

// 既存の同期的取得は残すが、MSAL を利用する前に initialize() を待つための非同期初期化関数を追加
function getMsalInstance(): PublicClientApplication | null {
  if (msalInstance) return msalInstance;
  if (typeof window === "undefined") {
    return null;
  }
  msalInstance = new PublicClientApplication(createMsalConfig());
  return msalInstance;
}

async function ensureMsalInitialized(): Promise<PublicClientApplication> {
  if (typeof window === "undefined") {
    throw new Error("MSAL must be initialized in the browser.");
  }

  if (msalInitPromise) {
    return msalInitPromise;
  }

  // create or reuse instance, then initialize it and cache the promise
  const instance = getMsalInstance() ?? new PublicClientApplication(createMsalConfig());
  msalInstance = instance;

  msalInitPromise = (async () => {
    // initialize() は MSAL Browser v2 で必須（または推奨）なので待機する
    if (typeof instance.initialize === "function") {
      await instance.initialize();
    }
    return instance;
  })();

  return msalInitPromise;
}

const DEFAULT_SCOPES = [authConfig.apiScope];

/**
 * アクセストークンを取得する
 * - まず acquireTokenSilent を試す
 * - 失敗したら loginPopup -> acquireTokenSilent
 */
export async function getAccessToken(scopes: string[] = DEFAULT_SCOPES): Promise<string> {
  // ブラウザ環境でのみ動作するようにガード
  if (typeof window === "undefined") {
    throw new Error("getAccessToken must be called from the browser.");
  }

  const msal = await ensureMsalInitialized();

  if (!msal) {
    throw new Error("MSAL is not available in this environment.");
  }

  const accounts = msal.getAllAccounts();
  let account: AccountInfo | null = accounts && accounts.length ? accounts[0] : null;

  try {
    // account がある場合のみサイレント取得を試す（account が無いと no_account_error が出る）
    if (account) {
      const silentResult: AuthenticationResult = await msal.acquireTokenSilent({ account, scopes } as any);
      if (silentResult && silentResult.accessToken) return silentResult.accessToken;
    }
  } catch (err) {
    console.warn("Silent token acquisition failed, attempting interactive login.", err);
  }

  // アカウントが無ければログイン（ログイン後にアクティブアカウントを設定）
  if (!account) {
    const loginResult = await msal.loginPopup({ scopes });
    account = loginResult.account ?? null;
    if (account && typeof msal.setActiveAccount === "function") {
      msal.setActiveAccount(account);
    }
  }

  // 再度トークン取得
  const result = await msal.acquireTokenSilent({ account: account!, scopes } as any);
  if (!result?.accessToken) throw new Error("Failed to acquire access token");
  return result.accessToken;
}
