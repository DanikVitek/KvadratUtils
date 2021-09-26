package com.danikvitek.kvadratutils.utils;

import org.bukkit.Location;
import org.bukkit.World;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Converter {
    public static byte[] uuidToBytes(final UUID uuid) {
        return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
    }

    public static UUID uuidFromBytes(final byte[] bytes) {
        if (bytes.length < 2) { throw new IllegalArgumentException("Byte array too small."); }
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

    public static Location locationFromLong(@Nullable final World world, final long l) {
        return new Location(world, (int)(l >> 38), (int)(l << 26 >> 52), (int)(l << 38 >> 38));
    }

    public static long locationToLong(final Location l) {
        return ((long) l.getX() & 67108863) << 38 | ((long) l.getY() & 4095) << 26 | ((long) l.getZ() & 67108863);
    }
}