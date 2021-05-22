package com.techiehelper.gitmc;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.techiehelper.gitmc.Util.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class GitMC implements ModInitializer {
    
    public static final String MODID = "gitmc";
    public static final String[] VALID_COMMANDS = {"help", "init", "login", "finishlogin"};
    
    private static final GithubApiClient apiClient = new GithubApiClient();
    
    public static boolean isSignedIn = false;
    private static String gitusername = "";
    private static String deviceCode = "";
    
    static void invalidCommand(CommandContext<ServerCommandSource> ctx) {
        displayMessage(ctx, "Not a valid command. Use /gitmc help for help.", Formatting.RED);
    }
    
    static File getSaveFile() throws IOException {return server.getSavePath(WorldSavePath.ROOT).toRealPath().getParent().getParent().resolve("gitmc").resolve(".gitmc.txt").toFile();}
    
    public static void signIn() {
        if (!isSignedIn) {
            try {
                File mcFile = getSaveFile();
                mcFile.getParentFile().mkdirs();
                System.out.println(mcFile);
                if (!mcFile.createNewFile()) {
                    HashMap<String, String> parsedData = Util.parseFile(mcFile);
                    if (apiClient.testToken(parsedData.getOrDefault("token", "null"))) {
                        isSignedIn = true;
                    } else {
                        displayMessage("Past token was invalid, trying to generate a new one!");
                        HashMap<String, String> tokenData = apiClient.refreshToken(parsedData.getOrDefault("refresh_token", "null"));
                        if (apiClient.testToken(tokenData.getOrDefault("token", "null"))) {
                            isSignedIn = true;
                            Util.writeToFile(getSaveFile(), new HashMap<String, String>() {{put("token", tokenData.getOrDefault("token", "null"));put("refresh_token", tokenData.getOrDefault("refresh_token", "null"));}});
                        } else {
                            HashMap<String, String> codes = apiClient.getOneTimeCode();
                            displayMessage("1 time code: " + codes.get("user_code"));
                            MutableText signInText = Text.of("Click here to sign in to github.").shallowCopy();
                            signInText.setStyle(Style.EMPTY.withUnderline(true).withColor(Formatting.BLUE).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/login/device")));
                            displayText(signInText);
                            displayMessage("Please type /gitmc finishlogin to finish github validation after signing in via the link.");
                            deviceCode = codes.get("device_code");
                        }
                    }
                } else {
                    displayMessage("Something went wrong! Please use /gitmc login to login.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            displayMessage("Signed in!");
        }
    }
    
    private static int execute(CommandContext<ServerCommandSource> ctx, String argument) {
        String command = getString(ctx, "command");
        if (!isSignedIn && !(command.equals("login") || command.equals("finishlogin"))) {
            displayMessage(ctx, "You're not signed in! Please use /gitmc login to login.");
        } else {
            String cmdOutput = null;
            switch (command) {
                case "help":
                    displayMessage(ctx, "This is the help section of the mod. Most commands are the same as on github, you can find them online at §9https://guides.github.com/introduction/git-handbook/ §a. Some custom commands have been implemented, as seen here:\n");
                    displayMessage("/gitmc login -> Begins login process to github.");
                    displayMessage("/gitmc finishlogin -> Ends login process to github.");
                    break;
                case "init":
                    if ((cmdOutput = runCommand("git init")) != null) {
                        displayMessage(ctx, cmdOutput);
                    }
                    break;
                case "add":
                    if ((cmdOutput = runCommand("git add " + argument)) != null) {
                        displayMessage(ctx, cmdOutput);
                    }
                    break;
                case "login":
                    signIn();
                    break;
                case "finishlogin":
                    HashMap<String, String> tokens = apiClient.validateLogin(deviceCode);
                    if (apiClient.testToken(tokens.getOrDefault("token", "null"))) {
                        try {
                            HashMap<String, String> justTokens = new HashMap<>();
                            justTokens.put("token", tokens.getOrDefault("token", "null"));
                            justTokens.put("refresh_token", tokens.getOrDefault("refresh_token", "null"));
                            writeToFile(getSaveFile(), justTokens);
                        } catch (IOException e) {
                            e.printStackTrace();
                            displayMessage("Something went wrong!");
                        }
                    } else {
                        displayMessage("Something went wrong!");
                    }
                    break;
                default:
                    invalidCommand(ctx);
                    break;
            }
        }
        return 1;
    }
    
    @Override
    public void onInitialize() {
        
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("gitmcset")
                .then(argument("variable", word())
                    .executes(ctx -> {
                        String command = getString(ctx, "variable");
                        if (command.contains("=")) {
                            String[] data = command.split("=");
                            if (data[0].toLowerCase().equals("gitusername")) {
                                gitusername = data[1];
                                displayMessage(ctx, "To allow gitmc to work, please now use /gitmc login to login to github.");
                            }
                        } else {
                            displayMessage(ctx, "No variable being set. Use /gitmcset variable=<variable>");
                        }
                        return 1;
                    })
                )
            );
            dispatcher.register(literal("gitmc")
                .then(argument("command", word())
                    .suggests((ctx, builder) -> {
                        for (String command : VALID_COMMANDS) builder.suggest(command);
                        return builder.buildFuture();
                    })
                        .executes(ctx -> execute(ctx, null))
                    .then(argument("argument", word()))
                        .executes(ctx -> execute(ctx, getString(ctx, "argument"))))
                .executes(ctx -> {
                    invalidCommand(ctx);
                    return 1;
                })
            );
        });
        
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            System.out.println("Server Started.");
            Util.server = server;
        });
        
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            displayMessage(server, "ServerPlayConnectionEvents.JOIN");
        });
    }
}
