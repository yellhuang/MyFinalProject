package crawler.example;

import com.github.abola.crawler.CrawlerPack;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by yellow on 2017/4/23.
 */
class PttToSqlServer {
    static List<String> articleInfo = new ArrayList<>();
    static int count = 1;
    final static String board = "MakeUp";
    final static String pttMainPage = "https://www.ptt.cc/bbs/" + board + "/index.html";
    final static String pttIndexPage = "https://www.ptt.cc/bbs/" + board + "/index%s.html";

    // 取得最後幾篇的文章數量(bug?)
    static Integer loadLastPosts = 11380;

    public static void main(String[] argv) {
        long startTime = System.currentTimeMillis();


        // 取得前一頁的index
//        data sample
//        ---
//        <div class="action-bar">
//            <div class="btn-group btn-group-dir">
//                <a class="btn selected" href="/bbs/Gossiping/index.html">看板</a>
//                <a class="btn" href="/man/Gossiping/index.html">精華區</a>
//            </div>
//            <div class="btn-group btn-group-paging">
//                <a class="btn wide" href="/bbs/Gossiping/index1.html">最舊</a>
//                <a class="btn wide" href="/bbs/Gossiping/index14940.html">‹ 上頁</a>
//                <a class="btn wide disabled">下頁 ›</a>
//                <a class="btn wide" href="/bbs/Gossiping/index.html">最新</a>
//            </div>
//        </div>
        String prevPage =
                CrawlerPack.start()
//                .addCookie("over18","1")                // 八卦版進入需要設定cookie
                        .getFromHtml(pttMainPage)               // 遠端資料格式為 HTML
                        .select(".action-bar a:matchesOwn(上頁)")  // 取得右上角『上頁』的內容
                        .get(0).attr("href")
                        .replaceAll("/bbs/" + board + "/index([0-9]+).html", "$1");


        // 目前最末頁 index 編號
        Integer lastPage = Integer.valueOf(prevPage) + 1;
//        System.out.println(lastPage);
        List<String> lastPostsLink = new ArrayList<>();

        while (lastPostsLink.size() < loadLastPosts) {

            String currPage = String.format(pttIndexPage, lastPage--);

            Elements links =
                    CrawlerPack.start()
//                    .addCookie("over18", "1")
                            .getFromHtml(currPage)
                            .select(".title > a:matchesOwn(^(?!\\[公告))");

            for (Element link : links) lastPostsLink.add(link.attr("href"));
        }

        System.out.println("作者,標題,推總數,不重複推,噓總數,不重複噓,→總數,不重複→,總參與人數");

        // 個別分頁每一頁資訊
        for (String url : lastPostsLink) {

            try {
                analyzeFeed(url);
                Thread.sleep(150); // 重要：為什麼要有這一行？

            } catch (Exception e) {
            }
        }

        System.out.println("Using Time:" + (System.currentTimeMillis() - startTime)/1000/60/60.0 + "hr");
    }

