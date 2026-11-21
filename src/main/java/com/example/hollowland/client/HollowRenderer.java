package com.example.hollowland.client;

import com.example.hollowland.HollowlandMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Shared renderer: a scaled humanoid model with a custom texture. */
public class HollowRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public HollowRenderer(EntityRendererProvider.Context ctx, String textureName, float scale) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F * scale);
        this.texture = new ResourceLocation(HollowlandMod.MOD_ID, "textures/entity/" + textureName + ".png");
        this.scale = scale;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }
}
