package connect.sqlserver.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yellow on 2017/4/23.
 */
public class PreparedStatementBatchUpdate {
    static int count = 0;

    public static void main(String[] args) {

        List<String> Info = new ArrayList<>();
        List<List<String>> productInfo = new ArrayList<>();


        Info.add("productName1");
        Info.add("productName2");
        Info.add("productName3");
        Info.add("productName4");
        Info.add("productName5");
        Info.add("productName6");
        productInfo.add(Info);
        Info.clear();
        Info.add("Name1");
        Info.add("Name2");
        Info.add("Name3");
        Info.add("Name4");
        Info.add("Name5");
        Info.add("Name6");
        productInfo.add(Info);
//        Info.clear();
        insertToSqlServer(productInfo);

        productInfo.clear();

    }

    static void insertToSqlServer(List<List<String>> productInfo) {

        try (Connection conn = DriverManager
                .getConnection(
                        "jdbc:sqlserver://localhost:1433;databaseName=DMDB",
                        "sa", "yellow");
             PreparedStatement pstmt = conn.prepareStatement("insert into [Weather-Norminal-C] (ID,天氣,溫度,濕度,颳風,決策) "
                     + "values (?,?,?,?,?,?)")
        ) {

            for (List<String> str1 : productInfo) {
                int i = 1;
                for (String str2 : str1) {
                    pstmt.setString(i, str2);
                    i++;
                }
                pstmt.addBatch();
                pstmt.clearParameters();
            }

            pstmt.executeBatch();
            pstmt.clearBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

