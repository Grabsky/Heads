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
package cloud.grabsky.heads.object;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import org.bukkit.NamespacedKey;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import static com.squareup.moshi.Types.getRawType;

@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityLootContainer {

    @Json(name = "default_chance")
    @Getter(AccessLevel.PUBLIC)
    private EntityLootEntry.Chance defaultChance;

    @Json(name = "default_matcher")
    @Getter(AccessLevel.PUBLIC)
    private EntityLootEntry.Matcher defaultMatcher;

    @Getter(AccessLevel.PUBLIC)
    private String permission;

    @Getter(AccessLevel.PUBLIC)
    private String message;

    @Getter(AccessLevel.PUBLIC)
    private List<String> commands;

    @Getter(AccessLevel.PUBLIC)
    private Map<NamespacedKey, List<EntityLootEntry>> entities;

    /* SERIALIZATION */

    public enum Factory implements JsonAdapter.Factory {
        INSTANCE; // SINGLETON

        private static final Type LIST_STRING = Types.newParameterizedType(List.class, String.class);
        private static final Type MAP_STRING_ENTITY_LOOT_ENTRY = Types.newParameterizedType(Map.class, NamespacedKey.class, Types.newParameterizedType(List.class, EntityLootEntry.class));

        @Override @SuppressWarnings("unchecked")
        public @Nullable JsonAdapter<EntityLootContainer> create(final @NotNull Type type, final @NotNull Set<? extends Annotation> annotations, final @NotNull Moshi moshi) {
            if (EntityLootContainer.class.isAssignableFrom(getRawType(type)) == false)
                return null;
            // Getting adapters...
            final var ChanceAdapter = moshi.adapter(EntityLootEntry.Chance.class).nullSafe();
            final var MatcherAdapter = moshi.adapter(EntityLootEntry.Matcher.class).nullSafe();
            final var ListAdapter = moshi.adapter(LIST_STRING).nullSafe();
            final var MapAdapter = moshi.adapter(MAP_STRING_ENTITY_LOOT_ENTRY);
            // Constructing and returning the JsonAdapter for this type.
            return new JsonAdapter<>() {

                @Override
                public @NotNull EntityLootContainer fromJson(final @NotNull JsonReader in) throws IOException {
                    final EntityLootContainer result = new EntityLootContainer();
                    in.beginObject();
                    while (in.hasNext() == true) {
                        // Getting the next key / property name of this object.
                        final String key = in.nextName().toLowerCase();
                        // Building the result object.
                        switch (key) {
                            case "default_chance" -> result.defaultChance = ChanceAdapter.fromJson(in);
                            case "default_matcher" -> result.defaultMatcher = MatcherAdapter.fromJson(in);
                            case "permission" -> result.permission = (in.peek() != JsonReader.Token.NULL) ? in.nextString() : in.nextNull();
                            case "message" -> result.message = (in.peek() != JsonReader.Token.NULL) ? in.nextString() : in.nextNull();
                            case "commands" -> result.commands = (in.peek() != JsonReader.Token.NULL) ? (List<String>) ListAdapter.fromJson(in) : in.nextNull();
                            case "entities" -> result.entities = (Map<NamespacedKey, List<EntityLootEntry>>) MapAdapter.fromJson(in);
                        }
                    }
                    in.endObject();
                    // Throwing exception if both, 'base' and 'variants' turn out to be null.
                    if (result.entities != null) {
                        result.entities.values().stream().flatMap(List::stream).forEach(value -> {
                            // Setting default matcher values.
                            if (value.matcher == null)
                                value.matcher = result.defaultMatcher;
                            if (value.matcher.variant == null)
                                value.matcher.variant = result.defaultMatcher.variant;
                            if (value.matcher.attackers == null)
                                value.matcher.attackers = result.defaultMatcher.attackers;
                            if (value.matcher.damageTypes == null)
                                value.matcher.damageTypes = result.defaultMatcher.damageTypes;
                            // Setting default chance values.
                            if (value.chance == null)
                                value.chance = result.defaultChance;
                            else if (value.chance.base == null)
                                value.chance.base = result.defaultChance.base;
                            else if (value.chance.perLootingLevel == null)
                                value.chance.perLootingLevel = result.defaultChance.perLootingLevel;
                            // Setting default permission.
                            if (value.permission == null)
                                value.permission = result.permission;
                            // Setting default commands.
                            if (value.commands == null || value.commands.isEmpty() == true)
                                if (result.commands != null && result.commands.isEmpty() == false)
                                    value.commands = result.commands;
                            // Setting default message.
                            if (value.message == null)
                                value.message = result.message;
                        });
                        // Reverse the order of elements in each list value stored in the map.
                        // This way fallback defined as first entry in the list, will be processed last.
                        result.entities.values().forEach(Collections::reverse);

                    }
                    // Returning the result object.
                    return result;
                }

                @Override
                public void toJson(final @NotNull JsonWriter out, final @Nullable EntityLootContainer value) {
                    throw new UnsupportedOperationException("NOT_IMPLEMENTED");
                }

            };

        }

    }
}
