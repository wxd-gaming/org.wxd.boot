package org.wxd.boot.struct;

import org.wxd.boot.lang.ConvertUtil;
import straightedge.geom.Vector3;

/**
 * 任意多边形，
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2023-12-04 19:23
 */
public class CheckPolygon extends Check {

    /*多边形的顶点*/
    double[] pointXs;
    double[] pointZs;
    /*当前已经添加的坐标点*/
    int pointCount = 0;

    public CheckPolygon() {
    }

    /**
     * 任意多边形
     *
     * @param center
     */
    public CheckPolygon(Vector3 center) {
        super(center);
    }

    private void init(int size) {
        pointXs = new double[size];
        pointZs = new double[size];
    }

    /**
     * 当前坐标点位中心点的等角（等边）三角形，当前朝向位A点顶点延伸
     *
     * @param direction
     * @param vr        中心点偏移位置
     * @param vr_width  三角形，中心点距离顶点距离
     */
    public void initTriangle(double direction, double vr, double vr_width) {
        init(3);
        Vector vector = MoveUtil.getVectorBy360Atan(direction);
        double x = getCenter().getX();
        double z = getCenter().getZ();
        if (vr != 0) {
            /* 根据三角函数计算出 中心点 偏移量 */
            double v12_V_X = 0;
            double v12_V_Y = 0;
            if (vr < 0) {
                /* 传入负数的时候方向刚好是相反方向运动 */
                v12_V_X = -1 * vector.getDir_x() * MoveUtil.getV12XD(vr, vector.getAtan());
                v12_V_Y = -1 * vector.getDir_z() * MoveUtil.getV12ZD(vr, vector.getAtan());
            } else {
                /* 正前方移动 */
                v12_V_X = vector.getDir_x() * MoveUtil.getV12XD(vr, vector.getAtan());
                v12_V_Y = vector.getDir_z() * MoveUtil.getV12ZD(vr, vector.getAtan());
            }
            x += v12_V_X;
            z += v12_V_Y;
        }

        Vector bVector = MoveUtil.getVectorBy360Atan(MoveUtil.getATan360(vector.getAtan360(), 120));
        Vector cVector = MoveUtil.getVectorBy360Atan(MoveUtil.getATan360(vector.getAtan360(), 240));

        double ax = x + (vector.getDir_x() * MoveUtil.getV12XD(vr, vector.getAtan()));
        double az = z + (vector.getDir_z() * MoveUtil.getV12ZD(vr, vector.getAtan()));

        double bx = x + (bVector.getDir_x() * MoveUtil.getV12XD(vr, bVector.getAtan()));
        double bz = z + (bVector.getDir_z() * MoveUtil.getV12ZD(vr, bVector.getAtan()));

        double cx = x + (cVector.getDir_x() * MoveUtil.getV12XD(vr, cVector.getAtan()));
        double cz = z + (cVector.getDir_z() * MoveUtil.getV12ZD(vr, cVector.getAtan()));

        add(ax, az);
        add(bx, bz);
        add(cx, cz);
    }

