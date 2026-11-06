import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./../../views/**/*.vue')

const usePermissionStore = defineStore(
  'permission',
  {
    state: () => ({
      routes: [],
      addRoutes: [],
      defaultRoutes: [],
      topbarRouters: [],
      sidebarRouters: []
    }),
    actions: {
      setRoutes(routes) {
        this.addRoutes = routes
        this.routes = constantRoutes.concat(routes)
      },
      setDefaultRoutes(routes) {
        this.defaultRoutes = constantRoutes.concat(routes)
      },
      setTopbarRoutes(routes) {
        this.topbarRouters = routes
      },
      setSidebarRouters(routes) {
        this.sidebarRouters = routes
      },
      generateRoutes(roles) {
        return new Promise(resolve => {
          // 向后端请求路由数据
          getRouters().then(res => {
            const sdata = JSON.parse(JSON.stringify(res.data))
            const rdata = JSON.parse(JSON.stringify(res.data))
            const defaultData = JSON.parse(JSON.stringify(res.data))
            const sidebarRoutes = filterAsyncRouter(sdata)
            const rewriteRoutes = filterAsyncRouter(rdata, false, true)
            const defaultRoutes = filterAsyncRouter(defaultData)
            const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
            asyncRoutes.forEach(route => { router.addRoute(route) })
            // 兜底：不同目录下相同 path 会生成同名路由（如 /task/list 与 /feedback/list 都叫 List），
            // vue-router 4 会移除先注册的那条导致页面 404；这里对重复名称加父级前缀
            dedupeRouteNames(rewriteRoutes)
            this.setRoutes(rewriteRoutes)
            this.setSidebarRouters(constantRoutes.concat(sidebarRoutes))
            this.setDefaultRoutes(sidebarRoutes)
            this.setTopbarRoutes(defaultRoutes)
            resolve(rewriteRoutes)
          })
        })
      }
    }
  })

/**
 * 路由名称去重兜底
 *
 * 后端 route_name 为空时会用 path 首字母大写当路由名，
 * 不同目录下相同的 path（如 /task/list、/feedback/list 都叫 List）会生成同名路由，
 * vue-router 4 遇到同名会移除先注册的记录 → 先注册的页面打开 404。
 * 这里对重复名称加父级 path 前缀重命名（只影响路由匹配，不影响菜单/权限/标题）。
 * 根治方式仍是：菜单管理里给这类菜单配置唯一的「路由名称」。
 */
function dedupeRouteNames(routes) {
  const seen = new Set()
  const capitalize = value => (value ? value.charAt(0).toUpperCase() + value.slice(1) : '')
  const walk = (list, parentPath) => {
    ;(list || []).forEach(route => {
      if (route.name) {
        if (seen.has(route.name)) {
          const prefix = capitalize((parentPath || '').replace(/[^a-zA-Z0-9]/g, ''))
          let newName = (prefix || 'Route') + route.name
          let index = 2
          while (seen.has(newName)) {
            newName = (prefix || 'Route') + route.name + index++
          }
          if (import.meta.env.DEV) {
            console.warn(`[路由] 路由名称 "${route.name}" 重复，已重命名为 "${newName}"；建议在菜单管理中配置唯一的「路由名称」`)
          }
          route.name = newName
        }
        seen.add(route.name)
      }
      if (route.children && route.children.length) {
        walk(route.children, route.path || parentPath)
      }
    })
  }
  walk(routes, '')
  return routes
}

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {  return asyncRouterMap.filter(route => {
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      // Layout ParentView 组件特殊处理
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  var children = []
  childrenMap.forEach(el => {
    el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
    if (el.children && el.children.length && el.component === 'ParentView') {
      children = children.concat(filterChildren(el.children, el))
    } else {
      children.push(el)
    }
  })
  return children
}

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes) {
  const res = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

export const loadView = (view) => {
  let res
  for (const path in modules) {
    const dir = path.split('views/')[1].split('.vue')[0]
    if (dir === view) {
      res = () => modules[path]()
    }
  }
  return res
}

export default usePermissionStore
