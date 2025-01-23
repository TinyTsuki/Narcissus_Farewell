package xin.vanilla.narcissus.util;

import lombok.NonNull;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.capability.IPlayerTeleportData;
import xin.vanilla.narcissus.capability.PlayerTeleportDataCapability;
import xin.vanilla.narcissus.capability.TeleportRecord;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.config.TeleportCost;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.enums.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NarcissusUtils {

    // region 安全坐标

    /**
     * TODO 不安全的方块
     */
    private static final Set<Block> UNSAFE_BLOCKS = new HashSet<>(Arrays.asList(
            Blocks.LAVA,
            Blocks.FIRE,
            Blocks.CAMPFIRE,
            Blocks.CACTUS,
            Blocks.MAGMA_BLOCK
    ));

    public static Coordinate findTopCandidate(ServerWorld world, Coordinate start) {
        if (start.getY() >= world.getMaxBuildHeight()) return null;
        for (int y : IntStream.range((int) start.getY() + 1, world.getMaxBuildHeight()).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue).reversed())
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findBottomCandidate(ServerWorld world, Coordinate start) {
        if (start.getY() <= 0) return null;
        for (int y : IntStream.range(0, (int) start.getY() - 1).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue))
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findUpCandidate(ServerWorld world, Coordinate start) {
        if (start.getY() >= world.getMaxBuildHeight()) return null;
        for (int y : IntStream.range((int) start.getY() + 1, world.getMaxBuildHeight()).boxed()
                .sorted(Comparator.comparingInt(a -> a - (int) start.getY()))
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findDownCandidate(ServerWorld world, Coordinate start) {
        if (start.getY() <= 0) return null;
        for (int y : IntStream.range(0, (int) start.getY() - 1).boxed()
                .sorted(Comparator.comparingInt(a -> (int) start.getY() - a))
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findSafeCoordinate(Coordinate coordinate) {
        World world = getWorld(coordinate.getDimension());

        int chunkX = (int) coordinate.getX() >> 4;
        int chunkZ = (int) coordinate.getZ() >> 4;

        // 搜索安全位置，限制在目标区块内
        return searchForSafeCoordinateInChunk(world, coordinate, chunkX, chunkZ);
    }

    private static Coordinate searchForSafeCoordinateInChunk(World world, Coordinate coordinate, int chunkX, int chunkZ) {
        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;

        // FIXME 1.18及之后版本range应该为(-64, world.getHeight())
        List<Integer> yList;
        List<Integer> xList;
        List<Integer> zList;
        if (coordinate.getSafeMode() == ESafeMode.Y_DOWN) {
            xList = new ArrayList<Integer>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<Integer>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) coordinate.getY(), 0).boxed()
                    .sorted(Comparator.comparingInt(a -> (int) coordinate.getY() - a))
                    .collect(Collectors.toList());
        } else if (coordinate.getSafeMode() == ESafeMode.Y_UP) {
            xList = new ArrayList<Integer>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<Integer>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) coordinate.getY(), world.getHeight()).boxed()
                    .sorted(Comparator.comparingInt(a -> a - (int) coordinate.getY()))
                    .collect(Collectors.toList());
        } else if (coordinate.getSafeMode() == ESafeMode.Y_OFFSET_3) {
            xList = new ArrayList<Integer>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<Integer>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) (coordinate.getY() - 3), (int) (coordinate.getY() + 3)).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getY())))
                    .collect(Collectors.toList());
        } else {
            xList = IntStream.range(chunkMinX, chunkMaxX).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getX())))
                    .collect(Collectors.toList());
            zList = IntStream.range(chunkMinZ, chunkMaxZ).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getZ())))
                    .collect(Collectors.toList());
            yList = IntStream.range(0, world.getHeight()).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getY())))
                    .collect(Collectors.toList());
        }
        for (int y : yList) {
            if (coordinate.getSafeMode() == ESafeMode.NONE && y <= 0 || (y <= 0 || y > world.getHeight())) continue;
            for (int x : xList) {
                for (int z : zList) {
                    Coordinate candidate = new Coordinate().setX(x + 0.25).setY(y + 0.1).setZ(z + 0.25)
                            .setYaw(coordinate.getYaw()).setPitch(coordinate.getPitch())
                            .setDimension(coordinate.getDimension())
                            .setSafe(coordinate.isSafe()).setSafeMode(coordinate.getSafeMode());
                    if (isSafeCoordinate(world, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return coordinate;
    }

    private static boolean isSafeCoordinate(World world, Coordinate coordinate) {
        BlockState block = world.getBlockState(coordinate.toBlockPos());
        BlockState blockAbove = world.getBlockState(coordinate.toBlockPos().above());
        BlockState blockBelow = world.getBlockState(coordinate.toBlockPos().below());
        return isSafeBlock(block, blockAbove, blockBelow);
    }

    private static boolean isSafeBlock(BlockState block, BlockState blockAbove, BlockState blockBelow) {
        return (block.getBlock() == Blocks.AIR || block.getBlock() == Blocks.CAVE_AIR)
                && (blockAbove.getBlock() == Blocks.AIR || blockAbove.getBlock() == Blocks.CAVE_AIR)
                && blockBelow.getMaterial().isSolid()
                && !UNSAFE_BLOCKS.contains(blockBelow.getBlock());
    }

    // endregion 安全坐标

    /**
     * 获取玩家当前位置的环境亮度
     *
     * @param player 当前玩家实体
     * @return 当前环境亮度（范围0-15）
     */
    public static int getEnvironmentBrightness(PlayerEntity player) {
        int result = 0;
        if (player != null) {
            World world = player.level;
            BlockPos pos = player.blockPosition();
            // 获取基础的天空光亮度和方块光亮度
            int skyLight = world.getBrightness(LightType.SKY, pos);
            int blockLight = world.getBrightness(LightType.BLOCK, pos);
            // 获取世界时间、天气和维度的影响
            boolean isDay = world.isDay();
            boolean isRaining = world.isRaining();
            boolean isThundering = world.isThundering();
            boolean isUnderground = !world.canSeeSky(pos);
            // 判断世界维度（地表、下界、末地）
            if (world.dimension() == World.OVERWORLD) {
                // 如果在地表
                if (!isUnderground) {
                    if (isDay) {
                        // 白天地表：最高亮度
                        result = isThundering ? 6 : isRaining ? 9 : 15;
                    } else {
                        // 夜晚地表
                        // 获取月相，0表示满月，4表示新月
                        int moonPhase = world.getMoonPhase();
                        result = getMoonBrightness(moonPhase, isThundering, isRaining);
                    }
                } else {
                    // 地下环境
                    // 没有光源时最黑，有光源则受距离影响
                    result = Math.max(Math.min(blockLight, 12), 0);
                }
            } else if (world.dimension() == World.NETHER) {
                // 下界亮度较暗，但部分地方有熔岩光源
                // 近光源则亮度提升，但不会超过10
                result = Math.min(7 + blockLight / 2, 10);
            } else if (world.dimension() == World.END) {
                // 末地亮度通常较暗
                // 即使贴近光源，末地的亮度上限设为10
                result = Math.min(6 + blockLight / 2, 10);
            } else {
                result = Math.max(skyLight, blockLight);
            }
        }
        // 其他维度或者无法判断的情况，返回环境和方块光的综合值
        return result;
    }

    /**
     * 根据月相、天气等条件获取夜间月光亮度
     *
     * @param moonPhase    月相（0到7，0为满月，4为新月）
     * @param isThundering 是否雷暴
     * @param isRaining    是否下雨
     * @return 夜间月光亮度
     */
    private static int getMoonBrightness(int moonPhase, boolean isThundering, boolean isRaining) {
        if (moonPhase == 0) {
            // 满月
            return isThundering ? 3 : isRaining ? 5 : 9;
        } else if (moonPhase == 4) {
            // 新月（最暗）
            return isThundering ? 1 : 2;
        } else {
            // 其他月相，亮度随月相变化逐渐减小
            int moonLight = 9 - moonPhase;
            return isThundering ? Math.max(moonLight - 3, 1) : isRaining ? Math.max(moonLight - 2, 1) : moonLight;
        }
    }

    /**
     * 获取指定维度的世界实例
     */
    public static ServerWorld getWorld(RegistryKey<World> dimension) {
        return NarcissusFarewell.getServerInstance().getLevel(dimension);
    }

    public static Biome getBiome(String id) {
        return getBiome(new ResourceLocation(id));
    }

    public static Biome getBiome(ResourceLocation id) {
        return NarcissusFarewell.getServerInstance().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null);
    }

    /**
     * 获取指定范围内某个生物群系位置
     *
     * @param world       世界
     * @param start       开始位置
     * @param biome       目标生物群系
     * @param radius      搜索半径
     * @param minDistance 最小距离
     */
    public static Coordinate findNearestBiome(ServerWorld world, Coordinate start, Biome biome, int radius, int minDistance) {
        // for (int x : IntStream.range((int) (start.getX() - radius), (int) (start.getX() + radius)).boxed()
        //         .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) start.getX())))
        //         .collect(Collectors.toList())) {
        //     for (int z : IntStream.range((int) (start.getZ() - radius), (int) (start.getZ() + radius)).boxed()
        //             .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) start.getZ())))
        //             .collect(Collectors.toList())) {
        //         Coordinate clone = start.clone();
        //         BlockPos pos = clone.setX(x).setZ(z).toBlockPos();
        //         Biome b = world.getBiome(pos);
        //         if (b == biome) {
        //             return clone;
        //         }
        //     }
        // }
        // // 未找到目标生物群系
        // return null;
        BlockPos pos = world.findNearestBiome(biome, start.toBlockPos(), radius, minDistance);
        if (pos != null) {
            return start.clone().setX(pos.getX()).setZ(pos.getZ()).setSafe(true);
        }
        return null;
    }

    public static Structure<?> getStructure(String id) {
        return getStructure(new ResourceLocation(id));
    }

    public static Structure<?> getStructure(ResourceLocation id) {
        return ForgeRegistries.STRUCTURE_FEATURES.getValue(id);
    }

    /**
     * 获取指定范围内某个生物群系位置
     *
     * @param world  世界
     * @param start  开始位置
     * @param struct 目标结构
     * @param radius 搜索半径
     */
    public static Coordinate findNearestStruct(ServerWorld world, Coordinate start, Structure<?> struct, int radius) {
        BlockPos pos = world.findNearestMapFeature(struct, start.toBlockPos(), radius, true);
        if (pos != null) {
            return start.clone().setX(pos.getX()).setZ(pos.getZ()).setSafe(true);
        }
        return null;
    }

    /**
     * 检查传送范围
     */
    public static int checkRange(ServerPlayerEntity player, int range) {
        if (range > ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get()) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "range_too_large"), ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get());
        } else if (range <= 0) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "range_too_small"), 1);
        }
        return Math.min(Math.max(range, 1), ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get());
    }

    /**
     * 执行传送请求
     */
    public static void teleportTo(@NonNull TeleportRequest request) {
        teleportTo(request.getRequester(), request.getTarget(), request.getTeleportType(), request.isSafe());
    }

    /**
     * 传送玩家到指定玩家
     *
     * @param from 传送者
     * @param to   目标玩家
     */
    public static void teleportTo(@NonNull ServerPlayerEntity from, @NonNull ServerPlayerEntity to, ETeleportType type, boolean safe) {
        if (ETeleportType.TP_HERE == type) {
            teleportTo(to, new Coordinate(from).setSafe(safe), type);
        } else {
            teleportTo(from, new Coordinate(to).setSafe(safe), type);
        }
    }

    /**
     * 传送玩家到指定坐标
     *
     * @param player     玩家
     * @param coordinate 坐标
     */
    public static void teleportTo(@NonNull ServerPlayerEntity player, @NonNull Coordinate coordinate, ETeleportType type) {
        World world = player.level;
        MinecraftServer server = player.getServer();
        if (world != null && server != null) {
            ServerWorld level = server.getLevel(coordinate.getDimension());
            if (level != null) {
                if (coordinate.isSafe()) {
                    coordinate = findSafeCoordinate(coordinate);
                }
                player.teleportTo(level, coordinate.getX(), coordinate.getY(), coordinate.getZ(), player.yRot, player.xRot);
                TeleportRecord record = new TeleportRecord();
                record.setTeleportTime(new Date());
                record.setTeleportType(type);
                record.setBefore(new Coordinate(player));
                record.setAfter(coordinate);
                PlayerTeleportDataCapability.getData(player).addTeleportRecords(record);
            }
        }
    }

    /**
     * 获取随机玩家
     */
    public static ServerPlayerEntity getRandomPlayer() {
        try {
            List<ServerPlayerEntity> players = NarcissusFarewell.getServerInstance().getPlayerList().getPlayers();
            return players.get(new Random().nextInt(players.size()));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取随机玩家UUID
     */
    public static UUID getRandomPlayerUUID() {
        PlayerEntity randomPlayer = getRandomPlayer();
        return randomPlayer != null ? randomPlayer.getUUID() : null;
    }

    /**
     * 通过UUID获取对应的玩家
     *
     * @param uuid 玩家UUID
     */
    public static ServerPlayerEntity getPlayer(UUID uuid) {
        try {
            return Minecraft.getInstance().level.getServer().getPlayerList().getPlayer(uuid);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 移除玩家背包中的指定物品
     *
     * @param player       玩家
     * @param itemToRemove 要移除的物品
     * @return 是否全部移除成功
     */
    public static boolean removeItemFromPlayerInventory(ServerPlayerEntity player, ItemStack itemToRemove) {
        IInventory inventory = player.inventory;

        // 剩余要移除的数量
        int remainingAmount = itemToRemove.getCount();
        // 记录成功移除的物品数量，以便失败时进行回滚
        int successfullyRemoved = 0;

        // 遍历玩家背包的所有插槽
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            // 获取背包中的物品
            ItemStack stack = inventory.getItem(i);
            ItemStack copy = itemToRemove.copy();
            copy.setCount(stack.getCount());

            // 如果插槽中的物品是目标物品
            if (stack.equals(copy, false)) {
                // 获取当前物品堆叠的数量
                int stackSize = stack.getCount();

                // 如果堆叠数量大于或等于剩余需要移除的数量
                if (stackSize >= remainingAmount) {
                    // 移除指定数量的物品
                    stack.shrink(remainingAmount);
                    // 记录成功移除的数量
                    successfullyRemoved += remainingAmount;
                    // 移除完毕
                    remainingAmount = 0;
                    break;
                } else {
                    // 移除该堆所有物品
                    stack.setCount(0);
                    // 记录成功移除的数量
                    successfullyRemoved += stackSize;
                    // 减少剩余需要移除的数量
                    remainingAmount -= stackSize;
                }
            }
        }

        // 如果没有成功移除所有物品，撤销已移除的部分
        if (remainingAmount > 0) {
            // 创建副本并还回成功移除的物品
            ItemStack copy = itemToRemove.copy();
            copy.setCount(successfullyRemoved);
            // 将已移除的物品添加回背包
            player.inventory.add(copy);
        }

        // 是否成功移除所有物品
        return remainingAmount == 0;
    }

    /**
     * 广播消息
     *
     * @param player  发送者
     * @param message 消息
     */
    public static void broadcastMessage(ServerPlayerEntity player, Component message) {
        player.server.getPlayerList().broadcastMessage(new TranslationTextComponent("chat.type.announcement", player.getDisplayName(), message.toTextComponent(player.getLanguage())), ChatType.SYSTEM, Util.NIL_UUID);
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(ServerPlayerEntity player, Component message) {
        player.sendMessage(message.toTextComponent(player.getLanguage()), player.getUUID());
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(ServerPlayerEntity player, String message) {
        player.sendMessage(Component.literal(message).toTextComponent(), player.getUUID());
    }

    /**
     * 发送翻译消息
     *
     * @param player 玩家
     * @param key    翻译键
     * @param args   参数
     */
    public static void sendTranslatableMessage(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(Component.translatable(key, args).setLanguageCode(player.getLanguage()).toTextComponent(), player.getUUID());
    }

    /**
     * 判断传送类型是否开启
     *
     * @param type 传送类型
     */
    public static boolean isTeleportEnabled(ETeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return ServerConfig.SWITCH_TP_COORDINATE.get();
            case TP_STRUCTURE:
                return ServerConfig.SWITCH_TP_STRUCTURE.get();
            case TP_ASK:
                return ServerConfig.SWITCH_TP_ASK.get();
            case TP_HERE:
                return ServerConfig.SWITCH_TP_HERE.get();
            case TP_RANDOM:
                return ServerConfig.SWITCH_TP_RANDOM.get();
            case TP_SPAWN:
                return ServerConfig.SWITCH_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
                return ServerConfig.SWITCH_TP_WORLD_SPAWN.get();
            case TP_TOP:
                return ServerConfig.SWITCH_TP_TOP.get();
            case TP_BOTTOM:
                return ServerConfig.SWITCH_TP_BOTTOM.get();
            case TP_UP:
                return ServerConfig.SWITCH_TP_UP.get();
            case TP_DOWN:
                return ServerConfig.SWITCH_TP_DOWN.get();
            case TP_VIEW:
                return ServerConfig.SWITCH_TP_VIEW.get();
            case TP_HOME:
                return ServerConfig.SWITCH_TP_HOME.get();
            case TP_STAGE:
                return ServerConfig.SWITCH_TP_STAGE.get();
            case TP_BACK:
                return ServerConfig.SWITCH_TP_BACK.get();
            default:
                return false;
        }
    }

    public static String getCommand(ETeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return ServerConfig.COMMAND_TP_COORDINATE.get();
            case TP_STRUCTURE:
                return ServerConfig.COMMAND_TP_STRUCTURE.get();
            case TP_ASK:
                return ServerConfig.COMMAND_TP_ASK.get();
            case TP_HERE:
                return ServerConfig.COMMAND_TP_HERE.get();
            case TP_RANDOM:
                return ServerConfig.COMMAND_TP_RANDOM.get();
            case TP_SPAWN:
                return ServerConfig.COMMAND_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
                return ServerConfig.COMMAND_TP_WORLD_SPAWN.get();
            case TP_TOP:
                return ServerConfig.COMMAND_TP_TOP.get();
            case TP_BOTTOM:
                return ServerConfig.COMMAND_TP_BOTTOM.get();
            case TP_UP:
                return ServerConfig.COMMAND_TP_UP.get();
            case TP_DOWN:
                return ServerConfig.COMMAND_TP_DOWN.get();
            case TP_VIEW:
                return ServerConfig.COMMAND_TP_VIEW.get();
            case TP_HOME:
                return ServerConfig.COMMAND_TP_HOME.get();
            case TP_STAGE:
                return ServerConfig.COMMAND_TP_STAGE.get();
            case TP_BACK:
                return ServerConfig.COMMAND_TP_BACK.get();
            default:
                return "";
        }
    }

    /**
     * 获取当前mod支持的mc版本
     *
     * @return 主版本*1000000+次版本*1000+修订版本， 如 1.16.5 -> 1 * 1000000 + 16 * 1000 + 5 = 10016005
     */
    public static int getMcVersion() {
        int version = 0;
        ModContainer container = ModList.get().getModContainerById(NarcissusFarewell.MODID).orElse(null);
        if (container != null) {
            IModInfo.ModVersion minecraftVersion = container.getModInfo().getDependencies().stream()
                    .filter(dependency -> dependency.getModId().equalsIgnoreCase("minecraft"))
                    .findFirst()
                    .orElse(null);
            if (minecraftVersion != null) {
                ArtifactVersion lowerBound = minecraftVersion.getVersionRange().getRestrictions().get(0).getLowerBound();
                int majorVersion = lowerBound.getMajorVersion();
                int minorVersion = lowerBound.getMinorVersion();
                int incrementalVersion = lowerBound.getIncrementalVersion();
                version = majorVersion * 1000000 + minorVersion * 1000 + incrementalVersion;
            }
        }
        return version;
    }

    // region 跨维度传送

    public static boolean isTeleportAcrossDimensionEnabled(ServerPlayerEntity player, RegistryKey<World> to, ETeleportType type) {
        boolean result = true;
        if (player.level.dimension() != to) {
            if (ServerConfig.TELEPORT_ACROSS_DIMENSION.get()) {
                if (!NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, type)) {
                    result = false;
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "across_dimension_not_enable_for"), getCommand(type));
                }
            } else {
                result = false;
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "across_dimension_not_enable"));
            }
        }
        return result;
    }

    /**
     * 判断传送类型跨维度传送是否开启
     */
    public static boolean isTeleportTypeAcrossDimensionEnabled(ServerPlayerEntity player, ETeleportType type) {
        int permission;
        switch (type) {
            case TP_COORDINATE:
                permission = ServerConfig.PERMISSION_TP_COORDINATE_ACROSS_DIMENSION.get();
                break;
            case TP_STRUCTURE:
                permission = ServerConfig.PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION.get();
                break;
            case TP_ASK:
                permission = ServerConfig.PERMISSION_TP_ASK_ACROSS_DIMENSION.get();
                break;
            case TP_HERE:
                permission = ServerConfig.PERMISSION_TP_HERE_ACROSS_DIMENSION.get();
                break;
            case TP_RANDOM:
                permission = ServerConfig.PERMISSION_TP_RANDOM_ACROSS_DIMENSION.get();
                break;
            case TP_SPAWN:
                permission = ServerConfig.PERMISSION_TP_SPAWN_ACROSS_DIMENSION.get();
                break;
            case TP_WORLD_SPAWN:
                permission = ServerConfig.PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION.get();
                break;
            case TP_HOME:
                permission = ServerConfig.PERMISSION_TP_HOME_ACROSS_DIMENSION.get();
                break;
            case TP_STAGE:
                permission = ServerConfig.PERMISSION_TP_STAGE_ACROSS_DIMENSION.get();
                break;
            case TP_BACK:
                permission = ServerConfig.PERMISSION_TP_BACK_ACROSS_DIMENSION.get();
                break;
            default:
                permission = 0;
                break;
        }
        return permission > -1 && player.hasPermissions(permission);
    }

    // endregion 跨维度传送

    // region 传送冷却

    /**
     * 获取传送/传送请求冷却时间
     *
     * @param player 玩家
     * @param type   传送类型
     */
    public static int getTeleportCoolDown(ServerPlayerEntity player, ETeleportType type) {
        // 如果传送卡类型为抵消冷却时间，则不计算冷却时间
        if (ServerConfig.TELEPORT_CARD_TYPE.get() == ECardType.REFUND_COOLDOWN || ServerConfig.TELEPORT_CARD_TYPE.get() == ECardType.REFUND_ALL_COST_AND_COOLDOWN) {
            if (PlayerTeleportDataCapability.getData(player).getTeleportCard() > 0) {
                return 0;
            }
        }
        Instant current = Instant.now();
        int commandCoolDown = getCommandCoolDown(type);
        Instant lastTpTime = PlayerTeleportDataCapability.getData(player).getTeleportRecords(type).stream()
                .map(TeleportRecord::getTeleportTime)
                .max(Comparator.comparing(Date::toInstant))
                .orElse(new Date(0)).toInstant();
        switch (ServerConfig.TELEPORT_REQUEST_COOLDOWN_TYPE.get()) {
            case COMMON:
                return calculateCooldown(player.getUUID(), current, lastTpTime, ServerConfig.TELEPORT_REQUEST_COOLDOWN.get(), null);
            case INDIVIDUAL:
                return calculateCooldown(player.getUUID(), current, lastTpTime, commandCoolDown, type);
            case MIXED:
                int globalCommandCoolDown = ServerConfig.TELEPORT_REQUEST_COOLDOWN.get();
                int individualCooldown = calculateCooldown(player.getUUID(), current, lastTpTime, commandCoolDown, type);
                int globalCooldown = calculateCooldown(player.getUUID(), current, lastTpTime, globalCommandCoolDown, null);
                return Math.max(individualCooldown, globalCooldown);
            default:
                return 0;
        }
    }

    /**
     * 获取传送命令冷却时间
     *
     * @param type 传送类型
     */
    public static int getCommandCoolDown(ETeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return ServerConfig.COOLDOWN_TP_COORDINATE.get();
            case TP_STRUCTURE:
                return ServerConfig.COOLDOWN_TP_STRUCTURE.get();
            case TP_ASK:
                return ServerConfig.COOLDOWN_TP_ASK.get();
            case TP_HERE:
                return ServerConfig.COOLDOWN_TP_HERE.get();
            case TP_RANDOM:
                return ServerConfig.COOLDOWN_TP_RANDOM.get();
            case TP_SPAWN:
                return ServerConfig.COOLDOWN_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
                return ServerConfig.COOLDOWN_TP_WORLD_SPAWN.get();
            case TP_TOP:
                return ServerConfig.COOLDOWN_TP_TOP.get();
            case TP_BOTTOM:
                return ServerConfig.COOLDOWN_TP_BOTTOM.get();
            case TP_UP:
                return ServerConfig.COOLDOWN_TP_UP.get();
            case TP_DOWN:
                return ServerConfig.COOLDOWN_TP_DOWN.get();
            case TP_VIEW:
                return ServerConfig.COOLDOWN_TP_VIEW.get();
            case TP_HOME:
                return ServerConfig.COOLDOWN_TP_HOME.get();
            case TP_STAGE:
                return ServerConfig.COOLDOWN_TP_STAGE.get();
            case TP_BACK:
                return ServerConfig.COOLDOWN_TP_BACK.get();
            default:
                return 0;
        }
    }

    private static int calculateCooldown(UUID uuid, Instant current, Instant lastTpTime, int cooldown, ETeleportType type) {
        Optional<TeleportRequest> latestRequest = NarcissusFarewell.getTeleportRequest().values().stream()
                .filter(request -> request.getRequester().getUUID().equals(uuid))
                .filter(request -> type == null || request.getTeleportType() == type)
                .max(Comparator.comparing(TeleportRequest::getRequestTime));

        Instant lastRequestTime = latestRequest.map(r -> r.getRequestTime().toInstant()).orElse(current.minusSeconds(cooldown));
        return Math.max(0, Math.max(cooldown - (int) Duration.between(lastRequestTime, current).getSeconds(), cooldown - (int) Duration.between(lastTpTime, current).getSeconds()));
    }

    // endregion 传送冷却

    // region 传送代价

    /**
     * 验证传送代价
     *
     * @param player 请求传送的玩家
     * @param target 目标坐标
     * @param type   传送类型
     * @param submit 是否收取代价
     * @return 是否验证通过
     */
    public static boolean validTeleportCost(ServerPlayerEntity player, Coordinate target, ETeleportType type, boolean submit) {
        return validateCost(player, target.getDimension(), calculateDistance(new Coordinate(player), target), type, submit);
    }

    /**
     * 验证并收取传送代价
     *
     * @param request 传送请求
     * @param submit  是否收取代价
     * @return 是否验证通过
     */
    public static boolean validTeleportCost(TeleportRequest request, boolean submit) {
        Coordinate requesterCoordinate = new Coordinate(request.getRequester());
        Coordinate targetCoordinate = new Coordinate(request.getTarget());
        return validateCost(request.getRequester(), request.getTarget().getLevel().dimension(), calculateDistance(requesterCoordinate, targetCoordinate), request.getTeleportType(), submit);
    }

    /**
     * 通用的传送代价验证逻辑
     *
     * @param player       请求传送的玩家
     * @param targetDim    目标维度
     * @param distance     计算的距离
     * @param teleportType 传送类型
     * @param submit       是否收取代价
     * @return 是否验证通过
     */
    private static boolean validateCost(ServerPlayerEntity player, RegistryKey<World> targetDim, double distance, ETeleportType teleportType, boolean submit) {
        TeleportCost cost = NarcissusUtils.getCommandCost(teleportType);
        if (cost.getType() == ECostType.NONE) return true;

        double adjustedDistance;
        if (player.getLevel().dimension() == targetDim) {
            adjustedDistance = Math.min(ServerConfig.TELEPORT_DISTANCE_LIMIT.get(), distance);
        } else {
            adjustedDistance = ServerConfig.TELEPORT_DISTANCE_ACROSS_DIMENSION.get();
        }

        double need = cost.getNum() * adjustedDistance * cost.getRate();
        int costNeed = getTeleportCostNeedPost(player, need);
        int cardNeed = getTeleportCardNeedPost(player, need);
        int cardNeedTotal = getTeleportCardNeedPre(player, need);
        boolean result = false;

        switch (cost.getType()) {
            case EXP_POINT:
                result = player.totalExperience >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(player.getLanguage(), EI18nType.WORD, "exp_point"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.giveExperiencePoints(-costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case EXP_LEVEL:
                result = player.experienceLevel >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(player.getLanguage(), EI18nType.WORD, "exp_level"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.giveExperienceLevels(-costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case HEALTH:
                result = player.getHealth() > costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(player.getLanguage(), EI18nType.WORD, "health"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.hurt(DamageSource.MAGIC, costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case HUNGER:
                result = player.getFoodData().getFoodLevel() >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(player.getLanguage(), EI18nType.WORD, "hunger"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case ITEM:
                try {
                    ItemStack itemStack = ItemStack.of(JsonToNBT.parseTag(cost.getConf()));
                    result = getItemCount(player.inventory.items, itemStack) >= costNeed && cardNeed == 0;
                    if (!result && cardNeed == 0) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), itemStack.getDisplayName(), (int) Math.ceil(need));
                    } else if (result && submit) {
                        itemStack.setCount(costNeed);
                        result = removeItemFromPlayerInventory(player, itemStack);
                        // 代价不足
                        if (result) {
                            PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                        } else {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), itemStack.getDisplayName(), (int) Math.ceil(need));
                        }
                    }
                } catch (Exception ignored) {
                }
                break;
            case COMMAND:
                try {
                    result = cardNeed == 0;
                    if (result && submit) {
                        String command = cost.getConf().replaceAll("\\[num]", String.valueOf(costNeed));
                        int commandResult = player.getServer().getCommands().performCommand(player.createCommandSourceStack(), command);
                        if (commandResult > 0) {
                            PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                        }
                        result = commandResult > 0;
                    }
                } catch (Exception ignored) {
                }
                break;
        }
        if (!result && cardNeed > 0) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(player.getLanguage(), EI18nType.WORD, "teleport_card"), (int) Math.ceil(need));
        }
        return result;
    }

    /**
     * 使用传送卡后还须支付多少代价
     */
    public static int getTeleportCostNeedPost(ServerPlayerEntity player, double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD.get()) return ceil;
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        switch (ServerConfig.TELEPORT_CARD_TYPE.get()) {
            case NONE:
                return data.getTeleportCard() > 0 ? ceil : -1;
            case LIKE_COST:
                return data.getTeleportCard() >= ceil ? ceil : -1;
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
                return Math.max(0, ceil - data.getTeleportCard());
            case REFUND_ALL_COST:
            case REFUND_ALL_COST_AND_COOLDOWN:
                return data.getTeleportCard() > 0 ? 0 : ceil;
            case REFUND_COOLDOWN:
            default:
                return ceil;
        }
    }

    /**
     * 须支付多少传送卡
     */
    public static int getTeleportCardNeedPre(ServerPlayerEntity player, double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD.get()) return 0;
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        switch (ServerConfig.TELEPORT_CARD_TYPE.get()) {
            case LIKE_COST:
                return ceil;
            case NONE:
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
            case REFUND_ALL_COST:
            case REFUND_ALL_COST_AND_COOLDOWN:
            case REFUND_COOLDOWN:
            default:
                return 1;
        }
    }

    /**
     * 使用传送卡后还须支付多少传送卡
     */
    public static int getTeleportCardNeedPost(ServerPlayerEntity player, double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD.get()) return 0;
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        switch (ServerConfig.TELEPORT_CARD_TYPE.get()) {
            case NONE:
                return data.getTeleportCard() > 0 ? 0 : 1;
            case LIKE_COST:
                return Math.max(0, ceil - data.getTeleportCard());
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
            case REFUND_ALL_COST:
            case REFUND_ALL_COST_AND_COOLDOWN:
            case REFUND_COOLDOWN:
            default:
                return 0;
        }
    }

    public static TeleportCost getCommandCost(ETeleportType type) {
        TeleportCost cost = new TeleportCost();
        switch (type) {
            case TP_COORDINATE:
                cost.setType(ServerConfig.COST_TP_COORDINATE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_COORDINATE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_COORDINATE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_COORDINATE_CONF.get());
                break;
            case TP_STRUCTURE:
                cost.setType(ServerConfig.COST_TP_STRUCTURE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_STRUCTURE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_STRUCTURE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_STRUCTURE_CONF.get());
                break;
            case TP_ASK:
                cost.setType(ServerConfig.COST_TP_ASK_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_ASK_NUM.get());
                cost.setRate(ServerConfig.COST_TP_ASK_RATE.get());
                cost.setConf(ServerConfig.COST_TP_ASK_CONF.get());
                break;
            case TP_HERE:
                cost.setType(ServerConfig.COST_TP_HERE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_HERE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_HERE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_HERE_CONF.get());
                break;
            case TP_RANDOM:
                cost.setType(ServerConfig.COST_TP_RANDOM_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_RANDOM_NUM.get());
                cost.setRate(ServerConfig.COST_TP_RANDOM_RATE.get());
                cost.setConf(ServerConfig.COST_TP_RANDOM_CONF.get());
                break;
            case TP_SPAWN:
                cost.setType(ServerConfig.COST_TP_SPAWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_SPAWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_SPAWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_SPAWN_CONF.get());
                break;
            case TP_WORLD_SPAWN:
                cost.setType(ServerConfig.COST_TP_WORLD_SPAWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_WORLD_SPAWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_WORLD_SPAWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_WORLD_SPAWN_CONF.get());
                break;
            case TP_TOP:
                cost.setType(ServerConfig.COST_TP_TOP_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_TOP_NUM.get());
                cost.setRate(ServerConfig.COST_TP_TOP_RATE.get());
                cost.setConf(ServerConfig.COST_TP_TOP_CONF.get());
                break;
            case TP_BOTTOM:
                cost.setType(ServerConfig.COST_TP_BOTTOM_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_BOTTOM_NUM.get());
                cost.setRate(ServerConfig.COST_TP_BOTTOM_RATE.get());
                cost.setConf(ServerConfig.COST_TP_BOTTOM_CONF.get());
                break;
            case TP_UP:
                cost.setType(ServerConfig.COST_TP_UP_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_UP_NUM.get());
                cost.setRate(ServerConfig.COST_TP_UP_RATE.get());
                cost.setConf(ServerConfig.COST_TP_UP_CONF.get());
                break;
            case TP_DOWN:
                cost.setType(ServerConfig.COST_TP_DOWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_DOWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_DOWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_DOWN_CONF.get());
                break;
            case TP_VIEW:
                cost.setType(ServerConfig.COST_TP_VIEW_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_VIEW_NUM.get());
                cost.setRate(ServerConfig.COST_TP_VIEW_RATE.get());
                cost.setConf(ServerConfig.COST_TP_VIEW_CONF.get());
                break;
            case TP_HOME:
                cost.setType(ServerConfig.COST_TP_HOME_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_HOME_NUM.get());
                cost.setRate(ServerConfig.COST_TP_HOME_RATE.get());
                cost.setConf(ServerConfig.COST_TP_HOME_CONF.get());
                break;
            case TP_STAGE:
                cost.setType(ServerConfig.COST_TP_STAGE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_STAGE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_STAGE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_STAGE_CONF.get());
                break;
            case TP_BACK:
                cost.setType(ServerConfig.COST_TP_BACK_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_BACK_NUM.get());
                cost.setRate(ServerConfig.COST_TP_BACK_RATE.get());
                cost.setConf(ServerConfig.COST_TP_BACK_CONF.get());
                break;
            default:
                break;
        }
        return cost;
    }

    public static int getItemCount(List<ItemStack> items, ItemStack itemStack) {
        ItemStack copy = itemStack.copy();
        return items.stream().filter(item -> {
            copy.setCount(item.getCount());
            return item.equals(copy, false);
        }).mapToInt(ItemStack::getCount).sum();
    }

    public static double calculateDistance(Coordinate coordinate1, Coordinate coordinate2) {
        double deltaX = coordinate1.getX() - coordinate2.getX();
        double deltaY = coordinate1.getY() - coordinate2.getY();
        double deltaZ = coordinate1.getZ() - coordinate2.getZ();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    // endregion 传送代价
}