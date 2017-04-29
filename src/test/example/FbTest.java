package test.example;

import com.github.abola.crawler.CrawlerPack;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yellow on 2017/4/27.
 * 爬FB粉絲團每篇文章底下的留言資訊
 */
public class FbTest {

    static List<String> fanclubList = new ArrayList<>();
    static List<String> msgTagInfo = new ArrayList<>();
    static int count = 1;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis(); // 開始執行時間

        String fanClubName = "PopDailyTW";
        String token = "118580478686560%7COf5Y7jVzJx_qbhiLKWc1v7qD9cM";

        try {

            String uri =
                    "https://graph.facebook.com/v2.8"
                            + "/" + fanClubName + "?since=1490486400&until=1493337600&fields="
                            + URLEncoder.encode("posts{message_tags}", "UTF-8")
                            + "&access_token=" + token;

            Document elems = CrawlerPack.start().getFromJson(uri);

            Elements elePosts = elems.child(1).children(); // 取<post>tag
//            System.out.println(elePosts.get(3).children().size());

            Elements nextPage;
            String postId;
            String msgTagName;
            String msgTagId;

            // 讀取每篇文章
            while (true) {

                try {

                    for (int i = 0; i < elePosts.size() - 1; i++) {  // 取<post>_<data>tag (每頁最多顯示25篇)

                        // 取<post>_<data>_<comments>tag, <id>tag
                        Elements postsLev2 = elePosts.get(i).children();

                        if (postsLev2.size() == 1) { // 不是轉PO別人的文章
                            postId = postsLev2.text();
                            System.out.println(postId);
//                            msgTagInfo.clear();
//                            msgTagInfo.add(postId);
//                            msgTagInfo.add("");
//                            msgTagInfo.add("");
//                            insertToSqlServer(msgTagInfo);
//                            count++;

                        } else { // 是轉PO別人的文章
                            postId = postsLev2.get(1).text();

                            // 取<post>_<data>_<comments>tag
                            Elements eleComments = postsLev2.get(0).children();

                            msgTagId = eleComments.get(1).text();
                            msgTagName = eleComments.get(3).text();

                            System.out.print(postId + ", ");
//
                            System.out.print(msgTagId + ", " + msgTagName + "\n");
//                            msgTagInfo.clear();
//                            msgTagInfo.add(postId);
//                            msgTagInfo.add(msgTagId);
//                            msgTagInfo.add(msgTagName);
//                            insertToSqlServer(msgTagInfo);
//                            count++;
                        }

                    }

                    // 文章換頁
                    nextPage = elePosts.last().getElementsByTag("next");
                    if (!nextPage.hasText())
                        break;
                    else {
                        Document elems2 = CrawlerPack.start().getFromJson(nextPage.text().replaceAll("\\|", "%7C"));
                        elePosts = elems2.children();
                    }

                } catch (IndexOutOfBoundsException e) {
                    System.out.println("已無資料");
                    break;
                }
            }

            System.out.println(count);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 顯示程式執行時間
        System.out.println("Using Time:" + (System.currentTimeMillis() - startTime) / 1000 / 60 / 60.0 + "hr");
    }

    /**
     * 從SQL Server載入粉絲團id
     */
    static void loadFanClubId() {

        // 建立連線
        try (
                Connection connection = DriverManager.getConnection("jdbc:sqlserver://localhost:1433;databaseName=testdb", "sa", "yellow");
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("select id from information")
        ) {
            // 產生Statement物件：執行SQL指令
            // 產生ResultSet物件
            // 	一筆一筆讀資料
            while (rs.next()) {
                fanclubList.add(rs.getString("ID"));
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * 把資料存到SQL Server
     */
    static void insertToSqlServer(List<String> postComentInfo) {

        try (Connection conn = DriverManager
                .getConnection(
                        "jdbc:sqlserver://localhost:1433;databaseName=FinalProjectDB",
                        "sa", "yellow");
             PreparedStatement pstmt = conn.prepareStatement(" insert into FbPostsMsgTag (postId, msgTagId, msgTagName)"
                     + "values (?, ?, ?)")
        ) {
            int i = 1;
            for (String str2 : postComentInfo) {
                pstmt.setString(i, str2);
                i++;
            }

            pstmt.execute();
            pstmt.clearParameters();
            System.out.println("第 " + count + " 筆新增成功");
            count++;

        } catch (SQLException e) {
            e.printStackTrace();
            for (String str2 : postComentInfo) {
                System.out.print(str2 + ", ");
            }
            System.out.println();
        }
    }

}