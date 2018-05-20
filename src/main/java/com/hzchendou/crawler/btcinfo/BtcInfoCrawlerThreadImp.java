package com.hzchendou.crawler.btcinfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hzchendou.crawler.btcinfo.annotation.BtcinfoPageSelect;
import com.xuxueli.crawler.annotation.PageFieldSelect;
import com.xuxueli.crawler.conf.XxlCrawlerConf;
import com.xuxueli.crawler.exception.XxlCrawlerException;
import com.xuxueli.crawler.model.PageLoadInfo;
import com.xuxueli.crawler.thread.CrawlerThread;
import com.xuxueli.crawler.util.FieldReflectionUtil;
import com.xuxueli.crawler.util.JsoupUtil;
import com.xuxueli.crawler.util.UrlUtil;

/**
 * Btcinfo自定义爬虫线程.
 *
 * @author chendou
 * @date 2018/5/20
 * @since 1.0
 */
public class BtcInfoCrawlerThreadImp extends BtcInfoCrawlerThread {

    private static Logger logger = LoggerFactory.getLogger(CrawlerThread.class);

    /**
     * 是否在运行
     */
    private boolean running;

    /**
     * 尝试停止
     */
    private boolean toStop;


    /**
     * 构造函数
     *
     * @param crawler
     */
    public BtcInfoCrawlerThreadImp(BtcInfoCrawler crawler) {
        super(crawler);
        this.running = true;
        this.toStop = false;
    }

    /**
     * 尝试停止爬虫
     */
    @Override
    public void toStop() {
        this.toStop = true;
    }

