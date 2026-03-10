package obtuseloot.names;

import obtuseloot.config.RuntimeSettings;

public final class ArtifactImplicationScorer {
    public double score(ArtifactNameContext context, ArtifactPersonalityProfile personality, ArtifactVoiceProfile voice, RuntimeSettings.Snapshot settings) {
        double score = settings.namingImplicationBaseScore();
        if (personality.isAny(ArtifactPersonalityTrait.INTIMATE)) score += settings.namingImplicationIntimateBoost();
        if (personality.isAny(ArtifactPersonalityTrait.PREDATORY)) score += settings.namingImplicationPredatoryBoost();
        if (personality.isAny(ArtifactPersonalityTrait.SECRETIVE)) score += 0.08D;
        if (voice.primary() == ArtifactVoiceRegister.WHISPERING || voice.primary() == ArtifactVoiceRegister.VELVETY || voice.primary() == ArtifactVoiceRegister.LITURGICAL) {
            score += settings.namingImplicationVoiceBoost();
        }
        if (context.storied()) score += settings.namingImplicationStoriedBoost();
        if (context.awakened()) score += 0.08D;
        if (context.fused()) score += 0.1D;
        if ("generic".equalsIgnoreCase(context.itemCategory())) score -= settings.namingImplicationUtilitarianPenalty();
        if (voice.primary() == ArtifactVoiceRegister.MARTIAL || voice.primary() == ArtifactVoiceRegister.SEVERE) score -= 0.1D;
        return Math.max(0.0D, Math.min(1.0D, score));
    }
}
