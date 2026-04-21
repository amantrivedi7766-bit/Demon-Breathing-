package com.demonbreathing.ability;

import com.demonbreathing.DemonBreathingPlugin;
import com.demonbreathing.model.BreathingStyle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public final class AbilityExecutor {
    private final DemonBreathingPlugin plugin;
    private final Random random = new Random();

    public AbilityExecutor(DemonBreathingPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, BreathingStyle style, int formIndex, double chargeRatio, double chargeSeconds) {
        switch (style) {
            case THUNDER -> thunder(player, formIndex, chargeRatio, chargeSeconds);
            case WATER -> water(player, formIndex, chargeRatio, chargeSeconds);
            case SUN -> sun(player, formIndex, chargeRatio, chargeSeconds);
            case WIND -> wind(player, formIndex, chargeRatio, chargeSeconds);
            case MOON -> moon(player, formIndex, chargeRatio, chargeSeconds);
            case FLAME -> flame(player, formIndex, chargeRatio, chargeSeconds);
            case MIST -> mist(player, formIndex, chargeRatio, chargeSeconds);
        }
    }

    // ==================== THUNDER BREATHING ====================
    private void thunder(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            sixfoldDash(player, ratio);
        } else if (form == 1) {
            thunderRift(player, ratio);
        } else {
            divineJudgment(player, ratio);
        }
    }

    private void sixfoldDash(Player player, double ratio) {
        int dashes = 6;
        double dashDistance = 3.2 + ratio * 1.8;
        double damagePerHit = 2.5 + ratio * 3.2;
        double finalBurstDamage = 12.0 + ratio * 12.0;
        Vector dashDirection = player.getLocation().getDirection().clone().normalize();

        new BukkitRunnable() {
            int step = 0;
            final Set<UUID> hitTargets = new HashSet<>();

            @Override
            public void run() {
                if (step >= dashes || !player.isOnline()) {
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 45, 1.6, 0.6, 1.6, 0.1);
                    crackGround(player.getLocation(), 2.4);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
                    player.getWorld().spawnParticle(Particle.FLASH, player.getLocation(), 3, 1.2, 0.3, 1.2, 0);
                    areaDamage(player, player.getLocation(), 3.2, finalBurstDamage);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
                    cancel();
                    return;
                }

                Location from = player.getLocation().clone();
                Location to = from.clone().add(dashDirection.clone().multiply(dashDistance));
                player.setVelocity(dashDirection.clone().multiply(2.35 + ratio * 0.7));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.7f, 1.8f);
                drawLineParticle(from, to, Particle.ELECTRIC_SPARK, 12);
                rayTraceDamage(player, from, dashDirection, dashDistance + 0.8, 1.3, damagePerHit, hitTargets, 6);

                step++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    private void thunderRift(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 0.6f);
        Location start = player.getLocation().clone();
        Vector direction = start.getDirection().normalize();
        double length = 15.0 + ratio * 9.0;
        double damage = 3.0 + ratio * 4.5;

        new BukkitRunnable() {
            double traveled = 0;
            Location current = start.clone().add(0, -0.5, 0);

            @Override
            public void run() {
                if (traveled >= length || !player.isOnline()) {
                    player.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, current, 4, 0.4, 0.2, 0.4, 0);
                    player.getWorld().playSound(current, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
                    areaDamage(player, current, 2.8, damage * 1.8);
                    cancel();
                    return;
                }
                traveled += 3.0;
                current.add(direction.clone().multiply(3.0));
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, current, 10, 0.6, 0.08, 0.6, 0.04);
                player.getWorld().spawnParticle(Particle.CRIT, current, 6, 0.4, 0.02, 0.4, 0.01);
                areaDamage(player, current, 1.8, damage);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void divineJudgment(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 24, 0.4, 0.8, 0.4, 0.05);
        Location skyLoc = player.getLocation().clone().add(0, 5 + ratio * 3, 0);
        player.teleport(skyLoc);
        player.setVelocity(new Vector(0, 0, 0));

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick == 0) {
                    Location ground = player.getLocation().clone();
                    ground.setY(player.getWorld().getHighestBlockYAt(ground) + 1);
                    drawVerticalBeam(player.getLocation(), ground, Particle.ELECTRIC_SPARK);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.5f);
                }
                if (tick == 10) {
                    player.setVelocity(new Vector(0, -2.9, 0));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.4f);
                }
                if (tick >= 18 || player.isOnGround()) {
                    Location impact = player.getLocation();
                    player.getWorld().strikeLightningEffect(impact);
                    areaDamage(player, impact, 4.0 + ratio * 2.5, 10.0 + ratio * 10);
                    player.getWorld().spawnParticle(Particle.FLASH, impact, 6, 1.5, 1.2, 1.5, 0);
                    cancel();
                    return;
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==================== WATER BREATHING ====================
    private void water(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            tidalSeveringFlow(player, ratio);
        } else if (form == 1) {
            abyssalDragonCollapse(player, ratio);
        } else {
            endlessCurrentDominion(player, ratio, chargeSeconds);
        }
    }

    private void tidalSeveringFlow(Player player, double ratio) {
        double radius = 4.0 + ratio * 3.0;
        double damage = 4.5 + ratio * 5.0;
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1f, 1.2f);

        new BukkitRunnable() {
            double angle = 0;

            @Override
            public void run() {
                if (angle >= 2 * Math.PI) {
                    for (Entity e : player.getNearbyEntities(radius, 2, radius)) {
                        if (e instanceof LivingEntity le && le != player) {
                            Vector pull = player.getLocation().toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.8);
                            le.setVelocity(pull);
                            damageTarget(player, le, damage);
                            microStun(le, 5);
                        }
                    }
                    player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation(), 80, radius / 2, 0.5, radius / 2, 0.1);
                    player.getWorld().spawnParticle(Particle.BUBBLE_POP, player.getLocation(), 80, radius / 2, 0.3, radius / 2, 0.08);
                    cancel();
                    return;
                }
                angle += Math.PI / 10;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = player.getLocation().clone().add(x, 0.5, z);
                player.getWorld().spawnParticle(Particle.SPLASH, loc, 4, 0.1, 0.1, 0.1, 0.01);
                player.getWorld().spawnParticle(Particle.WATER_WAKE, loc, 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void abyssalDragonCollapse(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.8f);
        LivingEntity target = findNearestEnemyInSight(player, 20 + ratio * 15);
        if (target == null) {
            launchWaterDragon(player, player.getLocation().getDirection(), ratio);
            return;
        }
        new BukkitRunnable() {
            int ticks = 0;
            Location dragonLoc = player.getEyeLocation().clone();
            Vector direction = player.getLocation().getDirection().normalize();

            @Override
            public void run() {
                if (ticks > 40 || !target.isValid() || !player.isOnline()) {
                    player.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, dragonLoc, 5, 0.5, 0.5, 0.5, 0);
                    player.getWorld().playSound(dragonLoc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
                    areaDamage(player, dragonLoc, 4.0, 8.0 + ratio * 10);
                    knockbackArea(player, dragonLoc, 4.5, 1.1);
                    cancel();
                    return;
                }
                Vector toTarget = target.getLocation().toVector().subtract(dragonLoc.toVector()).normalize();
                direction = direction.add(toTarget).normalize();
                dragonLoc.add(direction.multiply(1.1));
                drawDragonSegment(dragonLoc, direction, 2.0);
                player.getWorld().playSound(dragonLoc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.5f, 1.4f);
                if (dragonLoc.distance(target.getLocation()) < 3.0) {
                    damageTarget(player, target, 3.0 + ratio * 4.0);
                    target.setVelocity(new Vector(0, 0.3, 0));
                    microStun(target, 6);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void endlessCurrentDominion(Player player, double ratio, double chargeSeconds) {
        int duration = (int) (60 + chargeSeconds * 20 + ratio * 60);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, duration, 0, false, false));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 1f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline() || !player.hasPotionEffect(PotionEffectType.SPEED)) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.WATER_WAKE, player.getLocation(), 2, 0.1, 0.01, 0.1, 0);
                if (player.isSprinting() && ticks % 8 == 0) {
                    areaDamage(player, player.getLocation(), 2.5, 2.0 + ratio * 3.0);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==================== SUN BREATHING ====================
    private void sun(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            radiantSolarSpiral(player, ratio);
        } else if (form == 1) {
            blazingHorizonBreaker(player, ratio);
        } else {
            solarCataclysmCore(player, ratio);
        }
    }

    private void radiantSolarSpiral(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.5f);
        new BukkitRunnable() {
            double angle = 0;
            double radius = 2.5 + ratio * 2.0;

            @Override
            public void run() {
                if (angle >= 2 * Math.PI * 3) {
                    areaDamage(player, player.getLocation(), radius * 1.5, 6.0 + ratio * 8.0);
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 70, radius, 0.5, radius, 0.05);
                    cancel();
                    return;
                }
                angle += Math.PI / 6;
                double x = Math.cos(angle) * radius * (1 + angle / 10);
                double z = Math.sin(angle) * radius * (1 + angle / 10);
                Location loc = player.getLocation().clone().add(x, 0.2, z);
                player.getWorld().spawnParticle(Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.01);
                player.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
                radius += 0.05;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void blazingHorizonBreaker(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.2f);
        Vector dir = player.getLocation().getDirection().normalize();
        double length = 15 + ratio * 12;
        new BukkitRunnable() {
            double traveled = 0;
            Location loc = player.getEyeLocation().clone();

            @Override
            public void run() {
                if (traveled >= length) {
                    cancel();
                    return;
                }
                traveled += 2.0;
                loc.add(dir.clone().multiply(2.0));
                player.getWorld().spawnParticle(Particle.FLAME, loc, 9, 0.4, 0.2, 0.4, 0.01);
                areaDamage(player, loc, 2.3, 5.0 + ratio * 7.0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void solarCataclysmCore(Player player, double ratio) {
        Location center = player.getLocation();
        player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.6f);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 20) {
                    player.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, center, 2);
                    player.getWorld().spawnParticle(Particle.FLAME, center, 35, 1.8, 0.4, 1.8, 0.02);
                    player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
                    areaDamage(player, center, 6.0 + ratio * 4, 15.0 + ratio * 20);
                    cancel();
                    return;
                }
                if (ticks % 4 == 0) {
                    double progress = ticks / 20.0;
                    double radius = progress * (2.8 + ratio * 2.0);
                    player.getWorld().spawnParticle(Particle.END_ROD, center, 8, radius, 0.2, radius, 0.01);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==================== WIND BREATHING ====================
    private void wind(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            cycloneFragmentSlash(player, ratio);
        } else if (form == 1) {
            galeRush(player, ratio);
        } else {
            tempestCollapse(player, ratio);
        }
    }

    private void cycloneFragmentSlash(Player player, double ratio) {
        int slashes = 5 + (int) (ratio * 5);
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count++ >= slashes) {
                    cancel();
                    return;
                }
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 30, 2, 1, 2, 0.05);
                Vector dir = player.getLocation().getDirection();
                for (int i = -1; i <= 1; i++) {
                    Vector slashDir = dir.clone().rotateAroundY(i * 0.3);
                    areaConeDamage(player, player.getLocation(), slashDir, 4.0, 45, 2.5 + ratio * 3.0);
                }
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    private void galeRush(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.8f);
        Vector dir = player.getLocation().getDirection().normalize();
        player.setVelocity(dir.multiply(3.0 + ratio));
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 10 || player.isOnGround()) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                areaDamage(player, player.getLocation(), 3.0, 3.0 + ratio * 4.0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void tempestCollapse(Player player, double ratio) {
        Location center = player.getLocation();
        player.getWorld().playSound(center, Sound.ENTITY_HORSE_BREATHE, 1f, 0.5f);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 10) {
                    player.getWorld().spawnParticle(Particle.CLOUD, center, 55, 2.8, 1.2, 2.8, 0.1);
                    player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.6f);
                    areaDamage(player, center, 4.0 + ratio * 2.0, 8.0 + ratio * 10);
                    knockbackArea(player, center, 4.5, 1.5);
                    cancel();
                    return;
                }
                for (Entity e : center.getWorld().getNearbyEntities(center, 4, 2, 4)) {
                    if (e instanceof LivingEntity le && le != player) {
                        Vector pull = center.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.5);
                        le.setVelocity(pull);
                    }
                }
                player.getWorld().spawnParticle(Particle.CLOUD, center, 14, 2, 0.2, 2, 0.01);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==================== MOON BREATHING ====================
    private void moon(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            crescentPhantomBarrage(player, ratio);
        } else if (form == 1) {
            lunarDistortion(player, ratio);
        } else {
            eternalNightCollapse(player, ratio);
        }
    }

    private void crescentPhantomBarrage(Player player, double ratio) {
        int blades = 5 + (int) (ratio * 6);
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count++ >= blades) {
                    cancel();
                    return;
                }
                Vector dir = player.getLocation().getDirection().clone();
                dir.setX(dir.getX() + random.nextGaussian() * 0.5);
                dir.setZ(dir.getZ() + random.nextGaussian() * 0.5);
                dir.normalize();
                launchMoonBlade(player, dir, ratio);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void lunarDistortion(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.7f);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 40) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 20, 2, 1, 2, 0.01);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 1.5, 1, 1.5, 0);
                areaDamage(player, player.getLocation(), 4.0 + ratio * 3, 4.0 + ratio * 5.0);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void eternalNightCollapse(Player player, double ratio) {
        Location center = player.getLocation();
        player.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1f, 0.3f);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 30) {
                    player.getWorld().spawnParticle(Particle.FLASH, center, 5, 2, 2, 2, 0);
                    areaDamage(player, center, 7.0 + ratio * 5, 12.0 + ratio * 18);
                    cancel();
                    return;
                }
                double radius = (30 - ticks) * 0.3;
                player.getWorld().spawnParticle(Particle.SPELL, center, 50, radius, 1, radius, 0);
                player.getWorld().spawnParticle(Particle.SMOKE, center, 10, radius / 2, 0.5, radius / 2, 0);
                for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius / 2, radius)) {
                    if (e instanceof LivingEntity le && le != player) {
                        Vector pull = center.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.3);
                        le.setVelocity(pull);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==================== FLAME BREATHING ====================
    private void flame(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            infernalSlash(player, ratio);
        } else if (form == 1) {
            ascensionStrike(player, ratio);
        } else {
            flameBeastAssault(player, ratio);
        }
    }

    private void infernalSlash(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
        Vector dir = player.getLocation().getDirection();
        areaConeDamage(player, player.getLocation(), dir, 5.0, 60, 6.0 + ratio * 8.0);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(dir), 50, 1, 1, 1, 0.05);
        setFire(player, player.getLocation(), 4.0, 80);
    }

    private void ascensionStrike(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.9f);
        Location center = player.getLocation();
        new BukkitRunnable() {
            double height = 0;

            @Override
            public void run() {
                if (height >= 5 + ratio * 4) {
                    player.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, center.clone().add(0, height, 0), 5, 0.5, 0.5, 0.5, 0);
                    areaDamage(player, center.clone().add(0, height, 0), 4.0, 7.0 + ratio * 9.0);
                    cancel();
                    return;
                }
                height += 0.5;
                Location loc = center.clone().add(0, height, 0);
                player.getWorld().spawnParticle(Particle.FLAME, loc, 10, 0.5, 0.2, 0.5, 0.02);
                player.getWorld().spawnParticle(Particle.SMOKE, loc, 2, 0.2, 0.1, 0.2, 0);
                for (Entity e : center.getWorld().getNearbyEntities(center, 3, height, 3)) {
                    if (e instanceof LivingEntity le && le != player) {
                        le.setVelocity(new Vector(0, 0.8 + ratio, 0));
                        le.setFireTicks(60);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void flameBeastAssault(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1f, 0.8f);
        Vector dir = player.getLocation().getDirection().normalize();
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 15) {
                    cancel();
                    return;
                }
                player.setVelocity(dir.clone().multiply(1.5));
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 30, 1, 1, 1, 0.05);
                areaDamage(player, player.getLocation(), 3.5, 5.0 + ratio * 7.0);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ==================== MIST BREATHING ====================
    private void mist(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            silentDash(player, ratio);
        } else if (form == 1) {
            mistMaze(player, ratio);
        } else {
            voidExecution(player, ratio);
        }
    }

    private void silentDash(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.8f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0, false, false));
        Vector dir = player.getLocation().getDirection().normalize();
        player.setVelocity(dir.multiply(2.5 + ratio));
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks++ > 12) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 5, 0.1, 0.1, 0.1, 0);
                areaDamage(player, player.getLocation(), 2.5, 4.0 + ratio * 5.0);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void mistMaze(Player player, double ratio) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.5f);
        Location center = player.getLocation();
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 60) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.CLOUD, center, 50, 4, 1, 4, 0.01);
                player.getWorld().spawnParticle(Particle.SPELL, center, 20, 3, 0.5, 3, 0);
                for (Entity e : center.getWorld().getNearbyEntities(center, 5, 3, 5)) {
                    if (e instanceof LivingEntity le && le != player) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void voidExecution(Player player, double ratio) {
        LivingEntity target = findNearestEnemyInSight(player, 15 + ratio * 10);
        if (target == null) {
            silentDash(player, ratio);
            return;
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 30, 0.5, 1, 0.5, 0.05);
        Location behind = target.getLocation().clone().add(target.getLocation().getDirection().multiply(-1.5));
        behind.setDirection(target.getLocation().getDirection());
        player.teleport(behind);
        damageTarget(player, target, 10.0 + ratio * 12.0);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, behind, 5);
    }

    // ==================== UTILITY METHODS ====================
    private LivingEntity findNearestEnemyInSight(Player player, double range) {
        return player.getNearbyEntities(range, range, range).stream()
                .filter(e -> e instanceof LivingEntity && e != player)
                .map(e -> (LivingEntity) e)
                .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
    }

    private void drawLineParticle(Location from, Location to, Particle particle, int count) {
        Vector dir = to.clone().subtract(from).toVector();
        double length = dir.length();
        dir.normalize();
        for (int i = 0; i < count; i++) {
            Location loc = from.clone().add(dir.clone().multiply(i * length / count));
            from.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }

    private void drawVerticalBeam(Location top, Location bottom, Particle particle) {
        World world = top.getWorld();
        double height = top.getY() - bottom.getY();
        for (double y = 0; y < height; y += 0.5) {
            Location loc = bottom.clone().add(0, y, 0);
            world.spawnParticle(particle, loc, 2, 0.3, 0, 0.3, 0);
        }
    }

    private void drawDragonSegment(Location loc, Vector direction, double length) {
        World w = loc.getWorld();
        w.spawnParticle(Particle.SPLASH, loc, 10, 0.5, 0.5, 0.5, 0.02);
        w.spawnParticle(Particle.BUBBLE_POP, loc, 8, 0.3, 0.3, 0.3, 0.03);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.02, 0.02, 0.02, 0);
    }

    private void launchWaterDragon(Player player, Vector direction, double ratio) {
        new BukkitRunnable() {
            double traveled = 0;
            Location loc = player.getEyeLocation().clone();

            @Override
            public void run() {
                if (traveled > 20 + ratio * 15) {
                    cancel();
                    return;
                }
                traveled += 1.2;
                loc.add(direction.clone().multiply(1.2));
                drawDragonSegment(loc, direction, 2.0);
                areaDamage(player, loc, 2.5, 4.0 + ratio * 5.0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void launchMoonBlade(Player player, Vector direction, double ratio) {
        new BukkitRunnable() {
            double traveled = 0;
            Location loc = player.getEyeLocation().clone();

            @Override
            public void run() {
                if (traveled > 12 + ratio * 10) {
                    cancel();
                    return;
                }
                traveled += 1.0;
                loc.add(direction.clone().multiply(1.0));
                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 5, 0.2, 0.2, 0.2, 0.01);
                player.getWorld().spawnParticle(Particle.SPELL, loc, 2, 0, 0, 0, 0);
                areaDamage(player, loc, 1.5, 3.0 + ratio * 4.0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void rayTraceDamage(Player player, Location from, Vector direction, double range, double hitRadius,
                                double damage, Set<UUID> alreadyHit, int stunTicks) {
        RayTraceResult result = from.getWorld().rayTraceEntities(
                from,
                direction,
                range,
                hitRadius,
                entity -> entity instanceof LivingEntity && entity != player && !alreadyHit.contains(entity.getUniqueId())
        );
        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) {
            return;
        }
        alreadyHit.add(target.getUniqueId());
        damageTarget(player, target, damage);
        microStun(target, stunTicks);
    }

    private void microStun(LivingEntity target, int ticks) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 4, false, false, false));
    }

    private void crackGround(Location center, double radius) {
        World world = center.getWorld();
        int samples = 24;
        for (int i = 0; i < samples; i++) {
            double angle = (Math.PI * 2 * i) / samples;
            Location point = center.clone().add(Math.cos(angle) * radius, -0.1, Math.sin(angle) * radius);
            world.spawnParticle(Particle.BLOCK, point, 2, 0.1, 0.02, 0.1, 0,
                    Material.DEEPSLATE.createBlockData());
        }
    }

    private void areaConeDamage(Player source, Location center, Vector direction, double range, double angleDegrees, double damage) {
        for (Entity e : center.getWorld().getNearbyEntities(center, range, range, range)) {
            if (!(e instanceof LivingEntity target) || target == source) continue;
            Vector toTarget = target.getLocation().toVector().subtract(center.toVector());
            if (toTarget.angle(direction) < Math.toRadians(angleDegrees / 2)) {
                damageTarget(source, target, damage);
            }
        }
    }

    private void areaDamage(Player source, Location center, double radius, double damage) {
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (e instanceof LivingEntity target && target != source) {
                damageTarget(source, target, damage);
            }
        }
    }

    private void setFire(Player source, Location center, double radius, int ticks) {
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (e instanceof LivingEntity target && target != source) {
                target.setFireTicks(Math.max(target.getFireTicks(), ticks));
            }
        }
    }

    private void knockbackArea(Player source, Location center, double radius, double force) {
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (e instanceof LivingEntity target && target != source) {
                Vector kb = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(force);
                kb.setY(0.4);
                target.setVelocity(kb);
            }
        }
    }

    private void damageTarget(Player source, LivingEntity target, double damage) {
        target.damage(damage, source);
        target.setNoDamageTicks(0);
    }
}
