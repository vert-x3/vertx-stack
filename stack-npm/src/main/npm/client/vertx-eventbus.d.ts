declare namespace EventBus {

  interface EventBusStatic {
    new(url: string, options?: any): EventBus;
  }

  interface EventBus {
    url: string;
    options?: any;
    onopen: () => any;
    onclose: (error: Error) => any;
    registerHandler: (address: string, headers: any, callback: (error: Error, message: any) => any) => any;
    unregisterHandler: (address: string, headers: any, callback: (error: Error, message: any) => any) => any;
    send: (address: string, message: any, headers: any, callback: (error: Error, message: any) => any) => any;
    publish: (address: string, message: any, headers: any) => any;
    close: () => any;
  }
}

declare var EventBus: EventBus.EventBusStatic;
export = EventBus;
