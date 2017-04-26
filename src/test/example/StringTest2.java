package test.example;

/**
 * Created by Student on 2017/4/24.
 */
public class StringTest2 {
    public static void main(String args[]){
        String str1 = "asxd";
        String str2 = "eyhj";
        String str3 = "qqqqq";

        String[] arrStr = {str1, str2, str3};
        String[] arrKey = {"as", "q"};

        for(String s1: arrStr){
            for(String s2: arrKey){
                if(s1.contains(s2)){
                    System.out.println(s1 + "包含" + s2);
                    break;
                }
            }
        }


    }
}
