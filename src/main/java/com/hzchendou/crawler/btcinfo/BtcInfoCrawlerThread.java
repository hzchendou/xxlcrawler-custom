package com.hzchendou.crawler.btcinfo;

/**
 * Btcinfo自定义爬虫线程.
 *
 * @author chendou
 * @date 2018/5/20
 * @since 1.0
 */
public abstract class BtcInfoCrawlerThread implements Runnable {

    /**
     * 爬虫对象
     */
    protected BtcInfoCrawler crawler;

    BtcInfoCrawlerThread(BtcInfoCrawler crawler) {
        this.crawler = crawler;
    }


    /**
     * 尝试停止爬虫
     */
    public abstract void toStop();

    /**
     * 是否正在运行中
     *
     * @return
     */
    public abstract boolean isRunning();
}
