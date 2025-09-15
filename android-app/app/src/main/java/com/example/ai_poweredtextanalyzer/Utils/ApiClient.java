package com.example.ai_poweredtextanalyzer.Utils;

import static android.content.Context.MODE_PRIVATE;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;

import com.example.ai_poweredtextanalyzer.Exceptions.InvalidCredentialsException;
import com.example.ai_poweredtextanalyzer.Objects.ChatMessage;
import com.example.ai_poweredtextanalyzer.Objects.File;
import com.example.ai_poweredtextanalyzer.Exceptions.UserAlreadyExistsException;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

/**
 * This class provide utility methods for backend API calls.
 */
public class ApiClient {
    private static final OkHttpClient client = new OkHttpClient();
    private static final String BASE_URL = "http://10.0.2.2:5000/";

    /**
     * Login to the backend server.
     * @param email - user's email address.
     * @param password - user's password.
     * @return JWT access token for future authentications.
     * @throws RuntimeException if login failed.
     * @throws InvalidCredentialsException if the login credentials are invalid.
     */
    public static String login(String email, String password) throws InvalidCredentialsException, RuntimeException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + "auth/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            int statusCode = response.code();

            if (statusCode == 200) { // Success
                if (response.body() != null) {
                    String jsonResponse = response.body().string();
                    JSONObject obj = new JSONObject(jsonResponse);
                    String token = obj.getString("token");
                    Log.d("API", "JWT Token: " + token);
                    return token;
                } else {
                    throw new RuntimeException("Login succeeded but response body is empty!");
                }
            } else if (statusCode == 401) { // Invalid credentials
                throw new InvalidCredentialsException("Invalid email or password");
            } else { // Other errors
                String errorBody = response.body() != null ? response.body().string() : "No details";
                throw new RuntimeException("Login failed with status " + statusCode + ": " + errorBody);
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to login", e);
        }
    }

    /**
     * Register as a new user to the backend server.
     * @param fullName - user's full name.
     * @param email - user's email address.
     * @param password - user's password.
     * @return JWT access token for future authentications.
     * @throws RuntimeException if registration failed.
     * @throws UserAlreadyExistsException if the user is already exists in the database.
     */
    public static String register(String fullName, String email, String password) throws UserAlreadyExistsException, RuntimeException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{\"full_name\":\"" + fullName + "\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + "auth/register")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            int statusCode = response.code();

            if (statusCode == 201) { // Success
                if (response.body() != null) {
                    String jsonResponse = response.body().string();
                    JSONObject obj = new JSONObject(jsonResponse);
                    String token = obj.getString("token");
                    Log.d("API", "JWT Token: " + token);
                    return token;
                } else {
                    throw new RuntimeException("Registration succeeded but response body is empty!");
                }
            } else if (statusCode == 400) { // User already exists
                throw new UserAlreadyExistsException("User with this email already exists");
            } else { // Other errors
                String errorBody = response.body() != null ? response.body().string() : "No details";
                throw new RuntimeException("Registration failed with status " + statusCode + ": " + errorBody);
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to register", e);
        }
    }

    /**
     * Get the user's uploaded files from the backend server.
     * @param context - called activity context.
     * @return A list of the user's uploaded files.
     * @throws RuntimeException if failed while trying getting the files.
     */
    public static ArrayList<File> getMyFiles(Context context) throws RuntimeException {
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String savedToken = prefs.getString("jwt_token", null);

        if (savedToken == null) {
            throw new RuntimeException("Unauthorized");
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "files/")
                .addHeader("Authorization", "Bearer " + savedToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String jsonResponse = response.body().string();

                // parse the json
                return parseFilesJsonArray(jsonResponse);
            } else {
                throw new RuntimeException("Failed to get my files");
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to get my files", e);
        }
    }

    /**
     * Parse a JSONArray into ArrayList of File objects.
     * @param jsonResponse - the JSONArray from the http response.
     * @return A list of File objects.
     * @throws JSONException if parse attempt failed.
     */
    @NonNull
    private static ArrayList<File> parseFilesJsonArray(String jsonResponse) throws JSONException {
        JSONObject jsonObj = new JSONObject(jsonResponse);
        JSONArray filesArray = jsonObj.getJSONArray("files");

        // convert json array into ArrayList<File>
        ArrayList<File> myFiles = new ArrayList<>();
        for (int i = 0; i < filesArray.length(); i++) {
            JSONObject obj = filesArray.getJSONObject(i);
            File file = new File(
                    obj.getString("id"),
                    obj.getString("filename"),
                    obj.getString("uploaded_at")
            );
            myFiles.add(file);
        }
        return myFiles;
    }

    /**
     * Parse a JSONArray into ArrayList of ChatMessage objects.
     * @param jsonResponse - the JSONArray from the http response.
     * @return A list of ChatMessage objects.
     * @throws JSONException if parse attempt failed.
     */
    private static ArrayList<ChatMessage> parseChatJsonArray(String jsonResponse) throws JSONException {
        // parse the json
        JSONObject jsonObj = new JSONObject(jsonResponse);
        JSONArray chatMessageArray = jsonObj.getJSONArray("chat_history");

        // convert json array into ArrayList<ChatMessage>
        ArrayList<ChatMessage> chatMessages = new ArrayList<>();
        for (int i = 0; i < chatMessageArray.length(); i++) {
            JSONObject obj = chatMessageArray.getJSONObject(i);
            ChatMessage message = new ChatMessage(
                    obj.getString("sender"),
                    obj.getString("text")
            );
            chatMessages.add(message);
        }
        return chatMessages;
    }

    /**
     * Upload a file to the backend server.
     * @param context - called activity context.
     * @param fileUri - uri of the file to upload.
     * @param fileName - name of the file to upload.
     * @throws RuntimeException if upload attempt failed.
     */
    public static void uploadFile(Context context, Uri fileUri, String fileName) throws RuntimeException {
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String savedToken = prefs.getString("jwt_token", null);

        try {
            // Open InputStream from Uri
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) return;

            byte[] fileBytes = new byte[inputStream.available()];
            inputStream.read(fileBytes);
            inputStream.close();

            OkHttpClient client = new OkHttpClient();

            RequestBody fileBody = RequestBody.create(
                    fileBytes,
                    MediaType.parse(Objects.requireNonNull(context.getContentResolver().getType(fileUri)))
            );

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(BASE_URL + "upload/") // your endpoint
                    .addHeader("Authorization", "Bearer " + savedToken)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            Log.i("responseBody", responseBody);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload the file");
        }
    }

    /**
     * Summarize a file content using the backend AI Model.
     * @param context - called activity context.
     * @param fileId - ID of the file to summarize.
     * @param style - summary style (Bullets, Paragraph)
     * @param length - summary length (Short, Medium, Long)
     * @return The AI-Powered file summary
     * @throws RuntimeException if summarization attempt failed.
     */
    public static String summarizeFile(Context context, String fileId, String style, String length) throws RuntimeException {
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String savedToken = prefs.getString("jwt_token", null);

        try {
            // Build JSON body
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("file_id", fileId);
            jsonBody.put("style", style);
            jsonBody.put("length", length);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            // Build request
            Request request = new Request.Builder()
                    .url(BASE_URL + "summarize/file/")
                    .addHeader("Authorization", "Bearer " + savedToken)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            JSONObject jsonResponse = new JSONObject(responseBody);


            Object summaryObj = jsonResponse.get("summary");
            if (summaryObj instanceof JSONArray) {
                JSONArray summaryArray = (JSONArray) summaryObj;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < summaryArray.length(); i++) {
                    sb.append("• ").append(summaryArray.getString(i)).append("\n");
                }
                return sb.toString();
            } else if (summaryObj instanceof String) {
                return (String) summaryObj;
            } else {
                throw new RuntimeException("Unexpected summary format");
            }

        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to summarize", e);
        }
    }

    /**
     * Gets the chat history with the AI Model for a specific file.
     * @param context - called activity context.
     * @param fileId - ID of the chat context file.
     * @return A list of ChatMessage represent the history char conversation.
     * @throws RuntimeException if getting the chat history attempt failed.
     */
    public static ArrayList<ChatMessage> getChatHistory(Context context, String fileId) throws RuntimeException {
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String savedToken = prefs.getString("jwt_token", null);

        if (savedToken == null) {
            throw new RuntimeException("Unauthorized");
        }

        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(BASE_URL + "chat/history"))
                .newBuilder()
                .addQueryParameter("context_file_id", fileId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + savedToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String jsonResponse = response.body().string();
                return parseChatJsonArray(jsonResponse);
            } else {
                throw new RuntimeException("Failed to get chat history");
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to get chat history", e);
        }
    }

    /**
     * Ask the backend AI Model a question about the file.
     * @param context - called activity context.
     * @param fileId - ID of the chat context file.
     * @param question - the question to ask.
     * @return The AI Model answer.
     * @throws RuntimeException if asking the question attempt failed.
     */
    public static String askModel(Context context, String fileId, String question) throws RuntimeException {
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String savedToken = prefs.getString("jwt_token", null);

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{\"context_file_id\":\"" + fileId + "\",\"question\":\"" + question + "\"}";
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + "chat/")
                .addHeader("Authorization", "Bearer " + savedToken)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String jsonResponse = response.body().string();

                // Parse the JSON
                JSONObject obj = new JSONObject(jsonResponse);

                return obj.getString("answer");
            } else {
                throw new RuntimeException("Failed to ask model");
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to ask model", e);
        }
    }
}
