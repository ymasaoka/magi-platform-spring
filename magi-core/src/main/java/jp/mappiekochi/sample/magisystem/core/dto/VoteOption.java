package jp.mappiekochi.sample.magisystem.core.dto;

import jakarta.validation.constraints.NotBlank;

public record VoteOption(
    /**
     * 投票のテーマや議題
     */
    @NotBlank String topic,
    /**
     * 賛成票 (Yes) とみなすための基準や条件
     */
    @NotBlank String yesCriteria,
    /**
     * 反対票 (No) とみなすための基準や条件
     */
    @NotBlank String noCriteria
) {}
