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

interface EngineSubsystem {
    default void onPlayerInteract(PlayerInteractEvent event) {}
    default void onBlockBreak(BlockBreakEvent event) {}
    default void onPlayerMove(PlayerMoveEvent event) {}
    default void onEntityDamageByEntity(EntityDamageByEntityEvent event) {}
    default void onPlayerDamaged(EntityDamageEvent event) {}
    default void onEntityDeath(EntityDeathEvent event) {}
    default void onProjectileLaunch(ProjectileLaunchEvent event) {}
    default void onProjectileHit(ProjectileHitEvent event) {}
    default void onBlockDropItem(BlockDropItemEvent event) {}
}
