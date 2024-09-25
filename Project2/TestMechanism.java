import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.TreeMap;

public class TestMechanism {
    public static void main(String[] args) {
        try {
            TreeMap<Long, String> treeMap = new TreeMap<>();

            // Read number of nodes from config file
            int noOfNodes = 0;
            try (Scanner scanner_1 = new Scanner(new File("config.txt"))) {
                String current = "";
                while (scanner_1.hasNext()) {
                    if ((current = scanner_1.next()).equals("p") && (!(current.equals("#")))) {
                        noOfNodes = scanner_1.nextInt();
                        break;
                    }
                }
            }

            // Process log files
            for (int i = 0; i < noOfNodes; i++) {
                File logFile = new File("node" + i + ".log");
                try (Scanner scanner_2 = new Scanner(logFile)) {
                    while (scanner_2.hasNext()) {
                        String currentLine = scanner_2.next();
                        String[] keyValue = currentLine.split(":");
                        treeMap.put(Long.valueOf(keyValue[0]), keyValue[1]);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            String prevValue = null;
            int j = 1;

            // Check for violations
            for (String value : treeMap.values()) {
                String[] currentValues;
                if (prevValue != null && j % 2 == 0) {
                    currentValues = value.split("-");
                    String[] previous = prevValue.split("-");
                    if (previous[0].equals(currentValues[0])) {
                        if (previous[1].equalsIgnoreCase("Start") && currentValues[1].equalsIgnoreCase("End")) {
                            prevValue = null;
                            j++;
                            continue;
                        }
                    } else if ((previous[1].equalsIgnoreCase("Start") && currentValues[1].equalsIgnoreCase("Start"))
                            || (previous[1].equalsIgnoreCase("End") && currentValues[1].equalsIgnoreCase("End"))) {
                        System.out.println("Yes, one or more violations found!");
                        return;
                    } else if (previous[1].equalsIgnoreCase("Start") && currentValues[1].equalsIgnoreCase("End")) {
                        System.out.println("Yes, one or more violations found!");
                        return;
                    }
                }
                prevValue = value;
                j++;
            }

            System.out.println("No violations found!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
