package com.danikvitek.kvadratutils;

import org.bukkit.Location;
import org.bukkit.block.CommandBlock;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

public class CommandBlockInstance {
    private static final HashSet<CommandBlockInstance> commandBlockInstances = new HashSet<>();

    private Vector position;
    private UUID world;
    private long commandsExecuted = 0L;

    public CommandBlockInstance(Location location) {
        if (location.getWorld() != null && location.getBlock().getState() instanceof CommandBlock) {
            world = location.getWorld().getUID();
            position = location.toVector().clone();
            commandBlockInstances.add(this);
        }
        else if (location.getWorld() == null)
            throw new IllegalArgumentException("Локация должна иметь в себе мир");
        else if (!(location.getBlock().getState() instanceof CommandBlock))
            throw new IllegalArgumentException("");
    }

    public long getCommandExecuted() {
        return commandsExecuted;
    }

    public void addCommandExecuted() {
        commandsExecuted++;
    }

    public static HashSet<CommandBlockInstance> getCommandBlockInstances() {
        return commandBlockInstances;
    }

    public void removeInstance() {
        commandBlockInstances.remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandBlockInstance)) return false;
        CommandBlockInstance that = (CommandBlockInstance) o;
        return position.equals(that.position) && world.equals(that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, world);
    }
}