    /**
     * 根据中心点和朝向正前方的矩形
     *
     * @param direction
     * @param offset
     * @param vr_width
     * @param vr_hight
     */
    public void initRectangle(double direction, double offset, double vr_width, double vr_hight) {
        init(4);
        Vector vector = MoveUtil.getVectorBy360Atan(direction);
        double x = getCenter().getX();
        double z = getCenter().getZ();
        // 宽度修正
        vr_width = vr_width / 2;

        Vector aVector = MoveUtil.getVectorBy360Atan(MoveUtil.getATan360(vector.getAtan360(), -90));
        Vector bVector = MoveUtil.getVectorBy360Atan(MoveUtil.getATan360(vector.getAtan360(), 90));

        if (offset != 0) {
            /* 根据三角函数计算出 中心点 偏移量 */
            double v12_V_X = 0;
            double v12_V_Y = 0;
            if (offset < 0) {
                /* 传入负数的时候方向刚好是相反方向运动 */
                v12_V_X = -1 * vector.getDir_x() * MoveUtil.getV12XD(offset, vector.getAtan());
                v12_V_Y = -1 * vector.getDir_z() * MoveUtil.getV12ZD(offset, vector.getAtan());
            } else {
                /* 正前方移动 */
                v12_V_X = vector.getDir_x() * MoveUtil.getV12XD(offset, vector.getAtan());
                v12_V_Y = vector.getDir_z() * MoveUtil.getV12ZD(offset, vector.getAtan());
            }
            x += v12_V_X;
            z += v12_V_Y;
        }

        /* 根据三角函数计算出 A 点偏移量 */
        double v12_A_X = aVector.getDir_x() * MoveUtil.getV12XD(vr_width, aVector.getAtan());
        double v12_A_Y = aVector.getDir_z() * MoveUtil.getV12ZD(vr_width, aVector.getAtan());
        /* 由于在计算12方向位移函数里面已经计算偏移量是正负值 */
        double A_X = x + v12_A_X;
        double A_Y = z + v12_A_Y;

        /* 根据三角函数计算出 B 点偏移量 */
        double v12_B_X = bVector.getDir_x() * MoveUtil.getV12XD(vr_width, bVector.getAtan());
        double v12_B_Y = bVector.getDir_z() * MoveUtil.getV12ZD(vr_width, bVector.getAtan());
        /* 由于在计算12方向位移函数里面已经计算偏移量是正负值 */
        double B_X = x + v12_B_X;
        double B_Y = z + v12_B_Y;

        /* 根据三角函数计算出 C 或者 D 点偏移量 */
        double v12_CD_X = vector.getDir_x() * MoveUtil.getV12XD(vr_hight, vector.getAtan());
        double v12_CD_Y = vector.getDir_z() * MoveUtil.getV12ZD(vr_hight, vector.getAtan());

        /* C 点应该是 B 点的垂直方向也就是原来玩家的移动方向 由于在计算12方向位移函数里面已经计算偏移量是正负值*/
        double C_X = B_X + v12_CD_X;
        double C_Y = B_Y + v12_CD_Y;
        /* D 点应该是 A 点的垂直方向也就是原来玩家的移动方向 由于在计算12方向位移函数里面已经计算偏移量是正负值*/
        double D_X = A_X + v12_CD_X;
        double D_Y = A_Y + v12_CD_Y;

        add(A_X, A_Y);
        add(B_X, B_Y);
        add(C_X, C_Y);
        add(D_X, D_Y);
    }

    /**
     * @param x 坐标点
     * @param z 坐标点
     */
    private void add(double x, double z) {
        add(pointCount, x, z);
        pointCount++;
    }

    /**
     * @param index 当前索引
     * @param x     坐标点
     * @param z     坐标点
     */
    private void add(int index, double x, double z) {
        if (0 <= index && index < pointXs.length) {
            pointXs[index] = ConvertUtil.double4(x);
            pointZs[index] = ConvertUtil.double4(z);
        } else {
            throw new UnsupportedOperationException("index out of");
        }
    }

    @Deprecated
    public boolean isInPolygon(Vector3 targetPosition) {
        return isInPolygon(targetPosition.getX(), targetPosition.getZ());
    }

    /**
     * 判断点是否在多边形内 <br>
     * ----------原理---------- <br>
     * 注意到如果从P作水平向左的射线的话，如果P在多边形内部，那么这条射线与多边形的交点必为奇数，<br>
     * 如果P在多边形外部，则交点个数必为偶数(0也在内)。<br>
     *
     * @param x 要判断的点
     * @param z 要判断的点
     * @return
     */
    @Deprecated
    public boolean isInPolygon(double x, double z) {
        boolean inside = false;
        double p1x = 0, p1z = 0, p2x = 0, p2z = 0;

        for (int i = 0, j = pointCount - 1; i < pointCount; j = i, i++) {
            /*第一个点和最后一个点作为第一条线，之后是第一个点和第二个点作为第二条线，之后是第二个点与第三个点，第三个点与第四个点...*/
            p1x = pointXs[i];
            p1z = pointZs[i];

            p2x = pointXs[j];
            p2z = pointZs[j];

            if (z < p2z) {/*p2在射线之上*/
                if (p1z <= z) {/*p1正好在射线中或者射线下方*/
                    if ((z - p1z) * (p2x - p1x) >= (x - p1x) * (p2z - p1z))/*斜率判断,在P1和P2之间且在P1P2右侧*/ {
                        /*射线与多边形交点为奇数时则在多边形之内，若为偶数个交点时则在多边形之外。
                        由于inside初始值为false，即交点数为零。所以当有第一个交点时，则必为奇数，则在内部，此时为inside=(!inside)
                        所以当有第二个交点时，则必为偶数，则在外部，此时为inside=(!inside)*/
                        inside = (!inside);
                    }
                }
            } else if (z < p1z) {
                /*p2正好在射线中或者在射线下方，p1在射线上*/
                if ((z - p1z) * (p2x - p1x) <= (x - p1x) * (p2z - p1z))/*斜率判断,在P1和P2之间且在P1P2右侧*/ {
                    inside = (!inside);
                }
            }
        }
        return inside;
    }

