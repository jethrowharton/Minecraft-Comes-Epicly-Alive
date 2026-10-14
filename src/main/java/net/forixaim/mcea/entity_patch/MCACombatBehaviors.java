package net.forixaim.mcea.entity_patch;

import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.MobCombatBehaviors;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import net.forixaim.mcea.entity_patch.MCAVillagerEntityPatch;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

public class MCACombatBehaviors
{
	private static boolean isAnticipatingAttack(HumanoidMobPatch<?> mob) {
		if (EpicFightCapabilities.getEntityPatch(mob.getOriginal(), MCAVillagerEntityPatch.class) != null) {
			return EpicFightCapabilities.getEntityPatch(mob.getOriginal(), MCAVillagerEntityPatch.class).isAnticipatingAttack();
		}
		return false;
	}
	
    public static final CombatBehaviors.Builder<HumanoidMobPatch<?>> MCA_SWORD = CombatBehaviors.<HumanoidMobPatch<?>>builder()
            .newBehaviorSeries(
                    CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(100.0F).canBeInterrupted(false).looping(true)
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(Animations.SWORD_AUTO1).custom((mob) -> !isAnticipatingAttack(mob)).withinEyeHeight().withinDistance(0.0D, 2.0D))
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(Animations.SWORD_AUTO2).custom((mob) -> !isAnticipatingAttack(mob)).withinEyeHeight().withinDistance(0.0D, 2.0D))
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(Animations.SWORD_AUTO3).custom((mob) -> !isAnticipatingAttack(mob)).withinEyeHeight().withinDistance(0.0D, 2.0D))
            ).newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(0.0F).canBeInterrupted(true).looping(true)
            		.nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(Animations.BIPED_BLOCK).custom((mob) -> isAnticipatingAttack(mob)).withinEyeHeight().withinDistance(0.0D, 1.8D))
            		);
    public static final CombatBehaviors.Builder<HumanoidMobPatch<?>> MCA_SPEAR_TWOHAND = CombatBehaviors.<HumanoidMobPatch<?>>builder()
            .newBehaviorSeries(
                    CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(100.0F).canBeInterrupted(false).looping(false)
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(Animations.SPEAR_TWOHAND_AUTO1).custom((mob) -> !isAnticipatingAttack(mob)).withinEyeHeight().withinDistance(0.0D, 4.0D))
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(Animations.SPEAR_TWOHAND_AUTO2).custom((mob) -> !isAnticipatingAttack(mob)).withinEyeHeight().withinDistance(0.0D, 4.0D))
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().behavior(h -> {
                                h.getOriginal().jumpFromGround();
                                h.playAnimationSynchronized(Animations.SPEAR_TWOHAND_AIR_SLASH, 0);
                            }).custom(h -> h.getTarget().getY() > h.getOriginal().getEyeY() + 1d)
            )).newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(0.0F).canBeInterrupted(true).looping(true)
            		.nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(Animations.BIPED_BLOCK).custom((mob) -> isAnticipatingAttack(mob)).withinEyeHeight().withinDistance(0.0D, 1.8D))
            		);



    public static final CombatBehaviors.Builder<HumanoidMobPatch<?>> MCA_FIST = CombatBehaviors.<HumanoidMobPatch<?>>builder()
            .newBehaviorSeries(
                    CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(100.0F).canBeInterrupted(false).looping(true)
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(Animations.FIST_AUTO1).custom((mob) -> !isAnticipatingAttack(mob)).withinEyeHeight().withinDistance(0.0D, 1.8D))
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(Animations.FIST_AUTO2).custom((mob) -> !isAnticipatingAttack(mob)).withinEyeHeight().withinDistance(0.0D, 1.8D))
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(Animations.FIST_AUTO3).custom((mob) -> !isAnticipatingAttack(mob)).withinEyeHeight().withinDistance(0.0D, 1.8D))
            ).newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(0.0F).canBeInterrupted(true).looping(true)
            		.nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(Animations.BIPED_BLOCK).custom((mob) -> isAnticipatingAttack(mob)).withinEyeHeight().withinDistance(0.0D, 1.8D))
            		);
}
