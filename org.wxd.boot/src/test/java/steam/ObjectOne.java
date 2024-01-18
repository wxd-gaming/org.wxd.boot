package steam;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-05-16 17:19
 **/
@Getter
@Setter
@Accessors(chain = true)
public class ObjectOne implements Serializable {
    private int i1 = 0;
    private int i2 = 0;
}
