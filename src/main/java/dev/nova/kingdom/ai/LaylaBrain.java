
package dev.nova.kingdom.ai;

// @name: LaylaBrain
// @type: server-side Java class
// @location: src/main/java/dev/nova/kingdom/ai/

import java.util.EnumMap;
import java.util.Map;

public class LaylaBrain {

    public enum Skill {
        WOODCUTTING,
        MINING,
        CRAFTING,
        BUILDING,
        FARMING,
        COMBAT,
        COOKING,
        EXPLORATION,
        ANIMAL_CARE,
        REDSTONE,
        TRADING,
        FISHING
    }

    public enum Stage {
        UNKNOWN,
        OBSERVING,
        PRACTICING,
        CAPABLE,
        INDEPENDENT
    }

    public enum Goal {
        FOLLOW_PARTNER,
        WATCH_PARTNER,
        PRACTICE_WOODCUTTING,
        PRACTICE_MINING,
        PRACTICE_CRAFTING,
        PRACTICE_BUILDING,
        PRACTICE_FARMING,
        PRACTICE_COMBAT,
        PRACTICE_COOKING,
        PRACTICE_ANIMAL_CARE,
        EXPLORE,
        HELP_KINGDOM,
        REST
    }

    private final Map<Skill, Integer> experience =
            new EnumMap<>(Skill.class);

    private final Map<Skill, Integer> successfulAttempts =
            new EnumMap<>(Skill.class);

    private Goal currentGoal = Goal.FOLLOW_PARTNER;

    private int confidence = 0;
    private int curiosity = 75;
    private int independence = 0;

    public LaylaBrain() {
        for (Skill skill : Skill.values()) {
            experience.put(skill, 0);
            successfulAttempts.put(skill, 0);
        }
    }

    public void observe(Skill skill, int amount) {
        if (amount <= 0) {
            return;
        }

        experience.put(
                skill,
                Math.min(
                        10000,
                        getExperience(skill) + amount
                )
        );

        confidence = Math.min(100, confidence + 1);
    }

    public void recordPractice(Skill skill, boolean success) {
        if (success) {
            successfulAttempts.put(
                    skill,
                    successfulAttempts.get(skill) + 1
            );

            observe(skill, 5);
            confidence = Math.min(100, confidence + 2);
        } else {
            // Failure is still a learning experience.
            observe(skill, 1);
        }

        updateIndependence();
    }

    public int getExperience(Skill skill) {
        return experience.getOrDefault(skill, 0);
    }

    public int getSuccessfulAttempts(Skill skill) {
        return successfulAttempts.getOrDefault(skill, 0);
    }

    public Stage getStage(Skill skill) {
        int xp = getExperience(skill);
        int successes = getSuccessfulAttempts(skill);

        if (xp == 0) {
            return Stage.UNKNOWN;
        }

        if (xp < 20) {
            return Stage.OBSERVING;
        }

        if (xp < 100 || successes < 5) {
            return Stage.PRACTICING;
        }

        if (xp < 300 || successes < 20) {
            return Stage.CAPABLE;
        }

        return Stage.INDEPENDENT;
    }

    public boolean canPractice(Skill skill) {
        return getExperience(skill) >= 20;
    }

    public boolean canWorkIndependently(Skill skill) {
        return getStage(skill) == Stage.INDEPENDENT;
    }

    public Goal chooseGoal(boolean partnerNearby,
                           boolean partnerInDanger,
                           boolean kingdomNeedsHelp) {

        if (partnerInDanger) {
            currentGoal = Goal.FOLLOW_PARTNER;
            return currentGoal;
        }

        if (!partnerNearby) {
            currentGoal = Goal.FOLLOW_PARTNER;
            return currentGoal;
        }

        if (kingdomNeedsHelp && independence >= 40) {
            currentGoal = Goal.HELP_KINGDOM;
            return currentGoal;
        }

        if (canPractice(Skill.WOODCUTTING)
                && !canWorkIndependently(Skill.WOODCUTTING)) {
            currentGoal = Goal.PRACTICE_WOODCUTTING;
        } else if (canPractice(Skill.CRAFTING)
                && !canWorkIndependently(Skill.CRAFTING)) {
            currentGoal = Goal.PRACTICE_CRAFTING;
        } else if (canPractice(Skill.MINING)
                && !canWorkIndependently(Skill.MINING)) {
            currentGoal = Goal.PRACTICE_MINING;
        } else if (canPractice(Skill.BUILDING)
                && !canWorkIndependently(Skill.BUILDING)) {
            currentGoal = Goal.PRACTICE_BUILDING;
        } else {
            currentGoal = Goal.WATCH_PARTNER;
        }

        return currentGoal;
    }

    private void updateIndependence() {
        int independentSkills = 0;

        for (Skill skill : Skill.values()) {
            if (canWorkIndependently(skill)) {
                independentSkills++;
            }
        }

        independence = Math.min(100, independentSkills * 10);
    }

    public Goal getCurrentGoal() {
        return currentGoal;
    }

    public int getConfidence() {
        return confidence;
    }

    public int getCuriosity() {
        return curiosity;
    }

    public int getIndependence() {
        return independence;
    }

    public Map<Skill, Integer> getExperienceSnapshot() {
        return new EnumMap<>(experience);
    }

    public void restoreExperience(Skill skill, int xp) {
        experience.put(skill, Math.max(0, Math.min(10000, xp)));
    }

    public void restoreAttempts(Skill skill, int attempts) {
        successfulAttempts.put(skill, Math.max(0, attempts));
        updateIndependence();
    }
}
