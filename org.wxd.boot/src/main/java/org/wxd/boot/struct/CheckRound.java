package org.wxd.boot.struct;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import straightedge.geom.Vector3;

/**
 * 圆形测试
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-04 19:23
 */
@Slf4j
@Getter
@Setter
public class CheckRound extends Check {

    /*半径*/
    private volatile float radius;

    public CheckRound(float radius) {
        this.radius = radius;
    }

    /**
     * 圆形测试
     *
     * @param radius
     * @param center
     */
    public CheckRound(float radius, Vector3 center) {
        super(center);
        this.radius = radius;
    }

    @Override
    public boolean contains(double x, double z, float tmpradius) {
        return MoveUtil.distance(getCenter().getX(), getCenter().getZ(), x, z) - tmpradius <= radius;
    }

    @Override
    public boolean move() {
        if (getTranslation() != null) {
            if (getTranslationCount() > 0) {
                getCenter().x += getTranslation().getX();
                getCenter().z += getTranslation().getZ();
                setTranslationCount(getTranslationCount() - 1);
                return false;
            }
        }
        return true;
    }

}
