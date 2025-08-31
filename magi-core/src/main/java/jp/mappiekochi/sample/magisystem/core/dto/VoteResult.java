package jp.mappiekochi.sample.magisystem.core.dto;

import jp.mappiekochi.sample.magisystem.core.domain.DecisionType;
import jp.mappiekochi.sample.magisystem.core.domain.Sage;

public record VoteResult(
    Sage sage,
    DecisionType decision,
    String reason
) {}
