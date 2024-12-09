package com.aki.rpc.netty.codec;

import com.aki.rpc.compress.Compress;
import com.aki.rpc.constant.CompressTypeEnum;
import com.aki.rpc.constant.MessageTypeEnum;
import com.aki.rpc.constant.AkiRpcConstants;
import com.aki.rpc.constant.SerializationTypeEnum;
import com.aki.rpc.exception.AkiRpcException;
import com.aki.rpc.message.AkiMessage;
import com.aki.rpc.message.AkiRequest;
import com.aki.rpc.message.AkiResponse;
import com.aki.rpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;

/**
 * @Auther akizora
 * 消息解码器
 */

/**
 *   0     1     2     3     4        5     6   7    8      9            10      11       12    13    14  15  16
 *   +-----+-----+-----+-----+--------+----+----+----+------+------------+-------+--------+-----+-----+---+----+
 *   |   magic_code          |version |     full_length     | messageType| codec |compress|    RequestId       |
 *   +-----------------------+--------+---------------------+------------+-------+--------+--------------------+
 *   |                                                                                                         |
 *   |                                         body                                                            |
 *   |                                                                                                         |
 *   |                                        ... ...                                                          |
 *   +---------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 */
@Slf4j
public class AkiRpcDecoder extends LengthFieldBasedFrameDecoder {


    public AkiRpcDecoder(){
        this(8 * 1024 * 1024,5,4,-9,0);
    }

    /**
     * 重写父类构造函数，从起始位按固定偏移解析出本消息体大小的值，以便完整的切出该消息体
     * @param maxFrameLength 最大帧长度。它决定可以接收的数据的最大长度。如果超过，数据将被丢弃,根据实际环境定义
     * @param lengthFieldOffset 数据长度字段开始的偏移量, magic code+version=长度为5
     * @param lengthFieldLength 消息大小值的长度  full length（消息长度） 长度为4
     * @param lengthAdjustment 补偿偏移量 lengthAdjustment+数据长度取值=长度字段之后剩下包的字节数(x + 16=7 so x = -9)
     * @param initialBytesToStrip 忽略的字节长度，如果要接收所有的header+body 则为0，如果只接收body 则为header的长度 ,此处为0
     */
    public AkiRpcDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    /**
     * 解码器
     * @param ctx
     * @param in
     * @return
     * @throws Exception
     */
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 1. 调用父类的 decode 方法进行初步的解码操作
        Object decode = super.decode(ctx, in);

