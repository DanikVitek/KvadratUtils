package com.danikvitek.kvadratutils.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerTeleportMenuEvent extends PlayerTeleportEvent {
    private final Player destination;
    private final boolean forced;

    public PlayerTeleportMenuEvent(@NotNull Player target, @NotNull Player destination, boolean forced) {
        super(target, target.getLocation(), destination.getLocation());
        this.destination = destination;
        this.forced = forced;
    }

    public PlayerTeleportMenuEvent(@NotNull Player player, @NotNull Player destination, boolean forced, @NotNull PlayerTeleportEvent.TeleportCause cause) {
        super(player, player.getLocation(), destination.getLocation(), cause);
        this.destination = destination;
        this.forced = forced;
    }

    public Player getDestination() {
        return destination;
    }

    public Player getTarget() {
        return this.player;
    }

    public boolean isForced() {
        return forced;
    }
}