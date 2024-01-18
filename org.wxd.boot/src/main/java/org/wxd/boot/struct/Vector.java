package org.wxd.boot.struct;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 表示朝向，位移量
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-04 19:23
 */
@Setter
@Getter
@Accessors(chain = true)
public class Vector implements Serializable {

    /** 表示当前朝向修正值 0 - 11 包含 */
    private volatile int dir;
    /** 表示未修正的x方向正负位移量 只能是1或者-1 */
    private volatile int dir_x;
    /** 表示未修正的y方向正负位移量 只能是1或者-1 */
    private volatile int dir_y;
    /** 表示未修正的z方向正负位移量 只能是1或者-1 */
    private volatile int dir_z;
    /** 在x轴方向位移 偏移量 >=0 */
    @Deprecated
    private volatile double vrx;
    /** 在z轴方向的位移 偏移量 >=0 */
    @Deprecated
    private volatile double vrz;
    /** 角 a 度数 0 - 90 包含 */
    private volatile double atan;
    /** 角 a 度数 0 ~ 360° 不包含 360 */
    private volatile double atan360;

    public Vector() {
    }

    public Vector(Vector vector) {
        this.dir = vector.dir;
        this.dir_x = vector.dir_x;
        this.dir_y = vector.dir_y;
        this.dir_z = vector.dir_z;
        this.atan = vector.atan;
        this.atan360 = vector.atan360;
        this.vrx = vector.vrx;
        this.vrz = vector.vrz;
    }

    public void copyVector(Vector vector) {
        this.dir = vector.dir;
        this.dir_x = vector.dir_x;
        this.dir_y = vector.dir_y;
        this.dir_z = vector.dir_z;
        this.atan = vector.atan;
        this.atan360 = vector.atan360;
        this.vrx = vector.vrx;
        this.vrz = vector.vrz;
    }

    @Override
    public String toString() {
        return "{dir=" + dir + ", dir_x=" + dir_x + ", dir_z=" + dir_z + ", atan=" + atan + ", atan360=" + atan360 + "}";
    }

    public String showString() {
        return "{dir=" + dir + ", atan=" + atan + ", atan360=" + atan360 + "}";
    }
}
