package jp.mappiekochi.sample.magisystem.bff.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CoreResponseDto(String body, int status) {
    @JsonCreator
    public CoreResponseDto(
        @JsonProperty("body") String body,
        @JsonProperty("status") int status
    ) {
        this.body = body;
        this.status = status;
    }
}
