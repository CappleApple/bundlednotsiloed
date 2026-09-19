package com.cappleapple.bundlednotsiloed.network;

import java.util.ArrayList;
import java.util.List;

public final class InventorySnapshotPlan {
    public static final int TARGET_BYTES = 256 * 1024;
    private InventorySnapshotPlan() {}

    public static List<Chunk> chunks(List<Integer> encodedSizes) {
        ArrayList<Chunk> chunks = new ArrayList<>();
        int first = 0;
        long bytes = 0;
        for (int slot = 0; slot < encodedSizes.size(); slot++) {
            int size = encodedSizes.get(slot);
            if (size < 0) throw new IllegalArgumentException("Negative item size");
            if (slot > first && (slot - first >= ModNetwork.SNAPSHOT_CHUNK_SIZE || bytes + size > TARGET_BYTES)) {
                chunks.add(new Chunk(first, slot));
                first = slot;
                bytes = 0;
            }
            bytes += size;
        }
        if (first < encodedSizes.size() || chunks.isEmpty()) chunks.add(new Chunk(first, encodedSizes.size()));
        return List.copyOf(chunks);
    }
    public record Chunk(int from, int to) {}
}
