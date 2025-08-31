package jp.mappiekochi.sample.magisystem.core.dto;

import java.util.List;

public record MagiResponse(
    String decision,
    int yesCount,
    int noCount,
    List<VoteResult> voteResults
) {}
