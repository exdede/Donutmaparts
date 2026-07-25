package dev.exdede.donutmaparts.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import java.util.function.Supplier;

/**
 * Curated set of vanilla sounds for the tracking alert. A fixed list rather than
 * a free text sound Identifier, so a typo cannot produce a silent alert.
 *
 * The SoundEvent is held behind a Supplier on purpose: SoundEvents fields are a
 * mix of SoundEvent and RegistryEntry.Reference<SoundEvent>, and resolving them
 * lazily keeps the SoundEvents class off the static init path of the config,
 * which the unit tests load without a client.
 */
public enum AlertSound implements IConfigOptionListEntry {
    PLING("pling", "Pling", () -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value()),
    BELL("bell", "Bell", () -> SoundEvents.BLOCK_BELL_USE),
    XP("xp", "XP Pickup", () -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
    LEVELUP("levelup", "Level Up", () -> SoundEvents.ENTITY_PLAYER_LEVELUP),
    ANVIL("anvil", "Anvil Land", () -> SoundEvents.BLOCK_ANVIL_LAND),
    ARROW_HIT("arrowhit", "Arrow Hit", () -> SoundEvents.ENTITY_ARROW_HIT_PLAYER);

    private final String configString;
    private final String displayName;
    private final Supplier<SoundEvent> event;

    AlertSound(String configString, String displayName, Supplier<SoundEvent> event) {
        this.configString = configString;
        this.displayName = displayName;
        this.event = event;
    }

    public SoundEvent soundEvent() {
        return this.event.get();
    }

    @Override
    public String getStringValue() {
        return this.configString;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        int index = this.ordinal() + (forward ? 1 : -1);
        if (index < 0) index = values().length - 1;
        else if (index >= values().length) index = 0;
        return values()[index];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        return fromStringStatic(value);
    }

    public static AlertSound fromStringStatic(String value) {
        for (AlertSound sound : values()) {
            if (sound.configString.equalsIgnoreCase(value)) return sound;
        }
        return PLING;
    }
}
