package com.aki.rpc.spring;

import com.aki.rpc.annotation.AkiReference;
import com.aki.rpc.annotation.AkiService;
import com.aki.rpc.annotation.EnableRpc;
import com.aki.rpc.config.AkiRpcConfig;
import com.aki.rpc.factory.SingletonFactory;
import com.aki.rpc.netty.client.NettyClient;
import com.aki.rpc.proxy.AkiRpcClientProxy;
import com.aki.rpc.register.nacos.NacosTemplate;
import com.aki.rpc.server.AkiServiceProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;

/**
 * @Auther Akizora
 */
//////////////////////////////////////////////////////////////////
// 对EnableRpc的类进行扫包(一般是主类，也会连锁扫到其牵连应用子包)       //
// 这里对被AkiService注解标志的Bean进行动态代理                      //
// 我们将在被标注的Bean的某些生命周期植入特定的处理函数，达到注解注入的效果//
//////////////////////////////////////////////////////////////////
@Slf4j
@Component
public class AkiRpcSpringBeanPostProcessor implements BeanPostProcessor, BeanFactoryPostProcessor {

    private AkiServiceProvider akiServiceProvider;
    private AkiRpcConfig akiRpcConfig;
    private NettyClient nettyClient;
    private NacosTemplate nacosTemplate;

    //通过扫描 Bean 的注解，自动发布带有 @AkiService 的服务，并为带有 @AkiReference 的字段生成远程调用的代理对象。
    // 初始化阶段，先单例式注入各个工具类
    public AkiRpcSpringBeanPostProcessor(){
        akiServiceProvider = SingletonFactory.getInstance(AkiServiceProvider.class);
        nettyClient = SingletonFactory.getInstance(NettyClient.class);
        nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
    }

