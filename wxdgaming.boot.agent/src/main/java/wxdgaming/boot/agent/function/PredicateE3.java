package wxdgaming.boot.agent.function;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2022-04-22 15:00
 **/
@FunctionalInterface
public interface PredicateE3<T1, T2, T3> extends SerializableLambda {

    boolean test(T1 t1, T2 t2, T3 t3) throws Throwable;

}
