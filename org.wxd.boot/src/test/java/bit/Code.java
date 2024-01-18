package bit;

import org.junit.Test;
import org.wxd.boot.lang.bit.BitFlag;

import java.io.Serializable;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-05-11 11:58
 **/
public class Code implements Serializable {

    @Test
    public void bigFlag() {
        final BitFlag bitFlag = new BitFlag();
        /*添加标记*/
        bitFlag.addFlags(1, 2, 3).pint();
        /*测试分组互斥*/
        bitFlag.addFlagRange(1, 10, 1).pint().addFlagRange(1, 10, 2).pint();
        /*测试分组互斥*/
        bitFlag.addFlag(67).pint().addFlagRange(65, 128, 69).pint();
        /*分组范围内，有一个是真 即为真*/
        System.out.println(bitFlag.hasFlagRange(1, 64));
        System.out.println(bitFlag.flagCount());
        System.out.println(bitFlag.flagCount(1, 64));
    }

    @Test
    public void w() {
        int a = 2, b = 3;
        System.out.println(BitFlag.toString(a) + " - " + BitFlag.toString(b));
        a = a ^ b;
        System.out.println(BitFlag.toString(a) + " - " + BitFlag.toString(b));
        b = a ^ b;
        System.out.println(BitFlag.toString(a) + " - " + BitFlag.toString(b));
        a = a ^ b;
        System.out.println(BitFlag.toString(a) + " - " + BitFlag.toString(b));
    }

    @Test
    public void h() {
        int a = 13, b = 16;

        System.out.println(BitFlag.toString(1));
        System.out.println(BitFlag.toString(a));
        System.out.println(BitFlag.toString(b));
        System.out.println(BitFlag.toString(a ^ b));
        System.out.println((a & b & 1));
        System.out.println((a / 2) + (b / 2) + (a & b & 1));
        System.out.println((a & b) + (a ^ b) / 2);
    }

    @Test
    public void w2() {

        int i = 500;
        System.out.println(BitFlag.toString(i));
        for (int j = 0; j < 5; j++) {
            byte b = (byte) (i & 127);
            i >>>= 7;
            if (i > 0) {
                b = (byte) (b | 1 << 7);
            }
            System.out.println(b + " - " + BitFlag.toString(b));
        }

        System.out.println((-12 & (1 << 7)) != 0);
        System.out.println((-12 & ~(1 << 7)));
    }

}
















