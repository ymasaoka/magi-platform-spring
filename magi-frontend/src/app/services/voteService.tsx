import { FinalDecisionEnum } from "../types/magi";
import type { VoteOption, MagiResponse } from "../types/magi";
import { getAccessToken } from "./authService";

/**
 * Calls the backend API that performs the majority vote.
 */
export async function majorityVote(voteOption: VoteOption): Promise<MagiResponse> {
  // SSR で呼ばれた場合は明確に失敗させて原因を切り分ける
  if (typeof window === "undefined") {
    throw new Error("majorityVote must be called from the browser (client-side).");
  }

  // MSAL を用いてアクセストークンを取得（スコープは必要に応じて調整）
  const token = await getAccessToken();

  // const res = await fetch("/api/magi/vote", {
  const res = await fetch("/api/core/magi/vote", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`,
    },
    body: JSON.stringify(voteOption),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Request failed with status ${res.status}`);
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
