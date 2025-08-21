package one.txrsp.hktools.pathfinding;

import net.minecraft.block.DoorBlock;
import net.minecraft.block.SoulSandBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class AStarPathfinder {

    private static class Node {
        BlockPos pos;
        double g; // cost from start
        double h; // heuristic to goal
        double f; // total cost (g + h)
        Node parent;
    }

    private static final BlockPos[] NEIGHBOR_OFFSETS = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0)
    };

    public static List<BlockPos> findPath(BlockPos start, BlockPos goal, World world) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<BlockPos, Node> openMap = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        if (world.getBlockState(start).getBlock() instanceof SoulSandBlock) start = start.up();
        if (world.getBlockState(goal).getBlock() instanceof SoulSandBlock) goal = goal.up();

        Node startNode = new Node();
        startNode.pos = start;
        startNode.g = 0;
        startNode.h = heuristic(start, goal);
        startNode.f = startNode.g + startNode.h;
        open.add(startNode);
        openMap.put(start, startNode);

        while (!open.isEmpty()) {
            Node current = open.poll();
            openMap.remove(current.pos);
            if (current.pos.equals(goal)) {
                return reconstructPath(current);
            }

            closed.add(current.pos);

            for (BlockPos neighbor : getNeighbors(current.pos, world)) {
                if (closed.contains(neighbor)) continue;

                double gScore = current.g + cost(current, neighbor, goal);
                double hScore = heuristic(neighbor, goal);
                double fScore = gScore + hScore;

                Node existing = openMap.get(neighbor);
                if (existing == null || gScore < existing.g) {
                    Node next = new Node();
                    next.pos = neighbor;
                    next.g = gScore;
                    next.h = hScore;
                    next.f = fScore;
                    next.parent = current;

                    if (existing != null) open.remove(existing);
                    open.add(next);
                    openMap.put(neighbor, next);
                }
            }
        }

        return Collections.emptyList();
    }

    private static List<BlockPos> getNeighbors(BlockPos pos, World world) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    BlockPos np = pos.add(dx, dy, dz);

                    if (!(isPassable(world, np) && isPassable(world, np.up()))) continue;

                    // prevent diagonal corner cutting
                    if (dx != 0 && dz != 0) {
                        BlockPos adj1 = pos.add(dx, 0, 0);
                        BlockPos adj2 = pos.add(0, 0, dz);

                        // both adjacent axis-aligned blocks must be passable too (with clearance)
                        if (!(isPassable(world, adj1) && isPassable(world, adj1.up()))) continue;
                        if (!(isPassable(world, adj2) && isPassable(world, adj2.up()))) continue;
                    }

                    neighbors.add(np);
                }
            }
        }
        return neighbors;
    }

    private static double cost(Node current, BlockPos neighbor, BlockPos goal) {
        ClientWorld world = MinecraftClient.getInstance().world;
        double base = current.pos.getSquaredDistance(neighbor);

        int dy = Math.abs(neighbor.getY() - current.pos.getY());
        base += dy * 2.0;

        if (current.parent != null) {
            int dx1 = current.pos.getX() - current.parent.pos.getX();
            int dz1 = current.pos.getZ() - current.parent.pos.getZ();
            int dx2 = neighbor.getX() - current.pos.getX();
            int dz2 = neighbor.getZ() - current.pos.getZ();

            if (dx1 != dx2 || dz1 != dz2) {
                base += 3;
            }
        }

        if (isPassable(world, neighbor) && isPassable(world, neighbor.up())) {
            if (!isPassable(world, neighbor.up(2))) {
                base += 20.0;
            }
        }

        return base;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        double dx = Math.abs(a.getX() - b.getX());
        double dy = Math.abs(a.getY() - b.getY());
        double dz = Math.abs(a.getZ() - b.getZ());

        double dist = dx + dz;
        if (dist < 10) {
            return dx + dz + dy * 2.0;
        } else {
            return dx + dz + dy * 0.5;
        }
    }

    private static List<BlockPos> reconstructPath(Node end) {
        List<BlockPos> path = new ArrayList<>();
        Node current = end;
        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }
        Collections.reverse(path);
        return smoothPath(path, MinecraftClient.getInstance().world);
    }

    public static boolean isPassable(World world, BlockPos pos) {
        var state = world.getBlockState(pos);

        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty()) return true;
        if (state.getBlock() instanceof DoorBlock && state.get(DoorBlock.OPEN)) return true;
        if (state.getBlock() instanceof TrapdoorBlock && state.get(TrapdoorBlock.OPEN)) return true;

        var shape = state.getCollisionShape(world, pos);
        if (shape.isEmpty()) return true;

        if (shape.getBoundingBox().maxY < 1.0) {
            return false;
        }

        return false;
    }


    private static boolean canTravelDirectly(BlockPos from, BlockPos to, World world) {
        Vec3d start = new Vec3d(from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5);
        Vec3d end = new Vec3d(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5);
        Vec3d direction = end.subtract(start);
        double length = direction.length();
        direction = direction.normalize();

        double step = 0.25;
        int steps = (int)(length / step);

        for (int i = 0; i <= steps; i++) {
            Vec3d point = start.add(direction.multiply(i * step));
            BlockPos checkPos = BlockPos.ofFloored(point);

            if (!isPassable(world, checkPos) || !isPassable(world, checkPos.up())) return false;
        }

        return true;
    }

    private static List<BlockPos> smoothPath(List<BlockPos> path, World world) {
        if (path.size() <= 2) return path;

        List<BlockPos> result = new ArrayList<>();
        result.add(path.get(0));

        BlockPos prev = path.get(0);
        BlockPos curr = path.get(1);
        BlockPos lastDir = curr.subtract(prev);

        for (int i = 2; i < path.size(); i++) {
            BlockPos next = path.get(i);
            BlockPos newDir = next.subtract(curr);

            if (!newDir.equals(lastDir)) {
                result.add(curr);
                lastDir = newDir;
            }

            prev = curr;
            curr = next;
        }

        result.add(path.get(path.size() - 1));
        return result;
    }
}
