import java.sql.*;

public class App {
    public static void main(String[] args) {
        Connection connection = null;
        Statement statement = null;
        try {
            // 載入 MySQL JDBC 驅動
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 連接到 MySQL 伺服器，無需指定資料庫
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/?useSSL=false", "root", "0000");

            // 創建 Statement 物件
            statement = connection.createStatement();

            // 創建資料庫（如果不存在）
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS baseballJavaGame");

            // 切換到 ctos 資料庫
            statement.executeUpdate("USE baseballJavaGame");

            // 創建 hie 表
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS hitter (team int , hid int , name VARCHAR(50), RL VARCHAR(50), POWER INT , CONTACT INT)");

            // 插入資料
            statement.executeUpdate("INSERT INTO hitter (name, RL, POWER, CONTACT) VALUES ('劉基鴻', 'R', 87 , 60 )");

            // 查詢資料
            ResultSet resultSet = statement.executeQuery("SELECT * FROM hitter");

            // 打印結果
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String type = resultSet.getString("RL");
                int age = resultSet.getInt("POWER");
                System.out.println("名稱: " + name + ", 型號: " + type + ", 年齡: " + age);
            }

        } catch (ClassNotFoundException e) {
            System.out.println("MySQL 驅動未找到: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("資料庫錯誤: " + e.getMessage());
        } finally {
            // 關閉資源
            try {
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                System.out.println("關閉資源時發生錯誤: " + e.getMessage());
            }
        }
    }
}