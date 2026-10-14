package net.forixaim.mcea.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.conczin.mca.client.model.VillagerEntityModelMCA;
import net.conczin.mca.client.render.layer.VillagerLayer;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.conczin.mca.client.render.VillagerEntityMCARenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.FastColor;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import it.unimi.dsi.fastutil.ints.IntList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.Mesh.DrawingFunction;
import yesman.epicfight.api.client.model.transformer.HumanoidModelBaker;
import yesman.epicfight.api.client.model.transformer.HumanoidModelTransformer;
import yesman.epicfight.api.client.model.transformer.HumanoidModelTransformer.PartTransformer;
import yesman.epicfight.api.client.model.transformer.VanillaModelTransformer;
import yesman.epicfight.api.client.model.transformer.VanillaModelTransformer.VanillaMeshPartDefinition;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.SingleGroupVertexBuilder;
import yesman.epicfight.api.client.model.SkinnedMesh.SkinnedMeshPart;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.QuaternionUtils;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.api.utils.math.Vec2f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.math.Vec4f;
import net.conczin.mca.client.model.CommonVillagerModel;

public class MCAPatchedLayer extends PatchedLayer<VillagerEntityMCA, HumanoidMobPatch<VillagerEntityMCA>, VillagerEntityModelMCA<VillagerEntityMCA>, VillagerLayer<VillagerEntityMCA, VillagerEntityModelMCA<VillagerEntityMCA>>> {
    final PMCAVillagerRenderer parent;
    final VillagerLayer<VillagerEntityMCA, VillagerEntityModelMCA<VillagerEntityMCA>> vanillaLayer;
    MCAPatchedLayer(PMCAVillagerRenderer parent, VillagerLayer<VillagerEntityMCA, VillagerEntityModelMCA<VillagerEntityMCA>> vanillaLayer)
    {
        this.parent = parent;
        this.vanillaLayer = vanillaLayer;
    }
    @Override
    protected void renderLayer(HumanoidMobPatch<VillagerEntityMCA> entitypatch, VillagerEntityMCA villager, @Nullable VillagerLayer<VillagerEntityMCA, VillagerEntityModelMCA<VillagerEntityMCA>> vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
        ResourceLocation resourceLocation = this.vanillaLayer.getSkin(villager);
        if (resourceLocation != null)
        {
            renderMCALayer(entitypatch, villager, poseStack, buffer, packedLight, poses, partialTicks, resourceLocation, vanillaLayer);
        }

    }

	private static record ModelPartition(ModelPart modelPart, String partName) {
	}
    
