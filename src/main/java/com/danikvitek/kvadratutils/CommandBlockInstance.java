package com.danikvitek.kvadratutils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.CommandBlock;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

public class CommandBlockInstance {
    private static final HashMap<CommandBlockInstance, Long> commandBlockInstances = new HashMap<>();

    private Vector position;
    private UUID world;
    private boolean conditional;
    private Material type;
    private String command;

    public CommandBlockInstance(Location location, boolean fromCommandExecution) {
        if (location.getWorld() != null && location.getBlock().getState() instanceof CommandBlock) {
            CommandBlock commandBlock = (CommandBlock) location.getBlock().getState();
            world = location.getWorld().getUID();
            position = location.getBlock().getLocation().toVector().clone();
            conditional = ((org.bukkit.block.data.type.CommandBlock) commandBlock.getBlockData()).isConditional();
            type = commandBlock.getType();
            command = commandBlock.getCommand();
            if (!commandBlockInstances.containsKey(this))
                commandBlockInstances.put(this, 0L);
            else
                addCommandExecuted();
            if (fromCommandExecution)
                addCommandExecuted();
        }
        else if (location.getWorld() == null)
            throw new IllegalArgumentException("Локация должна иметь в себе мир");
        else if (!(location.getBlock().getState() instanceof CommandBlock))
            throw new IllegalArgumentException("Блок в указанной локации не командный");
    }

    public UUID getWorld() {
        return world;
    }

    public Vector getPosition() {
        return position;
    }

    public long getCommandsExecuted() {
        return commandBlockInstances.get(this);
    }

    public void addCommandExecuted() {
        commandBlockInstances.put(this, commandBlockInstances.get(this) + 1L);
    }

    public static HashSet<CommandBlockInstance> getCommandBlockInstances() {
        return new HashSet<>(commandBlockInstances.keySet());
    }

    public void removeInstance() {
        commandBlockInstances.remove(this);
    }

    public boolean isConditional() {
        return conditional;
    }

    public Material getType() {
        return type;
    }

    public String getCommand() {
        return command;
    }

    @Contract(value = "null -> false", pure = true)
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
