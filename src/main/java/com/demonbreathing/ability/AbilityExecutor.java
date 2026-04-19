package com.demonbreathing.ability;

import com.demonbreathing.DemonBreathingPlugin;
import com.demonbreathing.model.BreathingStyle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class AbilityExecutor {
    private final DemonBreathingPlugin plugin;

    public AbilityExecutor(DemonBreathingPlugin plugin) { this.plugin = plugin; }

    public void execute(Player player, BreathingStyle style, int formIndex, double ratio, double chargeSeconds) {
        switch (style) {
            case THUNDER -> thunder(player, formIndex, ratio);
            case WATER -> water(player, formIndex, ratio, chargeSeconds);
            case SUN -> sun(player, formIndex, ratio);
            case WIND -> wind(player, formIndex, ratio);
            case MOON -> moon(player, formIndex, ratio);
            case FLAME -> flame(player, formIndex, ratio);
            case MIST -> mist(player, formIndex, ratio);
        }
    }

    private void thunder(Player p, int form, double r) {
        if (form == 0) dashChain(p, 2.2 + r * 2.0, 6, 4.0 + r * 2.5, Particle.ELECTRIC_SPARK, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, true);
        else if (form == 1) lightningRift(p, 8 + r * 16, 1.2 + r * 1.2, 2.2 + r * 2.4, true);
        else divineJudgment(p, r);
    }

    private void water(Player p, int form, double r, double chargeSeconds) {
        if (form == 0) radialMultiHit(p, 3.0 + r * 3.4, 3 + (int) (r * 5), 2.8 + r * 2.5, Particle.SPLASH, true);
        else if (form == 1) dragonRush(p, 11 + r * 16, 0.9 + r * 0.8, 4.0 + r * 8.5, Particle.BUBBLE_COLUMN_UP);
        else {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, (int) (90 + r * 150), 1));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, (int) (60 + r * 130), 0));
            radialMultiHit(p, 3.4 + r * 3.0, 2 + (int) (r * 4), 3.2 + r * 2.8 + chargeSeconds * 0.18, Particle.FALLING_WATER, false);
        }
    }

    private void sun(Player p, int form, double r) {
        if (form == 0) radialMultiHit(p, 3.2 + r * 2.8, 4 + (int) (r * 4), 3.5 + r * 3.5, Particle.FLAME, false);
        else if (form == 1) for (int i = 0; i < (r > 0.85 ? 3 : 1); i++) projectileSlash(p, 18 + r * 20, 1.0 + r, 5.5 + r * 6.5, Particle.FLAME, i * 3L);
        else solarCore(p, r);
    }

    private void wind(Player p, int form, double r) {
        if (form == 0) projectileFan(p, 4 + (int) (r * 8), 13 + r * 16, 0.85 + r * 0.8, 3.2 + r * 3.3);
        else if (form == 1) dashChain(p, 2.0 + r * 1.8, 4 + (int) (r * 4), 4.2 + r * 3.4, Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP, false);
        else tempestDomain(p, r);
    }

    private void moon(Player p, int form, double r) {
        if (form == 0) projectileFan(p, 4 + (int) (r * 8), 12 + r * 14, 0.7 + r * 0.7, 3.5 + r * 4.5);
        else if (form == 1) distortionZone(p, r);
        else eternalNight(p, r);
    }

    private void flame(Player p, int form, double r) {
        if (form == 0) {
            projectileSlash(p, 12 + r * 15, 1.0 + r * 0.8, 4.5 + r * 5.5, Particle.SOUL_FIRE_FLAME, 0L);
            fireTrail(p, 20 + (int) (r * 40));
        } else if (form == 1) {
            areaDamage(p, p.getLocation(), 3.3 + r * 2.4, 5 + r * 6.5);
            for (Entity e : p.getNearbyEntities(4, 4, 4)) if (e instanceof LivingEntity le && le != p) le.setVelocity(new Vector(0, 0.8 + r * 0.8, 0));
            Bukkit.getScheduler().runTaskLater(plugin, () -> areaDamage(p, p.getLocation(), 3.8 + r * 2.3, 4.2 + r * 5.6), 8L);
        } else beastAssault(p, r);
    }

    private void mist(Player p, int form, double r) {
        if (form == 0) dashChain(p, 2.2 + r * 1.9, 1, 7.0 + r * 10.0, Particle.WHITE_ASH, Sound.ENTITY_ENDERMAN_TELEPORT, false);
        else if (form == 1) {
            Location c = p.getLocation();
            c.getWorld().spawnParticle(Particle.WHITE_ASH, c, 160, 2.4, 1.2, 2.4, 0.02);
            for (Entity e : c.getWorld().getNearbyEntities(c, 5 + r * 3, 3, 5 + r * 3)) if (e instanceof LivingEntity le && le != p) {
                le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, (int) (80 + r * 120), 0));
                le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NAUSEA, (int) (60 + r * 120), 0));
            }
        } else voidExecution(p, r);
    }

    private void divineJudgment(Player p, double r) {
        Location origin = p.getLocation();
        p.teleport(origin.clone().add(0, 11 + r * 8, 0));
        for (int i = 0; i < 4 + (int) (r * 5); i++) {
            int idx = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location at = origin.clone();
                at.getWorld().strikeLightningEffect(at);
                areaDamage(p, at, 3.2 + r * 2.8, 3.5 + r * 4.0 + idx * 0.4);
            }, i * 6L);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> areaDamage(p, origin, 5 + r * 3, 9 + r * 12), 36L);
    }

    private void lightningRift(Player p, double dist, double step, double tickDamage, boolean launchEnd) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        new BukkitRunnable() {
            double d = 0;
            final Location pos = p.getLocation().clone();
            @Override
            public void run() {
                d += step;
                if (d > dist) {
                    if (launchEnd) {
                        pos.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pos, 80, 0.8, 0.3, 0.8, 0.03);
                        areaDamage(p, pos, 3, tickDamage * 3.2);
                        for (Entity e : pos.getWorld().getNearbyEntities(pos, 3, 3, 3)) if (e instanceof LivingEntity le && le != p) le.setVelocity(new Vector(0, 0.7, 0));
                    }
                    cancel();
                    return;
                }
                pos.add(dir.clone().multiply(step));
                pos.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pos, 18, 0.2, 0.1, 0.2, 0.02);
                areaDamage(p, pos, 1.8, tickDamage);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void dragonRush(Player p, double dist, double speed, double damage, Particle particle) {
        projectileSlash(p, dist, speed, damage, particle, 0L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> areaDamage(p, p.getLocation().add(p.getLocation().getDirection().multiply(4)), 3, damage * 1.2), 12L);
    }

    private void solarCore(Player p, double r) {
        Location l = p.getLocation();
        l.getWorld().spawnParticle(Particle.FLAME, l, 120, 1.2, 1.2, 1.2, 0.04);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            l.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, l, 2);
            areaDamage(p, l, 6 + r * 4.2, 10 + r * 15);
            for (Entity e : l.getWorld().getNearbyEntities(l, 7, 4, 7)) if (e instanceof LivingEntity le && le != p) le.setFireTicks(120 + (int) (r * 120));
        }, 20L);
    }

    private void tempestDomain(Player p, double r) {
        Location c = p.getLocation();
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                t++;
                if (t > 35 + r * 30) {
                    areaDamage(p, c, 5 + r * 3, 8 + r * 10);
                    cancel(); return;
                }
                c.getWorld().spawnParticle(Particle.GUST, c, 20, 2, 0.6, 2, 0.01);
                for (Entity e : c.getWorld().getNearbyEntities(c, 4 + r * 2.5, 3, 4 + r * 2.5)) if (e instanceof LivingEntity le && le != p) {
                    Vector pull = c.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.18 + r * 0.15);
                    le.setVelocity(le.getVelocity().add(pull));
                    le.damage(0.6 + r * 0.6, p);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void distortionZone(Player p, double r) {
        Location c = p.getLocation();
        c.getWorld().spawnParticle(Particle.DRAGON_BREATH, c, 120, 2.5, 0.8, 2.5, 0.03);
        for (Entity e : c.getWorld().getNearbyEntities(c, 5 + r * 3, 3, 5 + r * 3)) if (e instanceof LivingEntity le && le != p) {
            le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, (int) (80 + r * 130), 1));
            le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, (int) (80 + r * 130), 0));
        }
    }

    private void eternalNight(Player p, double r) {
        Location c = p.getLocation();
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                t++;
                c.getWorld().spawnParticle(Particle.DRAGON_BREATH, c, 35, 1.8, 1, 1.8, 0.02);
                for (Entity e : c.getWorld().getNearbyEntities(c, 5 + r * 2.5, 4, 5 + r * 2.5)) if (e instanceof LivingEntity le && le != p) {
                    Vector v = c.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.2 + r * 0.2);
                    le.setVelocity(le.getVelocity().add(v));
                }
                if (t > 20) {
                    areaDamage(p, c, 5 + r * 3, 9 + r * 12);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void beastAssault(Player p, double r) {
        dashChain(p, 2.0 + r * 1.6, 3 + (int) (r * 4), 4.0 + r * 5.5, Particle.SOUL_FIRE_FLAME, Sound.ENTITY_BLAZE_SHOOT, false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> areaDamage(p, p.getLocation(), 4 + r * 3, 6 + r * 8), 12L);
    }

    private void voidExecution(Player p, double r) {
        LivingEntity target = null;
        double best = Double.MAX_VALUE;
        for (Entity e : p.getNearbyEntities(18 + r * 12, 8, 18 + r * 12)) if (e instanceof LivingEntity le && le != p) {
            double d = le.getLocation().distanceSquared(p.getLocation()); if (d < best) { best = d; target = le; }
        }
        if (target == null) return;
        Location b = target.getLocation().clone().add(target.getLocation().getDirection().multiply(-1.1));
        p.teleport(b);
        target.damage(8 + r * 14, p);
        target.setNoDamageTicks(0);
        target.getWorld().spawnParticle(Particle.WHITE_SMOKE, target.getLocation().add(0, 1, 0), 50, 0.4, 0.4, 0.4, 0.03);
    }

    private void fireTrail(Player p, int ticks) {
        Location l = p.getLocation();
        for (int i = 1; i <= 5; i++) {
            Location at = l.clone().add(p.getLocation().getDirection().multiply(i));
            at.getBlock().setType(Material.FIRE);
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (at.getBlock().getType() == Material.FIRE) at.getBlock().setType(Material.AIR); }, ticks);
        }
    }

    private void dashChain(Player p, double speed, int hits, double damage, Particle particle, Sound sound, boolean finalBlast) {
        Vector d = p.getLocation().getDirection().normalize();
        p.setVelocity(d.multiply(speed));
        p.getWorld().playSound(p.getLocation(), sound, 1f, 1.15f);
        new BukkitRunnable() {
            int left = hits;
            @Override
            public void run() {
                if (!p.isOnline() || left-- <= 0) {
                    if (finalBlast) areaDamage(p, p.getLocation(), 3.8, damage * 1.9);
                    cancel(); return;
                }
                Location l = p.getLocation();
                l.getWorld().spawnParticle(particle, l.add(0, 1, 0), 26, 0.3, 0.3, 0.3, 0.03);
                areaDamage(p, l, 2.3, damage / Math.max(1, hits));
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void radialMultiHit(Player p, double radius, int hits, double damageEach, Particle particle, boolean pull) {
        for (int i = 0; i < hits; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location c = p.getLocation();
                c.getWorld().spawnParticle(particle, c, 90, radius * 0.28, 0.8, radius * 0.28, 0.03);
                areaDamage(p, c, radius, damageEach);
                if (pull) for (Entity e : c.getWorld().getNearbyEntities(c, radius, 2, radius)) if (e instanceof LivingEntity le && le != p) {
                    Vector pullV = c.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.22);
                    le.setVelocity(le.getVelocity().add(pullV));
                }
            }, i * 3L);
        }
    }

    private void projectileFan(Player p, int count, double dist, double speed, double damage) {
        for (int i = 0; i < count; i++) {
            long delay = i % 3;
            projectileSlash(p, dist, speed, damage, Particle.CLOUD, delay);
        }
    }

    private void projectileSlash(Player p, double dist, double speed, double damage, Particle particle, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Vector dir = p.getLocation().getDirection().normalize();
            new BukkitRunnable() {
                double t = 0;
                Location pos = p.getEyeLocation().clone();
                @Override
                public void run() {
                    t += speed;
                    if (t > dist || !p.isOnline()) { cancel(); return; }
                    pos.add(dir.clone().multiply(speed));
                    pos.getWorld().spawnParticle(particle, pos, 12, 0.18, 0.18, 0.18, 0.02);
                    areaDamage(p, pos, 1.7, damage);
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }, delay);
    }

    private void areaDamage(Player source, Location c, double r, double damage) {
        for (Entity e : c.getWorld().getNearbyEntities(c, r, r, r)) if (e instanceof LivingEntity le && le != source) {
            le.damage(damage, source);
            le.setNoDamageTicks(0);
        }
    }
}
