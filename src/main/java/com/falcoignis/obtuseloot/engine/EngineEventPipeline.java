package com.falcoignis.obtuseloot.engine;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

final class EngineEventPipeline {
    private final List<EngineSubsystem> subsystems;

    EngineEventPipeline(List<EngineSubsystem> subsystems) {
        this.subsystems = List.copyOf(subsystems);
    }

    void onPlayerInteract(PlayerInteractEvent event) { subsystems.forEach(s -> s.onPlayerInteract(event)); }
    void onBlockBreak(BlockBreakEvent event) { subsystems.forEach(s -> s.onBlockBreak(event)); }
    void onPlayerMove(PlayerMoveEvent event) { subsystems.forEach(s -> s.onPlayerMove(event)); }
    void onEntityDamageByEntity(EntityDamageByEntityEvent event) { subsystems.forEach(s -> s.onEntityDamageByEntity(event)); }
    void onPlayerDamaged(EntityDamageEvent event) { subsystems.forEach(s -> s.onPlayerDamaged(event)); }
    void onEntityDeath(EntityDeathEvent event) { subsystems.forEach(s -> s.onEntityDeath(event)); }
    void onProjectileLaunch(ProjectileLaunchEvent event) { subsystems.forEach(s -> s.onProjectileLaunch(event)); }
    void onProjectileHit(ProjectileHitEvent event) { subsystems.forEach(s -> s.onProjectileHit(event)); }
    void onBlockDropItem(BlockDropItemEvent event) { subsystems.forEach(s -> s.onBlockDropItem(event)); }
}
