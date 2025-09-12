package jp.mappiekochi.sample.magisystem.core.service;

import java.util.List;
import java.util.Optional;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.mappiekochi.sample.magisystem.core.domain.*;
import jp.mappiekochi.sample.magisystem.core.dto.*;

@Service
public class MagiService {
    private final List<Sage> _sages;
    private final AzureOpenAiChatModel _chatModel;

    @Autowired
    public MagiService(AzureOpenAiChatModel chatModel) {
        this._chatModel = chatModel;
        // デフォルトの3賢者をDIで生成
        this._sages = List.of(
            new Sage(SageType.LOGIC, "論理型"),
            new Sage(SageType.CAUTIOUS, "慎重型"),
            new Sage(SageType.EMOTIONAL, "感情型")
        );
    }

    // カスタム賢者用コンストラクタ
    public MagiService(List<Sage> sages, AzureOpenAiChatModel chatModel) {
        this._sages = List.copyOf(sages);
        this._chatModel = chatModel;
    }

    public MagiResponse majorityVote(VoteOption option) {
        if (this._chatModel == null) {
            throw new IllegalStateException("Chat model is not initialized.");
        }

        String userPrompt = String.format("""
            次の議題について投票してください。
            --------
            %s
            --------
            賛成の判断基準: %s
            反対の判断基準: %s

            あなたの立場で賛成か反対かを述べ、理由も説明してください。必ず「賛成」または「反対」で始めてください。
            どちらとも言えない場合は「判定不能」で始めてください。
            """,
            option.topic(), option.yesCriteria(), option.noCriteria()
        );
        
        List<VoteResult> results = this._sages.stream()
            .map(sage -> voteBySage(sage, userPrompt))
            .toList();

        int yesCount = (int) results.stream().filter(r -> r.decision() == DecisionType.YES).count();
        int noCount = (int) results.stream().filter(r -> r.decision() == DecisionType.NO).count();

        DecisionType decision = judgeDecision(yesCount, noCount);

        return new MagiResponse(decision.name(), yesCount, noCount, results);
    }

    private DecisionType judgeDecision(int yesCount, int noCount) {
        return yesCount > noCount ? DecisionType.YES
         : noCount > yesCount ? DecisionType.NO
         : DecisionType.TIE;
    }

    private String personaFor(Sage sage) {
        return String.format("""
            あなたは合議制における投票権を持つ一人の賢者です。与えられた議題に対して投票を行うことが求められます。
            あなたのパーソナリティは「%s（%s型）」です。必ず次のパーソナリティに従って行動してください。
            ---
            %s
            """,
            sage.name(), sage.type().name(), sage.personality());
    }

    private VoteResult voteBySage(Sage sage, String userPrompt) {
        String personaInstruction = personaFor(sage);
        List<Message> messages = List.of(
            new SystemMessage(personaInstruction),
            new UserMessage(userPrompt)
        );
        Prompt prompt = new Prompt(messages);

        ChatResponse response = this._chatModel.call(prompt);
        String content = Optional.ofNullable(response.getResult().getOutput().getText()).orElse("");

        if (content.startsWith("賛成")) {
            return new VoteResult(sage, DecisionType.YES, content);
        } else if (content.startsWith("反対")) {
            return new VoteResult(sage, DecisionType.NO, content);
        } else {
            return new VoteResult(sage, DecisionType.TIE, content);
        }
    }
}