    //调用阶段：bean【初始化方法】【调用前】被调用
    //用于检查是否存在 @EnableRpc 注解，
    // * 如果存在，则初始化 AkiRpcConfig 配置并设置到相关组件中。
    @SneakyThrows  //自动处理检查型异常（checked exceptions），在编写代码时不需要显式地捕获或声明抛出这些异常。
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // FIXME Info输出
        // log.info("postProcessBeforeInitialization会先于所有的Bean实例化之前执行");
        // 1. 检查当前Bean是否标注了 @EnableRpc 注解。
        EnableRpc enableRpc = bean.getClass().getAnnotation(EnableRpc.class);
        // 1.1 若标注，则尝试初始化
        if (enableRpc != null){
            // 2. 如果 AkiRpcConfig 尚未初始化，则执行以下初始化操作
            if (akiRpcConfig == null) {
                log.info("@EnableRpc: 读取到配置...");
                akiRpcConfig = new AkiRpcConfig();
                akiRpcConfig.setProviderPort(enableRpc.serverPort() == 0
                        ? findRandomAvailablePort()
                        : enableRpc.serverPort());
                akiRpcConfig.setNacosPort(enableRpc.nacosPort());
                akiRpcConfig.setNacosHost(enableRpc.nacosHost());
                akiRpcConfig.setNacosGroup(enableRpc.nacosGroup());
                nettyClient.setAkiRpcConfig(akiRpcConfig);
                akiServiceProvider.setAkiRpcConfig(akiRpcConfig);
                nacosTemplate.init(akiRpcConfig.getNacosHost(),akiRpcConfig.getNacosPort());

            }
        }
        return bean;
    }

    //通过扫描 Bean 的注解，自动发布带有 @AkiService 的服务，并为带有 @AkiReference 的字段生成远程调用的代理对象。
    //调用阶段：bean【初始化方法】【调用后】被调用
    @SneakyThrows   //自动处理检查型异常（checked exceptions），在编写代码时不需要显式地捕获或声明抛出这些异常。
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // FIXME Info输出
        //log.info("postProcessAfterInitialization会先于所有的Bean实例化之后执行");
        //1. 检查 bean 对象所属的类是否被 AkiService 注解标记。
        AkiService akiService = bean.getClass().getAnnotation(AkiService.class);

        //1.1 如果有，将其发布为服务
        if (akiService != null){
            akiServiceProvider.publishService(akiService,bean);
        }

        //2. 在这里判断所有的bean里面的字段有没有加@AkiRefrence注解
        //* 如果有 识别并生成代理实现类，发起网络请求
        Class<?> targetClass = bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();
        //* 对bean每个字段进行遍历。
        for (Field declaredField : declaredFields) {
            //2.1 检查字段上是否有 AkiReference注解。如果不为空可获取，则对注解实例annotation执行以下操作：
            AkiReference akiReference = declaredField.getAnnotation(AkiReference.class);
            if (akiReference != null){
                //2.2代理实现类，调用方法的时候 会触发invoke方法，在其中实现网络调用
                AkiRpcClientProxy akiRpcClientProxy = new AkiRpcClientProxy(akiReference,nettyClient);      //传入AkiReference注解上下文
                Object proxy = akiRpcClientProxy.getProxy(declaredField.getType());

                //2.3 将字段的可访问性设置为 true，以便在后续代码中反射的访问私有字段。
                declaredField.setAccessible(true);
                try {
                    //2.4 将生成的代理对象设置为字段的值，从而进行实际的网络请求。
                    declaredField.set(bean,proxy);
                    log.info("@AkiReference: 成功挂载代理类:"+declaredField.getType());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }

    /**
     * 此方法动态扫描被 @EnableRpc 注解标记的类，并将其注册到 Spring 应用上下文中。
     * 通过反射方式创建并配置 ClassPathBeanDefinitionScanner，添加自定义过滤器以仅包含目标注解，然后执行扫描操作。
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 1. 判断 beanFactory 是否是 BeanDefinitionRegistry 的实例。
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // 2. 使用反射初始化 ClassPathBeanDefinitionScanner 实例。
                // 2.1 通过绝对路径加载 ClassPathBeanDefinitionScanner 类。
                // * 任意类都可以用自身loader加载任意可具体定位的类
                // 且用AkiRpcSpringBeanPostProcessor类加载属于独立自定义注册器，对其他注册器不影响
                Class<?> scannerClass = ClassUtils.forName ( "org.springframework.context.annotation.ClassPathBeanDefinitionScanner",
                        AkiRpcSpringBeanPostProcessor.class.getClassLoader () );
                // 2.2 创建 ClassPathBeanDefinitionScanner 实例，参数为 BeanDefinitionRegistry 和 true（使用默认过滤器）。
                Object scanner = scannerClass.getConstructor ( new Class<?>[]{BeanDefinitionRegistry.class, boolean.class} )
                        .newInstance ( new Object[]{(BeanDefinitionRegistry) beanFactory, true} );

                // 3. 为扫描器添加 @EnableRpc 注解的包含过滤器。
                // 3.1 加载 AnnotationTypeFilter 类。
                Class<?> filterClass = ClassUtils.forName ( "org.springframework.core.type.filter.AnnotationTypeFilter",
                        AkiRpcSpringBeanPostProcessor.class.getClassLoader () );

                // 3.2 创建 AnnotationTypeFilter 实例，指定 @EnableRpc 注解。
                Object filter = filterClass.getConstructor ( Class.class ).newInstance ( EnableRpc.class );

                // 3.3 获取并调用 scanner 的 addIncludeFilter 方法，添加过滤器。
                Method addIncludeFilter = scannerClass.getMethod ( "addIncludeFilter",
                        ClassUtils.forName ( "org.springframework.core.type.filter.TypeFilter", AkiRpcSpringBeanPostProcessor.class.getClassLoader () ) );
                addIncludeFilter.invoke ( scanner, filter );

                // 4. 执行包扫描操作，扫描被 @EnableRpc 注解标记的类。
                // 4.1 获取 scanner 的 scan 方法。
                Method scan = scannerClass.getMethod ( "scan", new Class<?>[]{String[].class} );

                // 4.2 调用 scan 方法，指定目标扫描包。
                log.info("@EnableRpc: 开始进行扫包注册...");
                scan.invoke ( scanner, new Object[]{"com.aki.rpc.annontation"} );
            } catch (Throwable e) {
                // spring 2.0
            }
        }
    }

    private int findRandomAvailablePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("无法获取有效的动态端口，请稍后重试。。。", e);
        }
    }
}
