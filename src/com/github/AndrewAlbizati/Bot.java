package com.github.AndrewAlbizati;

import de.btobastian.sdc4fj.handler.JavacordHandler;
import de.btobastian.sdcf4j.CommandHandler;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

public class Bot {
    private static DiscordApi api;
    public static DiscordApi getApi() {
        return api;
    }

    public static void main(String[] args) {
        String token = "ODA4NTcwOTI3ODM0NTk1MzI4.YCIeWw.U-lGKndrtt-xIl875LzlL5CVVYQ";
        api = new DiscordApiBuilder().setToken(token).setAllIntents().login().join();
        CommandHandler handler = new JavacordHandler(api);

        handler.registerCommand(new Blackjack());
    }
}
