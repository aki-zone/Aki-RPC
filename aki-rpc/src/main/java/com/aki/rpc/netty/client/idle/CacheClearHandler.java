package com.aki.rpc.netty.client.idle;

import java.net.InetSocketAddress;

public interface CacheClearHandler {

    void clear(InetSocketAddress inetSocketAddress);
}
