package net.forixaim.mcea.entity_patch;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.conczin.mca.entity.EquipmentSet;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.MemoryModuleTypeMCA;
import net.conczin.mca.entity.ai.brain.VillagerTasksMCA;
import net.conczin.mca.entity.ai.brain.sensor.GuardEnemiesSensor;
import net.conczin.mca.registry.ProfessionsMCA;
import net.conczin.mca.entity.ai.brain.tasks.*;
import net.conczin.mca.server.world.data.villageComponents.VillageGuardsManager;
import net.conczin.mca.util.InventoryUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.forixaim.mcea.entity_patch.MCAVillagerEntityPatch;
import net.forixaim.mcea.entity_patch.EFAttackTask;
import yesman.epicfight.registry.entries.EpicFightExpandedEntityDataAccessors;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

import java.util.Optional;

public class MCATasksEF
{
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super VillagerEntityMCA>>> getGuardCorePackage(VillagerEntityMCA villager) {
        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super VillagerEntityMCA>>> pairs = ImmutableList.of(
                Pair.of(0, new ConditionalTask<>(
                        new VillagerPanicTrigger(),
                        MCATasksEF::guardTooHurt
                )),
                Pair.of(0,
                        new SayTask("villager.retreat", 100, e -> MCATasksEF.guardTooHurt(e) && e.getVillagerBrain().isPanicking())
                ),
                Pair.of(0,
                        new SayTask("villager.attack", 160, e -> !MCATasksEF.guardTooHurt(e) && MCATasksEF.getPreferredTarget(e).isPresent())
                ),
                Pair.of(0, new ConditionalTask<>(
                        new EFAttackTask(15, 2.5F, MemoryModuleType.NEAREST_HOSTILE),
                        v -> MCATasksEF.guardTooHurt(v)
                )),
                Pair.of(1, new EFEquipmentTask(VillagerTasksMCA::isOnDuty, v -> v.getResidency().getHomeVillage()
                        .map(vil -> vil.getVillageGuardsManager().getGuardEquipment(v.getProfession(), v.getDominantHand())).orElseGet((() -> v.getProfession() == ProfessionsMCA.ARCHER
                                ? VillageGuardsManager.getEquipmentFor(v.getDominantHand(), EquipmentSet.ARCHER_0, EquipmentSet.ARCHER_0_LEFT)
                                : VillageGuardsManager.getEquipmentFor(v.getDominantHand(), EquipmentSet.GUARD_0, EquipmentSet.GUARD_0_LEFT))))),
                Pair.of(2, StartAttacking.create(t -> true, MCATasksEF::getPreferredTarget)),
                Pair.of(3, StopAttackingIfTargetInvalid.create(livingEntity -> !MCATasksEF.isPreferredTarget(villager, livingEntity))),
                Pair.of(6, BehaviorBuilder.triggerIf(v -> v.isHolding(Items.BOW),
                        BackUpIfTooClose.create(12, 1.75F)
                )),
                Pair.of(4, new ArcherMovementTask<>(5)),
                Pair.of(5, new BowTask<>(20, 12)),
                Pair.of(6, BehaviorBuilder.triggerIf(v -> v.isHolding(Items.CROSSBOW),
                        BackUpIfTooClose.create(12, 1.75F)
                )),
                Pair.of(7, SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(0.75F)),
                Pair.of(8, new ConditionalTask<>(
                        new EFAttackTask(20, 2.0F),
                        v -> true
                )),
                Pair.of(9, new CrossbowAttack<VillagerEntityMCA, VillagerEntityMCA>())
        );
        return pairs;
    }
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super VillagerEntityMCA>>> getSelfDefensePackage(VillagerEntityMCA villager) {
        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super VillagerEntityMCA>>> pairs = ImmutableList.of(
                Pair.of(0, new ConditionalTask<>(
                        new VillagerPanicTrigger(),
                        MCATasksEF::civilianTooHurt
                )),
                Pair.of(0, new ConditionalTask<>(
                        new EFAttackTask(15, 2.5F, MemoryModuleType.NEAREST_HOSTILE),
                        v -> MCATasksEF.civilianTooHurt(v)
                )),
                Pair.of(1, new EFEquipmentTask(MCATasksEF::hasPreferredTarget, v -> EquipmentSet.NAKED)),
                Pair.of(2, StartAttacking.create(t -> true, MCATasksEF::getPreferredTarget)),
                Pair.of(3, StopAttackingIfTargetInvalid.create(livingEntity -> !MCATasksEF.isPreferredTarget(villager, livingEntity))),
                Pair.of(6, BehaviorBuilder.triggerIf(v -> v.isHolding(Items.BOW),
                        BackUpIfTooClose.create(12, 1.75F)
                )),
                Pair.of(5, new BowTask<>(20, 12)),
                Pair.of(6, BehaviorBuilder.triggerIf(v -> v.isHolding(Items.CROSSBOW),
                        BackUpIfTooClose.create(12, 1.75F)
                )),
                Pair.of(4, new ArcherMovementTask<>(15)),
                Pair.of(7, SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(0.75F)),
                Pair.of(8, new ConditionalTask<>(
                        new EFAttackTask(20, 2.0F),
                        v -> true
                )),
                Pair.of(9, new CrossbowAttack<VillagerEntityMCA, VillagerEntityMCA>())
        );
        return pairs;
    }
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super VillagerEntityMCA>>> getBarebonesPackage() {
        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super VillagerEntityMCA>>> pairs = ImmutableList.of(
        );
        return pairs;
    }

