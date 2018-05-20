package com.hzchendou.crawler.btcinfo;

import com.xuxueli.crawler.model.RunConf;
import com.xuxueli.crawler.rundata.RunData;

/**
 * btcinfo自定义爬虫.
 *
 * @author chendou
 * @date 2018/5/20
 * @since 1.0
 */
public interface BtcInfoCrawler {

    /**
     * 获取运行数据
     *
     * @return
     */
    RunData getRunData();

    /**
     * 获取运行配置
     *
     * @return
     */
    RunConf getRunConf();

    /**
     * 启动
     *
     * @param sync true=同步方式、false=异步方式
     */
    void start(boolean sync);

    /**
     * 尝试终止
     */
    void tryFinish();

    /**
     * 终止
     */
    void stop();
}
