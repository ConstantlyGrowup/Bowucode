const picBaseUrl = 'http://localhost:8090/api/file/getPic?name=';

const instance = axios.create({
    baseURL: 'http://localhost:8090/api',
    timeout: 10000
});

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
function post(url, data) {
    return new Promise((resolve, reject) => {
        instance.post(url, data,{
            headers:{
                'Content-Type':'application/json'
            }
        })
            .then((res) => {
                resolve(res.data);
            })
            .catch((err) => {
                reject(err);
            });
    });
}
function getQueryString(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return decodeURI(r[2]);
    return null;
}
function setSessionItem(key, value) {
    sessionStorage.setltem(key, JSON.stringify(value))
}
function getSessionItem(key) {
    const str = sessionStorage.getltem(key)
    if(str) {
        return JSON.parse(str)
    }
    return null
}
