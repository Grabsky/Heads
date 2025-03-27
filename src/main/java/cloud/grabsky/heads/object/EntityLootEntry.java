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
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import static com.squareup.moshi.Types.getRawType;

@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityLootEntry {

    @Getter(AccessLevel.PUBLIC)
    protected @Nullable Chance chance;

    @Getter(AccessLevel.PUBLIC)
    protected @Nullable Matcher matcher;

    @Getter(AccessLevel.PUBLIC)
    protected @Nullable String permission;

    @Getter(AccessLevel.PUBLIC)
    protected @Nullable String message;

    @Getter(AccessLevel.PUBLIC)
    protected List<String> commands;

    @Getter(AccessLevel.PUBLIC)
    private @UnknownNullability ItemStack item;

    // This NamespacedKey is used to match any attacker or damage type.
    private static final NamespacedKey ANY = new NamespacedKey("minecraft", "any");

    // This NamespacedKey is used to match unspecified / null attacker or damage type.
    private static final NamespacedKey UNSPECIFIED = new NamespacedKey("minecraft", "unspecified");

    @Accessors(fluent = true)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Chance {

        @Json(name = "base")
        @Getter(AccessLevel.PUBLIC)
        protected @Nullable Float base;

        @Json(name = "per_looting_level")
        @Getter(AccessLevel.PUBLIC)
        protected @Nullable Float perLootingLevel;

    }

    @Accessors(fluent = true)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Matcher {

        @Json(name = "variant")
        @Getter(AccessLevel.PUBLIC)
        protected @Nullable String variant;

        @Json(name = "attackers")
        @Getter(AccessLevel.PUBLIC)
        protected @Nullable List<NamespacedKey> attackers;

        @Json(name = "damage_types")
        @Getter(AccessLevel.PUBLIC)
        protected @Nullable List<NamespacedKey> damageTypes;

        public boolean matches(final @Nullable String variant, final @Nullable NamespacedKey attacker, final @Nullable NamespacedKey damageType) {
            final boolean isVariantMatch = (this.variant == null) || Objects.equals(this.variant, variant);
            final boolean isAttackerMatch = (this.attackers != null) && ((this.attackers.contains(ANY) == true || this.attackers.contains(attacker) == true) || (this.attackers.contains(UNSPECIFIED) == false && attacker == null));
            final boolean isDamageMatch = (this.damageTypes != null) && ((this.damageTypes.contains(ANY) == true || this.damageTypes.contains(damageType) == true) || (this.damageTypes.contains(UNSPECIFIED) == false && damageType == null));
            // Returning whether provided values match.
            return isVariantMatch && isAttackerMatch && isDamageMatch;
        }

    }

    /* SERIALIZATION */

    @SuppressWarnings("unchecked")
    public enum Factory implements JsonAdapter.Factory {
        INSTANCE; // SINGLETON

        private static final Type LIST_STRING = Types.newParameterizedType(List.class, String.class);

        @Override
        public @Nullable JsonAdapter<EntityLootEntry> create(final @NotNull Type type, final @NotNull Set<? extends Annotation> annotations, final @NotNull Moshi moshi) {
            if (EntityLootEntry.class.isAssignableFrom(getRawType(type)) == false)
                return null;
            // Getting adapters...
            final var ChanceAdapter = moshi.adapter(Chance.class).nullSafe();
            final var MatcherAdapter = moshi.adapter(Matcher.class).nullSafe();
            final var ListAdapter = moshi.adapter(LIST_STRING).nullSafe();
            final var ItemStackAdapter = moshi.adapter(ItemStack.class);

            // Constructing and returning the JsonAdapter for this type.
            return new JsonAdapter<>() {

                @Override
                public @NotNull EntityLootEntry fromJson(final @NotNull JsonReader in) throws IOException {
                    final EntityLootEntry result = new EntityLootEntry();
                    // ...
                    in.beginObject();
                    while (in.hasNext() == true) {
                        // Getting the next key / property name of this object.
                        final String key = in.nextName().toLowerCase();
                        // Building the result object.
                        switch (key) {
                            case "chance" -> result.chance = ChanceAdapter.fromJson(in);
                            case "matcher" -> result.matcher = MatcherAdapter.fromJson(in);
                            case "permission" -> result.permission = (in.peek() != JsonReader.Token.NULL) ? in.nextString() : in.nextNull();
                            case "message" -> result.message = (in.peek() != JsonReader.Token.NULL) ? in.nextString() : in.nextNull();
                            case "commands" -> result.commands = (in.peek() != JsonReader.Token.NULL) ? (List<String>) ListAdapter.fromJson(in) : in.nextNull();
                            case "item" -> result.item = ItemStackAdapter.fromJson(in);
                        }
                    }
                    in.endObject();
                    // Throwing exception if both, 'base' and 'variants' turn out to be null.
                    if (result.item == null)
                        throw new JsonDataException("Expected 'item' property but found nothing.");
                    // Returning the result object.
                    return result;
                }

                @Override
                public void toJson(final @NotNull JsonWriter out, final @Nullable EntityLootEntry value) {
                    throw new UnsupportedOperationException("NOT_IMPLEMENTED");
                }

            };

        }

    }

}