    /**
     * 验证点在多边形内
     *
     * @param x
     * @param z
     * @return
     */
    @Override
    public boolean contains(double x, double z, float tmpradius) {
        /*我们可以把多边形可以看做是一条从某点出发的闭合路，可以观察到在内部的点永远都在路的同一边。
        给定线段的两个点P0(x0,y0)和P1(x1,y1)，目标点P(x,y),它们有如下的关系：
        计算(y - y0)* (x1 - x0) - (x - x0) * (y1 - y0)
        如果答案小于0则说明P在线段的右边，大于0则在左边，等于0说明在线段上。
         */
        double p1x = 0, p1z = 0, p2x = 0, p2z = 0, ret = 0;
        for (int i = 0; i < pointCount; i++) {
            p1x = pointXs[i];
            p1z = pointZs[i];
            if (i == pointCount - 1) {
                p2x = pointXs[0];
                p2z = pointZs[0];
            } else {
                p2x = pointXs[i + 1];
                p2z = pointZs[i + 1];
            }
            double ss = sq(p1x, p1z, p2x, p2z, x, z);
            if (ss != 0) {
                /*答案小于0则说明P在线段的右边，大于0则在左边，等于0说明在线段上。*/
                if (ret != 0) {
                    /*如果不是0，表示方向反向了*/
                    if ((ss > 0 && ret < 0) || (ss < 0 && ret > 0)) {
                        return false;
                    }
                }
                ret = ss;
            }
        }
        return true;
    }

    double sq(double p1x, double p1z, double p2x, double p2z, double x, double z) {
        return (z - p1z) * (p2x - p1x) - (x - p1x) * (p2z - p1z);
    }

    @Override
    public boolean move() {
        if (getTranslation() != null) {
            if (getTranslationCount() > 0) {
                for (int i = 0; i < pointXs.length; i++) {
                    pointXs[i] = pointXs[i] + getTranslation().getX();
                    pointZs[i] = pointZs[i] + getTranslation().getZ();
                }
                getCenter().x += getTranslation().getX();
                getCenter().z += getTranslation().getZ();
                setTranslationCount(getTranslationCount() - 1);
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        String trString = "center" + getCenter().showString() + "; ";
        if (pointCount > 0) {
            for (int i = 0; i < pointCount; i++) {
                trString += "{" + ConvertUtil.double4(pointXs[i]) + "," + ConvertUtil.double4(pointZs[i]) + "},";
            }
        }
        return trString;
    }

    public static void main(String[] args) {
        Vector3 pos = Vector3.clone(2.5, 0, 4);
        Vector3 tar = Vector3.clone(2.0, 0, 4);

        double dir = MoveUtil.getATan360(pos.getX(), pos.getZ(), tar.getX(), tar.getZ());
        int TIMEPERIOD = 80;
        /*计算移动次数*/
        int moveCount = (1000 / TIMEPERIOD) + (1000 % TIMEPERIOD > 0 ? 1 : 0);
        /*计算每一次移动的距离*/
        float height = 6 * 1f / moveCount;// 每帧飞行的距离

        Vector vectorBy360Atan = MoveUtil.getVectorBy360Atan(dir);
        System.out.println(vectorBy360Atan.toString());
        /*计算朝向位移量*/
        Vector3 translation = Vector3.clone0();
        translation.x += vectorBy360Atan.getDir_x() * MoveUtil.getV12XD(height, vectorBy360Atan.getAtan());
        translation.z += vectorBy360Atan.getDir_z() * MoveUtil.getV12ZD(height, vectorBy360Atan.getAtan());
        CheckPolygon checkPolygon = new CheckPolygon(pos);
        checkPolygon.initRectangle(dir, 0, 3, height);
        checkPolygon.setId(1);
        checkPolygon.setTranslationCount(moveCount);
        checkPolygon.setTranslation(translation);
        do {
            System.out.println(checkPolygon.toString());
            System.out.print(checkPolygon.isInPolygon(tar) + "-----");
            System.out.println(checkPolygon.contains(tar, 5));
        } while (!checkPolygon.move());
    }

}
