package net.forixaim.mcea.entity_patch;

import com.google.common.collect.ImmutableMap;
import net.forixaim.mcea.MinecraftComesEpiclyAlive;
import net.conczin.mca.entity.VillagerLike;
import net.conczin.mca.entity.ai.MemoryModuleTypeMCA;
import net.conczin.mca.entity.ai.Messenger;
import net.conczin.mca.entity.ai.brain.VillagerTasksMCA;
import net.conczin.mca.registry.ProfessionsMCA;
import net.conczin.mca.util.InventoryUtils;
import net.minecraft.world.Container;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.conczin.mca.entity.EquipmentSet;
import net.conczin.mca.entity.VillagerEntityMCA;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;
import net.conczin.mca.server.world.data.villageComponents.VillageGuardsManager;


import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.entity.ModifyPlayerLivingMotionEvent;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.MobCombatBehaviors;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightExpandedEntityDataAccessors;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.SkeletonPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import yesman.epicfight.world.entity.data.ExpandedSyncedData;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.world.entity.ai.behavior.EntityTracker;


public class EFAttackTask extends Behavior<VillagerEntityMCA> {
    private final float range;
    private final int interval;
    private final MemoryModuleType<? extends LivingEntity> target;

    public EFAttackTask(int interval, float range) {
        this(interval, range, MemoryModuleType.ATTACK_TARGET);
    }

    public EFAttackTask(int interval, float range, MemoryModuleType<? extends LivingEntity> target) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, target, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.VALUE_ABSENT));
        this.range = range;
        this.interval = interval;
        this.target = target;
    }

    protected boolean checkExtraStartConditions(ServerLevel world, Mob attacker) {
        MCAVillagerEntityPatch patchEntity = EpicFightCapabilities.getEntityPatch(attacker, MCAVillagerEntityPatch.class);
        LivingEntity entitytarget = getTarget(attacker);
        return BehaviorUtils.canSee(attacker, entitytarget) && withinRange(attacker, entitytarget);
    }

    protected void start(ServerLevel world, Mob mobEntity, long l) {
        MCAVillagerEntityPatch patchEntity = EpicFightCapabilities.getEntityPatch(mobEntity, MCAVillagerEntityPatch.class);
        LivingEntity livingEntity = MCATasksEF.getPreferredTarget(patchEntity.getOriginal()).isEmpty() ? getTarget(mobEntity) : MCATasksEF.getPreferredTarget(patchEntity.getOriginal()).get();
        //BehaviorUtils.lookAtEntity(mobEntity, livingEntity);
        if(livingEntity != null) {
        	//patchEntity.setAttakTargetSync(livingEntity);
            BehaviorUtils.lookAtEntity(mobEntity, livingEntity);
            mobEntity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingEntity, true));
            if(patchEntity.getEntityState().attacking())
            mobEntity.getLookControl().setLookAt(livingEntity);
        	
        }
        if(patchEntity.isAnticipatingAttack()) { 
        	patchEntity.currentCompositeMotion = LivingMotions.BLOCK;
        } else if (livingEntity != null) {
        	
			
			//if (builder != null) {
			//	patchEntity.getOriginal().goalSelector.addGoal(0, new AnimatedAttackGoal<>(patchEntity, builder.build(patchEntity)));
			//	patchEntity.getOriginal().goalSelector.addGoal(1, new TargetChasingGoal(patchEntity, patchEntity.getOriginal(), 1.2D, true));
			//}
    		//patchEntity.setAIAsInfantry(patchEntity.getOriginal().getProfession() == ProfessionsMCA.ARCHER);
        	//patchEntity.attack(EpicFightDamageSources.mobAttack(mobEntity), livingEntity, InteractionHand.MAIN_HAND);
        }
        //if (mobEntity instanceof VillagerLike<?> villager) {
        //    mobEntity.swing(villager.getDominantHand());
        //} else {
        //    mobEntity.swing(InteractionHand.MAIN_HAND);
        //}
        //mobEntity.doHurtTarget(livingEntity);
        //mobEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, interval);

        // kill phrase
        if (livingEntity.isDeadOrDying() && mobEntity instanceof Messenger messenger) {
            if (mobEntity.getRandom().nextFloat() < 0.3) {
                messenger.sendChatToAllAround("villager.kill");
            }
        }
    }


    private boolean withinRange(LivingEntity attacker, LivingEntity target) {
        double d = attacker.distanceToSqr(target.getX(), target.getY(), target.getZ());
        double r = attacker.getBbWidth() + target.getBbWidth() + range;
        return d <= r * r;
    }

    private LivingEntity getTarget(Mob mobEntity) {
        return mobEntity.getBrain().getMemoryInternal(target).get();
    }
}