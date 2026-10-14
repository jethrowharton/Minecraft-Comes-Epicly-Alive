package net.forixaim.mcea.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.brain.VillagerTasksMCA;
import net.forixaim.mcea.entity_patch.MCATasksEF;
import net.forixaim.mcea.entity_patch.MCAVillagerEntityPatch;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.schedule.Activity;
import net.conczin.mca.registry.ProfessionsMCA;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(VillagerTasksMCA.class)
public class MixinVillagerTasks {

    @Inject(method = "getGuardCorePackage", at = @At("HEAD"), remap = false, cancellable = true)
    private static void getGuardCorePackage(VillagerEntityMCA villager, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super VillagerEntityMCA>>>> cir) {
        if (EpicFightCapabilities.getEntityPatch(villager, LivingEntityPatch.class) instanceof MCAVillagerEntityPatch entityPatch)
        {
            cir.setReturnValue(MCATasksEF.getGuardCorePackage(villager));
        }
    }
    @Inject(method = "getSelfDefencePackage", at = @At("HEAD"), remap = false, cancellable = true)
    private static void getSelfDefensePackage(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super VillagerEntityMCA>>>> cir) {
        //if (EpicFightCapabilities.getEntityPatch(villager, LivingEntityPatch.class) instanceof MCAVillagerEntityPatch entityPatch)
        //{
            cir.setReturnValue(MCATasksEF.getBarebonesPackage());
            //cir.cancel();
        //}
    }
    @Inject(method = "initializeTasks", at = @At("HEAD"), remap = false)
    private static void invokeVillagerSelfDefence(VillagerEntityMCA villager, Brain<VillagerEntityMCA> brain, CallbackInfoReturnable<Brain<VillagerEntityMCA>> cir) {
        if (EpicFightCapabilities.getEntityPatch(villager, LivingEntityPatch.class) instanceof MCAVillagerEntityPatch entityPatch && !villager.isGuard() && villager.getProfession() != ProfessionsMCA.MERCENARY)
        {
        	brain.addActivity(Activity.CORE, MCATasksEF.getSelfDefensePackage(villager));
        }
    }
}
