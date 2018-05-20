package com.hzchendou.crawler.btcinfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuxueli.crawler.XxlCrawler;
import com.xuxueli.crawler.loader.PageLoader;
import com.xuxueli.crawler.model.RunConf;
import com.xuxueli.crawler.parser.PageParser;
import com.xuxueli.crawler.proxy.ProxyMaker;
import com.xuxueli.crawler.rundata.RunData;
import com.xuxueli.crawler.rundata.strategy.LocalRunData;

/**
 * btcinfo 爬虫对象.
 *
 * @author chendou
 * @date 2018/5/20
 * @since 1.0
 */
public class BtcInfoCrawlerImp implements BtcInfoCrawler {

    private static Logger logger = LoggerFactory.getLogger(XxlCrawler.class);

    private Class<? extends BtcInfoCrawlerThread> threadClass = BtcInfoCrawlerThreadImp.class;

    /**
     * 运行时数据模型
     */
    private volatile RunData runData = new LocalRunData();

    /**
     * 运行时配置
     */
    private volatile RunConf runConf = new RunConf();

    /**
     * 爬虫线程数量
     */
    private int threadCount = 1;

    /**
     * 爬虫线程池
     */
    private ExecutorService crawlers =
            new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

    /**
     * 爬虫线程引用镜像
     */
    private List<BtcInfoCrawlerThread> crawlerThreads = new CopyOnWriteArrayList<>();

    /**
     * 获取运行数据
     *
     * @return
     */
    @Override
    public RunData getRunData() {
        return runData;
    }

    /**
     * 获取运行配置
     *
     * @return
     */
    @Override
    public RunConf getRunConf() {
        return runConf;
    }

