package net.forixaim.mcea.entity_patch;

import com.google.common.collect.ImmutableMap;
import net.conczin.mca.entity.EquipmentSet;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.ActivitiesMCA;
import net.conczin.mca.entity.ai.MemoryModuleTypeMCA;
import net.conczin.mca.util.InventoryUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

import java.util.function.Function;
import java.util.function.Predicate;

public class EFEquipmentTask extends Behavior<VillagerEntityMCA> {
    private static final int COOLDOWN = 20;
    private static final int CHECK_INTERVAL = 20;
    private final Predicate<VillagerEntityMCA> condition;
    private final Function<VillagerEntityMCA, EquipmentSet> equipmentSet;
    private int lastEquipTime;
    private boolean lastArmorWearState;
    private int lastCheckTick = -CHECK_INTERVAL;
    private boolean cachedConditionResult;
    private EquipmentSet cachedEquipmentSet;

    public EFEquipmentTask(Predicate<VillagerEntityMCA> condition, Function<VillagerEntityMCA, EquipmentSet> set) {
        super(ImmutableMap.of(MemoryModuleTypeMCA.WEARS_ARMOR, MemoryStatus.REGISTERED));
        this.condition = condition;
        equipmentSet = set;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, VillagerEntityMCA villager) {
        if (villager.isUsingRecoveryFood()) {
            return false;
        }

        //armor visibility settings have been changed
        if (lastArmorWearState != villager.getVillagerBrain().getArmorWear()) {
            return true;
        }

        // Check condition with cache to avoid repeated expensive tests
        if (villager.tickCount - lastCheckTick >= CHECK_INTERVAL || !hasBestWeapon(villager, (cachedEquipmentSet != null ? cachedEquipmentSet.getMainHand() : null))&& MCATasksEF.hasPreferredTarget(villager)) {
            lastCheckTick = villager.tickCount;
            cachedConditionResult = condition.test(villager) || !hasBestWeapon(villager, (cachedEquipmentSet != null ? cachedEquipmentSet.getMainHand() : null))&& MCATasksEF.hasPreferredTarget(villager);
            cachedEquipmentSet = cachedConditionResult ? equipmentSet.apply(villager) : null;
        }

        EquipmentSet set = cachedEquipmentSet;
        if (set != null && isNakedCombatSet(set, villager) && !hasBestWeapon(villager, set.getMainHand())&& MCATasksEF.hasPreferredTarget(villager)) {
            equipBestWeapon(villager, set.getMainHand());
        	cachedConditionResult = true;
            //return ;
        }

        boolean preserveMourningHands = isPeacefullyGrieving(villager);
        boolean present = villager.getBrain().getMemoryInternal(MemoryModuleTypeMCA.WEARS_ARMOR).isPresent();
        if (cachedConditionResult) {
            lastEquipTime = villager.tickCount;
            return !present || set != null && !preserveMourningHands && (isMissingRequestedHandItem(villager, set) || !hasBestWeapon(villager, set.getMainHand())&& MCATasksEF.hasPreferredTarget(villager));
        } else if (villager.tickCount - lastEquipTime > COOLDOWN) {
            return present;
        } else {
            return !hasBestWeapon(villager, (set != null ? set.getMainHand() : null))&& MCATasksEF.hasPreferredTarget(villager);
        }
    }

    private void equipBestArmor(VillagerEntityMCA villager, EquipmentSlot slot, Item fallback) {
        ItemStack stack = InventoryUtils.getBestArmor(villager.getInventory(), slot).orElse(fallback == null ? ItemStack.EMPTY : new ItemStack(fallback));
        villager.setItemSlot(slot, stack);
    }

    private void equipBestWeapon(VillagerEntityMCA villager, Item fallback) {
        MCAVillagerEntityPatch patchEntity = EpicFightCapabilities.getEntityPatch(villager, MCAVillagerEntityPatch.class);
        ItemStack stack = patchEntity.getBestWeapon(fallback == Items.AIR ? null : fallback);
        villager.setItemSlot(villager.getDominantSlot(), stack);
        villager.setItemInHand(villager.getDominantHand(), stack);
        //patchEntity.setWeaponMotions();
    }

