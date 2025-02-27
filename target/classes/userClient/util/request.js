const picBaseUrl = 'http://localhost:8090/api/file/getPic?name=';
const instance = axios.create({
    baseURL: 'http://localhost:8090/api', // 后端 API 基础路径
    //timeout: 10000, // 请求超时时间
});

// 请求拦截器：自动添加 token 到请求头
instance.interceptors.request.use(
    (config) => {
        // 从 sessionStorage 中获取 token
        const token = sessionStorage.getItem('token');
        if (token) {
            // 检查 token 是否已经包含 Bearer 前缀
            const finalToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
            config.headers.Authorization = finalToken;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// 响应拦截器：处理 401 等错误
instance.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        if (error.response && error.response.status === 401) {
            console.log('用户未登录或会话已过期');
            // 清除无效的 token
            sessionStorage.removeItem('token');
            
            // 对于需要登录的页面，重定向到登录页
            if (window.location.pathname.includes('setting.html') || 
                window.location.pathname.includes('myReserveDetail.html') ||
                window.location.pathname.includes('ChatTest.html')) {
                alert('请先登录！');
                window.location.href = 'login.html';
                return;
            }
        }
        return Promise.reject(error);
    }
);

// GET 请求封装
function get(url, params) {
    return new Promise((resolve, reject) => {
        instance.get(url, { params: params })
            .then((res) => {
                resolve(res.data);
            })
            .catch((err) => {
                reject(err);
            });
    });
}

// POST 请求封装
function post(url, data) {
    return new Promise((resolve, reject) => {
        instance.post(url, data, {
            headers: {
                'Content-Type': 'application/json',
            },
        })
            .then((res) => {
                resolve(res.data);
            })
            .catch((err) => {
                reject(err);
            });
    });
}

// 获取 URL 参数
function getQueryString(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return decodeURI(r[2]);
    return null;
}

// 设置 sessionStorage
function setSessionItem(key, value) {
    sessionStorage.setItem(key, JSON.stringify(value)); // 修复拼写错误
}

// 获取 sessionStorage
function getSessionItem(key) {
    const str = sessionStorage.getItem(key); // 修复拼写错误
    if (str) {
        return JSON.parse(str);
    }
    return null;
}

