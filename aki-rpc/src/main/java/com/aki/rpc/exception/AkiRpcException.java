package com.aki.rpc.exception;

/**
 * 错误捕捉的简单封装
 */
public class AkiRpcException extends RuntimeException {

    public AkiRpcException(String msg){
        super(msg);
    }

    public AkiRpcException(String msg, Exception e){
        super(msg,e);
    }
}
