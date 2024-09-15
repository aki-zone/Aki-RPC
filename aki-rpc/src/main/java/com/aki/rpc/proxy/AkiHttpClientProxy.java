package com.aki.rpc.proxy;

import com.aki.rpc.annontation.AkiMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//每一个动态代理类的调用处理程序都必须实现InvocationHandler接口，
// 并且每个代理类的实例都关联到了实现该接口的动态代理类调用处理程序中，
// 当我们通过动态代理对象调用一个方法时候，
// 这个方法的调用就会被转发到实现InvocationHandler接口类的invoke方法来调用
public class AkiHttpClientProxy implements InvocationHandler {
    public AkiHttpClientProxy(){

    }
    /**
     * proxy:代理类代理的真实代理对象com.sun.proxy.$Proxy0
     * method:我们所要调用某个对象真实的方法的Method对象
     * args:指代代理对象方法传递的参数
     */
    //当接口 实现调用的时候，实际上是代理类的invoke万法被调用了
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        System.out.println("invoke调用成功~~~~~~~~");
        //在此处实现调用
        AkiMapping akiMapping = method.getAnnotation(AkiMapping.class);
        if (akiMapping != null){
            RestTemplate restTemplate = new RestTemplate();
            String api = akiMapping.api();

            // 将api中{xxx}替换为代理类传入的args中的值
            Pattern compile = Pattern.compile("(\\{\\w+})");
            Matcher matcher = compile.matcher(api);
            if (matcher.find()){
                //简单判断一下 代表有路径参数需要替换
                int x = matcher.groupCount();
                for (int i = 0; i< x; i++){
                    String group = matcher.group(i);
                    api = api.replace(group, args[i].toString());
                }
            }

            // 使用HttpClient发送请求获取信息，完成一次Rpc请求调用。
            ResponseEntity forEntity = restTemplate.getForEntity(akiMapping.url()+ api, method.getReturnType());
            return forEntity.getBody();
        }
        return null;
    }

    public <T> T getProxy(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, this);
    }

}
