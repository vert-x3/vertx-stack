declare namespace __EventBus {
  interface CallbackHandler {
    (error: any, message: any): any;
  }
}

import CallbackHandler = __EventBus.CallbackHandler;

declare var EventBus: {
    onopen: CallbackHandler;
    onclose: CallbackHandler;
    registerHandler: (address: string, headers: any, callback: CallbackHandler) => void;
    unregisterHandler: (address: string, headers: any, callback: CallbackHandler) => void;
    send: (address: string, message: any, headers: any, callback: CallbackHandler) => void;
    publish: (address: string, message: any, headers: any) => void;
    close: () => void;
};

export = __EventBus;
export as namespace EventBus;
