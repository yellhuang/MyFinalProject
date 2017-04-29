package crawler.example;

import com.github.abola.crawler.CrawlerPack;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by yellow on 2017/4/27.
 * 爬FB粉絲團每篇文章底下的留言資訊並更新置資料庫
 */
public class FbCommentInfoToSqlServer {

    static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    static String postStartTime = "2017-03-28";

    static List<String> fanclubList = new ArrayList<>();
    static List<String> postComentInfo = new ArrayList<>();

    static int countSuccess = 0;
    static int countFail = 0;
    static boolean connServerInsert = false;


    public static void main(String[] args) {

        long startTime = System.currentTimeMillis(); // 開始執行時間
        String token = "118580478686560%7COf5Y7jVzJx_qbhiLKWc1v7qD9cM"; // App Token

        loadFanClubId(); // 載入所有粉絲團ID
//        fanclubList.add("UrCosme");

        // 連接資料庫
        try (Connection conn = DriverManager
                .getConnection(
                        "jdbc:sqlserver://localhost:1433;databaseName=FinalProjectDB",
                        "sa", "sa123456");
             PreparedStatement pstmt = conn.prepareStatement(" insert into FbPostsComments (postId, commentId, commentName, commentMessage, commentTime)"
                     + "values (?, ?, ?, ?, ?)")
        ) {

            // 一筆一筆取粉絲團ID
            for (String fanClubId : fanclubList) {
//            System.out.println("ID.=" + fanClubName);

                try {

                    // Facebook API 查詢條件
                    String uri =
                            "https://graph.facebook.com/v2.8"
                                    + "/" + fanClubId + "?fields="
                                    + URLEncoder.encode("posts{created_time,comments{created_time,from,message}}", "UTF-8")
                                    + "&access_token=" + token;

                    Document elems = CrawlerPack.start().getFromJson(uri);

                    Elements elePosts = elems.child(1).children(); // 取<post>tag
                    Elements nextPage;
                    String postId;
                    String postCreateTime;
                    String commentName;
                    String commentId;
                    String commentTime;
                    String commentMessage;
                    String[] tempId;

                    OuterLoop:
                    while (true) { // 讀取每篇文章

                        try {

                            for (int i = 0; i < elePosts.size() - 1; i++) {  // 一筆一筆取取<post>_<data>tag (每頁最多顯示25篇)

                                // 取<post>_<data>_<created_time>tag、<comments>tag, <id>tag
                                Elements postsLev2 = elePosts.get(i).children();


                                postCreateTime = formatTime(postsLev2.get(0).text());
                                // 比對文章時間是否晚於設定抓取的開始時間
                                if (timeCompare(postCreateTime, postStartTime)) {

                                    // 判斷文章有沒有人留言
                                    if (postsLev2.size() == 2) { // 無人留言

                                        postId = postsLev2.get(1).text(); // <id>tag

                                        // 判斷文章是否還存在
                                        if (checkPostId(postId)) {
//                                    System.out.println(postCreateTime + ", " + postId);
                                            postComentInfo.clear();
                                            postComentInfo.add(postId);
                                            postComentInfo.add("");
                                            postComentInfo.add("");
                                            postComentInfo.add("");
                                            postComentInfo.add("");
                                            insertToSqlServer(postComentInfo, pstmt);
                                        }
//
                                    } else { // 有人留言

                                        postId = postsLev2.get(2).text(); // <id>tag
                                        if (checkPostId(postId)) {
                                            // 取<post>_<data>_<comments>tag
                                            Elements eleComments = postsLev2.get(1).children();
//                            System.out.println(eleComments.size()); // 看<comments>tag底下有幾個<data>tag
                                            Elements nextPageComment;

                                            while (true) {

                                                for (int j = 0; j < eleComments.size() - 1; j++) {  // 一筆一筆取<comments>_<data>tag (最多顯示25篇)

                                                    tempId = eleComments.get(j).getElementsByTag("id").text().split(" ");
                                                    commentId = tempId[0];
                                                    commentName = eleComments.get(j).getElementsByTag("name").text();
                                                    commentMessage = eleComments.get(j).getElementsByTag("message").text();
                                                    commentTime = formatTime(eleComments.get(j).getElementsByTag("created_time").text());
//                                            System.out.print(postCreateTime + ", " + postId + ", " + commentName + ", " + commentId + ", " + commentTime + ", " + commentMessage + "\n");
                                                    postComentInfo.clear();
                                                    postComentInfo.add(postId);
                                                    postComentInfo.add(commentId);
                                                    postComentInfo.add(commentName);
                                                    postComentInfo.add(commentMessage);
                                                    postComentInfo.add(commentTime);
                                                    insertToSqlServer(postComentInfo, pstmt);
                                                }

                                                // Comment換頁
                                                nextPageComment = eleComments.last().getElementsByTag("next");
                                                if (!nextPageComment.hasText())
                                                    break;
                                                else {
                                                    Document elems3 = CrawlerPack.start().getFromJson(nextPageComment.text().replaceAll("\\|", "%7C"));
                                                    eleComments = elems3.children();
                                                }

                                            }
                                        }
                                    }

                                } else
                                    break OuterLoop;

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

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    countFail++;
                }

                connServerInsert = true;
            }

            pstmt.executeBatch();
            pstmt.clearBatch();

        } catch (SQLException e) {
            e.printStackTrace();
            countFail++;
        }

        // 顯示程式執行時間
        System.out.println("Using Time:" + (System.currentTimeMillis() - startTime) / 1000 / 60 / 60.0 + "hr");
        // 顯示執行失敗筆數
        System.out.println("countFail:" + countFail);
    }

    /**
     * 從SQL Server載入粉絲團id
     */
    static void loadFanClubId() {
        // 建立連線
        try (
                Connection connection = DriverManager.getConnection("jdbc:sqlserver://localhost:1433;databaseName=FinalProjectDB", "sa", "sa123456");
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("select fanClubId from FbFanClub")
        ) {
            // 產生Statement物件：執行SQL指令
            // 產生ResultSet物件
            // 	一筆一筆讀資料
            while (rs.next()) {
                fanclubList.add(rs.getString("fanClubId"));
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            countFail++;
        }

    }

    /**
     * 把資料存到SQL Server
     */
    static void insertToSqlServer(List<String> postMsgTagInfo, PreparedStatement pstmt) {

        int i = 1;

        try {

            for (String str2 : postMsgTagInfo) {
                pstmt.setString(i, str2);
                i++;
            }

            pstmt.addBatch();
            pstmt.clearParameters();

            if (connServerInsert) {
                pstmt.executeBatch();
                pstmt.clearBatch();
                connServerInsert = false;
            }

            countSuccess++;
            System.out.println("第 " + countSuccess + " 筆新增成功");

        } catch (SQLException e) {
            e.printStackTrace();
            countFail++;
        }

    }

    /**
     * 時間資料清理
     */
    static String formatTime(String time) {
        return time.substring(0, 10);
    }

    /**
     * 比對文章時間是否晚於設定的開始時間
     */
    static Boolean timeCompare(String postTime, String startTime) {
        Boolean result = true;

        try {
            java.util.Date post = df.parse(postTime);
            java.util.Date start = df.parse(startTime);
            result = post.compareTo(start) >= 0 ? true : false;

        } catch (ParseException e) {
            e.printStackTrace();
            countFail++;
        }

        return result;
    }

    /**
     * 檢查文章ID是否有效
     */
    static Boolean checkPostId(String postId) {
        String regex = "[0-9_]+";
        return postId.matches(regex);

    }

}
