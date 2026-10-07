package dev.cosmojar.stellaritypaper.items.api;

import dev.cosmojar.stellaritypaper.enchants.EnchantInstance;
import dev.cosmojar.stellaritypaper.items.api.spec.EquippableSpec;
import dev.cosmojar.stellaritypaper.items.api.spec.PotionSpec;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record ItemComponents(
        @Nullable PotionSpec potion,
        @Nullable EquippableSpec equippable,
        List<EnchantInstance> enchants,
        List<ItemFlag> itemFlags,
        @Nullable Integer dyedColor,
        @Nullable Integer maxDamage,
        @Nullable Boolean unbreakable,
        @Nullable Boolean fireResistant,
        @Nullable Integer foodNutrition,
        @Nullable Float foodSaturation,
        @Nullable Boolean foodCanAlwaysEat,
        @Nullable Float consumableSeconds,
        @Nullable Float useCooldownSeconds
) {

    public ItemComponents {
        enchants = enchants == null ? List.of() : List.copyOf(enchants);
        itemFlags = itemFlags == null ? List.of() : List.copyOf(itemFlags);
    }

    public static ItemComponents empty() {
        return new ItemComponents(null, null, List.of(), List.of(), null, null, null, null, null, null, null, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private PotionSpec potion;
        private EquippableSpec equippable;
        private final List<EnchantInstance> enchants = new ArrayList<>();
        private final List<ItemFlag> itemFlags = new ArrayList<>();
        private Integer dyedColor;
        private Integer maxDamage;
        private Boolean unbreakable;
        private Boolean fireResistant;
        private Integer foodNutrition;
        private Float foodSaturation;
        private Boolean foodCanAlwaysEat;
        private Float consumableSeconds;
        private Float useCooldownSeconds;

        public Builder dyedColor(@Nullable final Integer dyedColor) {
            this.dyedColor = dyedColor;
            return this;
        }

        public Builder maxDamage(@Nullable final Integer maxDamage) {
            this.maxDamage = maxDamage;
            return this;
        }

        public Builder unbreakable(@Nullable final Boolean unbreakable) {
            this.unbreakable = unbreakable;
            return this;
        }

        public Builder fireResistant(@Nullable final Boolean fireResistant) {
            this.fireResistant = fireResistant;
            return this;
        }

        public Builder foodNutrition(@Nullable final Integer foodNutrition) {
            this.foodNutrition = foodNutrition;
            return this;
        }

        public Builder foodSaturation(@Nullable final Float foodSaturation) {
            this.foodSaturation = foodSaturation;
            return this;
        }

        public Builder foodCanAlwaysEat(@Nullable final Boolean foodCanAlwaysEat) {
            this.foodCanAlwaysEat = foodCanAlwaysEat;
            return this;
        }

        public Builder consumableSeconds(@Nullable final Float consumableSeconds) {
            this.consumableSeconds = consumableSeconds;
            return this;
        }

        public Builder useCooldownSeconds(@Nullable final Float useCooldownSeconds) {
            this.useCooldownSeconds = useCooldownSeconds;
            return this;
        }

        public Builder potion(@Nullable final PotionSpec potion) {
            this.potion = potion;
            return this;
        }

        public Builder equippable(@Nullable final EquippableSpec equippable) {
            this.equippable = equippable;
            return this;
        }

        public Builder addEnchantInstance(final EnchantInstance enchant) {
            this.enchants.add(enchant);
            return this;
        }

        public ItemComponents build() {
            return new ItemComponents(potion, equippable, enchants, itemFlags, dyedColor, maxDamage, unbreakable, fireResistant,
                    foodNutrition, foodSaturation, foodCanAlwaysEat, consumableSeconds, useCooldownSeconds);
        }
    }
}
