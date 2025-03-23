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

import cloud.grabsky.bedrock.components.Message;
import cloud.grabsky.bedrock.helpers.ItemBuilder;
import cloud.grabsky.heads.object.EntityLootEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static cloud.grabsky.bedrock.helpers.Conditions.requirePresent;

@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public final class HeadsListener implements Listener {

    @Getter(AccessLevel.PUBLIC)
    public @NotNull Heads plugin;

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDeath(final @NotNull EntityDeathEvent event) {
        final NamespacedKey key = event.getEntityType().getKey();
        // Filtering to only listen for mobs.
        if (plugin.getHeadsLoader().getEntries().containsKey(key) == true) {
            // Getting the matching entry.
            final @Nullable String variant = switch (event.getEntity()) {
                case Axolotl axolotl -> axolotl.getVariant().name().toLowerCase();
                case Horse horse -> horse.getColor().name().toLowerCase();
                case Llama llama -> llama.getColor().name().toLowerCase();
                case Parrot parrot -> parrot.getVariant().name().toLowerCase();
                case Rabbit rabbit -> rabbit.getRabbitType().name().toLowerCase();
                case Villager villager -> villager.getVillagerType().getKey().asString();
                case ZombieVillager zombieVillager -> zombieVillager.getVillagerType().getKey().asString();
                case Cat cat -> cat.getCatType().getKey().asString();
                case Fox fox -> fox.getFoxType().name().toLowerCase();
                case Panda panda -> panda.getCombinedGene().name().toLowerCase();
                case Frog frog -> frog.getVariant().getKey().asString();
                case Sheep sheep -> requirePresent(sheep.getColor(), DyeColor.WHITE).name().toLowerCase();
                case Shulker shulker -> requirePresent(shulker.getColor(), DyeColor.PURPLE).name().toLowerCase();
                case Wolf wolf -> wolf.getVariant().getKey().asString();
                case MushroomCow mushroomCow -> mushroomCow.getVariant().name().toLowerCase();
                default -> null;
            };
            // Getting the type of the attacker.
            final @Nullable Entity attacker = (event.getDamageSource().getCausingEntity() != null) ? event.getDamageSource().getCausingEntity() : null;
            // ...
            final EntityLootEntry entry = plugin.getHeadsLoader().getEntries().get(key).stream().filter(it -> {
                // Checking permissions.
                if (it.permission() != null && it.permission().isBlank() == false)
                    if (!(attacker instanceof Player player) || player.hasPermission(it.permission()) == false)
                        return false;
                // ...
                return it.matcher() == null || it.matcher().matches(variant, (attacker != null) ? attacker.getType().getKey() : null, event.getDamageSource().getDamageType().getKey());
            }).findFirst().orElse(null);
            // Returning if final entry turned out to be null. This means neither any matching variants nor base is configured.
            if (entry == null)
                return;
            // Getting the level of 'minecraft:looting' enchantment on player's currently held item.
            final int level = (event.getDamageSource().getCausingEntity() instanceof Player player) ? player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING) : 0;
            // Returning if chance is null.
            if (entry.chance() == null)
                return;
            // Calculating the total chance for the head to drop.
            final float chance = requirePresent(entry.chance().base(), 0F);
            final float chancePerLootingLevel = requirePresent(entry.chance().perLootingLevel(), 0F);
            final float totalChance = Math.clamp(chance + (level * chancePerLootingLevel), 0.0F, 1.0F);
            // Rolling the dice and continuing if lucky enough. This also checks for empty items.
            if (Math.random() <= totalChance && entry.item().isEmpty() == false) {
                final ItemStack item = (event.getEntity() instanceof Player player)
                        ? new ItemBuilder(entry.item()).setSkullTexture(player).build()
                        : entry.item();
                // Adding item to the drops.
                event.getDrops().add(item);
                // Optionally, sending message to the attacker.
                if (event.getDamageSource().getCausingEntity() instanceof Player player)
                    Message.of(entry.message())
                            .placeholder("item", Component.text("[", item.getItemMeta().itemName().color()).append(item.effectiveName().hoverEvent(item.asHoverEvent())).append(Component.text("]")))
                            .send(player);
            }
        }
    }


}