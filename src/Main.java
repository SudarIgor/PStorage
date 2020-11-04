import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String string, strOpr;
        string = sc.nextLine();

        String [] termin = string.split(" - ");
        strOpr= termin [1];
        String [] opr = strOpr.split(", ");

        System.out.println(termin[0] + ":");

        for (String i :opr) {
            System.out.println("-" + i);
        }
    }




}

