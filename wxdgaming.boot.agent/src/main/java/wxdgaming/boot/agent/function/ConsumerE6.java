package wxdgaming.boot.agent.function;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-03-18 15:48
 **/
@FunctionalInterface
public interface ConsumerE6<T1, T2, T3, T4, T5, T6> extends SerializableLambda {

    void accept(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) throws Throwable;

}