    /**
     * 分析輸入的文章，簡易統計
     *
     * @param url
     * @return
     */
    static void analyzeFeed(String url) {

        try {

            // 取得 Jsoup 物件，稍後要做多次 select
            Document feed =
                    CrawlerPack.start()
//                            .addCookie("over18", "1")                // 八卦版進入需要設定cookie
                            .getFromHtml("https://www.ptt.cc" + url);           // 遠端資料格式為 HTML




            // 1. 文章作者
            String feedAuthor = feed.select("span:contains(作者) + span").text();

            // 2. 文章標題
            String feedTitle = feed.select("span:contains(標題) + span").text();


            // 3. 按推總數
            Integer feedLikeCount =
                    countReply(feed.select(".push-tag:matchesOwn(推) + .push-userid"));

            // 4. 不重複推文數
            Integer feedLikeCountNoRep =
                    countReplyNoRepeat(feed.select(".push-tag:matchesOwn(推) + .push-userid"));

            // 5. 按噓總數
            Integer feedUnLikeCount =
                    countReply(feed.select(".push-tag:matchesOwn(噓) + .push-userid"));

            // 6. 不重複噓文數
            Integer feedUnLikeCountNoRep =
                    countReplyNoRepeat(feed.select(".push-tag:matchesOwn(噓) + .push-userid"));

            // 5. 按→總數
            Integer feedArrowCount =
                    countReply(feed.select(".push-tag:matchesOwn(→) + .push-userid"));

            // 6. 不重複→數
            Integer feedArrowCountNoRep =
                    countReplyNoRepeat(feed.select(".push-tag:matchesOwn(→) + .push-userid"));

            // 7. 總參與人數
            Integer feedReplyCountNoRep =
                    countReplyNoRepeat(feed.select(".push-tag + .push-userid"));

            // 發文時間
            String feedTime =
                    feed.select(".bbs-screen.bbs-content div:eq(3) span:eq(1)").text();

            // 推 內容
            String feedLikeContent =
                    feed.select(".push-tag:matchesOwn(推)+span+span").text();

            // → 內容
            String feedArrowContent =
                    feed.select(".push-tag:matchesOwn(→)+span+span").text();
            // 噓 內容
            String feedUnLikeContent =
                    feed.select(".push-tag:matchesOwn(噓)+span+span").text();

            // 取內文
            Elements feedContentEle = feed.select("#main-content.bbs-screen.bbs-content");
            feedContentEle.select("div,span,a").remove();
            String feedContent = feedContentEle.text();

            articleInfo.clear();
            articleInfo.add(board);
            articleInfo.add(feedAuthor);
            articleInfo.add(feedTitle);
            articleInfo.add(Integer.toString(feedLikeCount));
            articleInfo.add(Integer.toString(feedLikeCountNoRep));
            articleInfo.add(Integer.toString(feedUnLikeCount));
            articleInfo.add(Integer.toString(feedUnLikeCountNoRep));
            articleInfo.add(Integer.toString(feedArrowCount));
            articleInfo.add(Integer.toString(feedArrowCountNoRep));
            articleInfo.add(Integer.toString(feedReplyCountNoRep));
            articleInfo.add(feedTime);
            articleInfo.add(feedContent);
            articleInfo.add(feedLikeContent);
            articleInfo.add(feedArrowContent);
            articleInfo.add(feedUnLikeContent);
            insertToSqlServer(articleInfo);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    static void insertToSqlServer(List<String> productInfo) {

        try (Connection conn = DriverManager
                .getConnection(
                        "jdbc:sqlserver://localhost:1433;databaseName=FinalProjectDB",
                        "sa", "yellow");
             PreparedStatement pstmt = conn.prepareStatement(" insert into PttArticle (board, author, title, likeCount, likeCountNoRep, unLikeCount, " +
                     "unLikeCountNoRep, arrowCount, arrowCountNoRep, replyCountNoRep, feedTime," +
                     "feedContent, feedLikeContent, feedArrowContent, feedUnLikeContent)"
                     + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        ) {
            int i = 1;
            for (String str2 : productInfo) {
                pstmt.setString(i, str2);
                i++;
            }

            pstmt.execute();
            pstmt.clearParameters();
            System.out.println("第 "+ count +" 筆新增成功");
            count++;

        } catch (SQLException e) {
            e.printStackTrace();
            for (String str2 : productInfo) {
                System.out.print(str2 + ", ");
            }
            System.out.println();
        }
    }

    /**
     * 推文人數總計
     *
     * @param reply
     * @return
     */
    static Integer countReply(Elements reply) {
        return reply.text().split(" ").length;
    }

    /**
     * 推文人數總計
     *
     * @param reply
     * @return
     */
    static Integer countReplyNoRepeat(Elements reply) {
        return new HashSet<String>(
                Arrays.asList(
                        reply.text().split(" ")
                )
        ).size();
    }

}


