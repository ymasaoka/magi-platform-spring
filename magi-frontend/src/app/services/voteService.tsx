import { FinalDecisionEnum } from "../types/magi";
import type { VoteOption, MagiResponse } from "../types/magi";
import { getAccessToken } from "./authService";

/**
 * Calls the backend API that performs the majority vote.
 */
export async function majorityVote(voteOption: VoteOption): Promise<MagiResponse> {
  // サーバサイド（SSR）実行時は絶対URLでの呼び出しは期待通り動かない可能性があるのでクライアント限定にする
  if (typeof window === "undefined") {
    throw new Error("majorityVote must be called from the browser (client-side).");
  }

  // ビルド時に埋められた VITE_API_BASE を優先（例: http://magi-bff）
  const envBase = (import.meta.env.VITE_API_BASE as string) || "";
  const apiBase = envBase.replace(/\/+$/g, ""); // 末尾スラッシュ削除
  const url = apiBase ? `${apiBase}/api/core/magi/vote` : "/api/core/magi/vote";

  // 既存のトークン取得処理を使用
  const token = await getAccessToken();

  const res = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { "Authorization": `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(voteOption),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Request failed: ${res.status}`);
  }

  const data = await res.json();
  console.debug("majorityVote response:", data);

  const mapDecision = (d: unknown): FinalDecisionEnum => {
    const s = String(d ?? "").toUpperCase();
    return s === "YES" ? FinalDecisionEnum.Yes : s === "NO" ? FinalDecisionEnum.No : FinalDecisionEnum.Tie;
  };

  const voteResults = Array.isArray(data?.voteResults) ? data.voteResults : [];

  const yesResponses = voteResults
    .filter((v: any) => String(v?.decision ?? "").toUpperCase() === "YES")
    .map((v: any) => ({
      personality: v?.sage?.name ?? String(v?.sage?.type ?? "unknown"),
      reason: v?.reason ?? "",
    }));

  const noResponses = voteResults
    .filter((v: any) => String(v?.decision ?? "").toUpperCase() === "NO")
    .map((v: any) => ({
      personality: v?.sage?.name ?? String(v?.sage?.type ?? "unknown"),
      reason: v?.reason ?? "",
    }));

  const yesCount = typeof data?.yesCount === "number" ? data.yesCount : (Number(data?.yesCount) || yesResponses.length);
  const noCount = typeof data?.noCount === "number" ? data.noCount : (Number(data?.noCount) || noResponses.length);

  return {
    finalDecision: mapDecision(data?.decision),
    countOfYes: yesCount,
    countOfNo: noCount,
    yesResponses,
    noResponses,
  } as MagiResponse;
}
