package com.example.examplemod;

import com.mojang.brigadier.CommandDispatcher;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "examplemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Akina mountain generator loaded");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("akina")
                .requires(source -> source.hasPermission(2))
                .executes(context -> generateAkina(context.getSource(), null))
                .then(Commands.argument("center", BlockPosArgument.blockPos())
                        .executes(context -> generateAkina(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "center")))));
    }

    private int generateAkina(CommandSourceStack source, BlockPos center) {
        BlockPos targetCenter = center != null ? center : BlockPos.containing(source.getPosition());
        int roadLength = AkinaMountainBuilder.build(source.getLevel(), targetCenter);
        source.sendSuccess(() -> Component.translatable("message.examplemod.akina_built", roadLength), true);
        return 1;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Use /akina [center] to build Akina mountain");
    }
}