    private boolean hasBestWeapon(VillagerEntityMCA villager, Item fallback) {
        MCAVillagerEntityPatch patchEntity = EpicFightCapabilities.getEntityPatch(villager, MCAVillagerEntityPatch.class);
        ItemStack stack = patchEntity.getBestWeapon(fallback == Items.AIR ? null : fallback);
        return villager.getItemBySlot(villager.getDominantSlot()) == stack;
    }



    @Override
    protected void start(ServerLevel world, VillagerEntityMCA villager, long time) {
        super.start(world, villager, time);

        if (villager.isUsingRecoveryFood()) {
            return;
        }

        lastArmorWearState = villager.getVillagerBrain().getArmorWear();
        boolean wear = cachedConditionResult;
        EquipmentSet set = cachedEquipmentSet;

        if ((wear || villager.getVillagerBrain().getArmorWear()) && set == null) {
            set = equipmentSet.apply(villager);
            cachedEquipmentSet = set;
        }

        if (set != null && isNakedCombatSet(set, villager) && !hasBestWeapon(villager, set.getMainHand())&& MCATasksEF.hasPreferredTarget(villager)) {
            //return;
        	wear = true;
        }
        MCAVillagerEntityPatch patchEntity = EpicFightCapabilities.getEntityPatch(villager, MCAVillagerEntityPatch.class);

        //remember last state
        if (wear) {
            villager.getBrain().setMemory(MemoryModuleTypeMCA.WEARS_ARMOR, true);
        } else {
            villager.getBrain().eraseMemory(MemoryModuleTypeMCA.WEARS_ARMOR);
        }

        // Peaceful grieving owns both hand slots so routine equipment refreshes
        // cannot remove or replace the flower. Combat still takes priority.
        if (!isPeacefullyGrieving(villager)) {
            if (wear) {
                    equipBestWeapon(villager, set.getMainHand());
                    boolean onehanded = patchEntity.getHoldingItemCapability(villager.getDominantHand()).canBePlacedOffhand();
                if(set != null && set.getGetOffHand() != null && set.getGetOffHand() != Items.AIR && onehanded)
                villager.setItemSlot(villager.getOpposingSlot(), new ItemStack(set.getGetOffHand()));
            } else {
                villager.setItemInHand(villager.getDominantHand(), ItemStack.EMPTY);
                villager.setItemSlot(villager.getDominantSlot(), ItemStack.EMPTY);
                villager.setItemInHand(villager.getOpposingHand(), ItemStack.EMPTY);
                villager.setItemSlot(villager.getOpposingSlot(), ItemStack.EMPTY);
            }
        }

        //armor
        if (wear || villager.getVillagerBrain().getArmorWear()) {
            equipBestArmor(villager, EquipmentSlot.HEAD, set.getHead());
            equipBestArmor(villager, EquipmentSlot.CHEST, set.getChest());
            equipBestArmor(villager, EquipmentSlot.LEGS, set.getLegs());
            equipBestArmor(villager, EquipmentSlot.FEET, set.getFeet());
        } else {
            villager.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            villager.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            villager.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
            villager.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        }
    }

    private static boolean isNakedCombatSet(EquipmentSet set, VillagerEntityMCA villager) {
        return EquipmentSet.NAKED.equals(set)
               && MCATasksEF.hasPreferredTarget(villager);
    }

    private static boolean isPeacefullyGrieving(VillagerEntityMCA villager) {
        MCAVillagerEntityPatch patchEntity = EpicFightCapabilities.getEntityPatch(villager, MCAVillagerEntityPatch.class);
        return villager.getBrain().isActive(ActivitiesMCA.GRIEVE)
               && !MCATasksEF.hasPreferredTarget(villager)
               && !villager.getVillagerBrain().isPanicking();
    }

    private static boolean isMissingRequestedHandItem(VillagerEntityMCA villager, EquipmentSet set) {
        return isMissingRequestedItem(villager.getItemBySlot(villager.getDominantSlot()), set.getMainHand())
               || isRequestedItem(set.getGetOffHand()) && villager.getItemBySlot(villager.getOpposingSlot()).isEmpty();
    }

    private static boolean isMissingRequestedItem(ItemStack equipped, Item requested) {
        if (!isRequestedItem(requested)) {
            return false;
        }
        if (requested instanceof ProjectileWeaponItem) {
            return !(equipped.getItem() instanceof ProjectileWeaponItem);
        }
        return equipped.isEmpty();
    }

    private static boolean isRequestedItem(Item item) {
        return item != null && item != Items.AIR;
    }
}