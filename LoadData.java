import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

class LoadData {

    private static File obj = new File("dataFile/data.csv");
    private static List<Double[]> coordinates = new ArrayList<>();
    private static List<String> names = new ArrayList<>();
    private static List<Integer> numOfHarassments = new ArrayList<>();
    private static List<String[]> harassments = new ArrayList<>();
    private static List<String> addresses = new ArrayList<>();

    public static void setData() throws FileNotFoundException {
        Scanner sc = new Scanner(obj);
        sc.nextLine();
        while (sc.hasNextLine()) {
            String tmp = sc.nextLine();
            String[] tmpArr = tmp.split(",");
            names.add(tmpArr[0]);
            addresses.add(tmpArr[1]);
            numOfHarassments.add(Integer.parseInt(tmpArr[2]));
            harassments.add(tmpArr[3].split(";"));
            coordinates.add(new Double[]{Double.parseDouble(tmpArr[4]), Double.parseDouble(tmpArr[5])});
        }
        sc.close();
    }

    public static List<Double[]> getCoordinates() {
        return coordinates;
    }

    public static List<String> getNames() {
        return names;
    }

    public static List<Integer> getNumOfHarassments() {
        return numOfHarassments;
    }

    public static List<String[]> getHarassments() {
        return harassments;
    }

    public static List<String> getAddresses() {
        return addresses;
    }

    public static void putLocation(String name, String address, String categoryOfOffences,  double latitude, double longitude) {
        int tmp = positonInDataSet(latitude, longitude);
        if (tmp != -1) {
            numOfHarassments.set(tmp, numOfHarassments.get(tmp) + 1);
        } else {
            names.add(name);
            addresses.add(address);
            numOfHarassments.add(1);
            harassments.add(categoryOfOffences.split(";"));
            coordinates.add(new Double[]{latitude, longitude});
        }
    }

    private static int positonInDataSet(double latitude, double longitude) {
        int size = coordinates.size();
        for (int i = 0; i < size; i++) {
            Double[] arr = coordinates.get(i);
            if (Math.abs(arr[0] - latitude) <= 0.00015 && Math.abs(arr[1] - longitude) <= 0.00015) {
                return i;
            }
        }
        return -1;
    }
}