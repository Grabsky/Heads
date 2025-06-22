/*
 * Heads (https://github.com/Grabsky/Heads)
 *
 * Copyright (C) 2024  Grabsky <michal.czopek.foss@proton.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License v3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License v3 for more details.
 */
package cloud.grabsky.heads;

import cloud.grabsky.bedrock.BedrockPlugin;
import com.google.gson.Gson;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import revxrsal.commands.Lamp;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public final class Heads extends BedrockPlugin {

    @Getter(AccessLevel.PUBLIC)
    private static Heads instance;

    @Getter(AccessLevel.PUBLIC)
    private HeadsLoader headsLoader;

    @Getter(AccessLevel.PUBLIC)
    private Lamp<BukkitCommandActor> commands;

    @Override
    public void onEnable() {
        super.onEnable();
        // Updating the instance of the plugin.
        instance = this;
        // Initializing HeadsLoader and loading the data.
        this.headsLoader = new HeadsLoader(this);
        // Running the plugin reload logic.
        this.onReload();
        // Registering event listeners.
        this.getServer().getPluginManager().registerEvents(new HeadsListener(this), this);
        // Initializing Lamp.
        this.commands = BukkitLamp.builder(this)
                .responseHandler(String.class, (value, context) -> {
                    // Returning in case message is null or empty.
                    if (value == null || value.isEmpty() == true)
                        return;
                    // Sending message to the sender.
                    context.actor().requirePlayer().sendRichMessage(value);
                })
                .build();
        // Registering command(s).
        commands.register(PluginCommand.INSTANCE);
    }

    @Override
    public boolean onReload() {
        return headsLoader.reloadHeads();
    }

    // Command is defined as a nested class here. This is to keep things clean and simple.
    private enum PluginCommand {
        INSTANCE; // SINGLETON

        @Command("heads reload")
        @CommandPermission("heads.command.reload")
        public String onReload(final @NotNull CommandSender sender) {
            return (instance.onReload() == true) ? "<dark_gray>› <gray>Plugin <gold>Heads<gray> has been reloaded." : "<dark_gray>› <red>An error occurred while reloading <gold>Heads<red> plugin.";
        }

        @Command("heads get")
        @CommandPermission("heads.command.get")
        public String onGet(final @NotNull Player sender, final @NotNull EntityType type) {
            final NamespacedKey key = type.getKey();
            // Sending error message if no head variants are found for the given entity type.
            if (instance.getHeadsLoader().getEntries().containsKey(key) == false)
                return "<dark_gray>› <red>No head variants found for <gold><lang:" + type.translationKey() + "><red>.";
            // Iterating over all head variants and adding them to the sender's inventory.
            instance.getHeadsLoader().getEntries().get(key).forEach(entry -> sender.getInventory().addItem(entry.item()));
            // Sending success message to the sender.
            return "<dark_gray>› <gray>You have received all head variants for <gold><lang:" + type.translationKey() + "><gray>.";
        }

    }

    /* PLUGIN LOADER; FOR USE WITH PLUGIN-YML FOR GRADLE */

    @SuppressWarnings("UnstableApiUsage")
    public static final class PluginLoader implements io.papermc.paper.plugin.loader.PluginLoader {

        @Override
        public void classloader(final @NotNull PluginClasspathBuilder classpathBuilder) throws IllegalStateException {
            final MavenLibraryResolver resolver = new MavenLibraryResolver();
            // Parsing the file.
            try (final InputStream in = getClass().getResourceAsStream("/paper-libraries.json")) {
                final PluginLibraries libraries = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), PluginLibraries.class);
                // Adding repositories to the maven library resolver.
                libraries.asRepositories(classpathBuilder.getContext().getLogger()).forEach(resolver::addRepository);
                // Adding dependencies to the maven library resolver.
                libraries.asDependencies().forEach(resolver::addDependency);
                // Adding library resolver to the classpath builder.
                classpathBuilder.addLibrary(resolver);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
        private static class PluginLibraries {

            private final Map<String, String> repositories;
            private final List<String> dependencies;

            public Stream<RemoteRepository> asRepositories(final @NotNull Logger logger) {
                return repositories.entrySet().stream().map(entry -> {
                    try {
                        // Replacing Maven Central repository with a pre-configured mirror.
                        // See: https://docs.papermc.io/paper/dev/getting-started/paper-plugins/#loaders
                        if (entry.getValue().contains("maven.org") == true || entry.getValue().contains("maven.apache.org") == true) {
                            return new RemoteRepository.Builder(entry.getKey(), "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build();
                        }
                        return new RemoteRepository.Builder(entry.getKey(), "default", entry.getValue()).build();
                    } catch (final NoSuchFieldError e) {
                        return new RemoteRepository.Builder(entry.getKey(), "default", "https://maven-central.storage-download.googleapis.com/maven2").build();
                    }
                });
            }

            public Stream<Dependency> asDependencies() {
                return dependencies.stream().map(value -> new Dependency(new DefaultArtifact(value), null));
            }
        }
    }

}
