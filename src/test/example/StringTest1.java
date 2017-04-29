package test.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yellow on 2017/4/19.
 * 字串處理
 */

class StringTest1 {

    public static void main(String[] args) throws Exception {
        String capacity = "150ml";
        String unitPrice = "NT $1000";
        String str1 = "說6.1說";
        System.out.println(calculator(unitPrice, capacity));
        System.out.println(getNumbers(str1));

        String str2 = "NT$ 300/550";
        String str3 = "80ml/150ml";
        System.out.println(minAva(str2, str3));

        String str4 = "300550";
        System.out.println(str4.contains("/"));

        String str5 = "天天";
        String[] productNameArray = cutProductName(str5).split(",");
        for (int i=0; i < productNameArray.length; i++) {
            System.out.println(productNameArray[i]);
        }

        List<String> collector = new ArrayList<>();
        collector.add("A");
        collector.add("B");
        collector.add("");
        collector.add("C");
//        System.out.println(collector.size()); // 4
        for (String i : collector) {
            System.out.println(i);
        }
        collector.clear();
        System.out.println(collector.size());

        String  str6 = "   天 天 天     ";
        System.out.println(str6.trim());
    }

    // 分割中文字及英文字
    static String cutProductName(String productName) {
        String chineseName = "";
        String englishName = "";
        for (int i = 0; i < productName.length(); i++) {
            String test = productName.substring(i, i + 1);
            if (test.matches("[\\u4E00-\\u9FA5]+")) {
                englishName += test;
            } else {
                chineseName += test;
            }
        }
        return (chineseName + "," + englishName);
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

    // 單價/容量
    static Float calculator(String unitPrice, String capacity) {
        return (Float.valueOf(getNumbers(unitPrice)) / Float.valueOf(getNumbers(capacity)));
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

}


