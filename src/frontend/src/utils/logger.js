const isDevelopment = process.env.NODE_ENV === 'development'

const logger = {
  debug(...args) {
    if (isDevelopment) {
      console.log('[DEBUG]', ...args)
    }
  },
  info(...args) {
    if (isDevelopment) {
      console.log('[INFO]', ...args)
    }
  },
  warn(...args) {
    console.warn('[WARN]', ...args)
  },
  error(...args) {
    console.error('[ERROR]', ...args)
    this.reportError(...args)
  },
  reportError(...args) {
    try {
      const errorData = {
        timestamp: new Date().toISOString(),
        message: args.map(arg => String(arg)).join(' '),
        url: window.location.href,
        userAgent: navigator.userAgent
      }
      console.log('[错误上报]', JSON.stringify(errorData))
    } catch (e) {
      console.error('错误上报失败:', e)
    }
  }
}

export function installGlobalErrorHandling() {
  window.addEventListener('error', (event) => {
    logger.error('全局未捕获错误:', event.message, 'at', event.filename, ':', event.lineno)
  })

  window.addEventListener('unhandledrejection', (event) => {
    logger.error('未处理的Promise拒绝:', event.reason)
  })

  if (isDevelopment) {
    logger.info('前端日志系统已初始化，当前为开发环境')
  } else {
    logger.info('前端日志系统已初始化，当前为生产环境')
  }
}

export default logger
