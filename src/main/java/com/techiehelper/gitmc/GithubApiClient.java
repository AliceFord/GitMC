package com.techiehelper.gitmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class GithubApiClient {
    
    private static final File SECRETS_FILE = Paths.get("").toAbsolutePath().resolve("gitmc").resolve(".gitmcsecrets.txt").toFile();
    private static final String CLIENT_ID = "Iv1.995c8762575f152c";
    
    GithubApiClient() {
//        File secretsFile = Paths.get("").toAbsolutePath().resolve("gitmc").resolve(".gitmcsecrets.txt").toFile();
//        secretsFile.getParentFile().mkdirs();
//        try {
//            if (secretsFile.createNewFile()) {
//                FileWriter writer = new FileWriter(secretsFile);
//                writer.write("");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
    
    private HashMap<String, String> formatApiOutput(String apiOutput) {
        HashMap<String, String> formattedData = new HashMap<>();
        String[] splitData = apiOutput.split("&");
        for (String d : splitData) {
            if (d.split("=").length == 2) {
                if (!d.split("=")[0].equals("access_token"))
                    formattedData.put(d.split("=")[0], d.split("=")[1]);
                else
                    formattedData.put("token", d.split("=")[1]);
            } else if (d.split("=").length == 1) {
                formattedData.put(d.split("=")[0], "");
            }
        }
        return formattedData;
    }
    
    private String makeRequest(String stringUrl, String urlParams, RequestMethod requestMethod) throws IOException {
        return makeRequest(stringUrl, urlParams, requestMethod, null);
    }
    
    private String makeRequest(String stringUrl, String urlParams, RequestMethod requestMethod, String authToken) throws IOException {
        StringBuilder outputData = new StringBuilder();
        URL url = new URL(stringUrl + urlParams);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput( true );
        conn.setInstanceFollowRedirects( false );
        conn.setRequestMethod(requestMethod.toString());
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        if (authToken != null)
            conn.setRequestProperty("Authorization", "token " + authToken);
        conn.setUseCaches( false );
    
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            outputData.append(inputLine);
        in.close();
    
        System.out.println(outputData.toString());
        return outputData.toString();
    }
    
    public HashMap<String, String> refreshToken(String refresh_token) {
        Map<String, String> secretsData = Util.parseFile(SECRETS_FILE);
        String urlParams = "?refresh_token=" + refresh_token + "&grant_type=refresh_token&client_id=Iv1.995c8762575f152c&client_secret=" + secretsData.get("client_secret");
        String outputData = "";
        try {
            outputData = makeRequest("https://github.com/login/oauth/access_token", urlParams, RequestMethod.GET);
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        return formatApiOutput(outputData);
    }
    
    public boolean testToken(String token) {
        try {
            String output = makeRequest("https://api.github.com/user/repos", "", RequestMethod.GET, token);
            
            return true;
        } catch (IOException e) {
            if (!e.getMessage().contains("java.io.IOException: Server returned HTTP response code: 401 for URL")) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    public HashMap<String, String> getOneTimeCode() throws GitMCException {
        String output = "";
        try {
            output = makeRequest("https://github.com/login/device/code", "?client_id=Iv1.995c8762575f152c", RequestMethod.POST);
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        HashMap<String, String> formattedOutput = formatApiOutput(output);
        String userCode = formattedOutput.get("user_code");
        String deviceCode = formattedOutput.get("device_code");
        if (userCode == null || deviceCode == null) {
            throw new GitMCException();
        }
        return formattedOutput;
    }
}
