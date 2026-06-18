import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TestOSM {
    public static void main(String[] args) {
        try {
            double lat = 37.7749;
            double lng = -122.4194;
            String query = "[out:json];(way(around:1500," + lat + "," + lng + ")[highway~\"motorway|trunk|primary|secondary|tertiary|unclassified|residential\"];way(around:4000," + lat + "," + lng + ")[highway~\"motorway|trunk|primary\"];);out geom;";

            String[] endpoints = {
                "https://lz4.overpass-api.de/api/interpreter",
                "https://overpass-api.de/api/interpreter"
            };

            for (String baseUrl : endpoints) {
                System.out.println("Querying " + baseUrl + "...");
                URL url = new URL(baseUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                // Use a standard browser User-Agent
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);

                String postData = "data=" + URLEncoder.encode(query, "UTF-8");
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = postData.getBytes("UTF-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                System.out.println("Response Code: " + responseCode);
                
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    System.out.println("Success! Characters read: " + response.length());
                } else {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream() != null ? connection.getErrorStream() : connection.getInputStream()));
                    String inputLine;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        errorResponse.append(inputLine);
                    }
                    in.close();
                    System.out.println("Error body: " + errorResponse.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
