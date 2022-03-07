
enum ReadyState {
  UNCONNECTED = -1,// 未连接
  CONNECTING = 0,// 连接中
  OPEN = 1, // 已连接
  CLOSING = 2, // 连接正在关闭
  CLOSED = 3,// 连接已经关闭或不可用
}
export default class SimpleWebSocket {
  private ws: WebSocket | null = null;
  private url = '';
  private openPromise: { resolve?: any, reject?: any } = {};
  private closePromise: { resolve?: any, reject?: any } = {};
  public get state(): ReadyState {
    if (!this.ws) {
      return ReadyState.UNCONNECTED
    } else {
      return this.ws.readyState
    }
  }
  constructor(url: string) {

    this.url = url
  }
  public close(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.state == ReadyState.CLOSED || this.state == ReadyState.UNCONNECTED) {
        resolve()
      } else if (this.state == ReadyState.OPEN) {
        this.closePromise = {
          reject,
          resolve
        }

        this.ws && this.ws.close()
      }
    })
  }
  public open(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.state == ReadyState.OPEN) {
        resolve()
      }
      else if (this.state != ReadyState.UNCONNECTED && this.state != ReadyState.CLOSED) {
      } else {
        this.openPromise = {
          reject,
          resolve
        }
        this.init()
      }
    })
  }

  private init(): void {
    if (!window.WebSocket) {
      console.error('浏览器不支持websocket')
      if (this.openPromise.reject) {
        this.openPromise.reject()
      }
      return
    }
    this.ws = new window.WebSocket(this.url)
    this.ws.onopen = () => {
      if (this.openPromise.resolve) {
        this.openPromise.resolve()
      }
    }
    this.ws.onerror = () => {
      if (this.openPromise.reject) {
        this.openPromise.reject()
      }
    }
    this.ws.onclose = () => {
      if (this.closePromise.resolve) {
        this.closePromise.reject()
      }
    }
  }
  public send(data: string): void {
    if (this.state == ReadyState.OPEN) {
      this.ws?.send(data)
    }
  }

}