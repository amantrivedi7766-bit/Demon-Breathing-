package com.demonbreathing.ability;

import com.demonbreathing.DemonBreathingPlugin;
import com.demonbreathing.model.BreathingStyle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public final class AbilityExecutor {
    private final DemonBreathingPlugin plugin;

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

    private void thunder(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            int hits = 3 + (int) Math.round(ratio * 6.0);
            dashHits(player, 2.0 + ratio * 1.6, hits, 3.6 + ratio * 2.7, Particle.ELECTRIC_SPARK, Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
        } else if (form == 1) {
            int strikes = 5 + (int) Math.round(ratio * 8.0);
            for (int i = 0; i < strikes; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Location center = player.getLocation();
                    double x = (Math.random() - 0.5) * (6 + ratio * 12);
                    double z = (Math.random() - 0.5) * (6 + ratio * 12);
                    Location strike = center.clone().add(x, 0, z);
                    strike.getWorld().strikeLightningEffect(strike);
                    areaDamage(player, strike, 2.5 + ratio * 1.5, 4.0 + ratio * 7.0);
                }, i * 3L);
            }
        } else {
            Location target = player.getLocation().clone().add(0, 10 + ratio * 8, 0);
            player.teleport(target);
            player.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location impact = player.getLocation();
                impact.getWorld().strikeLightning(impact);
                areaDamage(player, impact, 4.0 + ratio * 2.5, 9.0 + ratio * 13.0);
            }, 10L);
        }
    }

    private void water(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            radialSlash(player, 3.5 + ratio * 2.8, 5.0 + ratio * 6.0, Particle.SPLASH);
        } else if (form == 1) {
            projectileWave(player, 15 + ratio * 18, 0.9 + ratio * 0.9, 5.0 + ratio * 8.0, Particle.BUBBLE_COLUMN_UP, Sound.ENTITY_DROWNED_SHOOT);
        } else {
            radialSlash(player, 4.0 + ratio * 3.5, 5.5 + ratio * 7.5 + chargeSeconds * 0.22, Particle.FALLING_WATER);
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, (int) (30 + ratio * 70), 1));
        }
    }

    private void sun(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            radialSlash(player, 3.4 + ratio * 2.5, 6.0 + ratio * 7.5, Particle.FLAME);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.2f);
        } else if (form == 1) {
            projectileWave(player, 17 + ratio * 18, 1.0 + ratio * 1.0, 6.5 + ratio * 9.0, Particle.FLAME, Sound.ITEM_TRIDENT_THUNDER);
        } else {
            Location loc = player.getLocation();
            loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2);
            areaDamage(player, loc, 5.5 + ratio * 3.8, 10.0 + ratio * 15.0);
            setFire(player, loc, 4.5 + ratio * 4.0, 60 + (int) (ratio * 120));
        }
    }

    private void wind(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            Location base = player.getLocation();
            for (int i = 0; i < 36; i++) {
                double angle = i * (Math.PI / 18);
                base.getWorld().spawnParticle(Particle.CLOUD, base.clone().add(Math.cos(angle) * 2.2, 0.2 + i * 0.05, Math.sin(angle) * 2.2), 1, 0, 0, 0, 0);
            }
            areaDamage(player, base, 3.8 + ratio * 2.4, 5.0 + ratio * 7.0);
        } else if (form == 1) {
            dashHits(player, 1.8 + ratio * 1.4, 2 + (int) (ratio * 3), 5.0 + ratio * 6.5, Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP);
        } else {
            Location loc = player.getLocation();
            loc.getWorld().spawnParticle(Particle.GUST_EMITTER_LARGE, loc, 1);
            areaDamage(player, loc, 5.0 + ratio * 3.2, 8.0 + ratio * 12.5);
            knockbackArea(player, loc, 5.0 + ratio * 3.2, 1.0 + ratio * 1.2);
        }
    }

    private void moon(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            int slices = 3 + (int) (ratio * 4);
            for (int i = 0; i < slices; i++) {
                projectileWave(player, 10 + ratio * 12, 0.8 + ratio * 0.8, 3.5 + ratio * 5.5, Particle.DRAGON_BREATH, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
            }
        } else if (form == 1) {
            int bursts = 5 + (int) (ratio * 6);
            for (int i = 0; i < bursts; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Location p = player.getLocation().clone().add((Math.random() - 0.5) * 8, 1, (Math.random() - 0.5) * 8);
                    p.getWorld().spawnParticle(Particle.DRAGON_BREATH, p, 20, 0.3, 0.3, 0.3, 0.02);
                    areaDamage(player, p, 2.3 + ratio * 1.8, 3.8 + ratio * 5.5);
                }, i * 2L);
            }
        } else {
            Location loc = player.getLocation();
            loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 40, 2.0, 1.2, 2.0, 0.02);
            areaDamage(player, loc, 5.3 + ratio * 3.8, 9.5 + ratio * 14.0);
        }
    }

    private void flame(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            projectileWave(player, 12 + ratio * 15, 1.0 + ratio * 0.8, 5.2 + ratio * 6.8, Particle.FLAME, Sound.ITEM_FIRECHARGE_USE);
        } else if (form == 1) {
            areaDamage(player, player.getLocation(), 3.2 + ratio * 2.0, 4.8 + ratio * 5.5);
            for (Entity e : nearbyLiving(player, player.getLocation(), 3.2 + ratio * 2.0)) {
                e.setVelocity(new Vector(0, 0.75 + ratio * 0.7, 0));
                if (e instanceof LivingEntity living) {
                    living.setFireTicks(60 + (int) (ratio * 120));
                }
            }
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.0f);
        } else {
            dashHits(player, 2.1 + ratio * 1.4, 2 + (int) (ratio * 4), 6.0 + ratio * 9.0, Particle.FLAME, Sound.ENTITY_BLAZE_SHOOT);
        }
    }

    private void mist(Player player, int form, double ratio, double chargeSeconds) {
        if (form == 0) {
            dashHits(player, 2.0 + ratio * 1.6, 2 + (int) (ratio * 3), 5.2 + ratio * 6.2, Particle.CLOUD, Sound.ENTITY_ENDERMAN_TELEPORT);
        } else if (form == 1) {
            Location loc = player.getLocation();
            loc.getWorld().spawnParticle(Particle.WHITE_ASH, loc, 80, 2.2, 1.1, 2.2, 0.01);
            for (Entity e : nearbyLiving(player, loc, 4.0 + ratio * 2.8)) {
                if (e instanceof LivingEntity living) {
                    living.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, (int) (50 + ratio * 80), 0));
                    living.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, (int) (40 + ratio * 70), 1));
                }
            }
        } else {
            LivingEntity target = nearestEnemy(player, 18 + ratio * 12);
            if (target != null) {
                Location behind = target.getLocation().clone().add(target.getLocation().getDirection().multiply(-1.2));
                behind.setDirection(target.getLocation().getDirection());
                player.teleport(behind);
                player.getWorld().playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.4f);
                damageTarget(player, target, 7.0 + ratio * 11.5);
            } else {
                dashHits(player, 1.7 + ratio * 1.2, 1, 6.0 + ratio * 8.0, Particle.CLOUD, Sound.ENTITY_ENDERMAN_TELEPORT);
            }
        }
    }

    private void radialSlash(Player player, double radius, double damage, Particle particle) {
        World world = player.getWorld();
        Location center = player.getLocation();
        world.spawnParticle(particle, center, 100, radius * 0.35, 0.8, radius * 0.35, 0.03);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        areaDamage(player, center, radius, damage);
    }

    private void projectileWave(Player player, double distance, double speed, double damage, Particle particle, Sound sound) {
        player.getWorld().playSound(player.getLocation(), sound, 1f, 1f);
        Vector direction = player.getLocation().getDirection().normalize();
        new BukkitRunnable() {
            double traveled = 0;
            Location pos = player.getEyeLocation().clone();

            @Override
            public void run() {
                traveled += speed;
                if (traveled > distance || !player.isOnline()) {
                    cancel();
                    return;
                }
                pos.add(direction.clone().multiply(speed));
                pos.getWorld().spawnParticle(particle, pos, 8, 0.15, 0.15, 0.15, 0.01);
                areaDamage(player, pos, 1.7, damage);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void dashHits(Player player, double speed, int hits, double damage, Particle particle, Sound sound) {
        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(speed));
        player.getWorld().playSound(player.getLocation(), sound, 1f, 1.2f);

        new BukkitRunnable() {
            int remaining = hits;

            @Override
            public void run() {
                if (remaining-- <= 0 || !player.isOnline()) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation();
                loc.getWorld().spawnParticle(particle, loc, 25, 0.3, 0.3, 0.3, 0.04);
                areaDamage(player, loc, 2.4, damage / Math.max(1, hits));
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    private void areaDamage(Player source, Location center, double radius, double damage) {
        for (Entity entity : nearbyLiving(source, center, radius)) {
            if (entity instanceof LivingEntity target) {
                damageTarget(source, target, damage);
            }
        }
    }

    private void setFire(Player source, Location center, double radius, int ticks) {
        for (Entity entity : nearbyLiving(source, center, radius)) {
            if (entity instanceof LivingEntity target) {
                target.setFireTicks(Math.max(target.getFireTicks(), ticks));
            }
        }
    }

    private void knockbackArea(Player source, Location center, double radius, double force) {
        for (Entity entity : nearbyLiving(source, center, radius)) {
            Vector kb = entity.getLocation().toVector().subtract(center.toVector()).normalize().multiply(force);
            kb.setY(0.35 + force * 0.15);
            entity.setVelocity(kb);
        }
    }

    private List<Entity> nearbyLiving(Player source, Location center, double radius) {
        return center.getWorld().getNearbyEntities(center, radius, radius, radius,
                entity -> entity instanceof LivingEntity && entity != source && !(entity instanceof org.bukkit.entity.ArmorStand)).stream().toList();
    }

    private LivingEntity nearestEnemy(Player player, double radius) {
        double best = Double.MAX_VALUE;
        LivingEntity result = null;
        for (Entity entity : nearbyLiving(player, player.getLocation(), radius)) {
            double d = entity.getLocation().distanceSquared(player.getLocation());
            if (d < best && entity instanceof LivingEntity living) {
                best = d;
                result = living;
            }
        }
        return result;
    }

    private void damageTarget(Player source, LivingEntity target, double damage) {
        target.damage(damage, source);
        target.setNoDamageTicks(0);
    }
}
