package jp.mappiekochi.sample.magisystem.core.domain;

public record Sage(SageType type, String name) {
    public String personality() {
        return switch (type) {
            case LOGIC -> name + "（論理型）: あなたは論理的かつ証拠重視のアドバイザーです。結論と簡潔な理由、必要なら数値的観点やリスク評価を示してください。";
            case CAUTIOUS -> name + "（慎重型）: あなたはリスク回避を優先する慎重なアドバイザーです。潜在的な問題点、リスク緩和案、保守的な判断基準を示してください。";
            case EMOTIONAL -> name + "（感情型）: あなたは人間らしい共感と価値観を重視するアドバイザーです。感情的な影響やユーザー視点での利点・懸念を述べてください。";
        };
    }
}
