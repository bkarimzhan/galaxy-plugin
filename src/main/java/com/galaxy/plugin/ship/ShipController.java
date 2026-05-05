package com.galaxy.plugin.ship;

import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class ShipController implements Listener {

    private static final double THRUST = 0.6;
    private static final double VERTICAL = 0.55;
    private static final double DRAG = 0.85;
    private static final double MAX_SPEED = 1.4;

    private final Plugin plugin;
    private final Logger log;
    private final NamespacedKey shipKey;
    private final NamespacedKey displayKey;
    private final Map<UUID, Vector> riderInput = new HashMap<>();
    private final BukkitRunnable tick;

    public ShipController(Plugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.shipKey = new NamespacedKey(plugin, "galaxy_ship");
        this.displayKey = new NamespacedKey(plugin, "galaxy_ship_display");
        this.tick = new BukkitRunnable() {
            @Override public void run() { tickShips(); }
        };
        this.tick.runTaskTimer(plugin, 1L, 1L);
    }

    public Pig spawnAndSeat(Player player) {
        Location front = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(2));
        front.setY(player.getLocation().getY() + 0.2);

        Pig pig = (Pig) player.getWorld().spawnEntity(front, org.bukkit.entity.EntityType.PIG);
        pig.setSaddle(true);
        pig.setGravity(false);
        pig.setSilent(true);
        pig.setInvulnerable(true);
        pig.setInvisible(true);
        pig.setAI(false);
        pig.setCollidable(false);
        if (pig.getAttribute(Attribute.SCALE) != null) {
            pig.getAttribute(Attribute.SCALE).setBaseValue(0.5);
        }
        pig.getPersistentDataContainer().set(shipKey, PersistentDataType.BYTE, (byte) 1);

        BlockDisplay disp = (BlockDisplay) player.getWorld().spawnEntity(front, org.bukkit.entity.EntityType.BLOCK_DISPLAY);
        disp.setBlock(Material.IRON_BLOCK.createBlockData());
        disp.setTransformation(new Transformation(
                new Vector3f(-0.5f, -0.5f, -0.5f),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(1.2f, 0.4f, 1.6f),
                new AxisAngle4f(0, 0, 1, 0)));
        disp.setBrightness(new Display.Brightness(15, 15));
        disp.getPersistentDataContainer().set(displayKey, PersistentDataType.STRING, pig.getUniqueId().toString());
        pig.addPassenger(disp);
        pig.addPassenger(player);

        log.info("Ship spawned by " + player.getName() + " at " + front.toVector());
        return pig;
    }

    public boolean isShip(Entity e) {
        return e != null && e.getPersistentDataContainer().has(shipKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onInput(PlayerInputEvent ev) {
        Player p = ev.getPlayer();
        Entity v = p.getVehicle();
        if (!isShip(v)) {
            riderInput.remove(p.getUniqueId());
            return;
        }
        var input = ev.getInput();
        double forward = (input.isForward() ? 1 : 0) - (input.isBackward() ? 1 : 0);
        double strafe  = (input.isLeft() ? 1 : 0) - (input.isRight() ? 1 : 0);
        double vert    = (input.isJump() ? 1 : 0) - (input.isSneak() ? 1 : 0);
        riderInput.put(p.getUniqueId(), new Vector(forward, vert, strafe));
    }

    @EventHandler
    public void onDismount(EntityDismountEvent ev) {
        if (!(ev.getEntity() instanceof Player p)) return;
        if (!isShip(ev.getDismounted())) return;
        riderInput.remove(p.getUniqueId());
        Pig pig = (Pig) ev.getDismounted();
        Bukkit.getScheduler().runTask(plugin, () -> despawn(pig));
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent ev) {
        if (isShip(ev.getRightClicked())) ev.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent ev) {
        riderInput.remove(ev.getPlayer().getUniqueId());
    }

    private void tickShips() {
        for (var entry : riderInput.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;
            Entity v = p.getVehicle();
            if (!(v instanceof Pig pig) || !isShip(pig)) continue;

            Vector input = entry.getValue();
            Vector look = p.getEyeLocation().getDirection();
            Vector right = new Vector(-look.getZ(), 0, look.getX()).normalize();

            Vector velocity = pig.getVelocity().multiply(DRAG);
            velocity.add(look.clone().multiply(input.getX() * THRUST));
            velocity.add(right.clone().multiply(input.getZ() * THRUST));
            velocity.setY(velocity.getY() + input.getY() * VERTICAL);

            if (velocity.lengthSquared() > MAX_SPEED * MAX_SPEED) {
                velocity = velocity.normalize().multiply(MAX_SPEED);
            }
            pig.setVelocity(velocity);
        }
    }

    private void despawn(Pig pig) {
        for (Entity passenger : pig.getPassengers()) {
            if (passenger instanceof BlockDisplay) passenger.remove();
        }
        pig.remove();
    }

    public void shutdown() {
        tick.cancel();
        HandlerList.unregisterAll(this);
        for (var w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (isShip(e)) {
                    if (e instanceof Pig pig) despawn(pig);
                }
            }
        }
    }
}
