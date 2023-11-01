package net.aerh.terminalmonitor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class AirportQueueMonitor {
    
    private static final String GET_DYNAMIC_POIS_URL = "https://rest.locuslabs.com/v1/venue/jfk/account/A119NSPH8JLU80/get-all-dynamic-pois/";
    private static final String GET_DYNAMIC_QUEUE_TIMES_URL = "https://rest.locuslabs.com/v3/venueId/jfk/accountId/A119NSPH8JLU80/get-dynamic-queue-times/";
    private static final String[] TIME_THRESHOLD_COLORS = new String[]{"\033[92m", "\u001B[32m", "\u001B[33m", "\u001B[31m", "\u001B[41m\u001B[30m"};
    private static final String ANSI_RESET = "\u001B[0m";
    
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Map<Integer, Terminal> terminalMap = new HashMap<>();
    
    public static void main(String[] args) {
        System.out.print("Loading...");
        
        Timer timer = new Timer();
        
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                loadTerminalMap();
            }
        }, 0, Duration.ofDays(7).toMillis());
        
        Scanner scanner = new Scanner(System.in);
        int alertInterval = 0;
        
        while (alertInterval <= 0) {
            System.out.print("\rEnter the alert interval in minutes (greater than 0): ");
            alertInterval = scanner.nextInt();
            
            if (alertInterval <= 0) {
                System.out.println("Please enter a valid interval!");
            }
        }
        
        System.out.println();
        
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getQueueTimes();
            }
        }, 0, Duration.ofMinutes(1).toMillis());
        
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Current queue times as of " + DateTimeFormatter.ofPattern("MMMM d, yyyy @ hh:mm a").format(LocalDateTime.now()) + ":");
                
                terminalMap.values().stream()
                    .sorted((t1, t2) -> {
                        if (t1.isClosed() && !t2.isClosed()) {
                            return 1;
                        } else if (!t1.isClosed() && t2.isClosed()) {
                            return -1;
                        } else {
                            return Integer.compare(t1.getQueueTime(), t2.getQueueTime());
                        }
                    })
                    .forEach(terminal -> {
                        String terminalName = terminal.getName() + " (" + terminal.getCheckpointName() + ")";
                        
                        if (terminal.isClosed()) {
                            System.out.println("  " + TIME_THRESHOLD_COLORS[4] + terminalName + " is currently closed!" + ANSI_RESET);
                        } else {
                            String queueTime = getQueueTimeColor(terminal.getQueueTime()) + terminal.getQueueTime() + " minute" + (terminal.getQueueTime() == 1 ? "" : "s");
                            System.out.println("  " + terminalName + ": " + queueTime + ANSI_RESET);
                        }
                    });
                
                System.out.println();
            }
        }, 0, Duration.ofMinutes(alertInterval).toMillis());
    }
    
    private static void loadTerminalMap() {
        terminalMap.clear();
        
        try {
            JSONObject pois = fetchDynamicPOIs();
            pois.keySet().forEach(key -> {
                JSONObject poi = pois.getJSONObject(key);
                int poiId = poi.getInt("poiId");
                String checkpointName = poi.getString("name");
                String floorId = poi.getJSONObject("position").getString("floorId");
                String terminalName = getReadableTerminalName(floorId);
                Terminal terminal = new Terminal(poiId, checkpointName, terminalName, floorId);
                
                terminalMap.put(poiId, terminal);
            });
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
        }
    }
    
    private static JSONObject fetchDynamicPOIs() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GET_DYNAMIC_POIS_URL))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        return new JSONObject(response.body());
    }
    
    private static void getQueueTimes() {
        try {
            JSONArray queueTimeData = fetchDynamicQueueTimes();
            
            queueTimeData.forEach(o -> {
                JSONObject queueTime = (JSONObject) o;
                Terminal terminal = terminalMap.get(queueTime.getInt("poiId"));
                
                terminal.setClosed(queueTime.getBoolean("isTemporarilyClosed"));
                terminal.setQueueTime(queueTime.getInt("queueTime"));
            });
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
        }
    }
    
    private static JSONArray fetchDynamicQueueTimes() throws IOException, InterruptedException {
        HttpRequest queueTimeRequest = HttpRequest.newBuilder()
            .uri(URI.create(GET_DYNAMIC_QUEUE_TIMES_URL))
            .GET()
            .build();
        HttpResponse<String> response = client.send(queueTimeRequest, HttpResponse.BodyHandlers.ofString());
        
        return new JSONArray(response.body());
    }
    
    public static String getReadableTerminalName(String floorId) {
        String[] parts = floorId.split("-");
        
        if (parts.length >= 3) {
            String terminalNumber = parts[1].substring(parts[1].length() - 1);
            String terminalName = capitalizeFirstLetter(parts[2]);
            
            return "Terminal " + terminalNumber + " " + terminalName;
        } else {
            return "Unknown Area";
        }
    }
    
    private static String capitalizeFirstLetter(String original) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        
        return Character.toUpperCase(original.charAt(0)) + original.substring(1);
    }
    
    private static String getQueueTimeColor(int minutes) {
        if (minutes <= 15) {
            return TIME_THRESHOLD_COLORS[0];
        } else if (minutes <= 44) {
            return TIME_THRESHOLD_COLORS[2];
        } else {
            return TIME_THRESHOLD_COLORS[3];
        }
    }
}
