package com.aki.rpc.bean;

import com.aki.rpc.annontation.AkiHttpClient;
import com.aki.rpc.proxy.AkiHttpClientProxy;
import lombok.Getter;
import org.springframework.beans.factory.FactoryBean;

/////////////////////////////////////////////////////////////////////////////////////////////////
//FactoryBean是一个工厂Bean，可以生成某一个类型Bean实例，它最大的一个作用是：可以让我们自定义Bean的创建过程。//
//重写getObject getObjectType isSingleton方法来自定义创建过程                                      //
/////////////////////////////////////////////////////////////////////////////////////////////////
@Getter
public class AkiHttpClientFactoryBean<T> implements FactoryBean<T> {

    private Class<T> interfaceClass;

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }


    //返回的代理实现类
    @Override
    public T getObject() throws Exception {
        return new AkiHttpClientProxy().getProxy(interfaceClass);
    }

    //Bean的类型
    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    // 是否为单例
    @Override
    public boolean isSingleton() {
        // return FactoryBean.super.isSingleton();
        return true;
    }
}
