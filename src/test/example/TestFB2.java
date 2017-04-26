package test.example;

import com.github.abola.crawler.CrawlerPack;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;

/**
 * Created by yellow on 2017/4/26.
 * nextPage已編輯
 */
public class TestFB2 {
    public static void main(String[] args) {

        String fanClubName = "PopDailyTW";
        String token = "EAACEdEose0cBAMLZCBa6bcCXZAkrJY9DnqY3iIhgnDcvuZAJD05ujL9uuJf1DXtQuMGFmbDUYioHqdnZAGLnRxK9CZAmZBHM0YrZAvD2I2jMbWtdDvkaZCS4uKBRBevB4DLQlUusUbwigVHQuLbBhf3285EvoTqfTa9SURxiZAzp7tcnZAIBqZBNUC7dVYLKayXMq8ZD";

        try {

            String uri =
                    "https://graph.facebook.com/v2.8"
                            + "/" + fanClubName + "?fields=talking_about_count,fan_count,category,"
                            + URLEncoder.encode("tagged{id,story}", "UTF-8") + ","
                            + URLEncoder.encode("posts{message,created_time,name,reactions{id,name,type},comments{created_time,from,message},message_tags}", "UTF-8")
                            + "&access_token=" + token;

            Document elems = CrawlerPack.start().getFromJson(uri);
//            System.out.println(elems.html());

            System.out.println("fan_count: " + elems.child(0).text());
            System.out.println("talking_about_count: " + elems.child(2).text());
            System.out.println("fan_club_id: " + elems.child(3).text());
            System.out.println("category: " + elems.child(4).text());

            System.out.println("==================================taggedId, taggedStory==================================");
            Element eleTagged = elems.child(1);
            int count = 0;
            Elements nextPage;
            while (true) {
                try {
                    for (int i = 0; i < eleTagged.children().size() - 1; i++) {
                        System.out.print(eleTagged.child(i).getElementsByTag("id").text() + ", ");
                        System.out.print(eleTagged.child(i).getElementsByTag("story").text() + "\n");
                        count++;
                    }

                    nextPage = eleTagged.getElementsByTag("next");
                    if (!nextPage.hasText())
                        break;
                    else {
                        Document elems2 = CrawlerPack.start().getFromJson(nextPage.text());
                        eleTagged = elems2;
                    }
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("已無資料");
                    break;
                }
            }

            System.out.println(count);


//            String uri1 = "https://graph.facebook.com/v2.8/445164788956922/tagged?format=json&fields=id,story&access_token=EAACEdEose0cBAHK2ZBZC5RTzC9irDDnewsPjAvhzogFS48I5rMmTaF2yz0ppLSZCgPE8jF3lnwxZC5xE3CKvzEIhNFmyxsAJFZCXsyWlTirb66PMRoaZAE1IER1g2oqSbW1lyvxStWhx2NHKfXuvekHhZCMKqaXv498DSVGsbXTFpGJ19KUffXJYVdZC6uT2BqwZD&limit=25&until=1492699780&__paging_token=enc_AdBpPUbt98qptXHCkiGQFv9qQOCWcd0BtGsetW9urDi0fbJcYpRk4SYJmNZCbzUJH3z0v6tTu4th3CUsDUTTNDTuMcN1kv6bqv3PEnmj4Nm7jZAwZDZD";
//
//            Document elems2 = CrawlerPack.start().getFromJson(uri1);
//
//
//            System.out.println(elems2.children().size());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
