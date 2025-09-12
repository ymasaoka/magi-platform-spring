package jp.mappiekochi.sample.magisystem.core.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import jp.mappiekochi.sample.magisystem.core.dto.*;
import jp.mappiekochi.sample.magisystem.core.service.MagiService;

@RestController
@RequestMapping("/api/magi")
public class MagiController {
    private final MagiService _magiService;

    public MagiController(MagiService magiService) {
        this._magiService = magiService;
    }

    @PostMapping("/vote")
    public MagiResponse vote(@Valid @RequestBody VoteOption option) {
        return this._magiService.majorityVote(option);
    }
}
