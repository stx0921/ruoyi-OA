import Cookies from 'js-cookie'

// Cookie 名带上项目前缀：浏览器的 Cookie 不区分端口，
// 本机同时跑其它若依系项目时，同名的 Admin-Token 会被互相覆盖/串用，
// 导致本系统拿到别的系统的 token，后端反序列化 LoginUser 时报类型转换异常。
const TokenKey = 'RuoYiOA-Token'

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}