    private void renderMCALayer(HumanoidMobPatch<VillagerEntityMCA> entitypatch, VillagerEntityMCA villager, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float partialTicks, ResourceLocation resourceLocation, @Nullable VillagerLayer<VillagerEntityMCA, VillagerEntityModelMCA<VillagerEntityMCA>> vanillaLayer)
    {
        int packerOverlay = LivingEntityRenderer.getOverlayCoords(villager, 0);
        RenderType renderType = RenderType.entityCutoutNoCull(resourceLocation);
        int color = this.vanillaLayer.getColor(villager, partialTicks);
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float alpha = a / 255.0F;
        float red   = r / 255.0F;
        float green = g / 255.0F;
        float blue  = b / 255.0F;
        parent.getDefaultMesh().get().draw(poseStack, buffer, renderType, packedLight, red, green, blue, alpha, packerOverlay, entitypatch.getArmature(), poses);
        		List<ModelPartition> breastmodel = Lists.newArrayList();
        		this.vanillaLayer.model.breasts.loadPose(this.vanillaLayer.model.breasts.getInitialPose());
        		this.vanillaLayer.model.breastsWear.loadPose(this.vanillaLayer.model.breastsWear.getInitialPose());
        		this.vanillaLayer.model.applyVillagerDimensions(villager);
        		breastmodel.add(new ModelPartition(this.vanillaLayer.model.breasts, "breasts"));
        		breastmodel.add(new ModelPartition(this.vanillaLayer.model.breastsWear, "breastsWear"));
        		poseStack.pushPose();
        		HumanoidArmature armature = entitypatch.getArmature();
                Joint chest = armature.chest;
        	float breastSize = villager.getGenetics().getBreastSize();
        		SkinnedMesh breastMesh = bakeMeshFromCubes(breastmodel, new Vector3f(0.125f, 0, -0.95f), new Vector3f(((breastSize * 0.2f) + 1.05f), ((breastSize * 0.75f) + 0.75f), ((breastSize * 0.75f) + 0.75f)), chest.getLocalTransform());
        if(this.vanillaLayer.model.getBreastPart().visible && this.vanillaLayer.model.getBodyPart().visible) {
        	if (breastSize > 0 && resourceLocation != null && entitypatch.getArmature() instanceof HumanoidArmature) {
        		breastMesh.initialize();
        		for(SkinnedMeshPart part : breastMesh.getAllParts()) {
        			part.setHidden(parent.getDefaultMesh().get().torso.isHidden());
        		}
        		
        		breastMesh.draw(poseStack, buffer, renderType, packedLight, red, green, blue, alpha, packerOverlay, entitypatch.getArmature(), poses);
        	}
        }
        		poseStack.popPose();

        
    }
    private static void bakeCube(PoseStack poseStack, MeshPartDefinition partDefinition, ModelPart.Cube cube,  List<SingleGroupVertexBuilder> vertices,Map<MeshPartDefinition, IntList> indices,PartTransformer.IndexCounter indexCounter) {
        for (ModelPart.Polygon polygon : cube.polygons) {
            Vector3f normal = new Vector3f(polygon.normal);
            Matrix3f matrix3f = poseStack.last().normal();
            normal.mul(matrix3f);
            
            for (ModelPart.Vertex vertex : polygon.vertices) {
            	Matrix4f matrix4f = poseStack.last().pose();
            	Vector4f verpos = new Vector4f(vertex.pos, 1F);
            	verpos.mul(poseStack.last().pose());
            	vertices.add(new SingleGroupVertexBuilder()
            			.setPosition(new Vec3f(verpos.x,verpos.y,verpos.z).scale(0.0625F))
            			.setNormal(new Vec3f(normal.x,normal.y,normal.z))
						.setTextureCoordinate(new Vec2f(vertex.u, vertex.v))
						.setEffectiveJointIDs(new Vec3f(8, 0, 0))
						.setEffectiveJointWeights(new Vec3f(1, 0.0F, 0.0F))
						.setEffectiveJointNumber(1)
            			);
            	
            }
            PartTransformer.triangluatePolygon(indices, partDefinition, indexCounter);
        }
    }
    
	private static SkinnedMesh bakeMeshFromCubes(List<ModelPartition> models, Vector3f offset, Vector3f scale,  OpenMatrix4f transform) {
		List<SingleGroupVertexBuilder> vertices = Lists.newArrayList();
		Map<MeshPartDefinition, IntList> indices = Maps.newHashMap();
		PoseStack poseStack = new PoseStack();
		PartTransformer.IndexCounter indexCounter = new PartTransformer.IndexCounter();
		poseStack.mulPose(QuaternionUtils.YP.rotationDegrees(180.0F));
		poseStack.mulPose(QuaternionUtils.XP.rotationDegrees(180.0F));
		poseStack.translate(0.0F, -24.0F, 0.0F);
		for (ModelPartition modelpartition : models) {
			ModelPart part = modelpartition.modelPart;
			part.offsetPos(offset);
			poseStack.pushPose();
			poseStack.translate(part.x, part.y, part.z);
			//poseStack.translate(offset.x, offset.y, offset.z);
			if (part.xRot != 0.0F || part.yRot != 0.0F || part.zRot != 0.0F) {
			poseStack.mulPose(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
			}
			if (part.xScale != 1.0F || part.yScale != 1.0F || part.zScale != 1.0F) {
			poseStack.scale(part.xScale, part.yScale, part.zScale);
			}
			if (scale.x != 1.0F || scale.y != 1.0F || scale.z != 1.0F) {
			poseStack.scale(scale.x, scale.y, scale.z);
			}
			poseStack.mulPose(OpenMatrix4f.exportToMojangMatrix(transform));
			for(ModelPart.Cube cube : part.cubes) {
				bakeCube(poseStack, VanillaMeshPartDefinition.of(modelpartition.partName), cube, vertices, indices, indexCounter);
			}
			poseStack.popPose();
		}
		
		return SingleGroupVertexBuilder.loadVertexInformation(vertices, indices);
	}
	
}
