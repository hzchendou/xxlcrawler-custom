package com.hzchendou.crawler;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.hzchendou.crawler.btcinfo.BtcInfoCrawler;
import com.hzchendou.crawler.btcinfo.BtcInfoCrawlerImp;
import com.hzchendou.crawler.btcinfo.BtcInfoCrawlerThreadImp;
import com.hzchendou.crawler.btcinfo.annotation.BtcinfoPageSelect;
import com.xuxueli.crawler.annotation.PageFieldSelect;
import com.xuxueli.crawler.conf.XxlCrawlerConf;
import com.xuxueli.crawler.parser.PageParser;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        BtcInfoCrawler crawler =
                new BtcInfoCrawlerImp.Builder().setUrls("https://www.feixiaohao.com/notice/list_1.html")
                        .setWhiteUrlRegexs("https://www\\.feixiaohao\\.com/notice/list_\\d+\\.html").setThreadCount(1)
                        .setThreadClass(BtcInfoCrawlerThreadImp.class).setAllowSpread(Boolean.FALSE).setUserAgent(null)
                        .setPageParser(new PageParser<PageVo>() {
                            @Override
                            public void parse(Document html, Element pageVoElement, PageVo pageVo) {
                                // 解析封装 PageVo 对象
                                String pageUrl = html.baseUri();
                                System.out.println(pageUrl + "：" + pageVo.toString());
                            }
                        }).build();
        crawler.start(false);
    }

    /**
     * PageSelect 注解：从页面中抽取出一个或多个VO对象；
     */
    @BtcinfoPageSelect(cssQuery = "body > div.w1200 > div > div > div.boxContain > ul")
    public static class PageVo {

        @BtcinfoPageSelect(cssQuery = "li")
        List<FeiXiaoHaoNoticeVo> feiXiaoHaoNoticeVos;

        public List<FeiXiaoHaoNoticeVo> getFeiXiaoHaoNoticeVos() {
            return feiXiaoHaoNoticeVos;
        }

        public void setFeiXiaoHaoNoticeVos(List<FeiXiaoHaoNoticeVo> feiXiaoHaoNoticeVos) {
            this.feiXiaoHaoNoticeVos = feiXiaoHaoNoticeVos;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("PageVo{");
            sb.append("feiXiaoHaoNoticeVos=").append(feiXiaoHaoNoticeVos);
            sb.append('}');
            return sb.toString();
        }
    }


    /**
     * 公告对象
     */
    public static class FeiXiaoHaoNoticeVo {

        /**
         * 公告标题
         */
        @PageFieldSelect(cssQuery = "img", selectType = XxlCrawlerConf.SelectType.ATTR, selectVal = "alt")
        private String title;

        /**
         * 公告来源
         */
        @PageFieldSelect(cssQuery = "a.web", selectType = XxlCrawlerConf.SelectType.TEXT)
        private String source;

        /**
         * 公告链接地址
         */
        @PageFieldSelect(cssQuery = "a.tit", selectType = XxlCrawlerConf.SelectType.ATTR, selectVal = "href")
        private String url;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("FeiXiaoHaoNoticeVo{");
            sb.append("title='").append(title).append('\'');
            sb.append(", source='").append(source).append('\'');
            sb.append(", url='").append(url).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
