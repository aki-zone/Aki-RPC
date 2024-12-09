package com.aki.rpc.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Auther akizora
 * RPC服务方响应消息体
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AkiResponse<T> implements Serializable {

    private String requestId;        // 请求唯一标识，用于追踪或匹配请求与响应
    private Integer code;            // 返回状态码
    private String message;          // 返回消息体
    private T data;                  // 返回数据

    public static <T> AkiResponse<T> success(T data, String requestId) {
        AkiResponse<T> response = new AkiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setRequestId(requestId);
        if (null != data) {
            response.setData(data);
        }
        return response;
    }

    public static <T> AkiResponse<T> fail(String message) {
        AkiResponse<T> response = new AkiResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

}
