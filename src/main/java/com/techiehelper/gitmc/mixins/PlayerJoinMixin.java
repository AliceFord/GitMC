package com.techiehelper.gitmc.mixins;

import com.techiehelper.gitmc.GitMC;
import com.techiehelper.gitmc.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.WorldSaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.techiehelper.gitmc.Util.displayMessage;

@Mixin(PlayerManager.class)
public class PlayerJoinMixin {
//    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/util/registry/DynamicRegistryManager/Impl;Lnet/minecraft/world/WorldSaveHandler;I)V")
//    private void PlayerManager(MinecraftServer server, DynamicRegistryManager.Impl registryManager, WorldSaveHandler saveHandler, int maxPlayers, CallbackInfo info) {
//        this.server = server;
//        System.out.println("Player manager mixin");
//    }
    
    @Inject(at = @At("TAIL"), method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V")
    private void onPlayerConnect(CallbackInfo info) {
        if (Util.server.getCurrentPlayerCount() == 1) {
            if (!GitMC.isSignedIn) {
                displayMessage("You're not signed in! Please set your username with /gitmcset gitusername=<username>, then use /gitmc login to login.");
            }
        }
    }
}
