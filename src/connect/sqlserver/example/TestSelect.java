package connect.sqlserver.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yellow on 2017/4/23.
 */
public class TestSelect {
    public static void main(String[] args) {

        List<String> fanclubList = new ArrayList<>();

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
//                System.out.println("ID.=" + rs.getString("ID"));
                fanclubList.add(rs.getString("ID"));
            }

            for(String str:fanclubList){
                System.out.println("ID.=" + str);
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}





