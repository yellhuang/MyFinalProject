package connect.sqlserver.example;

import java.sql.*;

/**
 * Created by yellow on 2017/4/23.
 */
public class TestSelect {
    public static void main(String[] args) {

        // 建立連線
        try (
                Connection connection = DriverManager.getConnection("jdbc:sqlserver://localhost:1433;databaseName=DMDB", "sa", "yellow");
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("select * from [Weather-Norminal-C]")
        ) {
            // 產生Statement物件：執行SQL指令
            // 產生ResultSet物件
            // 	一筆一筆讀資料
            while (rs.next()) {
                System.out.println("ID.=" + rs.getInt("ID"));
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}





