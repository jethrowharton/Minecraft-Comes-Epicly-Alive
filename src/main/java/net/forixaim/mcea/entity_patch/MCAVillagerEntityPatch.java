package net.forixaim.mcea.entity_patch;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;

import net.conczin.mca.entity.EquipmentSet;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.brain.tasks.BowTask;
import net.conczin.mca.registry.EntitiesMCA;
import net.conczin.mca.registry.ProfessionsMCA;
import net.conczin.mca.server.world.data.villageComponents.VillageGuardsManager;
import net.conczin.mca.util.InventoryUtils;
import net.conczin.mca.entity.ai.MemoryModuleTypeMCA;
import net.conczin.mca.entity.ai.brain.VillagerTasksMCA;
import net.conczin.mca.entity.ai.brain.sensor.GuardEnemiesSensor;
import net.forixaim.mcea.MinecraftComesEpiclyAlive;
import net.forixaim.mcea.entity_patch.MCATasksEF;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.ComboAttackAnimation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessorImpl;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.entity.ModifyPlayerLivingMotionEvent;
import yesman.epicfight.api.event.types.entity.ModifyAttackSpeedEvent;
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
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

import yesman.epicfight.world.entity.ai.behavior.MoveToTargetSinkStopInaction;
import yesman.epicfight.world.entity.ai.brain.BrainRecomposer;
import yesman.epicfight.world.entity.ai.behavior.AnimatedCombatBehavior;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.api.ex_cap.data.Moveset;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors.BehaviorSeries.Builder;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.PlaybackSpeedModifier;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;
import yesman.epicfight.world.entity.data.ExpandedSyncedData;
import java.util.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.function.Predicate;
import yesman.epicfight.world.capabilities.item.Style;
import java.util.List;

public class MCAVillagerEntityPatch extends HumanoidMobPatch<VillagerEntityMCA> {
    private float stamina;
    ItemStack prevItem;
    protected int tickSinceLastAction;
    protected boolean staminaDepleted;
    protected int staminaRegenAwaitTicks;
    protected int hitPrepare;
    protected int consecutiveHits;

    public MCAVillagerEntityPatch(VillagerEntityMCA original) {
        super(original, Factions.VILLAGER);
    }

    @Override
    public void onConstructed(VillagerEntityMCA original) {
        super.onConstructed(original);
    }

