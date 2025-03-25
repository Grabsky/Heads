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

import cloud.grabsky.configuration.adapter.AbstractEnumJsonAdapter;
import cloud.grabsky.configuration.paper.adapter.ComponentAdapter;
import cloud.grabsky.configuration.paper.adapter.EnchantmentAdapterFactory;
import cloud.grabsky.configuration.paper.adapter.EnchantmentEntryAdapterFactory;
import cloud.grabsky.configuration.paper.adapter.EntityTypeAdapterFactory;
import cloud.grabsky.configuration.paper.adapter.ItemStackAdapterFactory;
import cloud.grabsky.configuration.paper.adapter.MaterialAdapterFactory;
import cloud.grabsky.configuration.paper.adapter.NamespacedKeyAdapter;
import cloud.grabsky.configuration.paper.adapter.PersistentDataEntryAdapterFactory;
import cloud.grabsky.configuration.paper.adapter.PersistentDataTypeAdapterFactory;
import cloud.grabsky.heads.object.EntityLootContainer;
import cloud.grabsky.heads.object.EntityLootEntry;
import com.squareup.moshi.Moshi;
import net.kyori.adventure.text.Component;
import okio.BufferedSource;
import okio.Okio;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemRarity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static cloud.grabsky.configuration.paper.util.Resources.ensureResourceExistence;

@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public final class HeadsLoader {

    @Getter(AccessLevel.PUBLIC)
    private final Heads plugin;

    @Getter(AccessLevel.PUBLIC)
    private final Map<NamespacedKey, List<EntityLootEntry>> entries = new HashMap<>();

    public boolean reloadHeads() {
        final File directory = new File(plugin.getDataFolder(), "heads");
        // Trying...
        try {
            // Creating directory and default file.
            if (directory.mkdirs() == true) {
                ensureResourceExistence(plugin, new File(directory, "player.json"));
                ensureResourceExistence(plugin, new File(directory, "passive.json"));
                ensureResourceExistence(plugin, new File(directory, "aggressive.json"));
            }
            // Listing files inside the directory.
            final @Nullable File[] files = directory.listFiles();
            // Returning amd logging a message if directory is empty.
            if (files == null || files.length == 0) {
                plugin.getLogger().info("No head definitions has been found inside " + directory + "...");
                return true;
            }
            // Creating and configuring a Moshi instance used for heads deserialization.
            final Moshi moshi = new Moshi.Builder()
                    // Adding required adapters.
                    .add(Component.class, ComponentAdapter.INSTANCE)
                    .add(NamespacedKey.class, NamespacedKeyAdapter.INSTANCE)
                    .add(EnchantmentAdapterFactory.INSTANCE)
                    .add(EnchantmentEntryAdapterFactory.INSTANCE)
                    .add(EntityTypeAdapterFactory.INSTANCE)
                    .add(ItemStackAdapterFactory.INSTANCE)
                    .add(MaterialAdapterFactory.INSTANCE)
                    .add(PersistentDataEntryAdapterFactory.INSTANCE)
                    .add(PersistentDataTypeAdapterFactory.INSTANCE)
                    .add(ItemRarity.class, new AbstractEnumJsonAdapter<>(ItemRarity.class, false) {})
                    // Adding plugin-specific adapters.
                    .add(EntityLootEntry.Factory.INSTANCE)
                    .add(EntityLootContainer.Factory.INSTANCE)
                    // Building the Moshi instance.
                    .build();
            // Creating a temporary map to store the data.
            final Map<NamespacedKey, List<EntityLootEntry>> data = new HashMap<>();
            // Total number of files that attempted to be loaded.
            int filesTotal = 0;
            // Total number of files that were successfully loaded.
            int filesLoaded = 0;
            // Iterating over all files inside the dialogs directory.
            for (final File file : files) {
                // Skipping null (?) or non-JSON files.
                if (file == null || file.getName().endsWith(".json") == false)
                    continue;
                // Incrementing total number of files as we're about to attempt loading of the file.
                filesTotal++;
                // Another try...catch block is used here to make sure exception will interrupt loading of current file only, not all of them.
                try {
                    // Creating new BufferedSource instance from the file.
                    final BufferedSource source = Okio.buffer(Okio.source(file));
                    // Converting file contents to a result map.
                    final @Nullable EntityLootContainer result = moshi.adapter(EntityLootContainer.class).lenient().fromJson(source);
                    // Logging error in case result ended up being null.
                    if (result == null) {
                        plugin.getLogger().severe("Could not load head definitions located inside \"" + file + "\" file.");
                        plugin.getLogger().severe("  null");
                        // Continuing to the next file...
                        continue;
                    }
                    // Adding all loaded containers to the map.
                    data.putAll(result.entities());
                    // Incrementing number of files that were successfully loaded.
                    filesLoaded++;
                } catch (final IOException e) {
                    plugin.getLogger().severe("Could not load head definitions located inside \"" + file + "\" file.");
                    plugin.getLogger().severe("  " + e.getMessage());
                }
            }
            if (filesTotal == filesLoaded) {
                // Clearing the map before populating it again.
                entries.clear();
                // Adding all loaded containers to the map.
                entries.putAll(data);
                // Logging success message.
                plugin.getLogger().warning("Successfully loaded " + entries.values().stream().mapToLong(List::size).sum() + " heads defined across " + filesLoaded + " files.");
                // Returning true as everything went fine.
                return true;
            }
            // Returning false as something went wrong with the loading process.
            return false;
        } catch (final IOException e) {
            plugin.getLogger().severe("An error occurred while trying to save default dialogs file.");
            e.printStackTrace();
        }
        // An exception must have been thrown, so returning false here is expected.
        return false;
    }



}
