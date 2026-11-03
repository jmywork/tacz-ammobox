package com.taczammobox.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Worn bandolier: a sling of linked rounds crossing the chest, built from blocky
 * steps so it reads like the pixel-art item instead of a smooth ribbon.
 *
 * <p>Coordinates follow the vanilla player model: {@code y} grows downward from
 * the neck to the hips (0..12), {@code x} is right/left (-4..4), {@code z} is
 * front/back (-2..2). The sling goes over the left shoulder, down to the right
 * hip on the front, and mirrors on the back to close the loop.</p>
 */
public final class AmmoBeltWornModel extends HumanoidModel<LivingEntity> {
    /** Number of blocks used to step the sling across the torso. */
    private static final int STEPS = 8;
    /** How far the sling sits outside the torso. */
    private static final int FRONT_Z = -3;
    private static final int BACK_Z = 2;

    public AmmoBeltWornModel() {
        super(bake());
    }

    private static ModelPart bake() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        CubeListBuilder body = CubeListBuilder.create();
        for (int i = 0; i < STEPS; i++) {
            int x = -4 + i;
            int y = 1 + (i * 10) / (STEPS - 1);
            body.texOffs(0, 0).addBox(x, y, FRONT_Z, 1, 2, 1);
            body.texOffs(0, 0).addBox(x, y, BACK_Z, 1, 2, 1);
        }
        // shoulder and hip connectors close the loop over the body
        body.texOffs(0, 0).addBox(-4, 0, FRONT_Z, 1, 1, 6);
        body.texOffs(0, 0).addBox(3, 11, FRONT_Z, 1, 1, 6);
        // brass rounds standing in the front half of the sling
        for (int i = 1; i < STEPS; i += 2) {
            int x = -4 + i;
            int y = 1 + (i * 10) / (STEPS - 1);
            body.texOffs(16, 0).addBox(x, y - 2, FRONT_Z - 1, 1, 3, 1);
            body.texOffs(16, 0).addBox(x, y - 2, BACK_Z + 1, 1, 3, 1);
        }
        root.addOrReplaceChild("body", body, PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }
}
