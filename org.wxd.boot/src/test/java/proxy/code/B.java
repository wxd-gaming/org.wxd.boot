package proxy.code;//package com.cqd.proxy.test;
//
//
//import com.cqd.proxy.ProxyHandler;
//
//import java.io.Serializable;
//import java.lang.reflect.Method;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @author: Troy.Chen(無心道, 15388152619)
// * @version: 2021-08-19 09:23
// **/
//public class A_proxy_ty05648 extends com.cqd.proxy.test.A implements Serializable, com.cqd.proxy.IProxy {
//
//    private static final long serialVersionUID = 1L;
//
//    private Object superObject;
//    private ProxyHandler proxyHandler;
//    private Map<String, Method> methodMap = new HashMap<>();
//
//
//    public Object getSuperObject() {
//        return superObject;
//    }
//
//    public void setSuperObject(Object superObject) {
//        this.superObject = superObject;
//    }
//
//    public ProxyHandler getProxyHandler() {
//        return proxyHandler;
//    }
//
//    public void setProxyHandler(ProxyHandler proxyHandler) {
//        this.proxyHandler = proxyHandler;
//    }
//    public Map<String, Method> getMethodMap() {
//        return methodMap;
//    }
//
//
//    public java.lang.String test2(java.lang.String v0,java.lang.String v1) {
//        Method method = methodMap.get("test2");
//        proxyHandler.invoke(method, superObject, v0, v1);
//    }
//
//    public void test1(java.lang.String v0) {
//        Method method = methodMap.get("test1");
//        proxyHandler.invoke(method, superObject, v0);
//    }
//
//    public void t0(java.lang.String v0) {
//        Method method = methodMap.get("t0");
//        proxyHandler.invoke(method, superObject, v0);
//    }
//
//}