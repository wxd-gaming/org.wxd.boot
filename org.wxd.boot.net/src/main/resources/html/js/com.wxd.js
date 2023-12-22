/** 常用辅助 */
const wxd = {
    /**
     * 判空
     * @param test
     * @returns {boolean}
     */
    isNull: function (test) {
        return test === undefined || test === null || test === '';
    },

    notNull: function (test) {
        return !wxd.isNull(test);
    },

    /** 实现暂停功能 */
    sleep: function (numberMillis) {
        let now = new Date();
        let exitTime = now.getTime() + numberMillis;
        while (true) {
            now = new Date();
            if (now.getTime() > exitTime) return;
        }
    },

    /** 把文本换行换成 br */
    replaceLine: function (obj) {
        if (wxd.isNull(obj)) {
            obj = "";
        }
        try {
            obj = obj.replace(/\r\n/ig, '<br>');
        } catch (e) {
            console.error(obj, e);
        }
        try {
            obj = obj.replace(/\n/ig, '<br>');
        } catch (e) {
            console.error(obj, e);
        }
        try {
            obj = obj.replace("\n", '<br>');
        } catch (e) {
            console.error(obj, e);
        }
        try {
            obj = obj.replace("\\n", '<br>');
        } catch (e) {
            console.error(obj, e);
        }
        return obj;
    },

    /**
     * 定时器任务
     * @param action 函数调用
     * @param interval 间隔时间
     * @param execCount 执行次数
     * @returns {Promise<unknown>}
     */
    delayed: function (action, interval, execCount) {
        if (execCount < 1) return;
        return new Promise((resolve, reject) => {
                let curExecCount = 0;
                let intervalTmp = setInterval(() => {
                    try {
                        let action1 = action(curExecCount);
                        if (action1 === false) {
                            resolve('');
                            clearInterval(intervalTmp);
                            return;
                        }
                    } catch (ex) {
                        console.info("delayed exception " + ex);
                        resolve('');
                        clearInterval(intervalTmp);
                        return;
                    }
                    curExecCount++;
                    if (curExecCount >= execCount) {
                        resolve('');
                        clearInterval(intervalTmp);
                    }
                }, interval);
            }
        );
    },

    /** 从 Cookie 获取登录授权 */
    getAuthor: function () {
        return wxd.getCookie("authorization");
    },

    /**
     * 获取 cookie
     * @param {any} name
     */
    getCookie: function (name) {
        let arr, reg = new RegExp("(^| )" + name + "=([^;]*)(;|$)");
        if (arr = document.cookie.match(reg)) {
            return unescape(arr[2]);
        } else {
            return null;
        }
    },

    /** 清除 Cookie */
    clearCookie: function () {
        let keys = document.cookie.match(/[^ =;]+(?=\=)/g);
        if (keys) {
            for (let i = keys.length; i--;) {
                document.cookie = keys[i] + '=0;path=/;expires=' + new Date(0).toUTCString();//清除当前域名下的,例如：m.kevis.com
                document.cookie = keys[i] + '=0;path=/;domain=' + document.domain + ';expires=' + new Date(0).toUTCString();//清除当前域名下的，例如 .m.kevis.com
            }
        }
    },

    /** 把内容复制到剪切板 */
    copy: function (content) {
        // 创建元素用于复制
        const aux = document.createElement('input');
        // 设置元素内容
        aux.setAttribute('value', content);
        // 将元素插入页面进行调用
        document.body.appendChild(aux);
        // 复制内容
        aux.select();
        // 将内容复制到剪贴板
        document.execCommand('copy');
        // 删除创建元素
        document.body.removeChild(aux);
    },

    /**
     * 增加拖拽事件
     * @param mbox_title 需要被拖拽的控件的子元素
     */
    draggable: function (mbox_title) {
        $(mbox_title).mousedown(function (e) {
            //鼠标按下
            var old_mouse = e || window.event;
            //原鼠标坐标
            var old_m_x = old_mouse.clientX;
            var old_m_y = old_mouse.clientY;
            //原div坐标
            var parent_x = $(this).parent().offset().left;
            var parent_y = $(this).parent().offset().top;

            $(document.body).mousemove(function (e) {
                $(mbox_title).parent().css("transform", "translate(0,0)");
                //鼠标拖动时，新鼠标坐标
                let new_mouse = e || window.event;
                let new_m_x = new_mouse.clientX;
                let new_m_y = new_mouse.clientY;
                //新div坐标
                let x = parent_x + (new_m_x - old_m_x);
                let y = parent_y + (new_m_y - old_m_y);
                //改变面板位置
                $(mbox_title).parent().css("left", x + "px");
                $(mbox_title).parent().css("top", y + "px");
            }).mouseup(function () {
                //鼠标提起后，解绑鼠标移动事件
                $(document.body).unbind("mousemove");
            });
        }).mouseup(function () {
            //鼠标提起后，解绑鼠标移动事件
            $(document.body).unbind("mousemove");
        });
    },

    /**
     * 获取复选框的值
     * @param ui 复选框的name字段
     * @returns {*[]}
     */
    checkVal: function (ui) {
        let arr = [];
        let i = 0;
        $("[name='" + ui + "']").each(function () {//反选
            if ($(this).prop("checked")) {
                arr[i] = $(this).val();
                i++;
            }
        });
        return arr;
    },

    /**
     * 选择全部复选框
     * @param ui 复选框的name字段
     */
    checkAllChecked: function (ui) {
        $("[name='" + ui + "']").each(function () {
            $(this).prop("checked", true);
        });
    },

    /**
     * 全部取消选中
     * @param ui 复选框的name字段
     */
    checkNotChecked: function (ui) {
        $("[name='" + ui + "']").each(function () {
            $(this).prop("checked", false);
        });
    },

    /**
     * 反选
     * @param ui 复选框的name字段
     */
    checkNeChecked: function (ui) {
        $("[name='" + ui + "']").each(function () {
            $(this).prop("checked", !$(this).prop("checked"));
        });
    },

    /** 键值对存储 */
    Map: class {
        nodes = {};

        constructor() {
        }

        /** 当前 top.location.search */
        loadTopSearch() {
            let query = top.location.search.substring(1);
            this.actionData(query);
            return this;
        }

        /** 当前location.search */
        loadSearch() {
            let query = location.search.substring(1);
            this.actionData(query);
            return this;
        }

        /** 解析http键值对格式 */
        actionData(query) {
            if (query.length > 2) {
                let vars = query.split("&");
                for (let i = 0; i < vars.length; i++) {
                    let pv = vars[i];
                    let index = pv.indexOf("=");
                    if (index >= 0) {
                        let p = pv.substring(0, index);
                        let v = pv.substring(index + 1);
                        this.nodes[p] = v;
                    }
                }
            }
            return this;
        }

        /** 从 json 字符串 格式化  */
        parse(jsonStr) {
            this.nodes = JSON.parse(jsonStr);
            return this;
        }

        get(p) {
            return this.nodes[p];
        }

        put(p, v) {
            this.nodes[p] = v;
            return this;
        }

        /** 转化成http网络格式的字符串 */
        toString() {
            let pv = "";
            for (let param in this.nodes) {
                if (pv.length > 1) pv += '&';
                let v = this.nodes[param];
                if (wxd.isNull(v)) v = '';
                pv += param + '=' + v;
            }
            return pv;
        }
    },

    /**
     * 显示 loading 层
     * @param img 默认是 /loading-4.gif
     * @param w 默认是 64px
     * @param h 默认是 64px
     */
    loading: function (img, w, h, parent) {

        if (wxd.isNull(img)) img = "/loading-2.gif";
        if (wxd.isNull(w)) w = "54px";
        if (wxd.isNull(h)) h = "54px";

        let load_div = `
<div class="full_loading_bg">
    <img style="width: ${w};height: ${h};" src="${img}">
</div>
`;
        if (wxd.isNull(parent)) parent = document.body;
        $(parent).append(load_div);

    },

    /** 关闭 loading 层 */
    loading_close: function () {
        /*先隐藏面板*/
        setTimeout(() => {
            $(".full_loading_bg").remove();
        }, 200);

    },

    /** 网络辅助 */
    netty: {

        formatByteSize: function (bytes) {
            let k = (bytes / 1024);
            let m = (k / 1024);
            let g = (m / 1024);

            let b = "";
            if (g > 1) {
                b = g.toFixed(2) + " Gb ";
            } else if (m > 1) {
                b = m.toFixed(2) + " Mb ";
            } else if (k > 1) {
                b = k.toFixed(2) + " Kb";
            } else {
                b = bytes + " b";
            }
            return b;
        },

        WSClient: class {
            /** 异常 */
            onError = null;
            /** 链接成功 */
            onSource = null;
            /** 收到消息后回调 */
            onRead = null;
            //socket对象
            socket = null;
            socketOpen = false;

            close() {
                if (this.socket != null) {
                    this.socket.close();
                    this.socket = null;
                }
            }

            connect(url) {
                console.log("开始尝试向服务器：" + url + ", 发出链接请求！！！");
                if (!window.WebSocket) {
                    /*如果不支持 socket 那么重置*/
                    window.WebSocket = window.MozWebSocket;
                }

                if (window.WebSocket) {
                    this.socket = new window.WebSocket(url);

                    this.socket.onmessage = (event) => {
                        console.log("recv：" + event.data);
                        if (!wxd.isNull(this.onRead)) this.onRead(event.data);
                    };

                    this.socket.onerror = (error) => {
                        console.error("出现异常：" + error.data);
                        if (!wxd.isNull(this.onError)) this.onError(event.data);
                    };

                    this.socket.onopen = (event) => {
                        this.socketOpen = true;
                        console.log("链接成功");
                        if (!wxd.isNull(this.onSource)) this.onSource(event.data);
                    };

                    this.socket.onclose = (event) => {
                        if (this.socketOpen) {
                            wxd.message.notice("web socket 链接关闭", true, 3000);
                        } else {
                            wxd.message.notice("web socket 链接失败", true, 3000);
                        }
                        this.socketOpen = false;
                        this.socket = null;
                    };
                } else {
                    wxd.message.notice("您的浏览器不支持 web socket 协议！");
                }
            }

            sendMsg(msg) {
                if (this.socketOpen) {
                    if (this.socket.readyState === WebSocket.OPEN) {
                        this.socket.send(msg);
                    } else {
                        wxd.message.notice("链接断开", true);
                    }
                } else {
                    wxd.message.notice("尚未初始化", true);
                }
            }

        },
        /** 下载文件 */
        download: function (url) {
            let formElement = document.createElement("form");
            formElement.setAttribute('style', 'display:none'); //在form表单中添加查询参数
            formElement.setAttribute('target', '_blank');
            formElement.setAttribute('method', 'post');
            formElement.setAttribute('action', url);
            document.body.appendChild(formElement);
            formElement.submit();
            document.body.removeChild(formElement);
        },

        upload_file_url: null,
        upload_file_formData: null,
        upload_file_onSource: null,
        /** 上传文件 */
        upload: function (url, formData, onSource) {

            wxd.netty.upload_file_url = url;
            wxd.netty.upload_file_formData = formData;
            wxd.netty.upload_file_onSource = onSource;

            let upload_file_bg = document.createElement("div");
            upload_file_bg.setAttribute("class", "upload_file_bg");

            /*根据当前页面设置最大高度*/
            let bodyHeight = ($("body").height() * 0.4).toFixed(0);
            console.log("alert-max-height " + bodyHeight);

            let box = `
<div class="upload_file_box">
    <strong>文件上传</strong>
    <span class="span_close" title="关闭" onclick="$('.upload_file_bg').remove()"></span>
    <div class="upload_file_c">
        <div class="upload_file_c_1" style="max-height: ${bodyHeight}px;">
            <br>
            <input id="wxd_upload_file" type="file" style="width: 340px;" value="上传配置" multiple/>
            <br>
            <br>
            <div id="wxd_upload_progress_box">       
                
            </div>
        </div>
        <div class="upload_file_c_b">
            <button onclick="wxd.netty.upFile()">上传</button>
            <button onclick="$('.upload_file_bg').remove()">关闭</button>
        </div>
    </div>
</div>
            `;

            upload_file_bg.innerHTML = box;

            $(document.body).append(upload_file_bg);
        },

        /**
         * 上传异步上传文件，带进度条展示
         * @param url 上传地址
         * @param formData 附近参数
         * @param onSource 成功回调
         */
        upFile: function () {

            let files = $('#wxd_upload_file').prop('files');
            let fileCount = files.length;//文件数量
            if (fileCount < 1) {
                wxd.message.notice("未选择需要上传的文件");
                return;
            }

            if (wxd.isNull(wxd.netty.upload_file_formData)) {
                wxd.netty.upload_file_formData = new FormData();
            }

            $("#wxd_upload_progress_box").html("");

            for (let i = 0; i < fileCount; i++) {
                let tmpFormData = new FormData();

                for (let [a, b] of wxd.netty.upload_file_formData.entries()) {
                    tmpFormData.set(a, b);
                }

                let file = files[i];// js 获取文件对象

                $("#wxd_upload_progress_name").text(file.name);

                let fileName = file.name; // get file name
                let fileSize = file.size; // get file size
                let fileType = file.type; // get file type

                tmpFormData.set(file.name + "_lastModified", file.lastModified);
                tmpFormData.set("file", file); // 文件对象

                let box = `
<span id="wxd_upload_progress_name_${i}">文件：${fileName}</span>
<br>
<progress id="wxd_upload_progress_bar_${i}" value="0" max="100" style="width: 430px;"></progress>&nbsp;&nbsp;<span id="wxd_p_b_v_${i}">0</span>%
<br>
&nbsp;&nbsp;&nbsp;&nbsp;大小：<span id="wxd_p_b_cur_${i}"></span> / <span id="wxd_p_b_max_${i}"></span>,&nbsp;&nbsp;&nbsp;&nbsp;速度：<span id="wxd_p_b_s_${i}"></span>
<br>
<br>
`;
                /*追加文件进度 容器*/
                $("#wxd_upload_progress_box").append(box);
                /*设置文件总大小*/
                $("#wxd_p_b_max_" + i).text(wxd.netty.formatByteSize(fileSize));
                /*设置当前文件上传进度条总大小*/
                $("#wxd_upload_progress_bar_" + i).attr("max", fileSize);

                $.ajax({
                    url: wxd.netty.upload_file_url, /*接口域名地址*/
                    type: 'post',
                    data: tmpFormData,
                    async: true,/*必须是异步才能成功执行上传进度*/
                    contentType: false,
                    processData: false,
                    xhr: () => {
                        let xhr = $.ajaxSettings.xhr();
                        // 获取文件上传的进度
                        if (xhr.upload) {
                            let lastUploadSize = 0;
                            let lastUploadDate = new Date();
                            xhr.upload.addEventListener("progress", function (e) {

                                let date = new Date();
                                let seconds_elapsed = (date.getTime() - lastUploadDate.getTime()) / 1000;
                                let uploadSize = e.loaded - lastUploadSize;

                                lastUploadDate = date;
                                lastUploadSize = e.loaded;
                                /*设置当前文件上传进度条*/
                                $("#wxd_upload_progress_bar_" + i).val(e.loaded);
                                /*设置当前文件上传百分比*/
                                $("#wxd_p_b_v_" + i).text((e.loaded / e.total * 100).toFixed(2));
                                /*当前上传的总字节数*/
                                $("#wxd_p_b_cur_" + i).text(wxd.netty.formatByteSize(e.loaded));
                                /*每秒钟上传字节数*/
                                $("#wxd_p_b_s_" + i).text(wxd.netty.formatByteSize(uploadSize / seconds_elapsed));
                            });
                            // 文件加载完成
                            xhr.upload.addEventListener("onload", function () {
                                /*增加一些处理逻辑*/
                            });
                        }
                        return xhr
                    },
                    success: (res) => {
                        $("#wxd_p_b_s_" + i).append(" - 上传完成 ");
                        wxd.message.notice("成功上传：" + file.name);
                        if (wxd.isNull(wxd.netty.upload_file_onSource)) {
                            console.warn(res);
                        } else {
                            wxd.netty.upload_file_onSource(res);
                        }
                    }, error: (jqXHR, textStatus, errorMsg) => {
                        // jqXHR 是经过jQuery封装的XMLHttpRequest对象
                        // textStatus 可能为null、 'timeout'、 'error'、 'abort'和'parsererror'等
                        // errorMsg 是错误信息字符串(响应状态的文本描述部分，例如'Not Found'或'Internal Server Error')
                        console.error(textStatus + " - " + errorMsg);
                        wxd.message.notice(textStatus + " - " + errorMsg, true);
                    }
                });
            }
        },

        /**
         * post 请求
         * @param url 地址
         * @param params 参数
         * @param onLoad 接收消息回调
         * @param onError 异常回调
         * @param sync true 表示同步请求
         * @param time_out 超时时间
         */
        post: function (url, params, onLoad, onError, sync, time_out) {
            this.post0(url, "application/x-www-form-urlencoded; charset=UTF-8", params, onLoad, onError, sync, time_out);
        },

        ajax_timeout: 3000,//超时时间设置，单位毫秒

        /**
         * post 请求
         * @param url 地址
         * @param contentType 数据格式类型
         * @param params 参数
         * @param onLoad 接收消息回调
         * @param onError 异常回调
         * @param sync true 表示同步请求
         * @param time_out 超时时间
         */
        post0: function (url, contentType, params, onLoad, onError, sync, time_out) {
            if (wxd.isNull(sync)) {
                sync = false;
            }
            if (wxd.isNull(time_out)) {
                time_out = this.ajax_timeout;
            }
            $.ajax({
                type: "post",
                url: url,
                timeout: time_out, //超时时间设置，单位毫秒
                contentType: contentType,
                data: params,
                async: sync,
                success: function (data) {
                    if (!wxd.isNull(onLoad)) {
                        try {
                            onLoad(data);
                        } catch (e) {
                            console.error("error: " + e + "\n" + JSON.stringify(data));
                            wxd.message.notice("error: " + e, true);
                        }
                    } else {
                        console.log(data);
                    }
                }, error: function (jqXHR, textStatus, errorMsg) {
                    // jqXHR 是经过jQuery封装的XMLHttpRequest对象
                    // textStatus 可能为null、 'timeout'、 'error'、 'abort'和'parsererror'等
                    // errorMsg 是错误信息字符串(响应状态的文本描述部分，例如'Not Found'或'Internal Server Error')
                    if (!wxd.isNull(onError)) {
                        onError(errorMsg);
                    } else {
                        console.error("error: " + errorMsg);
                        wxd.message.notice("error: " + errorMsg, true);
                    }
                }
            });
        },

    },

    /** 页面消息提示*/
    message: {

        /** 类似于控制台，输出格式自己控制，包括换行 */
        console: function (text) {
            let box_content = document.createElement("div");
            box_content.setAttribute("id", "message_console_bg");
            let box_c = `
    <div class="message_console">
        <div style="width: 100%;height: 100%; overflow: auto;">
            <pre id="message_console_c">${text}</pre>
        </div>
    </div>
    <strong onclick="$('#message_console_bg').remove();">点击这里关闭</strong>
            `;
            box_content.innerHTML = box_c;
            $(document.body).append(box_content);
        },

        /** 类似于控制台，输出格式自己控制，包括换行 */
        console_append: function (text) {
            $("#message_console_c").append(text);
        },

        /** 类似于控制台，输出格式自己控制，自动换行 */
        box: function (text) {
            let box_content = document.createElement("div");
            box_content.setAttribute("class", "message_box_bg");
            let box_c = `
    <div class="message_box">
        <div id="message_box_c" style="width: 100%;height: 100%; overflow: auto;word-wrap: break-word;">
${text}
        </div>
    </div>
    <strong onclick="$('.message_box_bg').remove();">点击这里关闭</strong>
            `;
            box_content.innerHTML = box_c;
            $(document.body).append(box_content);
        },

        /** 类似于控制台，输出格式自己控制，包括换行 */
        box_append: function (text) {
            $(".message_box_c").append(text);
        },

        notice_init_end: false,

        notice_init: function () {
            if (this.notice_init_end) return;
            /*输出背景框*/
            $(document.body).append(`<div class="message_notice_bg"></div>`);
            this.notice_init_end = true;
        },

        /** 页面上方的提示框，*/
        notice: function (content, isError, closeTime) {
            this.notice_init();
            let box_content = document.createElement("div");
            box_content.setAttribute("class", "message_notice_box");
            if (isError === true) {
                box_content.setAttribute("style", "color: brown;");
            }
            let n_c = `<div class="message_notice_c">${content}</div>`;
            box_content.innerHTML = n_c;
            /*追加在最上面*/
            $(".message_notice_bg").prepend(box_content);

            if (wxd.isNull(closeTime)) closeTime = 0;

            setTimeout(() => {
                $(box_content).fadeOut(500 + closeTime);
            }, 1000 + closeTime);

            setTimeout(() => {
                $(box_content).remove();
            }, 5000 + closeTime);
        },

        alert_init_end: false,
        alert_ok_call: null,
        alert_cancel_call: null,
        alert_init: function () {
            if (this.alert_init_end) return;
            let box = `<div class="message_alert_bg"></div>`;
            $(document.body).append(box);
            this.alert_init_end = true;
        },
        /**
         *
         * 弹出提示框
         * @param content 内容，支持html
         * @param title 标题
         * @param ok 确认按钮显示内容
         * @param okCall 确认按钮回调
         * @param cancel 取消显示的内容
         * @param cancelCall 取消按钮回调
         */
        alert: function (content, title, ok, okCall, cancel, cancelCall) {
            this.alert_init();
            $('.message_alert_bg').show();
            if (wxd.isNull(title)) title = "提示：";
            if (wxd.isNull(ok)) ok = "OK";
            let btn_c = "";
            if (!wxd.isNull(cancel) || !wxd.isNull(cancelCall)) {
                if (wxd.isNull(cancel)) cancel = "Cancel";
                btn_c = `<button onclick="wxd.message.alert_cancel()">${cancel}</button>`;
            }
            /*根据当前页面设置最大高度*/
            let bodyHeight = ($("body").height() * 0.4).toFixed(0);
            console.log("alert-max-height " + bodyHeight);
            let a_c = `
<div class="message_alert_box">
    <strong class="ban_select">${title}</strong>
    <span class="span_close ban_select" title="关闭" onclick="wxd.message.alert_cancel()"></span>
    <div class="message_alert_c">
        <div class="message_alert_c_1" style="max-height: ${bodyHeight}px;">
            ${content}
        </div>
        <div class="message_alert_c_b">
            <button onclick="wxd.message.alert_ok()">${ok}</button>
            ${btn_c}
        </div>
    </div>
</div>
`;
            this.alert_ok_call = okCall;
            this.alert_cancel_call = cancelCall;
            $(".message_alert_bg").html(a_c);
            var $messageAlertBox = $(".message_alert_box strong:first");
            console.log($messageAlertBox);
            wxd.draggable($messageAlertBox);
        },

        alert_ok: function () {
            $('.message_alert_bg').hide();
            setTimeout(() => {
                if (!wxd.isNull(this.alert_ok_call)) this.alert_ok_call();
            }, 1);
        },

        alert_cancel: function () {
            $('.message_alert_bg').hide();
            setTimeout(() => {
                if (!wxd.isNull(this.alert_cancel_call)) this.alert_cancel_call();
            }, 1);
        },

        tips_x: 10,
        tips_y: 15,
        tips_tmp_title: "",
        tips_init_end: false,
        tips_init: function () {
            if (this.tips_init_end) return;

            $(document.body).append(`<span class="message_title_tips"></span>`);

            this.tips_init_end = true;
        },

        tips_reset_css: function (e, element) {
            // console.log($(element).css("font-size"));
            // console.log(parseFloat($(element).css("font-size")));
            var outerWidth = $('.message_title_tips').outerWidth();
            let max_width = $('body').width();
            let left = e.pageX + this.tips_x;
            // console.log("页面宽度：" + max_width + ", 当前宽度：" + e.pageX + ", 需要宽度：" + outerWidth);
            if (max_width - e.pageX < outerWidth + this.tips_x) {
                /*说明 鼠标在页面右侧*/
                left = max_width - this.tips_x - outerWidth;
            }

            var outerHeight = $('.message_title_tips').outerHeight();
            let max_height = $('body').height();
            let top = e.pageY + this.tips_y;
            // console.log("页面高度：" + max_height + ", 当前高度：" + e.pageY + ", 需要高度：" + outerHeight);
            if (max_height - e.pageY < outerHeight + this.tips_y) {
                /*说明 鼠标在页面底部*/
                top = e.pageY - this.tips_y - outerHeight;
            }

            let bodyHeight = $("body").height() * 0.40;

            $('.message_title_tips').css({
                "opacity": "0.9",
                "left": (left) + "px",
                "top": (top) + "px",
                "max-height": bodyHeight + "px",
                "fontSize": (parseFloat($(element).css("fontSize")) + 8) + "px"
            });
        },
        tips_init_child: function (element) {

            if (($(element).attr("tips") === "true" && !wxd.isNull($(element).html())) || !wxd.isNull($(element).attr("title"))) {
                $(element).unbind("mouseover mouseout mousemove"); //移除之前绑定的事件防止冲突
                $(element).mouseover(function (e) {
                    if (!wxd.isNull($(this).attr("title"))) {
                        wxd.message.tips_tmp_title = $(this).attr("title");
                        try {
                            wxd.message.tips_tmp_title = wxd.replaceLine(wxd.message.tips_tmp_title);
                        } catch (e) {
                            console.error(e);
                        }
                        $(this).attr("title", "");
                        $('.message_title_tips').html(wxd.message.tips_tmp_title);
                    } else {
                        $('.message_title_tips').html($(this).html());
                    }
                    $('.message_title_tips').show();
                    wxd.message.tips_reset_css(e, this);
                    event.stopPropagation();
                }).mouseout(function () {
                    $('.message_title_tips').hide();
                    if (!wxd.isNull(wxd.message.tips_tmp_title)) {
                        $(this).attr("title", wxd.message.tips_tmp_title);
                        wxd.message.tips_tmp_title = "";
                    }
                }).mousemove(function (e) {
                    wxd.message.tips_reset_css(e, this);
                    event.stopPropagation();
                });
            }

            if ($(element).children().length > 0) {
                $(element).children().each(function (index, selement) {
                    wxd.message.tips_init_child(selement);
                });
            }
        },

        tips_init_bind: function () {
            this.tips_init();
            /* each()方法处理HTML元素 */
            $("body").children().each(function (index, element) {
                wxd.message.tips_init_child(element);
            });
        },

    }
}

$(() => {
    /** 页面加载完成初始化 tips 弹窗提示 */
    wxd.message.tips_init_bind();
});