    /**
     * 是否正在运行中
     *
     * @return
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 执行爬虫程序
     */
    @Override
    public void run() {
        while (!toStop) {
            try {
                running = false;
                crawler.tryFinish();
                String link = crawler.getRunData().getUrl();
                running = true;
                logger.info(">>>>>>>>>>> Btcinfo crawler, process link : {}", link);
                if (!UrlUtil.isUrl(link)) {
                    continue;
                }
                //允许失败重试
                for (int i = 0; i < (1 + crawler.getRunConf().getFailRetryCount()); i++) {
                    boolean ret = process(link);
                    if (crawler.getRunConf().getPauseMillis() > 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(crawler.getRunConf().getPauseMillis());
                        } catch (InterruptedException e) {
                            logger.info(">>>>>>>>>>> Btcinfo crawler thread is interrupted. 2{}", e.getMessage());
                        }
                    }
                    if (ret) {
                        break;
                    }
                }

            } catch (Throwable e) {
                if (e instanceof InterruptedException) {
                    logger.info(">>>>>>>>>>> Btcinfo crawler thread is interrupted. {}", e.getMessage());
                } else if (e instanceof XxlCrawlerException) {
                    logger.info(">>>>>>>>>>> Btcinfo crawler thread {}", e.getMessage());
                } else {
                    logger.error(e.getMessage(), e);
                }
            }

        }
    }

    /**
     * 页面解析（改造分析规则，分层解析）
     *
     * @param link
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    protected boolean process(String link) throws IllegalAccessException, InstantiationException {

        String userAgent = crawler.getRunConf().getUserAgentList().size() > 1 ?
                crawler.getRunConf().getUserAgentList()
                        .get(new Random().nextInt(crawler.getRunConf().getUserAgentList().size())) :
                crawler.getRunConf().getUserAgentList().size() == 1 ?
                        crawler.getRunConf().getUserAgentList().get(0) :
                        null;
        Proxy proxy = null;
        if (crawler.getRunConf().getProxyMaker() != null) {
            proxy = crawler.getRunConf().getProxyMaker().make();
        }

        PageLoadInfo pageLoadInfo = new PageLoadInfo();
        pageLoadInfo.setUrl(link);
        pageLoadInfo.setParamMap(crawler.getRunConf().getParamMap());
        pageLoadInfo.setCookieMap(crawler.getRunConf().getCookieMap());
        pageLoadInfo.setHeaderMap(crawler.getRunConf().getHeaderMap());
        pageLoadInfo.setUserAgent(userAgent);
        pageLoadInfo.setReferrer(crawler.getRunConf().getReferrer());
        pageLoadInfo.setIfPost(crawler.getRunConf().isIfPost());
        pageLoadInfo.setTimeoutMillis(crawler.getRunConf().getTimeoutMillis());
        pageLoadInfo.setProxy(proxy);

        //页面加载前处理
        crawler.getRunConf().getPageParser().preLoad(pageLoadInfo);
        //加载页面
        Document html = crawler.getRunConf().getPageLoader().load(pageLoadInfo);
        //页面加载后处理
        crawler.getRunConf().getPageParser().postLoad(html);

        if (html == null) {
            return false;
        }

        // 是否抓取页面连接（FIFO队列,广度优先）
        if (crawler.getRunConf().isAllowSpread()) {
            Set<String> links = JsoupUtil.findLinks(html);
            if (links != null && links.size() > 0) {
                for (String item : links) {
                    //是否符合链接过滤规则
                    if (crawler.getRunConf().validWhiteUrl(item)) {
                        crawler.getRunData().addUrl(item);
                    }
                }
            }
        }

        //是否符合页面抓取规则
        if (!crawler.getRunConf().validWhiteUrl(link)) {
            return false;
        }

        // 解析页面
        parsePage(html);
        return true;
    }

    /**
     * 处理页面
     *
     * @param html
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void parsePage(Document html) throws IllegalAccessException, InstantiationException {
        Type[] pageVoClassTypes =
                ((ParameterizedType) crawler.getRunConf().getPageParser().getClass().getGenericSuperclass())
                        .getActualTypeArguments();
        Class pageVoClassType = (Class) pageVoClassTypes[0];

        BtcinfoPageSelect pageVoSelect = (BtcinfoPageSelect) pageVoClassType.getAnnotation(BtcinfoPageSelect.class);
        String pageVoCssQuery = (pageVoSelect != null && pageVoSelect.cssQuery() != null
                && pageVoSelect.cssQuery().trim().length() > 0) ? pageVoSelect.cssQuery() : "html";

        // 获取元素
        Elements pageVoElements = html.select(pageVoCssQuery);
        if (pageVoElements != null && pageVoElements.hasText()) {
            for (Element pageVoElement : pageVoElements) {
                //解析网页内容
                Object pageVo = parseFieldValue(pageVoClassType, pageVoElement);
                //回调处理接口
                crawler.getRunConf().getPageParser().parse(html, pageVoElement, pageVo);
            }
        }
    }

    /**
     * 解析字段属性
     *
     * @param pageClassType
     * @param elem
     * @return
     */
    private Object parseFieldValue(Class pageClassType, Element elem)
            throws IllegalAccessException, InstantiationException {
        //创建字段对象
        Object page = pageClassType.newInstance();
        //获取类型字段
        Field[] fields = pageClassType.getDeclaredFields();
        if (fields != null) {
            for (Field field : fields) {
                //不解析静态字段
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                BtcinfoPageSelect pageSelect = field.getAnnotation(BtcinfoPageSelect.class);
                PageFieldSelect pageFieldSelect = field.getAnnotation(PageFieldSelect.class);
                Object fieldValue = null;
                //解析pageSelect注解
                if (pageSelect != null) {
                    String pageCssQuery = (pageSelect != null && pageSelect.cssQuery() != null
                            && pageSelect.cssQuery().trim().length() > 0) ? pageSelect.cssQuery() : null;
                    if (pageCssQuery != null) {
                        Elements pageVoElements = elem.select(pageCssQuery);
                        if (pageVoElements != null && pageVoElements.size() > 0) {
                            Type genericType = field.getGenericType();
                            //如果类型是参数化，可能是List
                            if (genericType instanceof ParameterizedType) {
                                ParameterizedType parameterizedType = ((ParameterizedType) genericType);
                                //如果是List类型
                                if (parameterizedType.getRawType().equals(List.class)) {
                                    //获取实际类型
                                    Class<?> fieldType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                                    List<Object> fieldValueTmp = new ArrayList<Object>();
                                    for (Element fieldElem : pageVoElements) {
                                        fieldValueTmp.add(parseFieldValue(fieldType, fieldElem));
                                    }
                                    fieldValue = fieldValueTmp;
                                }
                            }
                        }
                    }
                    //解析pageFieldSelect注解
                } else if (pageFieldSelect != null) {
                    fieldValue = parsePageFieldValue(field, elem);
                }
                if (fieldValue != null) {
                    field.setAccessible(Boolean.TRUE);
                    field.set(page, fieldValue);
                }
            }
        }
        return page;
    }

    /**
     * 解析字段属性
     *
     * @param field
     * @param elem
     * @return
     */
    private Object parsePageFieldValue(Field field, Element elem) {
        PageFieldSelect pageFieldSelect = field.getAnnotation(PageFieldSelect.class);
        String cssQuery = pageFieldSelect.cssQuery();
        XxlCrawlerConf.SelectType selectType = pageFieldSelect.selectType();
        String selectVal = pageFieldSelect.selectVal();
        if (cssQuery == null || cssQuery.trim().length() == 0) {
            return null;
        }
        //解析参数化类型字段
        if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType fieldGenericType = (ParameterizedType) field.getGenericType();
            if (fieldGenericType.getRawType().equals(List.class)) {
                Elements fieldElementList = elem.select(cssQuery);
                if (fieldElementList != null && fieldElementList.size() > 0) {
                    List<Object> fieldValueTmp = new ArrayList<Object>();
                    for (Element fieldElement : fieldElementList) {
                        String fieldElementOrigin = JsoupUtil.parseElement(fieldElement, selectType, selectVal);
                        if (fieldElementOrigin == null || fieldElementOrigin.length() == 0) {
                            continue;
                        }
                        try {
                            fieldValueTmp.add(FieldReflectionUtil.parseValue(field, fieldElementOrigin));
                        } catch (Exception e) {
                            logger.error("msg:[can not parse List field value]", e);
                        }
                    }

                    if (fieldValueTmp.size() > 0) {
                        return fieldValueTmp;
                    }
                }
            }
        } else {
            Elements fieldElements = elem.select(cssQuery);
            String fieldValueOrigin = null;
            if (fieldElements != null && fieldElements.size() > 0) {
                fieldValueOrigin = JsoupUtil.parseElement(fieldElements.get(0), selectType, selectVal);
            }
            if (fieldValueOrigin == null || fieldValueOrigin.length() == 0) {
                return null;
            }
            try {
                return FieldReflectionUtil.parseValue(field, fieldValueOrigin);
            } catch (Exception e) {
                logger.error("msg:[can not parse field value]", e);
            }
        }
        return null;
    }
}
