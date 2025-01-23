package xin.vanilla.narcissus.command;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.enums.EI18nType;
import xin.vanilla.narcissus.enums.ESafeMode;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class FarewellCommand {

    public static int HELP_INFO_NUM_PER_PAGE = 5;

    public static final List<KeyValue<String, String>> HELP_MESSAGE = new ArrayList<KeyValue<String, String>>() {{
        add(new KeyValue<>("narcissus help", "nf_help"));

        if (ServerConfig.CONCISE_TP_COORDINATE.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_COORDINATE.get(), "tp_coordinate_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_COORDINATE.get(), "tp_coordinate"));       // 传送至指定位置

        if (ServerConfig.CONCISE_TP_ASK.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_ASK.get(), "tp_ask_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_ASK.get(), "tp_ask"));                     // 请求传送至玩家
        if (ServerConfig.CONCISE_TP_ASK_YES.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_ASK_YES.get(), "tp_ask_yes_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_ASK_YES.get(), "tp_ask_yes"));             // 同意请求传送至玩家
        if (ServerConfig.CONCISE_TP_ASK_NO.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_ASK_NO.get(), "tp_ask_no_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_ASK_NO.get(), "tp_ask_no"));               // 拒绝请求传送至玩家

        if (ServerConfig.CONCISE_TP_HERE.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_HERE.get(), "tp_here_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_HERE.get(), "tp_here"));                   // 请求将玩家传送过来
        if (ServerConfig.CONCISE_TP_HERE_YES.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_HERE_YES.get(), "tp_here_yes_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_HERE_YES.get(), "tp_here_yes"));           // 同意请求将玩家传送过来
        if (ServerConfig.CONCISE_TP_HERE_NO.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_HERE_NO.get(), "tp_here_no_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_HERE_NO.get(), "tp_here_no"));             // 拒绝请求将玩家传送过来

        if (ServerConfig.CONCISE_TP_RANDOM.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_RANDOM.get(), "tp_random_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_RANDOM.get(), "tp_random"));               // 随机传送，允许指定范围
        if (ServerConfig.CONCISE_TP_SPAWN.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_SPAWN.get(), "tp_spawn_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_SPAWN.get(), "tp_spawn"));                 // 传送至出生点
        if (ServerConfig.CONCISE_TP_WORLD_SPAWN.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_WORLD_SPAWN.get(), "tp_world_spawn_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_WORLD_SPAWN.get(), "tp_world_spawn"));     // 传送至世界出生点
        if (ServerConfig.CONCISE_TP_TOP.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_TOP.get(), "tp_top_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_TOP.get(), "tp_top"));                     // 传送至头顶最上方方块
        if (ServerConfig.CONCISE_TP_BOTTOM.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_BOTTOM.get(), "tp_bottom_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_BOTTOM.get(), "tp_bottom"));               // 传送至脚下最下方方块
        if (ServerConfig.CONCISE_TP_UP.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_UP.get(), "tp_up_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_UP.get(), "tp_up"));                       // 传送至头顶最近方块
        if (ServerConfig.CONCISE_TP_DOWN.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_DOWN.get(), "tp_down_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_DOWN.get(), "tp_down"));                   // 传送至脚下最近方块
        if (ServerConfig.CONCISE_TP_VIEW.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_VIEW.get(), "tp_view_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_VIEW.get(), "tp_view"));                   // 传送至视线尽头

        if (ServerConfig.CONCISE_TP_HOME.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_HOME.get(), "tp_home_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_HOME.get(), "home"));                      // 回到设置的传送点
        if (ServerConfig.CONCISE_SET_HOME.get())
            add(new KeyValue<>(ServerConfig.COMMAND_SET_HOME.get(), "tp_home_default_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_SET_HOME.get(), "set_home"));                 // 添加传送点，允许添加多个，允许设置默认传送点，不同维度的默认传送点可以不同
        if (ServerConfig.CONCISE_DEL_HOME.get())
            add(new KeyValue<>(ServerConfig.COMMAND_DEL_HOME.get(), "tp_home_del_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_DEL_HOME.get(), "del_home"));                 // 删除传送点

        if (ServerConfig.CONCISE_TP_STAGE.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_STAGE.get(), "tp_stage_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_STAGE.get(), "stage"));                    // 传送至驿站
        if (ServerConfig.CONCISE_SET_STAGE.get())
            add(new KeyValue<>(ServerConfig.COMMAND_SET_STAGE.get(), "tp_stage_add_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_SET_STAGE.get(), "add_stage"));               // 添加驿站
        if (ServerConfig.CONCISE_DEL_STAGE.get())
            add(new KeyValue<>(ServerConfig.COMMAND_DEL_STAGE.get(), "tp_stage_del_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_DEL_STAGE.get(), "del_stage"));               // 删除驿站

        if (ServerConfig.CONCISE_TP_BACK.get())
            add(new KeyValue<>(ServerConfig.COMMAND_TP_BACK.get(), "tp_back_concise"));
        add(new KeyValue<>("narcissus " + ServerConfig.COMMAND_TP_BACK.get(), "back"));                      // 返回上次离开地方，允许选择类型：死亡时位置、tpa前位置、tpr前位置、home前位置，允许指定维度

    }};

    /*
        OP权限等级：
            1：绕过服务器原版的出生点保护系统，可以破坏出生点地形。
            2：使用原版单机一切作弊指令（除了/publish，因为其只能在单机使用，/debug也不能使用）。
            3：可以使用大多数多人游戏指令，例如/op，/ban（/debug属于3级OP使用的指令）。
            4：使用所有命令，可以使用/stop关闭服务器。

        ‌颜色代码‌：
            §0：黑色 §1：深蓝 §2：深绿 §3：天蓝
            §4：红色 §5：深紫 §6：金黄 §7：浅灰
            §8：深灰 §9：淡紫 §a：浅绿 §b：淡蓝
            §c：淡红 §d：淡紫 §e：淡黄 §f：白色
    */

    /**
     * 注册命令到命令调度器
     *
     * @param dispatcher 命令调度器，用于管理服务器中的所有命令
     */
    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        Command<CommandSource> helpCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            int page = 1;
            try {
                page = IntegerArgumentType.getInteger(context, "page");
            } catch (IllegalArgumentException ignored) {
            }
            int pages = (int) Math.ceil((double) HELP_MESSAGE.size() / HELP_INFO_NUM_PER_PAGE);
            if (page < 1 || page > pages) {
                throw new IllegalArgumentException("page must be between 1 and " + (HELP_MESSAGE.size() / HELP_INFO_NUM_PER_PAGE));
            }
            Component helpInfo = Component.literal("-----==== Narcissus Farewell Help (" + page + "/" + pages + ") ====-----\n");
            for (int i = 0; (page - 1) * HELP_INFO_NUM_PER_PAGE + i < HELP_MESSAGE.size() && i < HELP_INFO_NUM_PER_PAGE; i++) {
                KeyValue<String, String> keyValue = HELP_MESSAGE.get((page - 1) * HELP_INFO_NUM_PER_PAGE + i);
                Component commandTips = Component.translatable(player.getLanguage(), EI18nType.COMMAND, keyValue.getValue());
                commandTips.setColor(TextFormatting.GRAY.getColor());
                helpInfo.append("/").append(keyValue.getKey())
                        .append(new Component(" -> ").setColor(TextFormatting.YELLOW.getColor()))
                        .append(commandTips);
                if (i != HELP_MESSAGE.size() - 1) {
                    helpInfo.append("\n");
                }
            }
            NarcissusUtils.sendMessage(player, helpInfo);
            return 1;
        };

        // SuggestionProvider<CommandSource> dimensionSuggestions = (context, builder) -> {
        //     for (ServerWorld level : context.getSource().getServer().getAllLevels()) {
        //         builder.suggest(level.dimension().location().toString());
        //     }
        //     return builder.buildFuture();
        // };

        SuggestionProvider<CommandSource> safeSuggestions = (context, builder) -> {
            builder.suggest("safe");
            builder.suggest("unsafe");
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSource> rangeSuggestions = (context, builder) -> {
            for (int i = 1; i <= 5; i++) {
                int index = (int) Math.pow(10, i);
                if (index <= ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get()) {
                    builder.suggest(index);
                }
            }
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSource> structureSuggestions = (context, builder) -> {
            StringRange stringRange = context.getNodes().stream()
                    .filter(o -> o.getNode().getName().equalsIgnoreCase("struct"))
                    .map(ParsedCommandNode::getRange)
                    .findFirst().orElse(new StringRange(0, 0));
            String input = context.getInput().substring(stringRange.getStart(), stringRange.getEnd());
            boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
            ForgeRegistries.STRUCTURE_FEATURES.getKeys().stream()
                    .filter(resourceLocation -> isInputEmpty || resourceLocation.toString().contains(input))
                    .forEach(location -> builder.suggest(location.toString()));
            ForgeRegistries.BIOMES.getValues().stream()
                    .filter(resourceLocation -> isInputEmpty || resourceLocation.toString().contains(input))
                    .forEach(biome -> builder.suggest(biome.toString()));
            return builder.buildFuture();
        };

        Command<CommandSource> tpCoordinateCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_COORDINATE)) return 0;
            Coordinate coordinate;
            try {
                BlockPos pos = BlockPosArgument.getOrLoadBlockPos(context, "coordinate");
                RegistryKey<World> targetLevel;
                try {
                    targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
                } catch (IllegalArgumentException ignored) {
                    targetLevel = player.getLevel().dimension();
                }
                coordinate = new Coordinate(pos.getX(), pos.getY(), pos.getZ(), player.yRot, player.xRot, targetLevel);
            } catch (IllegalArgumentException ignored) {
                ServerPlayerEntity target = EntityArgument.getPlayer(context, "player");
                coordinate = new Coordinate(target.getX(), target.getY(), target.getZ(), target.yRot, target.xRot, target.getLevel().dimension());
            }
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_COORDINATE, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_COORDINATE);
            return 1;
        };

        Command<CommandSource> tpStructureCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_STRUCTURE)) return 0;
            ResourceLocation structId = ResourceLocationArgument.getId(context, "struct");
            Structure<?> structure = NarcissusUtils.getStructure(structId);
            Biome biome = NarcissusUtils.getBiome(structId);
            if (structure == null && biome == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "structure_biome_not_found"), structId);
                return 0;
            }
            int range;
            RegistryKey<World> targetLevel;
            try {
                range = IntegerArgumentType.getInteger(context, "range");
            } catch (IllegalArgumentException ignored) {
                range = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get();
            }
            range = NarcissusUtils.checkRange(player, range);
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            } catch (IllegalArgumentException ignored) {
                targetLevel = player.getLevel().dimension();
            }
            Coordinate coordinate;
            if (biome != null) {
                coordinate = NarcissusUtils.findNearestBiome(Objects.requireNonNull(NarcissusFarewell.getServerInstance().getLevel(targetLevel)), new Coordinate(player), biome, range, 8);
            } else {
                coordinate = NarcissusUtils.findNearestStruct(Objects.requireNonNull(NarcissusFarewell.getServerInstance().getLevel(targetLevel)), new Coordinate(player), structure, range);
            }
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "structure_biome_not_found_in_range"), structId);
                return 0;
            }
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_STRUCTURE, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_STRUCTURE);
            return 1;
        };

        Command<CommandSource> tpAskCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_ASK)) return 0;
            ServerPlayerEntity target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                // 如果没有指定目标玩家，则使用最近一次传送请求的目标玩家，依旧没有就随机一名幸运玩家
                target = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(request -> request.getRequester().getUUID().equals(player.getUUID()))
                        .filter(request -> {
                            PlayerEntity entity = request.getTarget();
                            return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, ETeleportType.TP_ASK)
                                    || entity != null && entity.level.dimension() == player.getLevel().dimension();
                        })
                        .max(Comparator.comparing(TeleportRequest::getRequestTime))
                        .orElse(new TeleportRequest().setTarget(NarcissusFarewell.getLastTeleportRequest()
                                .getOrDefault(player, NarcissusUtils.getRandomPlayer())))
                        .getTarget();
            }
            if (target == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "player_not_found"));
                return 0;
            }

            // 验证并添加传送请求
            TeleportRequest request = new TeleportRequest()
                    .setRequester(player)
                    .setTarget(target)
                    .setTeleportType(ETeleportType.TP_ASK)
                    .setRequestTime(new Date());
            try {
                request.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
            }
            if (checkTeleportPost(request)) return 0;
            NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request);

            // 通知目标玩家
            {
                // 创建 "Yes" 按钮
                Component yesButton = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "yes_button", target.getLanguage())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusFarewell.COMMAND_PREFIX, ServerConfig.COMMAND_TP_ASK_YES.get(), request.getRequestId())));
                // 创建 "No" 按钮
                Component noButton = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "no_button", target.getLanguage())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusFarewell.COMMAND_PREFIX, ServerConfig.COMMAND_TP_ASK_NO.get(), request.getRequestId())));
                Component msg = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "tp_ask_request_received"
                        , target.getDisplayName().getString(), yesButton, noButton);
                NarcissusUtils.sendMessage(target, msg);
            }
            // 通知请求者
            {
                NarcissusUtils.sendMessage(player, Component.translatable(player.getLanguage(), EI18nType.MESSAGE, "tp_ask_request_sent", target.getDisplayName().getString()));
            }
            return 1;
        };

        Command<CommandSource> tpAskYesCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_ASK);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            if (checkTeleportPost(request, true)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_invalid"));
                return 0;
            }
            NarcissusUtils.teleportTo(request);
            return 1;
        };

        Command<CommandSource> tpAskNoCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_ASK);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_rejected"), request.getTarget().getDisplayName().getString());
            return 1;
        };

        Command<CommandSource> tpHereCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_HERE)) return 0;
            ServerPlayerEntity target;
            try {
                target = EntityArgument.getPlayer(context, "player");
            } catch (IllegalArgumentException ignored) {
                // 如果没有指定目标玩家，则使用最近一次传送请求的目标玩家，依旧没有就随机一名幸运玩家
                target = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(request -> request.getRequester().getUUID().equals(player.getUUID()))
                        .filter(request -> {
                            PlayerEntity entity = request.getTarget();
                            return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, ETeleportType.TP_HERE)
                                    || entity != null && entity.level.dimension() == player.getLevel().dimension();
                        })
                        .max(Comparator.comparing(TeleportRequest::getRequestTime))
                        .orElse(new TeleportRequest().setTarget(NarcissusFarewell.getLastTeleportRequest()
                                .getOrDefault(player, NarcissusUtils.getRandomPlayer())))
                        .getTarget();
            }
            if (target == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "player_not_found"));
                return 0;
            }

            // 验证并添加传送请求
            TeleportRequest request = new TeleportRequest()
                    .setRequester(player)
                    .setTarget(target)
                    .setTeleportType(ETeleportType.TP_HERE)
                    .setRequestTime(new Date());
            try {
                request.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
            }
            if (checkTeleportPost(request)) return 0;
            NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request);

            // 通知目标玩家
            {
                // 创建 "Yes" 按钮
                Component yesButton = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "yes_button", target.getLanguage())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusFarewell.COMMAND_PREFIX, ServerConfig.COMMAND_TP_HERE_YES.get(), request.getRequestId())));
                // 创建 "No" 按钮
                Component noButton = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "no_button", target.getLanguage())
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusFarewell.COMMAND_PREFIX, ServerConfig.COMMAND_TP_HERE_NO.get(), request.getRequestId())));
                Component msg = Component.translatable(target.getLanguage(), EI18nType.MESSAGE, "tp_here_request_received"
                        , target.getDisplayName().getString(), Component.translatable(target.getLanguage(), EI18nType.WORD, request.isSafe() ? "tp_here_safe" : "tp_here_unsafe"), yesButton, noButton);
                NarcissusUtils.sendMessage(target, msg);
            }
            // 通知请求者
            {
                NarcissusUtils.sendMessage(player, Component.translatable(player.getLanguage(), EI18nType.MESSAGE, "tp_here_request_sent", target.getDisplayName().getString()));
            }
            return 1;
        };

        Command<CommandSource> tpHereYesCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_HERE);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            if (checkTeleportPost(request, true)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_invalid"));
                return 0;
            }
            NarcissusUtils.teleportTo(request);
            return 1;
        };

        Command<CommandSource> tpHereNoCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            String id = getRequestId(context, ETeleportType.TP_HERE);
            if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_not_found"));
                return 0;
            }
            TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EI18nType.MESSAGE, "tp_here_rejected"), request.getTarget().getDisplayName().getString());
            return 1;
        };

        Command<CommandSource> tpRandomCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_RANDOM)) return 0;
            RegistryKey<World> targetLevel;
            int range;
            try {
                range = IntegerArgumentType.getInteger(context, "range");
            } catch (IllegalArgumentException ignored) {
                range = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get();
            }
            range = NarcissusUtils.checkRange(player, range);
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            } catch (IllegalArgumentException ignored) {
                targetLevel = player.getLevel().dimension();
            }
            Coordinate coordinate = Coordinate.random(player, range, targetLevel).setSafe(true);
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_RANDOM, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_RANDOM);
            return 1;
        };

        Command<CommandSource> tpSpawnCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_SPAWN)) return 0;
            BlockPos respawnPosition = player.getRespawnPosition();
            if (respawnPosition == null) {
                respawnPosition = player.getLevel().getSharedSpawnPos();
            }
            Coordinate coordinate = new Coordinate(player).fromBlockPos(respawnPosition);
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_SPAWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_SPAWN);
            return 1;
        };

        Command<CommandSource> tpWorldSpawnCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_WORLD_SPAWN)) return 0;
            BlockPos respawnPosition = player.getLevel().getSharedSpawnPos();
            Coordinate coordinate = new Coordinate(player).fromBlockPos(respawnPosition);
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_WORLD_SPAWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_WORLD_SPAWN);
            return 1;
        };

        Command<CommandSource> tpTopCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_TOP)) return 0;
            Coordinate coordinate = NarcissusUtils.findTopCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_top_not_found"));
                return 0;
            }
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe"))).setSafeMode(ESafeMode.Y_DOWN);
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true).setSafeMode(ESafeMode.Y_DOWN);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_TOP, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_TOP);
            return 1;
        };

        Command<CommandSource> tpBottomCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_BOTTOM)) return 0;
            Coordinate coordinate = NarcissusUtils.findBottomCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_bottom_not_found"));
                return 0;
            }
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe"))).setSafeMode(ESafeMode.Y_UP);
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true).setSafeMode(ESafeMode.Y_UP);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_BOTTOM, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_BOTTOM);
            return 1;
        };

        Command<CommandSource> tpUpCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_UP)) return 0;
            Coordinate coordinate = NarcissusUtils.findUpCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_up_not_found"));
                return 0;
            }
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe"))).setSafeMode(ESafeMode.Y_UP);
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true).setSafeMode(ESafeMode.Y_UP);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_UP, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_UP);
            return 1;
        };

        Command<CommandSource> tpDownCommand = context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            // 传送功能前置校验
            if (checkTeleportPre(player, ETeleportType.TP_DOWN)) return 0;
            Coordinate coordinate = NarcissusUtils.findDownCandidate(player.getLevel(), new Coordinate(player));
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_down_not_found"));
                return 0;
            }
            try {
                coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe"))).setSafeMode(ESafeMode.Y_DOWN);
            } catch (IllegalArgumentException ignored) {
                coordinate.setSafe(true).setSafeMode(ESafeMode.Y_DOWN);
            }
            // 验证传送代价
            if (checkTeleportPost(player, coordinate, ETeleportType.TP_DOWN, true)) return 0;
            NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_DOWN);
            return 1;
        };

        // Command<CommandSource> tpViewCommand = context -> {
        //     ServerPlayerEntity player = context.getSource().getPlayerOrException();
        //     // 传送功能前置校验
        //     if (checkTeleportPre(player, ETeleportType.TP_VIEW)) return 0;
        //     Coordinate coordinate = new Coordinate(player).setY(player.getY() + 10);
        //     try {
        //         coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
        //     } catch (IllegalArgumentException ignored) {
        //         coordinate.setSafe(true);
        //     }
        //     // 验证传送代价
        //     if (checkTeleportPost(player, coordinate, ETeleportType.TP_VIEW, true)) return 0;
        //     NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_VIEW);
        //     return 1;
        // };
        //
        // Command<CommandSource> tpHomeCommand = context -> {
        //     ServerPlayerEntity player = context.getSource().getPlayerOrException();
        //     // 传送功能前置校验
        //     if (checkTeleportPre(player, ETeleportType.TP_HOME)) return 0;
        //     Coordinate coordinate = NarcissusUtils.getHome(player);
        //     if (coordinate == null) {
        //         NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found"));
        //         return 0;
        //     }
        //     try {
        //         coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
        //     } catch (IllegalArgumentException ignored) {
        //     }
        //     // 验证传送代价
        //     if (checkTeleportPost(player, coordinate, ETeleportType.TP_HOME, true)) return 0;
        //     NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_HOME);
        //     return 1;
        // };
        //
        // Command<CommandSource> setHomeCommand = context -> {
        //     ServerPlayerEntity player = context.getSource().getPlayerOrException();
        //     // 传送功能前置校验
        //     if (checkTeleportPre(player, ETeleportType.SET_HOME)) return 0;
        //     Coordinate coordinate = new Coordinate(player);
        //     try {
        //         coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
        //     } catch (IllegalArgumentException ignored) {
        //     }
        //     // 验证传送代价
        //     if (checkTeleportPost(player, coordinate, ETeleportType.SET_HOME, true)) return 0;
        //     NarcissusUtils.setHome(player, coordinate);
        //     return 1;
        // };
        //
        // Command<CommandSource> delHomeCommand = context -> {
        //     ServerPlayerEntity player = context.getSource().getPlayerOrException();
        //     // 传送功能前置校验
        //     if (checkTeleportPre(player, ETeleportType.DEL_HOME)) return 0;
        //     Coordinate coordinate = NarcissusUtils.getHome(player);
        //     if (coordinate == null) {
        //         NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "home_not_found"));
        //         return 0;
        //     }
        //     NarcissusUtils.delHome(player);
        //     return 1;
        // };
        //
        // Command<CommandSource> tpStageCommand = context -> {
        //     ServerPlayerEntity player = context.getSource().getPlayerOrException();
        //     // 传送功能前置校验
        //     if (checkTeleportPre(player, ETeleportType.TP_STAGE)) return 0;
        //     Coordinate coordinate = NarcissusUtils.getStage(player);
        //     if (coordinate == null) {
        //         NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_not_found"));
        //         return 0;
        //     }
        //     try {
        //         coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
        //     } catch (IllegalArgumentException ignored) {
        //     }
        //     // 验证传送代价
        //     if (checkTeleportPost(player, coordinate, ETeleportType.TP_STAGE, true)) return 0;
        //     NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_STAGE);
        //     return 1;
        // };
        //
        // Command<CommandSource> setStageCommand = context -> {
        //     ServerPlayerEntity player = context.getSource().getPlayerOrException();
        //     // 传送功能前置校验
        //     if (checkTeleportPre(player, ETeleportType.SET_STAGE)) return 0;
        //     Coordinate coordinate = new Coordinate(player);
        //     try {
        //         coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
        //     } catch (IllegalArgumentException ignored) {
        //     }
        //     // 验证传送代价
        //     if (checkTeleportPost(player, coordinate, ETeleportType.SET_STAGE, true)) return 0;
        //     NarcissusUtils.setStage(player, coordinate);
        //     return 1;
        // };
        //
        // Command<CommandSource> delStageCommand = context -> {
        //     ServerPlayerEntity player = context.getSource().getPlayerOrException();
        //     // 传送功能前置校验
        //     if (checkTeleportPre(player, ETeleportType.DEL_STAGE)) return 0;
        //     Coordinate coordinate = NarcissusUtils.getStage(player);
        //     if (coordinate == null) {
        //         NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "stage_not_found"));
        //         return 0;
        //     }
        //     NarcissusUtils.delStage(player);
        //     return 1;
        // };
        //
        // Command<CommandSource> tpBackCommand = context -> {
        //     ServerPlayerEntity player = context.getSource().getPlayerOrException();
        //     // 传送功能前置校验
        //     if (checkTeleportPre(player, ETeleportType.TP_BACK)) return 0;
        //     Coordinate coordinate = NarcissusUtils.getBack(player);
        //     if (coordinate == null) {
        //         NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "back_not_found"));
        //         return 0;
        //     }
        //     try {
        //         coordinate.setSafe("safe".equalsIgnoreCase(StringArgumentType.getString(context, "safe")));
        //     } catch (IllegalArgumentException ignored) {
        //     }
        //     // 验证传送代价
        //     if (checkTeleportPost(player, coordinate, ETeleportType.TP_BACK, true)) return 0;
        //     NarcissusUtils.teleportTo(player, coordinate, ETeleportType.TP_BACK);
        //     return 1;
        // };

        LiteralArgumentBuilder<CommandSource> tpx = Commands.literal(ServerConfig.COMMAND_TP_COORDINATE.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_COORDINATE.get()))
                .then(Commands.argument("coordinate", BlockPosArgument.blockPos())
                        .executes(tpCoordinateCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpCoordinateCommand)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(tpCoordinateCommand)
                                )
                        )
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpCoordinateCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpCoordinateCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSource> tpst = Commands.literal(ServerConfig.COMMAND_TP_STRUCTURE.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_STRUCTURE.get()))
                .then(Commands.argument("struct", ResourceLocationArgument.id())
                        .suggests(structureSuggestions)
                        .executes(tpStructureCommand)
                        .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                .suggests(rangeSuggestions)
                                .executes(tpStructureCommand)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(tpStructureCommand)
                                )
                        )
                );
        LiteralArgumentBuilder<CommandSource> tpa = Commands.literal(ServerConfig.COMMAND_TP_ASK.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_ASK.get()))
                .executes(tpAskCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpAskCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpAskCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSource> tpaYes = Commands.literal(ServerConfig.COMMAND_TP_ASK_YES.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_ASK.get()))
                .executes(tpAskYesCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_ASK))
                        .executes(tpAskYesCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpAskYesCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .suggests(buildReqIdSuggestions(ETeleportType.TP_ASK))
                        .executes(tpAskYesCommand)
                );
        LiteralArgumentBuilder<CommandSource> tpaNo = Commands.literal(ServerConfig.COMMAND_TP_ASK_NO.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_ASK.get()))
                .executes(tpAskNoCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_ASK))
                        .executes(tpAskNoCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpAskNoCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .suggests(buildReqIdSuggestions(ETeleportType.TP_ASK))
                        .executes(tpAskNoCommand)
                );
        LiteralArgumentBuilder<CommandSource> tph = Commands.literal(ServerConfig.COMMAND_TP_HERE.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_HERE.get()))
                .executes(tpHereCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpHereCommand)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(safeSuggestions)
                                .executes(tpHereCommand)
                        )
                );
        LiteralArgumentBuilder<CommandSource> tphYes = Commands.literal(ServerConfig.COMMAND_TP_HERE_YES.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_HERE.get()))
                .executes(tpHereYesCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_HERE))
                        .executes(tpHereYesCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpHereYesCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .suggests(buildReqIdSuggestions(ETeleportType.TP_HERE))
                        .executes(tpHereYesCommand)
                );
        LiteralArgumentBuilder<CommandSource> tphNo = Commands.literal(ServerConfig.COMMAND_TP_HERE_NO.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_HERE.get()))
                .executes(tpHereNoCommand)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(buildReqIndexSuggestions(ETeleportType.TP_HERE))
                        .executes(tpHereNoCommand)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(tpHereNoCommand)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .suggests(buildReqIdSuggestions(ETeleportType.TP_HERE))
                        .executes(tpHereNoCommand)
                );

        LiteralArgumentBuilder<CommandSource> tpRandom = Commands.literal(ServerConfig.COMMAND_TP_RANDOM.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_RANDOM.get()))
                .executes(tpRandomCommand)
                .then(Commands.argument("range", IntegerArgumentType.integer(1))
                        .suggests(rangeSuggestions)
                        .executes(tpRandomCommand)
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .executes(tpRandomCommand)
                                .then(Commands.argument("safe", StringArgumentType.word())
                                        .suggests(safeSuggestions)
                                        .executes(tpRandomCommand)
                                )
                        )
                );

        LiteralArgumentBuilder<CommandSource> tpSpawn = Commands.literal(ServerConfig.COMMAND_TP_SPAWN.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_SPAWN.get()))
                .executes(tpSpawnCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpSpawnCommand)
                );

        LiteralArgumentBuilder<CommandSource> tpWorldSpawn = Commands.literal(ServerConfig.COMMAND_TP_WORLD_SPAWN.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_WORLD_SPAWN.get()))
                .executes(tpWorldSpawnCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpWorldSpawnCommand)
                );

        LiteralArgumentBuilder<CommandSource> tpTop = Commands.literal(ServerConfig.COMMAND_TP_TOP.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_TOP.get()))
                .executes(tpTopCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpTopCommand)
                );

        LiteralArgumentBuilder<CommandSource> tpBottom = Commands.literal(ServerConfig.COMMAND_TP_BOTTOM.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_BOTTOM.get()))
                .executes(tpBottomCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpBottomCommand)
                );

        LiteralArgumentBuilder<CommandSource> tpUp = Commands.literal(ServerConfig.COMMAND_TP_UP.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_UP.get()))
                .executes(tpUpCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpUpCommand)
                );

        LiteralArgumentBuilder<CommandSource> tpDown = Commands.literal(ServerConfig.COMMAND_TP_DOWN.get())
                .requires(source -> source.hasPermission(ServerConfig.PERMISSION_TP_DOWN.get()))
                .executes(tpDownCommand)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(safeSuggestions)
                        .executes(tpDownCommand)
                );

        // 注册简短的指令
        {
            // 传送至指定位置 /tpx
            if (ServerConfig.CONCISE_TP_COORDINATE.get()) {
                dispatcher.register(tpx);
            }

            // 传送至指定结构或生物群系 /tpst
            if (ServerConfig.CONCISE_TP_STRUCTURE.get()) {
                dispatcher.register(tpst);
            }

            // 传送请求 /tpa
            if (ServerConfig.CONCISE_TP_ASK.get()) {
                dispatcher.register(tpa);
            }

            // 传送请求同意 /tpay
            if (ServerConfig.CONCISE_TP_ASK_YES.get()) {
                dispatcher.register(tpaYes);
            }

            // 传送请求拒绝 /tpan
            if (ServerConfig.CONCISE_TP_ASK_NO.get()) {
                dispatcher.register(tpaNo);
            }

            // 被传送请求 /tph
            if (ServerConfig.CONCISE_TP_HERE.get()) {
                dispatcher.register(tph);
            }

            // 被传送请求同意 /tphy
            if (ServerConfig.CONCISE_TP_HERE_YES.get()) {
                dispatcher.register(tphYes);
            }

            // 被传送请求拒绝 /tphn
            if (ServerConfig.CONCISE_TP_HERE_NO.get()) {
                dispatcher.register(tphNo);
            }

            // 随机传送，允许指定范围 /tpr
            if (ServerConfig.CONCISE_TP_RANDOM.get()) {
                dispatcher.register(tpRandom);
            }

            // 传送至出生点 /tps
            if (ServerConfig.CONCISE_TP_SPAWN.get()) {
                dispatcher.register(tpSpawn);
            }

            // 传送至世界出生点 /tpws
            if (ServerConfig.CONCISE_TP_WORLD_SPAWN.get()) {
                dispatcher.register(tpWorldSpawn);
            }

            // 传送至头顶最上方方块 /tpt
            if (ServerConfig.CONCISE_TP_TOP.get()) {
                dispatcher.register(tpTop);
            }

            // 传送至脚下最下方方块 /tpb
            if (ServerConfig.CONCISE_TP_BOTTOM.get()) {
                dispatcher.register(tpBottom);
            }

            // 传送至头顶最近方块 /tpu
            if (ServerConfig.CONCISE_TP_UP.get()) {
                dispatcher.register(tpUp);
            }

            // 传送至脚下最近方块 /tpd
            if (ServerConfig.CONCISE_TP_DOWN.get()) {
                dispatcher.register(tpDown);
            }

        }

        // 注册有前缀的指令
        {
            dispatcher.register(Commands.literal(NarcissusFarewell.COMMAND_PREFIX)
                    .executes(helpCommand)
                    .then(Commands.literal("help")
                            .executes(helpCommand)
                            .then(Commands.argument("page", IntegerArgumentType.integer(1, (int) Math.ceil((double) HELP_MESSAGE.size() / HELP_INFO_NUM_PER_PAGE)))
                                    .suggests((context, builder) -> {
                                        int totalPages = (int) Math.ceil((double) HELP_MESSAGE.size() / HELP_INFO_NUM_PER_PAGE);
                                        for (int i = 0; i < totalPages; i++) {
                                            builder.suggest(i + 1);
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(helpCommand)
                            )
                    )
                    // 传送至指定位置 /narcissus tpx
                    .then(tpx)
                    // 传送至指定结构或生物群系 /narcissus tpst
                    .then(tpst)
                    // 传送请求 /narcissus tpa
                    .then(tpa)
                    // 传送请求同意 /narcissus tpay
                    .then(tpaYes)
                    // 传送请求拒绝 /narcissus tpan
                    .then(tpaNo)
                    // 被传送请求 /narcissus tph
                    .then(tph)
                    // 被传送请求同意 /narcissus tphy
                    .then(tphYes)
                    // 被传送请求拒绝 /narcissus tphn
                    .then(tphNo)
                    // 随机传送，允许指定范围 /narcissus tpr
                    .then(tpRandom)
                    // 传送至出生点 /narcissus tps
                    .then(tpSpawn)
                    // 传送至世界出生点 /narcissus tpws
                    .then(tpWorldSpawn)
                    // 传送至头顶最上方方块 /narcissus tpt
                    .then(tpTop)
                    // 传送至脚下最下方方块 /narcissus tpb
                    .then(tpBottom)
                    // 传送至头顶最近方块 /narcissus tpu
                    .then(tpUp)
                    // 传送至脚下最近方块 /narcissus tpd
                    .then(tpDown)
                    // 获取服务器配置 /narcissus config get
                    .then(Commands.literal("config")
                            .then(Commands.literal("get")
                                    .then(Commands.literal("teleportCard")
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                                Component msg = Component.translatable(player.getLanguage()
                                                        , I18nUtils.getKey(EI18nType.MESSAGE, "server_config_status")
                                                        , I18nUtils.enabled(player.getLanguage(), ServerConfig.TELEPORT_CARD.get())
                                                        , Component.translatable(player.getLanguage(), EI18nType.WORD, "teleport_card"));
                                                NarcissusUtils.sendMessage(player, msg);
                                                return 1;
                                            })
                                    )
                            )
                            // 修改服务器配置
                            .then(Commands.literal("set")
                                    .requires(source -> source.hasPermission(3))
                                    .then(Commands.literal("teleportCard")
                                            .then(Commands.argument("bool", BoolArgumentType.bool())
                                                    .executes(context -> {
                                                        boolean bool = BoolArgumentType.getBool(context, "bool");
                                                        ServerConfig.TELEPORT_CARD.set(bool);
                                                        ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                                        Component msg = Component.translatable(player.getLanguage()
                                                                , I18nUtils.getKey(EI18nType.MESSAGE, "server_config_status")
                                                                , I18nUtils.enabled(player.getLanguage(), ServerConfig.TELEPORT_CARD.get())
                                                                , Component.translatable(player.getLanguage(), EI18nType.WORD, "teleport_card"));
                                                        NarcissusUtils.broadcastMessage(player, msg);
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )
            );
        }
    }

    private static String getRequestId(CommandContext<CommandSource> context, ETeleportType teleportType) {
        String result = null;
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            try {
                ServerPlayerEntity requester = EntityArgument.getPlayer(context, "player");
                Map.Entry<String, TeleportRequest> entry1 = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                        .filter(entry -> entry.getValue().getTarget().getUUID().equals(player.getUUID()))
                        .filter(entry -> entry.getValue().getRequester().getUUID().equals(requester.getUUID()))
                        .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                        .max(Comparator.comparing(entry -> entry.getValue().getRequestTime()))
                        .orElse(null);
                if (entry1 != null) {
                    result = entry1.getKey();
                }
            } catch (IllegalArgumentException ignored) {
                try {
                    result = StringArgumentType.getString(context, "requestId");
                    if (!NarcissusFarewell.getTeleportRequest().containsKey(result)) {
                        result = null;
                    }
                } catch (IllegalArgumentException ignored1) {
                    try {
                        int askIndex = IntegerArgumentType.getInteger(context, "requestIndex");
                        List<Map.Entry<String, TeleportRequest>> entryList = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                                .filter(entry -> entry.getValue().getTarget().getUUID().equals(player.getUUID()))
                                .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                                // 使用负数实现倒序排列
                                .sorted(Comparator.comparing(entry -> -entry.getValue().getRequestTime().getTime()))
                                .collect(Collectors.toList());
                        if (askIndex > 0 && askIndex <= entryList.size()) {
                            result = entryList.get(askIndex - 1).getKey();
                        }
                    } catch (IllegalArgumentException ignored2) {
                        // 使用负数实现倒序排列
                        Map.Entry<String, TeleportRequest> entry1 = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                                .filter(entry -> entry.getValue().getTarget().getUUID().equals(player.getUUID()))
                                .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                                .max(Comparator.comparing(entry -> entry.getValue().getRequestTime()))
                                .orElse(null);
                        if (entry1 != null) {
                            result = entry1.getKey();
                        }
                    }
                }
            }
        } catch (CommandSyntaxException ignored) {
        }
        return result;
    }

    public static SuggestionProvider<CommandSource> buildReqIdSuggestions(ETeleportType teleportType) {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            NarcissusFarewell.getTeleportRequest().entrySet().stream()
                    .filter(entry -> entry.getValue().getRequester().getUUID().equals(player.getUUID()))
                    .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                    .forEach(entry -> builder.suggest(entry.getKey()));
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<CommandSource> buildReqIndexSuggestions(ETeleportType teleportType) {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            for (int i = 0; i < NarcissusFarewell.getTeleportRequest().entrySet().stream()
                    .filter(entry -> entry.getValue().getRequester().getUUID().equals(player.getUUID()))
                    .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                    .count(); i++) {
                builder.suggest(i + 1);
            }
            return builder.buildFuture();
        };
    }

    /**
     * 传送解析前置校验
     *
     * @return true 表示校验失败，不应该执行传送
     */
    private static boolean checkTeleportPre(ServerPlayerEntity player, ETeleportType teleportType) {
        // 判断是否开启传送功能
        if (!NarcissusUtils.isTeleportEnabled(teleportType)) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_disabled"));
            return true;
        }
        // 判断是否有冷却时间
        int teleportCoolDown = NarcissusUtils.getTeleportCoolDown(player, teleportType);
        if (teleportCoolDown > 0) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "command_cooldown"), teleportCoolDown);
            return true;
        }
        return false;
    }

    /**
     * 传送解析后置校验
     *
     * @return true 表示校验失败，不应该执行传送
     */
    private static boolean checkTeleportPost(TeleportRequest request) {
        return checkTeleportPost(request, false);
    }

    /**
     * 传送解析后置校验
     *
     * @param submit 是否收取代价
     * @return true 表示校验失败，不应该执行传送
     */
    private static boolean checkTeleportPost(TeleportRequest request, boolean submit) {
        boolean result;
        // 判断跨维度传送
        result = NarcissusUtils.isTeleportAcrossDimensionEnabled(request.getRequester(), request.getTarget().getLevel().dimension(), request.getTeleportType());
        // 判断是否有传送代价
        result = result && NarcissusUtils.validTeleportCost(request, submit);
        return !result;
    }

    /**
     * 传送解析后置校验
     *
     * @param player 请求传送的玩家
     * @param target 目标坐标
     * @param type   传送类型
     * @return true 表示校验失败，不应该执行传送
     */
    public static boolean checkTeleportPost(ServerPlayerEntity player, Coordinate target, ETeleportType type) {
        return checkTeleportPost(player, target, type, false);
    }

    /**
     * 传送解析后置校验
     *
     * @param player 请求传送的玩家
     * @param target 目标坐标
     * @param type   传送类型
     * @param submit 是否收取代价
     * @return true 表示校验失败，不应该执行传送
     */
    public static boolean checkTeleportPost(ServerPlayerEntity player, Coordinate target, ETeleportType type, boolean submit) {
        boolean result;
        // 判断跨维度传送
        result = NarcissusUtils.isTeleportAcrossDimensionEnabled(player, target.getDimension(), type);
        // 判断是否有传送代价
        result = result && NarcissusUtils.validTeleportCost(player, target, type, submit);
        return !result;
    }

}