    @Override
    public LivingEntity getTarget() {
        if (original.getMCABrain().getMemory(MemoryModuleTypeMCA.NEAREST_GUARD_ENEMY).isPresent()) {
            return original.getMCABrain().getMemory(MemoryModuleTypeMCA.NEAREST_GUARD_ENEMY).get();
        } else if (original.getMCABrain().getMemory(MemoryModuleType.ATTACK_TARGET).isPresent()) {
            return original.getMCABrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
        } else if (original.getMCABrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).isPresent()) {
            return original.getMCABrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).get();
        }
        return super.getTarget();
    }

    @Override
    protected void initAI() {
    	//prevItem = original.getItemBySlot(original.getDominantSlot());
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
        event.add(EntitiesMCA.FEMALE_VILLAGER, Attributes.ATTACK_SPEED, 4.0D);
        event.add(EntitiesMCA.MALE_VILLAGER, Attributes.ATTACK_SPEED, 4.0D);
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
        this.weaponAttackMotions.put(CapabilityItem.WeaponCategories.SPEAR,
                ImmutableMap.of(CapabilityItem.Styles.TWO_HAND,
                        MCACombatBehaviors.MCA_SPEAR_TWOHAND));
        this.weaponAttackMotions.put(CapabilityItem.WeaponCategories.GREATSWORD,
                ImmutableMap.of(CapabilityItem.Styles.TWO_HAND,
                		MobCombatBehaviors.HUMANOID_GREATSWORD));
        this.weaponAttackMotions.put(CapabilityItem.WeaponCategories.FIST,
                ImmutableMap.of(CapabilityItem.Styles.COMMON,
                        MCACombatBehaviors.MCA_FIST));
    }

    public void setStamina(float value) {
        this.stamina = Mth.clamp(value, 0.0F, this.getMaxStamina());
        this.getExpandedSynchedData().set(EpicFightExpandedEntityDataAccessors.STAMINA, stamina);
    }

    @Override
    public void preTickServer() {
    	ItemStack stack = original.getItemBySlot(original.getDominantSlot()).isEmpty() ? getBestWeapon() : original.getItemBySlot(original.getDominantSlot());
    	if(prevItem != stack && !original.isNoAi()) {
        	prevItem = stack;
        	//this.updateHeldItem(getPrimaryItemCapability(), getPrimaryItemCapability(), stack, stack, original.getDominantHand());
        	original.goalSelector.removeAllGoals(goal -> goal instanceof AnimatedAttackGoal);
        	setAIAsInfantry(stack.getItem() instanceof ProjectileWeaponItem);
        	this.setWeaponMotions();
    	}
        super.preTickServer();
        if (this.state.canBasicAttack()) {
            this.tickSinceLastAction++;
        }
            this.hitPrepare = Math.max(0,  hitPrepare-1);

        if (!this.state.inaction()) {
            if (this.staminaRegenAwaitTicks > 0) this.staminaRegenAwaitTicks--;
        }

        float stamina = this.getStamina();
        float maxStamina = this.getMaxStamina();
        this.setMaxStunShield(1);
        this.setStunShield(stamina/maxStamina);
        if(stamina >= maxStamina) {
        	this.consecutiveHits = 0;
        }
        float staminaRegen = (float)this.original.getAttributeValue(EpicFightAttributes.STAMINA_REGEN);
        if (staminaRegen > 0.0F) {
            int regenWhenLessThan = 30 - (int)(600 / (int)(30 * staminaRegen));

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
        if(original.getItemBySlot(original.getDominantSlot()) != original.getItemBySlot(original.getDominantSlot())) {
        	if(original.getItemInHand(original.getDominantHand()).isEmpty()) {
        		//original.setItemInHand(original.getDominantHand(), original.getItemBySlot(original.getDominantSlot()));
            
        	} else if (original.getItemBySlot(original.getDominantSlot()).isEmpty()) {
        		//original.setItemSlot(original.getDominantSlot(), original.getItemInHand(original.getDominantHand()));
        		
        	}
            //villager.setItemSlot(villager.getDominantSlot(), stack);
        	
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
        if (getTarget() == null)
        {
            return false;
        }
        if (EpicFightCapabilities.getEntityPatch(getTarget(), LivingEntityPatch.class) == null)
        {
            return this.original.distanceTo(getTarget()) < 1.5F && (this.getEntityState().canUseSkill() || this.hitPrepare > 10);
        }
        else if (this.hitPrepare > 10 || EpicFightCapabilities.getEntityPatch(getTarget(), LivingEntityPatch.class).getEntityState().attacking())
        {
            return this.getEntityState().canUseSkill()|| this.hitPrepare > 0;
        }
        return false;
    }

    static Stream<ItemStack> stream(Container inventory) {
        return IntStream.range(0, inventory.getContainerSize()).mapToObj(inventory::getItem);
    }
    
    static Optional<ItemStack> getBestDamage(Container inv) {
        return InventoryUtils.getBestSword(inv).isPresent() ? InventoryUtils.getBestSword(inv) : (stream(inv).max(Comparator.comparingDouble(ItemStack::getMaxDamage)));
    }
    
    public ItemStack getBestWeapon() {
    	ItemStack stack = getBestDamage(original.getInventory()).orElse(original.getItemBySlot(original.getDominantSlot()).isEmpty() ? ItemStack.EMPTY : original.getItemBySlot(original.getDominantSlot()));

		if(original.getProfession() == ProfessionsMCA.ARCHER && !(getTarget() != null && getTarget().distanceTo(original) < 3) || stack.isEmpty()) {
	        stack = InventoryUtils.getBestRanged(original.getInventory()).orElse(stack);
		}
    	if(!original.getItemBySlot(original.getDominantSlot()).isEmpty() && original.getItemBySlot(original.getDominantSlot()).getMaxDamage() > stack.getMaxDamage()) {
    		return original.getItemBySlot(original.getDominantSlot());
    	}
		return stack;
    }
    
    
    
    public ItemStack getBestWeapon(Item fallback) {
    	ItemStack stack = getBestDamage(original.getInventory()).orElse(fallback != null && fallback != Items.AIR ? new ItemStack(fallback) : ItemStack.EMPTY);
		if(original.getProfession() == ProfessionsMCA.ARCHER && !(getTarget() != null && getTarget().distanceTo(original) < 3) || stack.isEmpty()) {
	        stack = InventoryUtils.getBestRanged(original.getInventory()).orElse(fallback != null && fallback != Items.AIR ? new ItemStack(fallback) : stack);
		}
    	if(!original.getItemBySlot(original.getDominantSlot()).isEmpty() && original.getItemBySlot(original.getDominantSlot()).getMaxDamage() > stack.getMaxDamage()) {
    		return original.getItemBySlot(original.getDominantSlot());
    	}
		return stack;
    }
    
    @Override

    public CombatBehaviors.Builder<HumanoidMobPatch<?>> getHoldingItemWeaponMotionBuilder() {
    	ItemStack stack = original.getItemBySlot(original.getDominantSlot()).isEmpty() ? getBestWeapon() : original.getItemBySlot(original.getDominantSlot());
		CapabilityItem itemCap = EpicFightCapabilities.getItemStackCapability(stack);
        //List<AnimationAccessor<? extends AttackAnimation>> combo = itemCap.getAutoAttackMotion(null);
		
		if (this.weaponAttackMotions.containsKey(itemCap.getWeaponCategory())) {
			Map<Style, CombatBehaviors.Builder<HumanoidMobPatch<?>>> motionByStyle = this.weaponAttackMotions.get(itemCap.getWeaponCategory());
			Style style = itemCap.getStyle(this);
			
			if (motionByStyle.containsKey(style) || motionByStyle.containsKey(CapabilityItem.Styles.COMMON)) {
				return motionByStyle.getOrDefault(style, motionByStyle.get(CapabilityItem.Styles.COMMON));
			}
		}
		return super.getHoldingItemWeaponMotionBuilder();
		//return this.original.getMainHandItem().isEmpty() ? MobCombatBehaviors.HUMANOID_FIST : MobCombatBehaviors.HUMANOID_ONEHAND_TOOLS;
	}
    
    public CombatBehaviors.Builder<HumanoidMobPatch<?>> getHoldingItemWeaponCapBuilder() {
    	ItemStack stack = this.getAdvancedHoldingItemStack(original.getDominantHand());
    	if(stack == null) {
    		stack = getBestWeapon();
    	}
		//if(!stack.isEmpty()) {
			CapabilityItem itemCap = EpicFightCapabilities.getItemStackCapability(stack);
			if(itemCap instanceof WeaponCapability weaponCap ) {
		        Moveset set = weaponCap.getCurrentSet(this);
	        List<AnimationAccessor<? extends AttackAnimation>> combo;
	        if(set == null) {
	        	combo = CapabilityItem.getBasicAutoAttackMotions();
	        	
	        } else {
	        	
	        	combo = set.getComboAttackAnimations();
	        }
	        CombatBehaviors.Builder<HumanoidMobPatch<?>> combobehavior = CombatBehaviors.<HumanoidMobPatch<?>>builder();
	        Builder<HumanoidMobPatch<?>> combobuilder = CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(100.0F).canBeInterrupted(true).looping(true);
	        for(AnimationAccessor<? extends AttackAnimation> combopart : combo) {
	        	//if(combopart.get().getAccessor().get() instanceof ComboAttackAnimation comboattack) {
	        	//	AnimationAccessor<ComboAttackAnimation> accessor =   new AnimationAccessorImpl<ComboAttackAnimation>(comboattack.getLocation(), combo.size() + 1, false, null);
	        	//	combopart.get().setAccessor(accessor);
	        		
	        	//}
	            Optional<Float> reachOpt = combopart.get().getRealAnimation().get().getProperty(AttackAnimationProperty.REACH);
	            //combopart.get().setTotalTime(totalTime);
	            //combopart.get().getAccessor().get().getAnimationClip().setClipTime(totalTime);
	        	combobuilder.nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(combopart.get().getRealAnimation()).withinEyeHeight().withinDistance(0.0D, (reachOpt.isPresent() ? reachOpt.get()+weaponCap.getReach() : weaponCap.getReach()+1.8D)));
	        }
	        
	        combobehavior.newBehaviorSeries(combobuilder);
	        return combobehavior;
				
			}
		//}
		return getHoldingItemWeaponMotionBuilder();
        
	}

    private AnimationManager.AnimationAccessor<? extends DodgeAnimation> selectDodge()
    {
        ClipContext backClip = new ClipContext(this.original.position().add(this.original.getLookAngle().scale(0.1).reverse()), this.original.position().add(this.original.getLookAngle()).scale(1.5), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.original);
        ClipContext leftClip = new ClipContext(this.original.position().add(this.original.getLookAngle().yRot(90).scale(0.1).reverse()), this.original.position().add(this.original.getLookAngle().yRot(90)).scale(1.5), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.original);
        ClipContext rightClip = new ClipContext(this.original.position().add(this.original.getLookAngle().yRot(-90).scale(0.1).reverse()), this.original.position().add(this.original.getLookAngle().yRot(-90)).scale(1.5), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.original);
        BlockHitResult backHit = this.original.level().clip(backClip);
        BlockHitResult leftHit = this.original.level().clip(leftClip);
        BlockHitResult rightHit = this.original.level().clip(rightClip);
        if (backHit.getType() == HitResult.Type.MISS) {
        	//this.original.setDeltaMovement(this.original.getLookAngle().scale(0.35).reverse());
            return Animations.BIPED_STEP_BACKWARD;
        }
        if (leftHit.getType() == HitResult.Type.MISS) {
        	//this.original.setDeltaMovement(this.original.getLookAngle().yRot(90).scale(0.35));
            return Animations.BIPED_STEP_LEFT;
        }
        if (rightHit.getType() == HitResult.Type.MISS) {
        	//this.original.setDeltaMovement(this.original.getLookAngle().yRot(-90).scale(0.35));
            return Animations.BIPED_STEP_RIGHT;
        }
    	//this.original.setDeltaMovement(this.original.getLookAngle().scale(0.35));
        return Animations.BIPED_STEP_FORWARD;
    }
    
    @Override
    public void setAIAsInfantry(boolean holdingRanedWeapon) {
		//CombatBehaviors.Builder<HumanoidMobPatch<?>> builder = this.getHoldingItemWeaponMotionBuilder();
    	prevItem = original.getItemBySlot(original.getDominantSlot()).isEmpty() ? getBestWeapon() : original.getItemBySlot(original.getDominantSlot());
		CombatBehaviors.Builder<HumanoidMobPatch<?>> builder = this.getHoldingItemWeaponCapBuilder();
		if (builder != null && !this.isAnticipatingAttack()) {
			this.original.goalSelector.addGoal(0, new AnimatedAttackGoal<>(this, builder.build(this)));
			this.original.goalSelector.addGoal(1, new TargetChasingGoal(this, this.getOriginal(), 1.0D, true));
		} else if (this.isAnticipatingAttack()) {
            this.currentCompositeMotion = LivingMotions.BLOCK;
            //this.reserveAnimation(Animations.BIPED_BLOCK);
			
		}
	}
    


    @Override
    public void updateMotion(boolean considerInaction) {
        if (this.original.getHealth() <= 0.0F) {
        	this.currentLivingMotion = LivingMotions.DEATH;

        }
        else if (this.original.isSleeping()) {
        	this.currentLivingMotion = LivingMotions.SLEEP;
        }
        else if (this.state.inaction() && considerInaction) {
        	this.currentLivingMotion = LivingMotions.IDLE;
        } else {
            if (original.getVehicle() != null) {
            	this.currentLivingMotion = LivingMotions.MOUNT;
            } else {
                if (this.original.getDeltaMovement().y < -0.55F || this.isAirborneState())
                	this.currentLivingMotion = LivingMotions.FALL;
                else if (original.walkAnimation.speed() > 0.08F) {
                    if (original.walkAnimation.speed() > 0.7f)
                    	this.currentLivingMotion = LivingMotions.RUN;
                    else
                    	this.currentLivingMotion = LivingMotions.WALK;
                }
                else
                	this.currentLivingMotion = LivingMotions.IDLE;
            }
        }
        
        //if (this.isAnticipatingAttack() && this.getHoldingItemCapability(InteractionHand.OFF_HAND).isWeaponCategory(CapabilityItem.WeaponCategories.SHIELD))
        //    currentCompositeMotion = LivingMotions.BLOCK_SHIELD;
        //else if (this.isAnticipatingAttack())
        //    currentCompositeMotion = LivingMotions.BLOCK;
        if (!this.state.updateLivingMotion() && considerInaction) {
            this.currentCompositeMotion = LivingMotions.NONE;
        } else {
            CapabilityItem mainhandItemCap = this.getHoldingItemCapability(original.getDominantHand());
            CapabilityItem offhandItemCap = this.getHoldingItemCapability(original.getOpposingHand());
            LivingMotion customLivingMotion = mainhandItemCap.getLivingMotion(this, original.getDominantHand());

            if (customLivingMotion == null) customLivingMotion = offhandItemCap.getLivingMotion(this, original.getOpposingHand());
            if (customLivingMotion != null)
            	this.currentCompositeMotion = customLivingMotion;
            else if (this.original.isUsingItem()) {
                UseAnim useAnim = this.original.getUseItem().getUseAnimation();
                if (useAnim == UseAnim.BLOCK)
                	this.currentCompositeMotion = LivingMotions.BLOCK_SHIELD;
                else if (useAnim == UseAnim.CROSSBOW)
                	this.currentCompositeMotion = LivingMotions.RELOAD;
                else if (useAnim == UseAnim.DRINK)
                	this.currentCompositeMotion = LivingMotions.DRINK;
                else if (useAnim == UseAnim.EAT)
                	this.currentCompositeMotion = LivingMotions.EAT;
                else if (useAnim == UseAnim.SPYGLASS)
                	this.currentCompositeMotion = LivingMotions.SPECTATE;
                else
                	this.currentCompositeMotion = currentLivingMotion;
            } else {
                if (this.getClientAnimator().getCompositeLayer(Layer.Priority.MIDDLE).animationPlayer.getRealAnimation().get().isReboundAnimation())
                	this.currentCompositeMotion = LivingMotions.SHOT;
                else if (this.isAnticipatingAttack() && this.getHoldingItemCapability(original.getOpposingHand()).isWeaponCategory(CapabilityItem.WeaponCategories.SHIELD))
                	this.currentCompositeMotion = LivingMotions.BLOCK_SHIELD;
                else if (this.isAnticipatingAttack() && this.state.canUseSkill())
                	this.currentCompositeMotion = LivingMotions.BLOCK;
                else if (this.original.swinging && this.original.getSleepingPos().isEmpty())
                	this.currentCompositeMotion = LivingMotions.DIGGING;
                else
                	this.currentCompositeMotion = currentLivingMotion;
            }
        }
    	if (this.isAnticipatingAttack() && this.state.canUseSkill())
        	this.currentCompositeMotion = LivingMotions.BLOCK;


    	//ItemStack stack = getBestWeapon();
		//if((MCATasksEF.hasPreferredTarget(original) || getTarget() != null) && !stack.isEmpty()) {
			//this.original.setItemInHand(this.original.getDominantHand(), stack);
			//this.original.setItemSlot(this.original.getDominantSlot(), stack);
			//this.setWeaponMotions();
		//}
        //this.modifyLivingMotionByCurrentItem(true);
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
            var impact = amount*0.1F;
            if(EpicFightCapabilities.getEntityPatch(damageSource.getEntity(), LivingEntityPatch.class) != null && EpicFightCapabilities.getEntityPatch(damageSource.getEntity(), LivingEntityPatch.class).getEpicFightDamageSource() != null) {
            	impact = EpicFightCapabilities.getEntityPatch(damageSource.getEntity(), LivingEntityPatch.class).getEpicFightDamageSource().calculateImpact()*0.6f;
            }
        	var willblock = !damageSource.is(EpicFightDamageTypeTags.UNBLOCKALBE) && !staminaDepleted &&(this.currentCompositeMotion == LivingMotions.BLOCK || this.currentCompositeMotion == LivingMotions.BLOCK_SHIELD ||  !this.state.attacking() && (Mth.randomBetween(this.getLevel().random, 0, 1) > (this.original.getProfession() == ProfessionsMCA.ARCHER ? 0.75 : 0.25)) && this.getStamina()-0.1f*impact > 0 || this.getStamina() < 3);
            if (willblock && (this.currentCompositeMotion == LivingMotions.BLOCK || this.currentCompositeMotion == LivingMotions.BLOCK_SHIELD || this.getStamina() >= 0) &&
                    !(damageSource.is(EpicFightDamageTypeTags.UNBLOCKALBE)) ) {
            	var parry = this.hitPrepare <= 30 && Mth.randomBetween(this.getLevel().random, 0, 1) > 0.5;
            		this.setStamina(parry ? this.getStamina()-0.1f : this.getStamina()-impact*(1+this.consecutiveHits));
            	if(this.getStamina() == 0) {
                    this.playSound(EpicFightSounds.NEUTRALIZE_MOBS.get(), 3.0F, 0.0F, 0.1F);
                    //this.playSound(EpicFightSounds.BLADE_RUSH_FINISHER.get(), -0.5F, -0.4F);
                    this.applyStun(StunType.LONG, 2F);
                    this.playAnimationSynchronized(Animations.BIPED_HIT_LONG, 1);
                    staminaDepleted = true;
            		this.hitPrepare = 60;
                    this.staminaRegenAwaitTicks = 5;
                    //this.staminaRegenAwaitTicks = 30;
                    this.consecutiveHits = 0;
                    return AttackResult.success(amount);
                    //return super.tryHurt(damageSource, amount);
            	} else if(parry) {
                    this.playSound(EpicFightSounds.CLASH.get(), 1.5F, -4.5F, -4.65F);
                    this.playSound(EpicFightSounds.CLASH.get(), 1.5F, -1.55F, -1.65F);
                    //this.playSound(EpicFightSounds.BLADE_RUSH_FINISHER.get(), 0.75F, -0.4F, -0.3F);
                    this.playAnimationSynchronized(Animations.SWORD_GUARD_ACTIVE_HIT1, 0);
            		this.hitPrepare = 10;
                    this.staminaRegenAwaitTicks = 10;
                    this.consecutiveHits = 0;
                    return AttackResult.blocked(amount);
            	} else {
            		//this.setStamina(this.getStamina()-amount);
                    this.playSound(EpicFightSounds.CLASH.get(), 0.75F, -4.75F, -4.95F);
                    this.playAnimationSynchronized(Animations.SWORD_GUARD_HIT, 0);
            		this.hitPrepare = 15;
                    this.staminaRegenAwaitTicks = 15;
                    this.consecutiveHits++;
                    return AttackResult.blocked(amount);
            	}
            }
            if (this.getStamina() >= 3 && !damageSource.is(EpicFightDamageTypeTags.BYPASS_DODGE)) {
                this.setStamina(this.getStamina() - 3);
                this.staminaRegenAwaitTicks = 40;
                //this.playAnimationSynchronized(selectDodge(), 0);
                this.playAnimationInstantly(selectDodge());
                this.playSound(EpicFightSounds.TUMBLE.get(), -0.05F, 0.1F);
                return AttackResult.missed(amount);
            }
        	} else {
        		this.hitPrepare = 45;
        		if(this.state.attacking()) {
        			this.hitPrepare += 90;
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
