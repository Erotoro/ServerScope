package com.serverscope.collectors.profile;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ProfiledEventCatalog {
    private static final List<ProfiledEventBinding> SUPPORTED = List.of(
            new ProfiledEventBinding("player_move", PlayerMoveEvent.class),
            new ProfiledEventBinding("player_interact", PlayerInteractEvent.class),
            new ProfiledEventBinding("block_break", BlockBreakEvent.class),
            new ProfiledEventBinding("block_place", BlockPlaceEvent.class),
            new ProfiledEventBinding("entity_damage", EntityDamageEvent.class),
            new ProfiledEventBinding("inventory_click", InventoryClickEvent.class),
            new ProfiledEventBinding("creature_spawn", CreatureSpawnEvent.class)
    );

    private static final Map<String, ProfiledEventBinding> BY_ID = SUPPORTED.stream()
            .collect(Collectors.toUnmodifiableMap(binding -> binding.id().toLowerCase(Locale.ROOT), Function.identity()));

    private ProfiledEventCatalog() {
    }

    public static Optional<ProfiledEventBinding> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(id.toLowerCase(Locale.ROOT)));
    }
}
