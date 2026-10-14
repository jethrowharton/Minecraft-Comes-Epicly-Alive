package net.forixaim.mcea.mixin;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.behavior.*;
import net.conczin.mca.entity.ai.brain.tasks.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.forixaim.mcea.entity_patch.MCAVillagerEntityPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.AttackResult.ResultType;

@Mixin(VillagerEntityMCA.class)
public abstract class MixinVillagerMCA {
    @Inject(method = "hurt", at = @At("HEAD"),remap = false, cancellable = true)
    public void hurt(DamageSource source, float damageAmount, CallbackInfoReturnable<Boolean> cir)
    {
        VillagerEntityMCA villager = ((VillagerEntityMCA)(Object)this);
        if (source.getEntity() instanceof Player && !villager.isHostile())
        {
            cir.setReturnValue(false);
        } else {
    	//cir.setReturnValue(false);
        villager.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, (LivingEntity)source.getEntity());
        MCAVillagerEntityPatch patchEntity = EpicFightCapabilities.getEntityPatch(villager, MCAVillagerEntityPatch.class);
        //patchEntity.setAttakTargetSync((LivingEntity)source.getEntity());
        AttackResult atk = patchEntity.tryHurt(source, damageAmount);
        if(atk.resultType != ResultType.SUCCESS)
        	cir.setReturnValue(false);
        	
        }
    }
}
