package com.github.AndrewAlbizati;

import de.btobastian.sdc4fj.handler.JavacordHandler;
import de.btobastian.sdcf4j.CommandHandler;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import java.util.Scanner;

public class Bot {
    private static DiscordApi api;
    public static DiscordApi getApi() {
        return api;
    }
    private static String token = "ODA4NTcwOTI3ODM0NTk1MzI4.YCIeWw.U-lGKndrtt-xIl875LzlL5CVVYQ";

    public static void main(String[] args) {
        if (token.length() == 0) {
            System.out.print("Token: ");
            token = new Scanner(System.in).nextLine();
        }

        api = new DiscordApiBuilder().setToken(token).setAllIntents().login().join();
        CommandHandler handler = new JavacordHandler(api);

        handler.registerCommand(new Blackjack());
    }
}
