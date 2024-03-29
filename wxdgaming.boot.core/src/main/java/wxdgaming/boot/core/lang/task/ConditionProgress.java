package wxdgaming.boot.core.lang.task;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wxdgaming.boot.core.lang.ObjectBase;

import java.io.Serializable;

/**
 * 完成条件
 *
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-10-10 15:36
 **/
@Getter
@Setter
@Accessors(chain = true)
public class ConditionProgress extends ObjectBase implements Serializable {

    private int cfgId;
    /** 当前进度 */
    private long progress;

    public boolean change(UpdateKey k1, UpdateKey k2, UpdateKey k3, long progress) {

        Condition condition = condition();

        if (condition.getK1().equals(k1)) return false;
        if (condition.getK2().getCode() != 0 && !condition.getK2().equals(k2)) return false;
        if (condition.getK3().getCode() != 0 && !condition.getK3().equals(k3)) return false;

        if (condition.getTarget() > 0 && progress >= condition.getTarget()) return false;

        switch (condition.getUpdateType()) {
            case Add: {
                this.progress = Math.addExact(this.progress, progress);
            }
            break;
            case Replace: {
                this.progress = progress;
            }
            break;
            case Min: {
                this.progress = Math.min(this.progress, progress);
            }
            break;
            case Max: {
                this.progress = Math.max(this.progress, progress);
            }
            break;
        }

        return true;
    }

    protected Condition condition() {
        /*自己考虑通过 cfgId 获取*/
        return null;
    }

    @JSONField(serialize = false, deserialize = false)
    public boolean isFinish() {
        return condition().getTarget() >= this.progress;
    }
}
