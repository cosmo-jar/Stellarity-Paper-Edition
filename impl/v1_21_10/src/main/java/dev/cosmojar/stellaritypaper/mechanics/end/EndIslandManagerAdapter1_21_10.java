package dev.cosmojar.stellaritypaper.mechanics.end;

import org.bukkit.GameRule;
import org.bukkit.World;

/**
 * Адаптер вызовов EndIslandManager под версию Minecraft 1.21.10.
 */
public class EndIslandManagerAdapter1_21_10 {

    public Boolean getSendCommandFeedback(World world) {
        GameRule<Boolean> rule = GameRule.SEND_COMMAND_FEEDBACK;
        return rule != null ? world.getGameRuleValue(rule) : null;
    }

    public void setSendCommandFeedback(World world, Boolean value) {
        GameRule<Boolean> rule = GameRule.SEND_COMMAND_FEEDBACK;
        if (rule != null && value != null) {
            world.setGameRule(rule, value);
        }
    }
}
