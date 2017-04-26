package stringcut.example;

import java.net.URLEncoder;

import com.github.abola.crawler.CrawlerPack;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Created by yellow on 2017/4/26.
 * nextPage尚未編輯
 */
public class TestFB1 {
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

            System.out.println(elems.child(0).text());

            Element eleTagged = elems.child(1);
            for (int i = 0; i < eleTagged.children().size() - 1; i++) {
                System.out.print(eleTagged.child(i).getElementsByTag("id").text() + ", ");
                System.out.print(eleTagged.child(i).getElementsByTag("story").text() + "\n");
            }

            System.out.println(elems.child(2).text());

            System.out.println(elems.child(3).text());

            System.out.println(elems.child(4).text());

            Element elePosts = elems.child(5);
            for (int i = 0; i < elePosts.children().size() - 1; i++) {
//                System.out.println(elePosts.child(i).children().size());

                for (int j = 0; j < elePosts.child(i).children().size(); j++) {

                    if (elePosts.child(i).child(j).children().size() == 0) {
                        System.out.print("posts1." + elePosts.child(i).child(j).tagName() + "/" + elePosts.child(i).child(j).text() + " , ");
                    } else {

                        for (int k = 0; k < elePosts.child(i).child(j).children().size(); k++) {

                            if (elePosts.child(i).child(j).child(k).children().size() == 0) {
                                System.out.print("posts2." + elePosts.child(i).child(j).child(k).tagName() + "/" + elePosts.child(i).child(j).child(k).text() + " , ");
                            } else {

                                for (int x = 0; x < elePosts.child(i).child(j).child(k).children().size(); x++) {

                                    if (elePosts.child(i).child(j).child(k).child(x).children().size() == 0) {
                                        System.out.print("posts3." + elePosts.child(i).child(j).child(k).child(x).tagName() + "/" + elePosts.child(i).child(j).child(k).child(x).text() + " , ");
                                    } else {

                                        for (int y = 0; y < elePosts.child(i).child(j).child(k).child(x).children().size(); y++) {

                                            if (elePosts.child(i).child(j).child(k).child(x).child(y).children().size() == 0) {
                                                System.out.print("posts4." + elePosts.child(i).child(j).child(k).child(x).child(y).tagName() + "/" + elePosts.child(i).child(j).child(k).child(x).child(y).text() + " , ");
                                            } else {

                                                System.out.print(elePosts.child(i).child(j).child(k).child(x).child(y).tagName() + "/" + elePosts.child(i).child(j).child(k).child(x).child(y).children().size() + ", ");
                                            }

                                        }


                                    }

                                }


                            }

                        }

                    }
                }

                System.out.println();
            }

//            System.out.println(elePosts.child(0).child(0).tagName());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
