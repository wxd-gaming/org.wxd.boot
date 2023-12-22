/**
 * url 参数解析
 */
class QueryMap {

    ps = {};

    constructor() {
    }

    /**
     * 从 url 参数加载
     */
    loadUrl() {
        let query = top.location.search.substring(1);
        this.actionUrl(query);
        return this;
    }

    actionUrl(query) {
        if (query.length > 2) {
            let vars = query.split("&");
            for (let i = 0; i < vars.length; i++) {
                let pv = vars[i];
                let index = pv.indexOf("=");
                if (index >= 0) {
                    let p = pv.substring(0, index);
                    let v = pv.substring(index + 1);
                    this.ps[p] = v;
                }
            }
        }
        return this;
    }

    /**
     * 从 json 字符串 格式化
     * @param jsonStr
     */
    parse(jsonStr) {
        this.ps = JSON.parse(jsonStr);
    }

    get(p) {
        return this.ps[p];
    }

    put(p, v) {
        this.ps[p] = v;
    }

    toString() {
        let pv = "";
        for (let param in this.ps) {
            if (pv.length > 1) pv += '&';
            let v = this.ps[param];
            if (isNull(v)) v = '';
            pv += param + '=' + v;
        }
        return pv;
    }

}