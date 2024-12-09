package com.aki.rpc.netty.codec;

import com.aki.rpc.compress.Compress;
import com.aki.rpc.constant.CompressTypeEnum;
import com.aki.rpc.constant.AkiRpcConstants;
import com.aki.rpc.constant.SerializationTypeEnum;
import com.aki.rpc.exception.AkiRpcException;
import com.aki.rpc.message.AkiMessage;
import com.aki.rpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Auther
 * 消息编码器
 */
@Slf4j
public class AkiRpcEncoder extends MessageToByteEncoder<AkiMessage> {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    //1. 4B magic code（魔法数）
    //2. 1B version（版本）
    //3. 4B full length（消息长度）
    //4. 1B messageType（消息类型）
    //5. 1B codec（序列化类型）
    //6. 1B compress（压缩类型）
    //7. 4B  requestId（请求的Id）
    //8. body（object类型数据）
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext,
                          AkiMessage akiMessage,
                          ByteBuf out) throws Exception {
        // 1.获取message进行编码处理
        out.writeBytes(AkiRpcConstants.MAGIC_NUMBER);       // 4B magic code（魔法数）
        out.writeByte(AkiRpcConstants.VERSION);             // 1B version（版本）
        out.writerIndex(out.writerIndex() + 4);          // *4B full length（消息长度） 预留后期填充
        out.writeByte(akiMessage.getMessageType());         // 1B messageType（消息类型）
        //序列化 先进行序列化 在进行压缩
        out.writeByte(akiMessage.getCodec());               // 1B codec（序列化类型）
        out.writeByte(akiMessage.getCompress());            // 1B compress（压缩类型）
        out.writeInt(ATOMIC_INTEGER.getAndIncrement());     // 4B requestId（请求的Id）
        Object data = akiMessage.getData();                 // 不定长 body（object类型数据）

        // 2. 归纳未填充的值
        // 2.1 header 长度为 16
        int fullLength = AkiRpcConstants.HEAD_LENGTH;

        // 2.2 序列化body数据
        Serializer serializer = loadSerializer(akiMessage.getCodec());
        byte[] bodyBytes = serializer.serialize(data);

        // 2.3 压缩body的序列化值
        Compress compress = loadCompress(akiMessage.getCompress());
        bodyBytes = compress.compress(bodyBytes);

        // 2.4 增添body长度，计算帧总长
        fullLength += bodyBytes.length;

        // 2.5 填入full length
        out.writeBytes(bodyBytes);
        int writerIndex = out.writerIndex();
        //将fullLength写入之前的预留的位置
        out.writerIndex(writerIndex - fullLength + AkiRpcConstants.MAGIC_NUMBER.length + 1);
        out.writeInt(fullLength);
        out.writerIndex(writerIndex);
    }

    /**
     * SPI动态获取实现的序列化器类
     * @param codec
     * @return
     */
    private Serializer loadSerializer(byte codec) {
        String name = SerializationTypeEnum.getName(codec);
        log.info("SPI: 编码类型为"+name);
        ServiceLoader<Serializer> load = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : load){
            if (serializer.name().equals(name)){
                return serializer;
            }
        }
        throw new AkiRpcException("无对应的序列化类型");
    }

    /**
     * SPI动态获取实现的压缩器类
     * @param compressType
     * @return
     */
    private Compress loadCompress(byte compressType) {
        String name = CompressTypeEnum.getName(compressType);
        log.info("SPI: 压缩类型为"+name);
        ServiceLoader<Compress> load = ServiceLoader.load(Compress.class);
        for (Compress compress : load){
            if (compress.name().equals(name)){
                return compress;
            }
        }
        throw new AkiRpcException("无对应的压缩类型");
    }
}
