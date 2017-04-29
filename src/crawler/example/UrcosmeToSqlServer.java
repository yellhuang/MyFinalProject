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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yellow on 2017/4/23.
 * 爬網頁資訊並批次更新至資料庫
 */
class UrcosmeToSqlServer {

    final static String byProductMainPage = "https://www.urcosme.com/find-product/42";
    final static String byProductIndexPage = "https://www.urcosme.com/find-product/42?page=%s";
    static List<String> productInfo = new ArrayList<>();

    static int countSuccess = 0;
    static int countFail = 0;
    static boolean connServerInsert = false;

    public static void main(String[] argv) {

        long startTime = System.currentTimeMillis(); // 開始執行時間

        // 防曬產品總數量
        Integer loadTotalProduct = Integer.valueOf(getNumbers(CrawlerPack.start()
                .getFromHtml(byProductMainPage)
                .select(".product-desc")
                .text()));

        String nextPage =
                CrawlerPack.start()
                        .getFromHtml(byProductMainPage)               // 遠端資料格式為 HTML
                        .select(".pagination a:matchesOwn(下一頁)")  // 取得『下一頁』的內容
                        .get(0).attr("href")
                        .replaceAll("/find-product/42\\?page=([0-9]+)", "$1");
        // 目前最新頁 index 編號
        Integer prePage = Integer.valueOf(nextPage) - 1;
//        System.out.println(prePage + "," + nextPage);

        List<String> lastPostsLink = new ArrayList<>();
        while (lastPostsLink.size() < loadTotalProduct) {
            String currPage = String.format(byProductIndexPage, prePage++);
            Elements links =
                    CrawlerPack.start()
                            .getFromHtml(currPage)
                            .select(".item-info > a");
            for (Element link : links) lastPostsLink.add(link.attr("href"));
        }

        System.out.println("產品名稱, 系列, 品牌(英文), 品牌(中文), 容量, 價格, 平均每ml價格, 上市日期, 產品屬性, 使用心得篇數," +
                "產品編號, UrCosme指數");

        // 連接資料庫
        try (Connection conn = DriverManager
                .getConnection(
                        "jdbc:sqlserver://localhost:1433;databaseName=FinalProjectDB",
                        "sa", "sa123456");
             PreparedStatement pstmt = conn.prepareStatement("insert into UrcSunRelateItems (productName,seriesName,brandEnglishName,brandChineseName,capacity,unitPrice,perMlPrice,releaseDate,productAttribute,articleNumber,productID,urCosmeScore) "
                     + "values (?,?,?,?,?,?,?,?,?,?,?,?)")
        ) {

            // 分析防曬產品每一頁資訊
            for (String url : lastPostsLink) {

                // 取得 Jsoup 物件，稍後要做多次 select
                Document feed =
                        CrawlerPack.start()
                                .getFromHtml("https://www.urcosme.com" + url); // 遠端資料格式為 HTML

                // 1. 產品名稱
                String productName = feed.select(".info-tbl div:eq(0) h1").text();

                // 2. 系列
                Elements seriesEle = feed.select(".info-tbl div:eq(1) .val");
                seriesEle.select("div,a").remove();
                String seriesName = seriesEle.text();

                // 3. 品牌
                Elements brandEle = feed.select(".info-tbl div:eq(2) .val");
                brandEle.select("div,a,br").remove();
                String[] productNameArray = cutProductName(brandEle.text()).split(",");
                String brandEnglishName = productNameArray[0];
                String brandChineseName = productNameArray.length == 2 ? productNameArray[1] : "";

                // 4. 容量
                String capacity = feed.select(".info-tbl div:eq(3) span").text();

                // 5. 單價
                String unitPrice = feed.select(".info-tbl div:eq(4) span").text();

                // 6. 平均每ml價格
                Float perMlPrice;
                if (calculator(unitPrice, capacity) != 0) {
                    perMlPrice = capacity.contains("/") ?
                            minAva(unitPrice, capacity) : calculator(unitPrice, capacity);
                } else {
                    perMlPrice = 0.0f;
                }

                // 7. 上市日期
                String releaseDate = feed.select(".info-tbl div:eq(5) span").text();

                // 8. 上市日期
                String productAttribute = feed.select(".info-tbl .val a:eq(0)").text();

                // 9. 使用心得篇數
                String articleNumber = getNumbers(
                        feed.select(".menu-item.v-align-middle.menu-item-current a").text());

                // 10. 產品編號
                Elements productIDEle = feed.select(".menu-item.v-align-middle.menu-item-current a");
                String productID = getNumbers(productIDEle.get(0).attr("href"));

                // 11. UrCosme指數
                String urCosmeScore = feed.select(".deg .text").text().substring(10, 13);

//            String output = productName + ", "
//                    + seriesName + ", "
//                    + brandEnglishName + ", "
//                    + brandChineseName + ", "
//                    + capacity + ", "
//                    + unitPrice + ", "
//                    + perMlPrice + ", "
//                    + releaseDate + ", "
//                    + productAttribute + ", "
//                    + articleNumber + ", "
//                    + productID + ", "
//                    + urCosmeScore;
//            System.out.println(output);


                productInfo.clear();
                productInfo.add(productName);
                productInfo.add(seriesName);
                productInfo.add(brandEnglishName);
                productInfo.add(brandChineseName);
                productInfo.add(capacity);
                productInfo.add(unitPrice);
                productInfo.add(Float.toString(perMlPrice));
                productInfo.add(releaseDate);
                productInfo.add(productAttribute);
                productInfo.add(articleNumber);
                productInfo.add(productID);
                productInfo.add(urCosmeScore);
                insertToSqlServer(productInfo, pstmt);

                if (countSuccess % 100 == 0)
                    connServerInsert = true;

                Thread.sleep(150); // 睡一下

            }

            pstmt.executeBatch();
            pstmt.clearBatch();

        } catch (Exception e) {
            countFail++;
        }

        // 顯示程式執行時間
        System.out.println("Using Time:" + (System.currentTimeMillis() - startTime) / 1000 / 60 / 60.0 + "hr");
        // 顯示執行失敗筆數
        System.out.println("countFail:" + countFail);
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

    //　取字串中的數字
    static String getNumbers(String content) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            return matcher.group(0);
        }
        return "";
    }

    // 單價/容量
    static Float calculator(String unitPrice, String capacity) {
        String u = getNumbers(unitPrice);
        String c = getNumbers(capacity);
        if (u != "" && c != "") {
            return (Float.valueOf(u) / Float.valueOf(getNumbers(c)));
        } else {
            return 0.0f;
        }
    }

    // 切割兩個單價/兩個容量
    static Float minAva(String unitPrice, String capacity) {
        String[] arrayUC = new String[4];
        int index = 0;
        Float min;

        for (String i : unitPrice.split("/")) {
            arrayUC[index] = getNumbers(i);
            index++;
        }
        for (String i : capacity.split("/")) {
            arrayUC[index] = getNumbers(i);
            index++;
        }

        min = calculator(arrayUC[0], arrayUC[2]) < calculator(arrayUC[1], arrayUC[3]) ?
                calculator(arrayUC[0], arrayUC[2]) : calculator(arrayUC[1], arrayUC[3]);

        return min;
    }

    // 分割中文字及英文字
    static String cutProductName(String productName) {
        String chineseName = "";
        String englishName = "";
        for (int i = 0; i < productName.length(); i++) {
            String test = productName.substring(i, i + 1);
            if (test.matches("[\\u4E00-\\u9FA5]+")) {
                chineseName += test;
            } else {
                englishName += test;
            }
        }
        return (englishName + "," + chineseName);
    }
}