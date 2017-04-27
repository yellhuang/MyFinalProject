package crawler.example;

import com.github.abola.crawler.CrawlerPack;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yellow on 2017/4/27.
 * 爬FB粉絲團每篇文章底下的留言資訊
 */
public class FbCommentInfoToSqlServer {

    static List<String> postComentInfo = new ArrayList<>();
    static int count = 1;
    static List<String> fanclubList = new ArrayList<>();

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis(); // 開始執行時間
        String token = "118580478686560%7COf5Y7jVzJx_qbhiLKWc1v7qD9cM";

        loadFanClubId();

        for (String fanClubId : fanclubList) {
//            System.out.println("ID.=" + fanClubName);

            try {

                String uri =
                        "https://graph.facebook.com/v2.8"
                                + "/" + fanClubId + "?since=1490486400&until=1493337600&fields="
                                + URLEncoder.encode("posts{comments{created_time,from,message}}", "UTF-8")
                                + "&access_token=" + token;

                Document elems = CrawlerPack.start().getFromJson(uri);

                Elements elePosts = elems.child(1).children(); // 取<post>tag
                Elements nextPage;
                String postId;
                String commentName;
                String commentId;
                String commentTime;
                String commentMessage;
                String[] tempId;

                // 讀取每篇文章
                while (true) {

                    try {

                        for (int i = 0; i < elePosts.size() - 1; i++) {  // 取<post>_<data>tag (每頁最多顯示25篇)

                            // 取<post>_<data>_<comments>tag, <id>tag
                            Elements postsLev2 = elePosts.get(i).children();
                            if (postsLev2.size() == 1) { // 文章無人留言
                                postId = postsLev2.text();
//                                System.out.println(postId);
                            postComentInfo.clear();
                            postComentInfo.add(postId);
                            postComentInfo.add("");
                            postComentInfo.add("");
                            postComentInfo.add("");
                            postComentInfo.add("");
                            insertToSqlServer(postComentInfo);
                            count++;
                            } else { // 文章有人留言
                                postId = postsLev2.get(1).text();

                                // 取<post>_<data>_<comments>tag
                                Elements eleComments = postsLev2.get(0).children();
//                            System.out.println(eleComments.size()); // 看<comments>tag底下有幾個<data>tag
                                Elements nextPageComment;

                                while (true) {
                                    for (int j = 0; j < eleComments.size() - 1; j++) {  // 取<comments>_<data>tag (最多顯示25篇)
//                                System.out.println(eleComments.get(j).children().size());
//                                        System.out.print(postId + ", ");
                                        tempId = eleComments.get(j).getElementsByTag("id").text().split(" ");
                                        commentId = tempId[0];
                                        commentName = eleComments.get(j).getElementsByTag("name").text();
                                        commentMessage = filter(eleComments.get(j).getElementsByTag("message").text());
                                        commentTime = eleComments.get(j).getElementsByTag("created_time").text();
//                                        System.out.print(commentName + ", " + commentId + ", " + commentTime + ", " + commentMessage + "\n");
                                    postComentInfo.clear();
                                    postComentInfo.add(postId);
                                    postComentInfo.add(commentId);
                                    postComentInfo.add(commentName);
                                    postComentInfo.add(commentMessage);
                                    postComentInfo.add(commentTime);
                                    insertToSqlServer(postComentInfo);
                                    count++;
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
             PreparedStatement pstmt = conn.prepareStatement(" insert into FbPostsComments (postId, commentId, commentName, commentMessage, commentTime)"
                     + "values (?, ?, ?, ?, ?)")
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

    /**
     * 過濾表情符號???
     */
    static String filter(String str) {

        if (str.trim().isEmpty()) {
            return str;
        }
        String pattern = "[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]|[\uD83E\uDD23\uD83E\uDD23\uD83E\uDD23]";
        String reStr = "";
        Pattern emoji = Pattern.compile(pattern);
        Matcher emojiMatcher = emoji.matcher(str);
        str = emojiMatcher.replaceAll(reStr);
        return str;
    }
}