    /**
     * 启动
     *
     * @param sync true=同步方式、false=异步方式
     */
    @Override
    public void start(boolean sync) {
        if (runData == null) {
            throw new RuntimeException("Btcinfo crawler runData can not be null.");
        }
        if (runData.getUrlNum() <= 0) {
            throw new RuntimeException("Btcinfo crawler indexUrl can not be empty.");
        }
        if (runConf == null) {
            throw new RuntimeException("Btcinfo crawler runConf can not be empty.");
        }
        if (threadCount < 1 || threadCount > 1000) {
            throw new RuntimeException("Btcinfo crawler threadCount invalid, threadCount : " + threadCount);
        }
        if (runConf.getPageLoader() == null) {
            throw new RuntimeException("Btcinfo crawler pageLoader can not be null.");
        }
        if (runConf.getPageParser() == null) {
            throw new RuntimeException("Btcinfo crawler pageParser can not be null.");
        }

        logger.info(">>>>>>>>>>> Btcinfo crawler start ...");
        for (int i = 0; i < threadCount; i++) {
            BtcInfoCrawlerThread crawlerThread = new BtcInfoCrawlerThreadImp(this);
            crawlerThreads.add(crawlerThread);
        }
        for (Runnable crawlerThread : crawlerThreads) {
            crawlers.execute(crawlerThread);
        }
        crawlers.shutdown();

        if (sync) {
            try {
                while (!crawlers.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.info(">>>>>>>>>>> Btcinfo crawler still running ...");
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 尝试终止
     */
    @Override
    public void tryFinish() {
        boolean isRunning = false;
        for (BtcInfoCrawlerThread crawlerThread : crawlerThreads) {
            if (crawlerThread.isRunning()) {
                isRunning = true;
                break;
            }
        }
        boolean isEnd = runData.getUrlNum() == 0 && !isRunning;
        if (isEnd) {
            logger.info(">>>>>>>>>>> Btcinfo crawler is finished.");
            stop();
        }
    }

    /**
     * 终止
     */
    @Override
    public void stop() {
        for (BtcInfoCrawlerThread crawlerThread : crawlerThreads) {
            crawlerThread.toStop();
        }
        crawlers.shutdownNow();
        logger.info(">>>>>>>>>>> Btcinfo crawler stop.");
    }


    /**
     * 爬虫对象构建类
     */
    public static class Builder {

        /**
         * 爬虫对象
         */
        private BtcInfoCrawlerImp crawler = new BtcInfoCrawlerImp();

        // run data

        /**
         * 设置运行数据类型
         *
         * @param runData
         * @return
         */
        public BtcInfoCrawlerImp.Builder setRunData(RunData runData) {
            crawler.runData = runData;
            return this;
        }

        /**
         * 待爬的URL列表
         *
         * @param urls
         * @return
         */
        public BtcInfoCrawlerImp.Builder setUrls(String... urls) {
            if (urls != null && urls.length > 0) {
                for (String url : urls) {
                    crawler.runData.addUrl(url);
                }
            }
            return this;
        }

        // run conf

        /**
         * 允许扩散爬取，将会以现有URL为起点扩散爬取整站
         *
         * @param allowSpread
         * @return
         */
        public BtcInfoCrawlerImp.Builder setAllowSpread(boolean allowSpread) {
            crawler.runConf.setAllowSpread(allowSpread);
            return this;
        }

        /**
         * URL白名单正则，非空时进行URL白名单过滤页面
         *
         * @param whiteUrlRegexs
         * @return
         */
        public BtcInfoCrawlerImp.Builder setWhiteUrlRegexs(String... whiteUrlRegexs) {
            if (whiteUrlRegexs != null && whiteUrlRegexs.length > 0) {
                for (String whiteUrlRegex : whiteUrlRegexs) {
                    crawler.runConf.getWhiteUrlRegexs().add(whiteUrlRegex);
                }
            }
            return this;
        }

        /**
         * 页面解析器
         *
         * @param pageParser
         * @return
         */
        public BtcInfoCrawlerImp.Builder setPageParser(PageParser pageParser) {
            crawler.runConf.setPageParser(pageParser);
            return this;
        }

        /**
         * 页面下载器
         *
         * @param pageLoader
         * @return
         */
        public BtcInfoCrawlerImp.Builder setPageLoader(PageLoader pageLoader) {
            crawler.runConf.setPageLoader(pageLoader);
            return this;
        }

        // site

        /**
         * 请求参数
         *
         * @param paramMap
         * @return
         */
        public BtcInfoCrawlerImp.Builder setParamMap(Map<String, String> paramMap) {
            crawler.runConf.setParamMap(paramMap);
            return this;
        }

        /**
         * 请求Cookie
         *
         * @param cookieMap
         * @return
         */
        public BtcInfoCrawlerImp.Builder setCookieMap(Map<String, String> cookieMap) {
            crawler.runConf.setCookieMap(cookieMap);
            return this;
        }

        /**
         * 请求Header
         *
         * @param headerMap
         * @return
         */
        public BtcInfoCrawlerImp.Builder setHeaderMap(Map<String, String> headerMap) {
            crawler.runConf.setHeaderMap(headerMap);
            return this;
        }

        /**
         * 请求UserAgent
         *
         * @param userAgents
         * @return
         */
        public BtcInfoCrawlerImp.Builder setUserAgent(String... userAgents) {
            if (userAgents != null && userAgents.length > 0) {
                for (String userAgent : userAgents) {
                    if (!crawler.runConf.getUserAgentList().contains(userAgent)) {
                        crawler.runConf.getUserAgentList().add(userAgent);
                    }
                }
            }
            return this;
        }

        /**
         * 请求Referrer
         *
         * @param referrer
         * @return
         */
        public BtcInfoCrawlerImp.Builder setReferrer(String referrer) {
            crawler.runConf.setReferrer(referrer);
            return this;
        }

        /**
         * 请求方式：true=POST请求、false=GET请求
         *
         * @param ifPost
         * @return
         */
        public BtcInfoCrawlerImp.Builder setIfPost(boolean ifPost) {
            crawler.runConf.setIfPost(ifPost);
            return this;
        }

        /**
         * 超时时间，毫秒
         *
         * @param timeoutMillis
         * @return
         */
        public BtcInfoCrawlerImp.Builder setTimeoutMillis(int timeoutMillis) {
            crawler.runConf.setTimeoutMillis(timeoutMillis);
            return this;
        }

        /**
         * 停顿时间，爬虫线程处理完页面之后进行主动停顿，避免过于频繁被拦截；
         *
         * @param pauseMillis
         * @return
         */
        public BtcInfoCrawlerImp.Builder setPauseMillis(int pauseMillis) {
            crawler.runConf.setPauseMillis(pauseMillis);
            return this;
        }

        /**
         * 代理生成器
         *
         * @param proxyMaker
         * @return
         */
        public BtcInfoCrawlerImp.Builder setProxyMaker(ProxyMaker proxyMaker) {
            crawler.runConf.setProxyMaker(proxyMaker);
            return this;
        }

        /**
         * 失败重试次数，大于零时生效
         *
         * @param failRetryCount
         * @return
         */
        public BtcInfoCrawlerImp.Builder setFailRetryCount(int failRetryCount) {
            if (failRetryCount > 0) {
                crawler.runConf.setFailRetryCount(failRetryCount);
            }
            return this;
        }

        /**
         * 爬虫并发线程数
         *
         * @param threadCount
         * @return
         */
        public BtcInfoCrawlerImp.Builder setThreadCount(int threadCount) {
            crawler.threadCount = threadCount;
            return this;
        }

        /**
         * 设置爬虫线程类
         *
         * @param threadClass
         * @return
         */
        public BtcInfoCrawlerImp.Builder setThreadClass(Class<? extends BtcInfoCrawlerThread> threadClass) {
            crawler.threadClass = threadClass;
            return this;
        }

        /**
         * 构建爬虫对象
         *
         * @return
         */
        public BtcInfoCrawler build() {
            return crawler;
        }
    }
}
