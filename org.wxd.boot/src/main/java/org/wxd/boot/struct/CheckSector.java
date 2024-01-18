package org.wxd.boot.struct;

import lombok.extern.slf4j.Slf4j;
import straightedge.geom.Vector3;

/**
 * 扇形
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-04 19:23
 */
@Slf4j
public class CheckSector extends Check {

    /*扇形的半径*/
    private volatile float radius;
    /*扇形的A边*/
    private volatile double sa;
    /*扇形的B边*/
    private volatile double sb;

    /**
     * 扇形
     *
     * @param center
     * @param direction
     * @param angle
     * @param radius
     */
    public CheckSector(Vector3 center, double direction, double angle, float radius) {
        super(center);
        this.radius = radius;
        angle /= 2;
        /*计算扇形的夹角度*/
        sa = MoveUtil.getATan360(direction, -1 * angle);
        sb = MoveUtil.getATan360(direction, angle);
        if (sa == sb) {
            throw new RuntimeException("计算扇形状态，便宜角度过后，左右角度一样；检查配置");
        }
    }

    /**
     * 这里的扇形都是羽毛球类似的扇形，
     *
     * @param x
     * @param z
     * @param tmpradius
     * @return
     */
    @Override
    public boolean contains(double x, double z, float tmpradius) {
        double tmpTan360 = MoveUtil.getATan360(getCenter().getX(), getCenter().getZ(), x, z);
        double distance = getCenter().distance(x, z) - tmpradius;
        if (distance <= 0.45) {
            return true;
        }
        if (0.45 < distance && distance <= radius/*判断距离范围，羽毛球，羽毛部分距离*/) {
            if (sa == sb) {
                return true;
            }
            /*羽毛球的羽毛部分夹角度*/
            if (sa > sb) {
                if (sa <= tmpTan360 && tmpTan360 <= 360) {
                    return true;
                }
                if (0 <= tmpTan360 && tmpTan360 <= sb) {
                    return true;
                }
            }
            /*羽毛球的羽毛部分夹角度*/
            if (sa < sb && sa <= tmpTan360 && tmpTan360 <= sb) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "SectorCheck{" + "radius=" + radius + ", sa=" + sa + ", sb=" + sb + ", center=" + getCenter() + '}';
    }

}
