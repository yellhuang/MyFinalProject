package connect.sqlserver.example;

import java.sql.*;

/**
 * Created by yellow on 2017/4/29.
 */
public class TestUpdate {


    public static void main(String[] args) {

        try (Connection conn = DriverManager
                .getConnection(
                        "jdbc:sqlserver://localhost:1433;databaseName=FinalProjectDB",
                        "sa", "sa123456");
             PreparedStatement pstmt = conn.prepareStatement("update TestClubId set fanClubCount=?,fanClubTAC=? where fanClubId=?");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("select * from TestClubId")
        ) {

            int count = 0;

            while (rs.next()) {
                String id = rs.getString("fanClubId");
                if (id.equals("149297548871             ") || id.equals("204343726270500          ")) {
                    pstmt.setString(1, "aaaaaaaaaaaaaaaa");
                    pstmt.setString(2, "bb");
                    pstmt.setString(3, id);
                    //加入批次
                    pstmt.addBatch();
                    count++;
                    //pstmt.executeUpdate();
                    pstmt.clearParameters();
                }

                //每隔20筆送資料庫一次
                if (count % 20 == 0) {
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                }
            }
            //可能有剩下的,必須再送一次
            pstmt.executeBatch();
            pstmt.clearBatch();

            System.out.println("PreparedStatementBatchUpdateLab finished");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
