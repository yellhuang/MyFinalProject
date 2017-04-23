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
 */
class UrcosmeToSqlServer {

    final static String byProductMainPage = "https://www.urcosme.com/find-product/42";
    final static String byProductIndexPage = "https://www.urcosme.com/find-product/42?page=%s";
    static List<String> productInfo = new ArrayList<>();

    public static void main(String[] argv) {

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

        // 個別分頁每一頁資訊
        for (String url : lastPostsLink) {
            try {
                analyzeFeed(url);
                Thread.sleep(150); // 重要：為什麼要有這一行？

            } catch (Exception e) {
            }
        }
    }

    /**
     * 分析防曬產品每一頁資訊
     *
     * @param url
     * @return
     */
    static void analyzeFeed(String url) {

        try {

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
            insertToSqlServer(productInfo);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 新增到資料庫
    static void insertToSqlServer(List<String> productInfo) {

        try (Connection conn = DriverManager
                .getConnection(
                        "jdbc:sqlserver://localhost:1433;databaseName=FinalProjectDB",
                        "sa", "yellow");
             PreparedStatement pstmt = conn.prepareStatement("insert into SunProtectionProduct (productName,seriesName,brandEnglishName,brandChineseName,capacity,unitPrice,perMlPrice,releaseDate,productAttribute,articleNumber,productID,urCosmeScore) "
                     + "values (?,?,?,?,?,?,?,?,?,?,?,?)")
        ) {
            int i = 1;
            for (String str2 : productInfo) {
                pstmt.setString(i, str2);
                i++;
            }

            pstmt.execute();
            pstmt.clearParameters();
            System.out.println("新增成功");

        } catch (SQLException e) {
            e.printStackTrace();
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