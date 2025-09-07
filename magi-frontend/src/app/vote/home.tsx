import React, { useState } from "react";
import { FinalDecisionEnum } from "../types/magi";
import type { MagiResponse, VoteOption } from "../types/magi";
import { majorityVote } from "../services/voteService";
import "./vote.css";

export function Vote() {
    const [topic, setTopic] = useState<string>("");
    const [yesCriteria, setYesCriteria] = useState<string>("");
    const [noCriteria, setNoCriteria] = useState<string>("");
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [voteResult, setVoteResult] = useState<MagiResponse | null>(null);

    const submitVote = async (e?: React.FormEvent) => {
        e?.preventDefault();
        if (!topic.trim() || !yesCriteria.trim() || !noCriteria.trim()) return;

        setIsLoading(true);
        setVoteResult(null);

        try {
            const payload: VoteOption = { topic, yesCriteria, noCriteria };
            const result = await majorityVote(payload);
            setVoteResult(result);
        } catch (err) {
            console.error("Error during voting:", err);
            // Minimal user feedback — adapt to your app's toast/notification system if available
            alert("投票中にエラーが発生しました。コンソールを確認してください。");
        } finally {
            setIsLoading(false);
        }
    };

    const resetForm = () => {
        setTopic("");
        setYesCriteria("");
        setNoCriteria("");
        setVoteResult(null);
    };

    const getDecisionClass = (decision: FinalDecisionEnum) =>
        decision === FinalDecisionEnum.Yes
            ? "decision-yes"
            : decision === FinalDecisionEnum.No
                ? "decision-no"
                : "decision-tie";

    const getDecisionText = (decision: FinalDecisionEnum) =>
        decision === FinalDecisionEnum.Yes ? "承認" : decision === FinalDecisionEnum.No ? "否決" : "引き分け";

    return (
        <div className="vote-page">
            <div className="container mt-4">
                <div className="row">
                    <div className="col-12">
                        <h1 className="text-center mb-4">
                            <i className="fas fa-brain me-2" />
                            MAGI投票システム
                        </h1>
                        <p className="text-center text-muted mb-5">
                            3つの異なる人格を持つAI（MAGI）による多数決投票システムです
                        </p>
                    </div>
                </div>

                <div className="row">
                    <div className="col-lg-12 mx-auto">
                        <div className="card">
                            <div className="card-header">
                                <h3 className="card-title mb-0">
                                    <i className="fas fa-vote-yea me-2" />
                                    投票設定
                                </h3>
                            </div>
                            <div className="card-body">
                                <form onSubmit={submitVote}>
                                    <div className="mb-3">
                                        <label htmlFor="topic" className="form-label">
                                            <strong>議題</strong>
                                        </label>
                                        <textarea
                                            id="topic"
                                            className="form-control"
                                            rows={3}
                                            placeholder="投票したい議題を入力してください..."
                                            required
                                            value={topic}
                                            onChange={(e) => setTopic(e.target.value)}
                                        />
                                    </div>

                                    <div className="mb-3">
                                        <label htmlFor="yesCriteria" className="form-label">
                                            <strong>Yes（賛成）の判断基準</strong>
                                        </label>
                                        <textarea
                                            id="yesCriteria"
                                            className="form-control"
                                            rows={2}
                                            placeholder="どのような場合にYesとするかの基準を入力してください..."
                                            required
                                            value={yesCriteria}
                                            onChange={(e) => setYesCriteria(e.target.value)}
                                        />
                                    </div>

                                    <div className="mb-3">
                                        <label htmlFor="noCriteria" className="form-label">
                                            <strong>No（反対）の判断基準</strong>
                                        </label>
                                        <textarea
                                            id="noCriteria"
                                            className="form-control"
                                            rows={2}
                                            placeholder="どのような場合にNoとするかの基準を入力してください..."
                                            required
                                            value={noCriteria}
                                            onChange={(e) => setNoCriteria(e.target.value)}
                                        />
                                    </div>

                                    <div className="text-center">
                                        <button type="submit" className="btn btn-primary btn-lg" disabled={isLoading}>
                                            {isLoading ? (
                                                <>
                                                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                                                    <span>投票中...</span>
                                                </>
                                            ) : (
                                                <>
                                                    <i className="fas fa-paper-plane me-2" />
                                                    <span>投票開始</span>
                                                </>
                                            )}
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>

                {voteResult && (
                    <div className="row mt-5">
                        <div className="col-lg-12 mx-auto">
                            <div className="card">
                                <div className="card-header">
                                    <h3 className="card-title mb-0">
                                        <i className="fas fa-poll me-2" />
                                        投票結果
                                    </h3>
                                </div>
                                <div className="card-body">
                                    <div className="text-center mb-4">
                                        <div className={`final-decision ${getDecisionClass(voteResult.finalDecision)}`}>
                                            <h2 className="mb-3">最終決定: {getDecisionText(voteResult.finalDecision)}</h2>
                                            <div className="vote-counts">
                                                <span className="badge bg-success me-2 fs-6">
                                                    <i className="fas fa-thumbs-up me-1" />
                                                    Yes: {voteResult.countOfYes}
                                                </span>
                                                <span className="badge bg-danger fs-6">
                                                    <i className="fas fa-thumbs-down me-1" />
                                                    No: {voteResult.countOfNo}
                                                </span>
                                            </div>
                                        </div>
                                    </div>

                                    {voteResult.yesResponses.length > 0 && (
                                        <div className="mb-4">
                                            <h4 className="text-success">
                                                <i className="fas fa-check-circle me-2" />
                                                賛成理由
                                            </h4>
                                            {voteResult.yesResponses.map((r, idx) => (
                                                <div className="alert alert-success" key={`yes-${idx}`}>
                                                    <i className="fas fa-quote-left me-2" />
                                                    <strong>{r.personality}</strong>
                                                    <br />
                                                    {r.reason}
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                    {voteResult.noResponses.length > 0 && (
                                        <div className="mb-4">
                                            <h4 className="text-danger">
                                                <i className="fas fa-times-circle me-2" />
                                                反対理由
                                            </h4>
                                            {voteResult.noResponses.map((r, idx) => (
                                                <div className="alert alert-danger" key={`no-${idx}`}>
                                                    <i className="fas fa-quote-left me-2" />
                                                    <strong>{r.personality}</strong>
                                                    <br />
                                                    {r.reason}
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                    <div className="text-center">
                                        <button className="btn btn-outline-primary" onClick={resetForm}>
                                            <i className="fas fa-redo me-2" />
                                            新しい投票
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
