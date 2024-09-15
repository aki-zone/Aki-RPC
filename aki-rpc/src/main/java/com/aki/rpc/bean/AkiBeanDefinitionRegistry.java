package com.aki.rpc.bean;

import com.aki.rpc.annontation.AkiHttpClient;
import com.aki.rpc.annontation.EnableHttpClient;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Set;

/**
 * 1. ImportBeanDefinitionRegistrar类只能通过其他类@Import的方式来加载，通常是启动类或配置类。
 * 2. 使用@Import，如果括号中的类是ImportBeanDefinitionRegistrar的实现类，则会调用接口方法，将其中要注册的类注册成bean
 * 3. 实现该接口的类拥有注册bean的能力
 */

///////////////////////////////////////////////////////////
// 本段代码是注册AkiHttpClient所指定的实例到bean管理器中      ///
///////////////////////////////////////////////////////////
public class AkiBeanDefinitionRegistry implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    // 资源和环境加载器可以闲置，注册单例注解代理类需要的是注册加载器
    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metaData, BeanDefinitionRegistry registry) {
        registerAkiHttpClient(metaData,registry);
    }

    ////////////////////////////////////////////////////////////////
    // 将AkiHttpClient所标识的接口，生成代理类，并月注册到spring容器当中 //
    ///////////////////////////////////////////////////////////////
    private void registerAkiHttpClient(AnnotationMetadata metaData, BeanDefinitionRegistry registry){
        // 获取被Enable注解所获取的参数
        Map<String, Object> annotationAttributes = metaData.getAnnotationAttributes(EnableHttpClient.class.getCanonicalName());

        //找到Enable注解，获取其中的basePackage属性，此属性标明了@AkiHttpClient所在的包
        Object basePackage = annotationAttributes.get("basePackage");

        // 包路径先判空
        if (basePackage != null){
            // 地址强转换为String
            String base = basePackage.toString();

            // ClassPathScanningCandidateComponentProvider是Spring提供的工具，可以按自定义的类型，查找classpath下符合要求的class文件
            ClassPathScanningCandidateComponentProvider scanner = getScanner();

            // 传入资源加载器
            scanner.setResourceLoader(resourceLoader);

            // 创建一个注解过滤器-AnnotationTypeFilter实例，过滤类型为AkiHttpClient的注解
            AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(AkiHttpClient.class);

            // 传入注解过滤器，指定条件为筛选AkiHttpClient
            scanner.addIncludeFilter(annotationTypeFilter);

            // 扫包，返回并获取所有符合条件的类的定义，类定义描述了bean的各种属性，比如本项目的bean：goodsHttpRpc
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(base);
            for (BeanDefinition candidateComponent : candidateComponents) {
                if (candidateComponent instanceof  AnnotatedBeanDefinition){
                    //对于集合中的每个BeanDefinition对象，检查它是否是一个带有注解的bean定义
                    //*这就是被@AkiHttpClient注解标识的类，这里将bean强转为注解型bean，以提供访问注解的途径
                    AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) candidateComponent;

                    // 获取注解元数据
                    AnnotationMetadata beanDefinitionMetadata = annotatedBeanDefinition.getMetadata();

                    // 断言是否为接口类型，若不是则抛出异常
                    Assert.isTrue(beanDefinitionMetadata.isInterface(),"@AkiHttpClient 必须定义在接口上");

                    //获取此注解的属性
                    Map<String, Object> clientAnnotationAttributes = beanDefinitionMetadata.getAnnotationAttributes(AkiHttpClient.class.getCanonicalName());

                    //这里判断是否value设置了值，为空时抛出运行期错误，value为此Bean的名称，定义bean的时候要用
                    String beanName = getClientName(clientAnnotationAttributes);

                    //Bean的定义，通过建造者Builder模式来实现,需要一个参数，FactoryBean的实现类
                    //FactoryBean是一个工厂Bean，可以生成某一个类型Bean实例，它最大的一个作用是：可以让我们自定义Bean的创建过程。
                    // 这里具体自定义的是：动态代理类
                    BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(AkiHttpClientFactoryBean.class);

                    //设置FactoryBean实现类中自定义的属性,这里我们设置@AkiHttpClient标识的类,用于生成代理实现类
                    beanDefinitionBuilder.addPropertyValue("interfaceClass",beanDefinitionMetadata.getClassName());

                    // 如果 beanName 为 null，那么断言就会失败，抛出异常。
                    assert beanName != null;

                    //传入bean名和构造器，将该bean注册，允许其自动注入
                    registry.registerBeanDefinition(beanName,beanDefinitionBuilder.getBeanDefinition());
                }
            }
        }

    }




    /**
     * 获取用于扫描候选组件的扫描器，组件候选条件为独立定义的bean（无bean依赖，无依赖注入，可独立创建）。
     * 该方法创建了一个ClassPathScanningCandidateComponentProvider的实例，并设置了指定的参数。
     * 方法来自于Feign
     * @return ClassPathScanningCandidateComponentProvider的实例
     */
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // 本段代码是组件扫描器，用于扫描候选组件，组件候选条件为独立定义的bean（无bean依赖，无依赖注入，可独立创建）。   //
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    protected ClassPathScanningCandidateComponentProvider getScanner() {
        // 实现ClassPathScanningCandidateComponentProvider接口为自定义扫描组件
        // 参数一useDefaultFilters：表示是否适用默认过滤器，比如过滤带有@Component注解的类
        // 参数二environment：指定扫描的环境
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, this.environment) {

            /**
             * 判断给定的bean定义是否为候选组件。
             * 此方法重写了默认行为，以过滤掉非注解的bean定义。
             *
             * @param beanDefinition 注解的bean定义
             * @return 如果bean定义为候选组件，则返回true；否则返回false
             */
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                // 标志，指示bean定义是否为候选组件
                boolean isCandidate = false;

                // 检查bean定义是否独立
                if (beanDefinition.getMetadata().isIndependent()) {
                    // 检查bean定义是否不是注解
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        // bean定义为候选组件
                        isCandidate = true;
                    }
                }

                return isCandidate;
            }
        };

        return scanner;
    }

    ///////////////////////////////////////////////////////////////////////////
    // 获取@AkiHttpClient中"value"对应的值，也就是代理类的名称，为空时抛出运行期错误  //
    ///////////////////////////////////////////////////////////////////////////
    private String getClientName(Map<String, Object> clientAnnotationAttributes) {
        if (clientAnnotationAttributes == null){
            throw new RuntimeException("value必须有值");
        }
        Object value = clientAnnotationAttributes.get("value");
        if (value != null && !value.toString().equals("")){
            return value.toString();
        }
        return null;
    }


}
