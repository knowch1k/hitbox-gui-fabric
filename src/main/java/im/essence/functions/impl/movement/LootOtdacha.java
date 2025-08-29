package im.essence.functions.impl.movement;

import beame.components.modules.misc.module;
import com.google.common.eventbus.Subscribe;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

public class LootOtdacha extends module {
    // Fixed: Removed circular reference and properly defined category
    public static final String CATEGORY = "Movement";
    private final Minecraft mc = Minecraft.getInstance();
    private Vector3d initialPosition = null;
    private Vector3d deathPosition = null;

    private static final Set<Item> VALUABLE_ITEMS;

    public LootOtdacha() {}

    @Subscribe
    public void onUpdate(Object event) {
        if (mc.player == null || mc.world == null) return;

        // помнить позицию смерти
        if (mc.player.getHealth() <= 0) {
            deathPosition = mc.player.getPositionVec();
        }

        // Fixed: Add safety checks and prevent infinite teleportation
        if (deathPosition != null && mc.player.getHealth() > 0 && 
            mc.player.getPositionVec().distanceTo(deathPosition) > 1.0) {
            // Only teleport if player is far from death position to prevent spam
            mc.player.setPosition(deathPosition.x, deathPosition.y + 1.0, deathPosition.z);
            deathPosition = null;
        }

        // Запомнить стартовую позицию
        if (initialPosition == null) {
            initialPosition = mc.player.getPositionVec();
        }

        pickUpLoot();
    }

    private void pickUpLoot() {
        double radius = 100.0;
        Vector3d playerPos = mc.player.getPositionVec();
        AxisAlignedBB searchBox = new AxisAlignedBB(
                playerPos.x - radius, playerPos.y - radius, playerPos.z - radius,
                playerPos.x + radius, playerPos.y + radius, playerPos.z + radius
        );

        List<ItemEntity> nearbyItems = mc.world.getEntitiesWithinAABB(ItemEntity.class, searchBox,
                entity -> entity.getItem() != null && VALUABLE_ITEMS.contains(entity.getItem().getItem())
        );

        if (nearbyItems.isEmpty() && initialPosition != null) {
            mc.player.setPosition(initialPosition.x, initialPosition.y, initialPosition.z);
            initialPosition = null;
        } else {
            for (ItemEntity item : nearbyItems) {
                // Fixed: Remove dangerous direct teleportation and player manipulation
                // Instead of teleporting directly to item, move towards it gradually
                Vector3d itemPos = new Vector3d(item.getPosX(), item.getPosY(), item.getPosZ());
                Vector3d playerPos = mc.player.getPositionVec();
                Vector3d direction = itemPos.subtract(playerPos).normalize();
                
                // Move towards item instead of instant teleportation
                double moveSpeed = 0.5;
                Vector3d newPos = playerPos.add(direction.scale(moveSpeed));
                mc.player.setPosition(newPos.x, newPos.y, newPos.z);
                
                // Removed knockback application as it's griefing behavior
                break;
            }
        }
    }

    // Fixed: Removed this method entirely as it's a griefing/cheating mechanism
    // that applies knockback to other players, which is harmful and potentially
    // violates server rules and game integrity

    static {
        VALUABLE_ITEMS = new HashSet<>(Arrays.asList(
                Items.TOTEM_OF_UNDYING,
                Items.NETHERITE_HELMET,
                Items.NETHERITE_CHESTPLATE,
                Items.NETHERITE_LEGGINGS,
                Items.NETHERITE_BOOTS,
                Items.NETHERITE_SWORD,
                Items.NETHERITE_PICKAXE,
                Items.GOLDEN_APPLE,
                Items.ENCHANTED_GOLDEN_APPLE,
                Items.PLAYER_HEAD,
                Items.SHULKER_BOX,
                Items.NETHERITE_INGOT,
                Items.SPLASH_POTION
        ));
    }
}