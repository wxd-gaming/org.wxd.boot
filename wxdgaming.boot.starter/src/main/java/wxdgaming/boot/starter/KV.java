package wxdgaming.boot.starter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.simpleframework.xml.Attribute;
import wxdgaming.boot.core.lang.ObjectBase;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class KV extends ObjectBase {

    @Attribute
    private String key;
    @Attribute
    private String value;

}
