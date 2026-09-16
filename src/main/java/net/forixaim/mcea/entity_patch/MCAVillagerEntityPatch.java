package net.forixaim.mcea.entity_patch;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.brain.tasks.BowTask;
import net.conczin.mca.registry.EntitiesMCA;
import net.conczin.mca.registry.ProfessionsMCA;
import net.forixaim.mcea.MinecraftComesEpiclyAlive;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
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
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.data.ExpandedSyncedData;
import java.util.Random;

public class MCAVillagerEntityPatch extends HumanoidMobPatch<VillagerEntityMCA> {
    private float stamina;
    protected int tickSinceLastAction;
    protected boolean staminaDepleted;
    protected int staminaRegenAwaitTicks;
    protected int hitPrepare;

    public MCAVillagerEntityPatch(VillagerEntityMCA original) {
        super(original, Factions.VILLAGER);
    }

    @Override
    public void onConstructed(VillagerEntityMCA original) {
        super.onConstructed(original);
    }

    @Override
    public LivingEntity getTarget() {
        if (original.getMCABrain().getMemory(MemoryModuleType.ATTACK_TARGET).isPresent()) {
            return original.getMCABrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
        }
        return super.getTarget();
    }

    @Override
    protected void initAI() {
        super.initAI();
    }

    public float getMaxStamina() {
        AttributeInstance maxStamina = this.original.getAttribute(EpicFightAttributes.MAX_STAMINA);
        return (float)(maxStamina == null ? 0 : maxStamina.getValue());
    }

    public float getStamina() {
        return this.getMaxStamina() <= 0.0F ? 0.0F : this.stamina;
    }

