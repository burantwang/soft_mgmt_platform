import { TOKEN_KEY } from './constants'

/** 获取本地 Token */
export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/** 保存 Token */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 清除 Token */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/** 获取本地缓存权限码 */
export function getPerms(): string[] {
  try {
    return JSON.parse(localStorage.getItem('DEV_PLATFORM_PERMS') || '[]')
  } catch {
    return []
  }
}

/** 保存权限码缓存 */
export function setPerms(perms: string[]): void {
  localStorage.setItem('DEV_PLATFORM_PERMS', JSON.stringify(perms))
}

/** 清除权限码缓存 */
export function removePerms(): void {
  localStorage.removeItem('DEV_PLATFORM_PERMS')
}
