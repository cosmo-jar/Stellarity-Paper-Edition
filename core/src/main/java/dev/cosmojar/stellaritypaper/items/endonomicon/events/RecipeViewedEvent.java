package dev.cosmojar.stellaritypaper.items.endonomicon.events;

import dev.cosmojar.stellaritypaper.items.endonomicon.model.RecipeViewModel;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class RecipeViewedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final RecipeViewModel viewModel;

    public RecipeViewedEvent(Player player, RecipeViewModel viewModel) {
        this.player = player;
        this.viewModel = viewModel;
    }

    public Player getPlayer() { return player; }
    public RecipeViewModel getViewModel() { return viewModel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
