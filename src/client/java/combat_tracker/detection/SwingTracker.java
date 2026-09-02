package combat_tracker.detection;

import combat_tracker.record.SessionRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SwingTracker {
    private static final SwingTracker INSTANCE = new SwingTracker();

    public static SwingTracker get() {
        return INSTANCE;
    }

    private static final double CANDIDATE_RANGE = 6.0;
    private static final double CANDIDATE_CONE_DEG = 30.0;

    private SwingTracker() {
    }

    public void onSwing(Minecraft client, HitResult hitResult) {
        LocalPlayer self = client.player;
        if (self == null || client.level == null) {
            return;
        }

        Vec3 eye = self.getEyePosition();
        Vec3 look = self.getViewVector(1.0F).normalize();

        boolean landed = false;
        Player target = null;
        if (hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof Player p && p != self) {
            target = p;
            landed = true;
        } else {
            target = findIntendedTarget(client, self, eye, look);
        }
        if (target == null) {
            return;
        }

        AABB box = target.getBoundingBox();
        Vec3 centre = box.getCenter();

        double reach = reachTo(eye, box);
        double aimDeg = angleBetween(look, centre.subtract(eye));
        double[] placement = placementOnHitbox(eye, look, box, centre);

        String targetName = target.getName().getString();
        OpponentTracker.get().note(targetName);
        SessionRecorder.get().noteHeldItem(self.getMainHandItem());
        if (landed && target.isBlocking()) {
            SessionRecorder.get().recordShieldBreakerAttempt(self.getMainHandItem().is(ItemTags.AXES));
        }
        SessionRecorder.get().recordSwing(
                reach, landed, aimDeg, placement[0], placement[1], targetName);
    }

    private static double reachTo(Vec3 eye, AABB box) {
        return Math.sqrt(box.distanceToSqr(eye));
    }

    private static double[] placementOnHitbox(Vec3 eye, Vec3 look, AABB box, Vec3 centre) {
        Vec3 toCentre = centre.subtract(eye);
        double along = Math.max(0.0, toCentre.dot(look));
        Vec3 nearest = eye.add(look.scale(along));
        Vec3 offset = nearest.subtract(centre);

        double vertical = offset.y;
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0e-6) {
            return new double[]{0.0, vertical};
        }
        double horizontal = offset.dot(right.normalize());
        return new double[]{horizontal, vertical};
    }

    private static double angleBetween(Vec3 look, Vec3 toTarget) {
        double len = toTarget.length();
        if (len < 1.0e-6) {
            return 0.0;
        }
        double cos = Math.max(-1.0, Math.min(1.0, look.dot(toTarget.scale(1.0 / len))));
        return Math.toDegrees(Math.acos(cos));
    }

    private static Player findIntendedTarget(Minecraft client, LocalPlayer self, Vec3 eye, Vec3 look) {
        Player best = null;
        double bestAngle = CANDIDATE_CONE_DEG;
        for (Player p : client.level.players()) {
            if (p == self || !p.isAlive()) {
                continue;
            }
            Vec3 toTarget = p.getBoundingBox().getCenter().subtract(eye);
            if (toTarget.length() > CANDIDATE_RANGE) {
                continue;
            }
            double angle = angleBetween(look, toTarget);
            if (angle < bestAngle) {
                bestAngle = angle;
                best = p;
            }
        }
        return best;
    }
}