    private static boolean guardTooHurt(VillagerEntityMCA villager) {
        return villager.getHealth() < villager.getMaxHealth() * 0.25;
    }
    private static boolean civilianTooHurt(VillagerEntityMCA villager) {
        MCAVillagerEntityPatch patchEntity = EpicFightCapabilities.getEntityPatch(villager, MCAVillagerEntityPatch.class);
        return villager.getHealth() < villager.getMaxHealth() * 0.5 || villager.getItemBySlot(villager.getDominantSlot()).isEmpty() && patchEntity.getBestWeapon(null).isEmpty() || patchEntity.isAnticipatingAttack() && patchEntity.getStamina() < 3;
    }

    private static Activity getActivity(VillagerEntityMCA villager) {
        return villager.getBrain().getSchedule().getActivityAt((int) (villager.level().getDayTime() % 24000L));
    }
    
    private static Optional<NearestVisibleLivingEntities> getVisibleMobs(LivingEntity entity) {
        return entity.getBrain().getMemoryInternal(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }
    private static Optional<LivingEntity> getNearestHostile(VillagerEntityMCA entity) {
        return getVisibleMobs(entity).flatMap((list) -> list.find(target -> GuardEnemiesSensor.isGuardEnemy(target, entity))
                .filter(target -> target.distanceToSqr(entity) < 8f)
                .min((a, b) -> Double.compare(a.distanceToSqr(entity), b.distanceToSqr(entity))));
    }

    public static Optional<? extends LivingEntity> getPreferredTarget(VillagerEntityMCA villager) {
    	
        if (guardTooHurt(villager) && villager.isGuard() || civilianTooHurt(villager) && !villager.isGuard()) {
            return Optional.empty();
        } else {
            MCAVillagerEntityPatch patchEntity = EpicFightCapabilities.getEntityPatch(villager, MCAVillagerEntityPatch.class);
            Optional<LivingEntity> primary = villager.getBrain().getMemoryInternal(MemoryModuleTypeMCA.NEAREST_GUARD_ENEMY);
            if (primary.isPresent() && (getActivity(villager) != Activity.REST || primary.get().distanceTo(villager) < 16.0)) {
                return primary;
            } else {
            	if(villager.getBrain().getMemoryInternal(MemoryModuleType.ATTACK_TARGET).isEmpty()) {
            		villager.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, getNearestHostile(villager));
            		return getNearestHostile(villager);
            	}
                return villager.getBrain().getMemoryInternal(MemoryModuleType.ATTACK_TARGET);
            }
        }
    }
    
    public static boolean hasPreferredTarget(VillagerEntityMCA villager) {
        Optional<? extends LivingEntity> target = getPreferredTarget(villager);
        return target.isPresent();
    }

    private static boolean isPreferredTarget(VillagerEntityMCA villager, LivingEntity entity) {
        Optional<? extends LivingEntity> target = getPreferredTarget(villager);
        return target.filter(livingEntity -> livingEntity == entity).isPresent();
    }

}