        // 2. 数据发送到服务端时，Netty 会先调用这个方法对数据进行解码，
        // 如果解码得到的对象是 ByteBuf 类型（即解码成功，并得到一个完整的数据帧）：
        if (decode instanceof  ByteBuf){
            // 3.将解码结果强制转换为 ByteBuf（Netty 的字节缓冲区）。
            ByteBuf frame = (ByteBuf) decode;

            // 4.判定长度是否合标
            int length = frame.readableBytes();
            if (length < AkiRpcConstants.TOTAL_LENGTH){
                throw new AkiRpcException("数据长度不符");
            }

            // 5.进入解码环节
            return decodeFrame(frame);
        }
        return decode;
    }

    //1. 4B  magic code（魔法数）
    //2. 1B version（版本）
    //3. 4B full length（消息长度）
    //4. 1B messageType（消息类型）
    //5. 1B codec（序列化类型）
    //6. 1B compress（压缩类型）
    //7. 4B  requestId（请求的Id）
    //8. body（object类型数据）
    private Object decodeFrame(ByteBuf frame) {
        //1.按顺序进行读取
        //1.1. 检测魔法数
        checkMagicCode(frame);

        //1.2. 检查版本
        checkVersion(frame);

        //1.3.数据长度
        int fullLength = frame.readInt();

        //1.4.messageType 消息类型
        byte messageType = frame.readByte();

        //1.5. 1B codec（序列化类型）
        byte codec = frame.readByte();

        //1.6. 1B compress（压缩类型）
        byte compressType = frame.readByte();

        //1.7. 4B  requestId（请求的Id）
        int requestId = frame.readInt();

        //1.8 获取数据体Body总长度
        int dataLength = fullLength - AkiRpcConstants.TOTAL_LENGTH;

        // 2. 将解析数据存入消息体类中
        AkiMessage akiMessage = AkiMessage.builder()
                .messageType(messageType)
                .codec(codec)
                .compress(compressType)
                .requestId(requestId)
                .build();

        // 3. 解压缩数据体
        if (dataLength > 0){
            //3.1.有数据,读取body的数据
            byte[] bodyData = new byte[dataLength];
            frame.readBytes(bodyData);

            // 3.2.解压缩并反序列化为AkiRequest
            // 3.2.1 通过SPI机制获取指定压缩实现类
            Compress compress = loadCompress(compressType);
            bodyData = compress.decompress(bodyData);

            // 3.2.2 反序列化为Serializer类型，后期强转为指定类
            Serializer serializer = loadSerializer(codec);

            //客户端-请求数据  服务端-响应数据  根据不同类型按不同的类反序列化
            //AkiRequest  AkiResponse
            if (MessageTypeEnum.REQUEST.getCode() == messageType){
                AkiRequest akiRequest = (AkiRequest) serializer.deserialize(bodyData, AkiRequest.class);
                akiMessage.setData(akiRequest);
            }
            if (MessageTypeEnum.RESPONSE.getCode() == messageType){
                AkiResponse akiResponse = (AkiResponse) serializer.deserialize(bodyData, AkiResponse.class);
                akiMessage.setData(akiResponse);
            }

        }
        return akiMessage;
    }

    /**
     * SPI动态获取实现的序列化器类
     * @param codec
     * @return
     */
    private Serializer loadSerializer(byte codec) {
        // 1. 根据压缩类型枚举值，获取对应的序列化类型名称
        String name = SerializationTypeEnum.getName(codec);
        log.info("SPI: 解码类型为"+name);
        /*
          2. 使用Java SPI(Service Provider Interface)机制加载所有Compress接口的实现类
          定义服务接口的实现列表：META-INF/services 文件是实现类的注册表，告诉 Java 系统有哪些实现可用。
          动态加载实现类：ServiceLoader 在运行时会扫描这个目录，根据配置文件动态加载实现类，从而实现解耦和动态扩展。
         */
        ServiceLoader<Serializer> load = ServiceLoader.load(Serializer.class);

        // 3. 返回指定压缩类：属性name匹配于name值的序列化类
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
        // 1. 根据压缩类型枚举值，获取对应的压缩类型名称
        String name = CompressTypeEnum.getName(compressType);
        log.info("SPI: 解压缩类型为"+name);

        /*
          2. 使用Java SPI(Service Provider Interface)机制加载所有Compress接口的实现类
          定义服务接口的实现列表：META-INF/services 文件是实现类的注册表，告诉 Java 系统有哪些实现可用。
          动态加载实现类：ServiceLoader 在运行时会扫描这个目录，根据配置文件动态加载实现类，从而实现解耦和动态扩展。
         */
        ServiceLoader<Compress> load = ServiceLoader.load(Compress.class);

        // 3. 返回指定压缩类：属性name匹配于name值的压缩类
        for (Compress compress : load){
            if (compress.name().equals(name)){
                    return compress;
            }
        }
        throw new AkiRpcException("无对应的压缩类型");
    }

    /**
     * 检查版本号是否匹配
     * @param frame
     */
    private void checkVersion(ByteBuf frame) {
        byte b = frame.readByte();
        if (b != AkiRpcConstants.VERSION){
            throw new AkiRpcException("未知的version");
        }
    }

    /**
     * 检查魔法数是否匹配
     * @param frame
     */
    private void checkMagicCode(ByteBuf frame) {
        int length = AkiRpcConstants.MAGIC_NUMBER.length;
        byte[] tmp = new byte[length];
        frame.readBytes(tmp);
        for (int i = 0;i< length; i++){
            if (tmp[i] != AkiRpcConstants.MAGIC_NUMBER[i]){
                throw new AkiRpcException("传递魔法数有误");
            }
        }

    }
}