    @Override
    protected void registerExpandedEntityDataAccessors(final ExpandedSyncedData expandedSynchedData) {
        super.registerExpandedEntityDataAccessors(expandedSynchedData);
        expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.STAMINA);
    }

    public static void initAttributes(EntityAttributeModificationEvent event) {
        event.add(EntitiesMCA.MALE_VILLAGER, EpicFightAttributes.IMPACT, 1.0D);
        event.add(EntitiesMCA.FEMALE_VILLAGER, EpicFightAttributes.IMPACT, 1.0D);
        //event.add(EntitiesMCA.MALE_VILLAGER, EpicFightAttributes.MAX_STAMINA, 6.0D);
        //event.add(EntitiesMCA.FEMALE_VILLAGER, EpicFightAttributes.MAX_STAMINA, 6.0D);
    }

    @Override
    protected void setWeaponMotions() {
        super.setWeaponMotions();
        this.weaponAttackMotions.put(CapabilityItem.WeaponCategories.SWORD,
                ImmutableMap.of(CapabilityItem.Styles.ONE_HAND,
                        MCACombatBehaviors.MCA_SWORD,
                        CapabilityItem.Styles.TWO_HAND,
                        MobCombatBehaviors.HUMANOID_DUAL_SWORD));
        this.weaponAttackMotions.put(CapabilityItem.WeaponCategories.NOT_WEAPON,
                ImmutableMap.of(CapabilityItem.Styles.COMMON,
                        MCACombatBehaviors.MCA_SWORD,
                        CapabilityItem.Styles.TWO_HAND,
                        MobCombatBehaviors.HUMANOID_ONEHAND_TOOLS));
        this.weaponAttackMotions.put(CapabilityItem.WeaponCategories.LONGSWORD,
                ImmutableMap.of(CapabilityItem.Styles.ONE_HAND,
                        MCACombatBehaviors.MCA_SWORD,
                        CapabilityItem.Styles.TWO_HAND,
                        MobCombatBehaviors.HUMANOID_LONGSWORD));
        //this.weaponAttackMotions.put(CapabilityItem.WeaponCategories.FIST,
        //        ImmutableMap.of(CapabilityItem.Styles.OCHS,
        //                MCACombatBehaviors.HUMANOID_FIST));
        this.weaponAttackMotions.put(CapabilityItem.WeaponCategories.SPEAR,
                ImmutableMap.of(CapabilityItem.Styles.TWO_HAND,
                        MCACombatBehaviors.HUMANOID_SPEAR_TWOHAND));
        this.weaponAttackMotions.put(CapabilityItem.WeaponCategories.GREATSWORD,
                ImmutableMap.of(CapabilityItem.Styles.TWO_HAND,
                		MobCombatBehaviors.HUMANOID_GREATSWORD));
    }

    public void setStamina(float value) {
        this.stamina = Mth.clamp(value, 0.0F, this.getMaxStamina());
        this.getExpandedSynchedData().set(EpicFightExpandedEntityDataAccessors.STAMINA, stamina);
    }

    @Override
    public void preTickServer() {
        super.preTickServer();
        if (this.state.canBasicAttack()) {
            this.tickSinceLastAction++;
        }
        if(this.state.canUseSkill()) {

            this.hitPrepare = Math.max(0,  hitPrepare-1);
        }

        if (!this.state.inaction()) {
            if (this.staminaRegenAwaitTicks > 0) this.staminaRegenAwaitTicks--;
        }

        float stamina = this.getStamina();
        float maxStamina = this.getMaxStamina();
        float staminaRegen = (float)this.original.getAttributeValue(EpicFightAttributes.STAMINA_REGEN);
        if (staminaRegen > 0.0F) {
            int regenWhenLessThan = 30 - (900 / (int)(30 * staminaRegen));

            if (stamina < maxStamina && this.staminaRegenAwaitTicks <= regenWhenLessThan) {
                float staminaFactor = 1.0F + (float)Math.pow((stamina / (maxStamina - stamina * 0.5F)), 2);
                this.setStamina(stamina + maxStamina * 0.01F * staminaFactor * staminaRegen);
                if(staminaDepleted && this.getStamina() > maxStamina/4) {
                	staminaDepleted = false;
                }
            }
        }

        if (maxStamina < stamina) {
            this.setStamina(maxStamina);
        }
    }

    @Override
    protected void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.CHASE, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.RUN, Animations.BIPED_RUN);
        animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.MOUNT, Animations.BIPED_MOUNT);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
        animator.addLivingAnimation(LivingMotions.SLEEP, Animations.BIPED_SLEEPING);
        animator.addLivingAnimation(LivingMotions.BLOCK, Animations.BIPED_BLOCK);
        animator.addLivingAnimation(LivingMotions.BLOCK_SHIELD, Animations.BIPED_BLOCK);
    }

    public boolean isAnticipatingAttack() {
        if (getTarget() == null || !this.original.isGuard())
        {
            return false;
        }
        if (EpicFightCapabilities.getEntityPatch(getTarget(), LivingEntityPatch.class) == null)
        {
            return this.original.distanceTo(getTarget()) < 1.5F && (this.getEntityState().canUseSkill() || this.hitPrepare > 0);
        }
        else if (EpicFightCapabilities.getEntityPatch(getTarget(), LivingEntityPatch.class).getEntityState().canBasicAttack()|| this.hitPrepare > 0 || EpicFightCapabilities.getEntityPatch(getTarget(), LivingEntityPatch.class).getEntityState().attacking())
        {
            return this.getEntityState().canUseSkill();
        }
        return false;
    }
    public static boolean getAnticipatingAttack(VillagerEntityMCA original) {
    	var patch = EpicFightCapabilities.getEntityPatch(original, MCAVillagerEntityPatch.class);
        if (patch.getTarget() == null || !original.isGuard())
        {
            return false;
        }
        if (EpicFightCapabilities.getEntityPatch(patch.getTarget(), LivingEntityPatch.class) == null)
        {
        	if(patch.getEntityState().attacking()) {
        		patch.hitPrepare = 30;
        	}
            return original.distanceTo(patch.getTarget()) < 1.5F && (patch.getEntityState().canUseSkill() || patch.hitPrepare > 0);
        }
        else if (EpicFightCapabilities.getEntityPatch(patch.getTarget(), LivingEntityPatch.class).getEntityState().canBasicAttack() || patch.hitPrepare > 0 || EpicFightCapabilities.getEntityPatch(patch.getTarget(), LivingEntityPatch.class).getEntityState().attacking())
        {
        	if(patch.getEntityState().attacking()) {
        		patch.hitPrepare = 30;
        	}
            return patch.getEntityState().canUseSkill();
        }
        return false;
    }

    private AnimationManager.AnimationAccessor<? extends DodgeAnimation> selectDodge()
    {
        ClipContext backClip = new ClipContext(this.original.position(), this.original.position().add(this.original.getLookAngle()).scale(1.5), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.original);
        ClipContext leftClip = new ClipContext(this.original.position(), this.original.position().add(this.original.getLookAngle().yRot(90)).scale(1.5), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.original);
        ClipContext rightClip = new ClipContext(this.original.position(), this.original.position().add(this.original.getLookAngle().yRot(-90)).scale(1.5), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.original);
        BlockHitResult backHit = this.original.level().clip(backClip);
        BlockHitResult leftHit = this.original.level().clip(leftClip);
        BlockHitResult rightHit = this.original.level().clip(rightClip);
        if (backHit.getType() == HitResult.Type.MISS) {
        	this.original.addDeltaMovement(this.original.getLookAngle().scale(0.35).reverse());
            return Animations.BIPED_STEP_BACKWARD;
        }
        if (leftHit.getType() == HitResult.Type.MISS) {
        	this.original.addDeltaMovement(this.original.getLookAngle().yRot(90).scale(0.35));
            return Animations.BIPED_STEP_LEFT;
        }
        if (rightHit.getType() == HitResult.Type.MISS) {
        	this.original.addDeltaMovement(this.original.getLookAngle().yRot(-90).scale(0.35));
            return Animations.BIPED_STEP_RIGHT;
        }
    	this.original.addDeltaMovement(this.original.getLookAngle().scale(0.35));
        return Animations.BIPED_STEP_FORWARD;
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        if (this.original.getHealth() <= 0.0F) {
            currentLivingMotion = LivingMotions.DEATH;

        }
        else if (this.original.isSleeping()) {
            currentLivingMotion = LivingMotions.SLEEP;
        }
        else if (this.state.inaction() && considerInaction) {
            currentLivingMotion = LivingMotions.IDLE;
        } else {
            if (original.getVehicle() != null) {
                currentLivingMotion = LivingMotions.MOUNT;
            } else {
                if (this.original.getDeltaMovement().y < -0.55F || this.isAirborneState())
                    currentLivingMotion = LivingMotions.FALL;
                else if (original.walkAnimation.speed() > 0.08F) {
                    if (original.walkAnimation.speed() > 0.7f)
                        currentLivingMotion = LivingMotions.RUN;
                    else
                        currentLivingMotion = LivingMotions.WALK;
                }
                else
                    currentLivingMotion = LivingMotions.IDLE;
            }
        }
        //if (this.isAnticipatingAttack() && this.getHoldingItemCapability(InteractionHand.OFF_HAND).isWeaponCategory(CapabilityItem.WeaponCategories.SHIELD))
        //    currentCompositeMotion = LivingMotions.BLOCK_SHIELD;
        //else if (this.isAnticipatingAttack())
        //    currentCompositeMotion = LivingMotions.BLOCK;
        if (!this.state.updateLivingMotion() && considerInaction) {
            this.currentCompositeMotion = LivingMotions.NONE;
        } else {
            CapabilityItem mainhandItemCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
            CapabilityItem offhandItemCap = this.getHoldingItemCapability(InteractionHand.OFF_HAND);
            LivingMotion customLivingMotion = mainhandItemCap.getLivingMotion(this, InteractionHand.MAIN_HAND);

            if (customLivingMotion == null) customLivingMotion = offhandItemCap.getLivingMotion(this, InteractionHand.OFF_HAND);
            if (customLivingMotion != null)
                currentCompositeMotion = customLivingMotion;
            else if (this.original.isUsingItem()) {
                UseAnim useAnim = this.original.getUseItem().getUseAnimation();
                if (useAnim == UseAnim.BLOCK)
                    currentCompositeMotion = LivingMotions.BLOCK_SHIELD;
                else if (useAnim == UseAnim.CROSSBOW)
                    currentCompositeMotion = LivingMotions.RELOAD;
                else if (useAnim == UseAnim.DRINK)
                    currentCompositeMotion = LivingMotions.DRINK;
                else if (useAnim == UseAnim.EAT)
                    currentCompositeMotion = LivingMotions.EAT;
                else if (useAnim == UseAnim.SPYGLASS)
                    currentCompositeMotion = LivingMotions.SPECTATE;
                else
                    currentCompositeMotion = currentLivingMotion;
            } else {
                if (this.getClientAnimator().getCompositeLayer(Layer.Priority.MIDDLE).animationPlayer.getRealAnimation().get().isReboundAnimation())
                    currentCompositeMotion = LivingMotions.SHOT;
                else if (this.isAnticipatingAttack() && this.getHoldingItemCapability(InteractionHand.OFF_HAND).isWeaponCategory(CapabilityItem.WeaponCategories.SHIELD))
                    currentCompositeMotion = LivingMotions.BLOCK_SHIELD;
                else if (this.isAnticipatingAttack() && this.state.canUseSkill())
                    currentCompositeMotion = LivingMotions.BLOCK;
                else if (this.original.swinging && this.original.getSleepingPos().isEmpty())
                    currentCompositeMotion = LivingMotions.DIGGING;
                else
                    currentCompositeMotion = currentLivingMotion;
            }
        }
    }

    private boolean isValidDamageSource(DamageSource source)
    {
        return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) ||
                source.is(DamageTypes.MOB_PROJECTILE) || source.is(DamageTypes.ARROW) ||
                source.is(DamageTypes.FIREBALL) || source.is(DamageTypes.THROWN) ||
                source.is(DamageTypes.TRIDENT) || source.is(DamageTypes.WITHER_SKULL) ||
                source.is(DamageTypes.WIND_CHARGE);
    }

    @Override
    public AttackResult tryHurt(DamageSource damageSource, float amount) {
        if (isValidDamageSource(damageSource)) {
        	if(this.state.canUseSkill() && !this.isStunned()) {
        	var willblock = !staminaDepleted &&( this.state.canBasicAttack() && Mth.randomBetween(this.getLevel().random, 0, 1) > (this.original.getProfession() == ProfessionsMCA.ARCHER ? 0.75 : 0.25) && this.getStamina()-0.25f*amount > 0 || this.getStamina() < 3);
            if (willblock && (this.getCurrentLivingMotion() == (LivingMotions.BLOCK) || this.currentCompositeMotion == LivingMotions.BLOCK || this.currentCompositeMotion == LivingMotions.BLOCK_SHIELD || this.getStamina() >= 0) &&
                    !(damageSource.is(EpicFightDamageTypeTags.UNBLOCKALBE)) ) {
            	var parry = this.hitPrepare <= 15 && Mth.randomBetween(this.getLevel().random, 0, 1) > 0.5;
            		this.setStamina(parry ? this.getStamina()-0.1f*amount : this.getStamina()-amount);
            	if(this.getStamina() == 0) {
                    this.playSound(EpicFightSounds.NEUTRALIZE_MOBS.get(), 3.0F, 0.0F, 0.1F);
                    this.playSound(EpicFightSounds.BLADE_RUSH_FINISHER.get(), -0.5F, -0.4F);
                    this.applyStun(StunType.LONG, 2F);
                    this.playAnimationSynchronized(Animations.BIPED_HIT_LONG, 1);
                    staminaDepleted = true;
            		this.hitPrepare = 60;
                    //this.staminaRegenAwaitTicks = 30;
                    return AttackResult.success(amount);
                    //return super.tryHurt(damageSource, amount);
            	} else if(parry) {
                    this.playSound(EpicFightSounds.CLASH.get(), 1.5F, -4.55F, -4.5F);
                    this.playSound(EpicFightSounds.BLADE_RUSH_FINISHER.get(), 0.45F, -0.4F, -0.3F);
                    this.playAnimationSynchronized(Animations.SWORD_GUARD_ACTIVE_HIT1, 0);
            		this.hitPrepare = 10;
                    this.staminaRegenAwaitTicks = 10;
                    return AttackResult.blocked(amount);
            	} else {
            		//this.setStamina(this.getStamina()-amount);
                    this.playSound(EpicFightSounds.CLASH.get(), 0.75F, -1.55F, -1.21F);
                    this.playAnimationSynchronized(Animations.SWORD_GUARD_HIT, 0);
            		this.hitPrepare = 15;
                    this.staminaRegenAwaitTicks = 15;
                    return AttackResult.blocked(amount);
            	}
            }
            if (this.getStamina() >= 3 && !damageSource.is(EpicFightDamageTypeTags.BYPASS_DODGE)) {
                this.setStamina(this.getStamina() - 3);
                this.staminaRegenAwaitTicks = 40;
                this.playAnimationSynchronized(selectDodge(), 0);
                this.playSound(EpicFightSounds.TUMBLE.get(), -0.05F, 0.1F);
                return AttackResult.missed(amount);
            }
        	} else {
        		this.hitPrepare = 65;
        		if(this.state.attacking()) {
        			this.hitPrepare += 30;
        		}
        		return AttackResult.success(amount);
        	}
        	
        }
        return super.tryHurt(damageSource, amount);
    }

    @Override
    public AttackResult tryHarm(Entity target, EpicFightDamageSource damagesource, float amount) {
        return super.tryHarm(target, damagesource, amount);
    }
}
