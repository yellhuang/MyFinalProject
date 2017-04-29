package crawler.example;

import com.github.abola.crawler.CrawlerPack;
import org.jsoup.nodes.Document;
import java.sql.*;

/**
 * Created by yellow on 2017/4/29.
 * 更新粉絲團按讚總數及活躍用戶數
 */
public class FbFanClubInfoUpdate {

    public static void main(String[] args) {

        String token = "118580478686560%7COf5Y7jVzJx_qbhiLKWc1v7qD9cM"; // App Token

        // 連接資料庫更新粉絲團按讚總數及活躍用戶數
        try (Connection conn = DriverManager
                .getConnection(
                        "jdbc:sqlserver://localhost:1433;databaseName=FinalProjectDB",
                        "sa", "sa123456");
             PreparedStatement pstmt = conn.prepareStatement("update FbFanClub set fanTAC=?,fanCount=? where fanClubId=?");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("select * from FbFanClub")
        ) {

            int count = 0;

            while (rs.next()) {
                String fanClubId = rs.getString("fanClubId");


                // Facebook API 查詢條件
                String uri =
                        "https://graph.facebook.com/v2.8"
                                + "/" + fanClubId.trim() + "?fields=talking_about_count,fan_count"
                                + "&access_token=" + token;

                Document elems = CrawlerPack.start().getFromJson(uri);

                String fanCount = elems.children().get(0).text(); // talking_about_count
                String fanTAC = elems.children().get(1).text(); // fan_count

//                System.out.println(fanCount + "," + fanTAC);

                pstmt.setString(1, fanTAC);
                pstmt.setString(2, fanCount);
                pstmt.setString(3, fanClubId);
                pstmt.addBatch();
                pstmt.clearParameters();

                count++;

                //每隔20筆送資料庫一次
                if (count % 50 == 0) {
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                }

                System.out.println("第 " + count + " 筆更新成功");
            }

            //可能有剩下的,必須再送一次
            pstmt.executeBatch();
            pstmt.clearBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
