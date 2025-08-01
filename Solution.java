import java.io.*;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Solution {

    public static void main(String[] args) {
        String[] inputFiles = { "input1.json", "input2.json" };

        for (String inputFile : inputFiles) {
            System.out.println("=".repeat(50));
            System.out.println("Processing: " + inputFile);
            System.out.println("=".repeat(50));

            try {

                processInputFile(inputFile);
            } catch (Exception e) {
                System.err.println("Error processing " + inputFile + ": " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
        }
    }

    private static void processInputFile(String inputFile) throws IOException {
        // Read input from JSON file
        String jsonContent = readFile(inputFile);

        // Parse JSON manually
        Map<String, Object> json = parseJSON(jsonContent);

        // Extract keys
        Map<String, Object> keys = (Map<String, Object>) json.get("keys");
        int n = Integer.parseInt(keys.get("n").toString());
        int k = Integer.parseInt(keys.get("k").toString());

        System.out.println("n: " + n + ", k: " + k);
        System.out.println("Degree of polynomial: " + (k - 1));
        System.out.println();

        // Collect points (x, y) where y is decoded from base
        List<Point> points = new ArrayList<>();

        for (int i = 1; i <= n; i++) {
            String key = String.valueOf(i);
            if (json.containsKey(key)) {
                Map<String, Object> root = (Map<String, Object>) json.get(key);
                int x = i;
                String baseStr = root.get("base").toString();
                String valueStr = root.get("value").toString();

                int base = Integer.parseInt(baseStr);
                BigInteger y = decodeFromBase(valueStr, base);

                points.add(new Point(x, y));
                System.out.println("Point " + i + ": x=" + x + ", y=" + y + " (decoded from base " + base + ")");
            }
        }

        System.out.println();

        // Find the secret C using polynomial interpolation
        BigInteger secretC = findSecretC(points, k);
        System.out.println("Secret C: " + secretC);

        // Verify the result by checking if the polynomial passes through the points
        System.out.println("\nVerification:");
        for (int i = 0; i < Math.min(k, points.size()); i++) {
            Point p = points.get(i);
            System.out.println("P(" + p.x + ") = " + p.y);
        }
    }

    // Simple JSON parser
    private static Map<String, Object> parseJSON(String json) {
        Map<String, Object> result = new HashMap<>();
        json = json.trim();

        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] pairs = splitJSON(json);

            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("\"", "");
                    String value = keyValue[1].trim();

                    if (value.startsWith("{") && value.endsWith("}")) {
                        result.put(key, parseJSON(value));
                    } else if (value.startsWith("\"") && value.endsWith("\"")) {
                        result.put(key, value.substring(1, value.length() - 1));
                    } else {
                        result.put(key, value);
                    }
                }
            }
        }

        return result;
    }

    private static String[] splitJSON(String json) {
        List<String> result = new ArrayList<>();
        int braceCount = 0;
        int start = 0;
        boolean inQuotes = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                } else if (c == ',' && braceCount == 0) {
                    result.add(json.substring(start, i));
                    start = i + 1;
                }
            }
        }

        if (start < json.length()) {
            result.add(json.substring(start));
        }

        return result.toArray(new String[0]);
    }

    // Decode a string from given base to BigInteger
    private static BigInteger decodeFromBase(String value, int base) {
        return new BigInteger(value, base);
    }

    // Find the secret C using polynomial interpolation
    // The secret C is the constant term (coefficient of x^0) of the polynomial
    private static BigInteger findSecretC(List<Point> points, int k) {
        if (points.size() < k) {
            throw new IllegalArgumentException("Not enough points. Need at least " + k + " points.");
        }

        List<Point> interpolationPoints = points.subList(0, k);

        // Use BigDecimal for the calculation to maintain precision
        BigDecimal secretC = BigDecimal.ZERO;
        int scale = 100; // A high scale for precision during division
        RoundingMode roundingMode = RoundingMode.HALF_UP;

        for (int i = 0; i < interpolationPoints.size(); i++) {
            Point p_i = interpolationPoints.get(i);
            BigDecimal y_i = new BigDecimal(p_i.y);
            BigDecimal term = y_i;

            // Calculate the Lagrange basis polynomial L_i(0)
            BigDecimal numerator = BigDecimal.ONE;
            BigDecimal denominator = BigDecimal.ONE;

            for (int j = 0; j < interpolationPoints.size(); j++) {
                if (i != j) {
                    Point p_j = interpolationPoints.get(j);
                    BigDecimal x_i_bd = new BigDecimal(p_i.x);
                    BigDecimal x_j_bd = new BigDecimal(p_j.x);

                    // Numerator product: (0 - x_j)
                    numerator = numerator.multiply(x_j_bd.negate());
                    // Denominator product: (x_i - x_j)
                    denominator = denominator.multiply(x_i_bd.subtract(x_j_bd));
                }
            }

            // The term is y_i * (numerator / denominator)
            BigDecimal lagrangeBasis = numerator.divide(denominator, scale, roundingMode);
            secretC = secretC.add(y_i.multiply(lagrangeBasis));
        }

        // Round the final result to the nearest integer and convert back to BigInteger
        return secretC.setScale(0, RoundingMode.HALF_UP).toBigInteger();
    }

    // Helper method to read file content
    private static String readFile(String filename) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    // Point class to represent (x, y) coordinates
    static class Point {
        BigInteger x, y;

        Point(int x, BigInteger y) {
            this.x = BigInteger.valueOf(x);
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
}
