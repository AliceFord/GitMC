package com.techiehelper.gitmc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class GitMC implements ModInitializer {
    
    public static final String MODID = "gitmc";
    
    @Override
    public void onInitialize() {
        
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("gitmc")
                .then(argument("command", word())
                    .executes(ctx -> {
                        System.out.println(getString(ctx, "command"));
                        return 1;
                    })
                )
                    .executes(ctx -> {
                        ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(new LiteralText("Not a valid command. Use /gitmc help for help.").formatted(Formatting.RED), MessageType.SYSTEM, UUID.fromString("2e26f35f-b6a7-4939-9377-76b1149b4b4d"));
                        return 1;
                    })
            );
            dispatcher.register(literal("foo").executes(ctx -> {
                System.out.println(ctx.getSource().getMinecraftServer().getSavePath(WorldSavePath.ROOT));
                System.out.println("foo");
                return 1;
            }));
        });
    }
}
