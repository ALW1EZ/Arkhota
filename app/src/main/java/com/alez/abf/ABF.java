package com.alez.abf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.mifmif.common.regex.Generex;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class ABF extends AppCompatActivity {

    private static String PASSWORD_ALPHABET = "";
    private static String USERNAME_ALPHABET = "";
    private static int FOUND = 0;

    public String escapeMetaCharacters(String inputString){
        final String[] metaCharacters = {"\\","^","$","{","}","[","]","(",")",".","*","+","?","|","<",">","-","&","%"};

        for (String metaCharacter : metaCharacters) {
            if (inputString.contains(metaCharacter)) {
                inputString = inputString.replace(metaCharacter, "\\" + metaCharacter);
            }
        }
        return inputString;
    }

    public static String executeReq(String targetURL, String urlBody, String type, String timeout, String useragent, String cookie) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();

            if (type.equals("1")) {
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Length", Integer.toString(urlBody.getBytes().length));
            } else {
                connection.setRequestMethod("GET");
            }
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            if (!timeout.equals("0")) connection.setConnectTimeout(Integer.parseInt(timeout));
            if (!useragent.equals("Original UA")) connection.setRequestProperty("User-Agent", useragent);
            if (!cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlBody);
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @SuppressLint({"SetTextI18n", "UseSwitchCompatOrMaterialCode"})
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView txt3 = findViewById(R.id.txt3); // For about

        // Connection
        EditText url_txt = findViewById(R.id.url_txt);
        EditText body_txt = findViewById(R.id.body_txt);

        // Username
        // username_spinner is at the bottom
        EditText username_txt = findViewById(R.id.username_txt);

        EditText username_chrst_txt = findViewById(R.id.username_chrst_txt);
        EditText username_min_txt = findViewById(R.id.username_min_txt);
        EditText username_max_txt = findViewById(R.id.username_max_txt);

        CheckBox username_num_chk = findViewById(R.id.username_num_chk);
        CheckBox username_az_chk = findViewById(R.id.username_az_chk);
        CheckBox username_AZ_chk = findViewById(R.id.username_AZ_chk);
        CheckBox username_spec_chk = findViewById(R.id.username_spec_chk);

        EditText username_pre_txt = findViewById(R.id.username_pre_txt);
        EditText username_suf_txt = findViewById(R.id.username_suf_txt);

        // Password
        // password_spinner is at the bottom
        EditText password_txt = findViewById(R.id.password_txt);

        EditText password_chrst_txt = findViewById(R.id.password_chrst_txt);
        EditText password_min_txt = findViewById(R.id.password_min_txt);
        EditText password_max_txt = findViewById(R.id.password_max_txt);

        CheckBox password_num_chk = findViewById(R.id.password_num_chk);
        CheckBox password_az_chk = findViewById(R.id.password_az_chk);
        CheckBox password_AZ_chk = findViewById(R.id.password_AZ_chk);
        CheckBox password_spec_chk = findViewById(R.id.password_spec_chk);

        EditText password_pre_txt = findViewById(R.id.password_pre_txt);
        EditText password_suf_txt = findViewById(R.id.password_suf_txt);

        // Configuration
        Switch beep_switch = findViewById(R.id.beep_switch);
        Switch by_switch = findViewById(R.id.by_switch);
        Switch type_switch = findViewById(R.id.type_switch);

        // User-Agent is at the bottom.
        EditText timeout_txt = findViewById(R.id.txt_timeout);

        EditText cook_txt = findViewById(R.id.cook_txt);
        EditText rgx_txt = findViewById(R.id.rgx_txt);

        EditText try_txt = findViewById(R.id.try_txt);
        try_txt.setInputType(InputType.TYPE_NULL);
        try_txt.setShowSoftInputOnFocus(false);

        TextView txtcount = findViewById(R.id.txtcount);
        Button start_btn = findViewById(R.id.start_btn);

        // ArrayAdapters
        Spinner username_spinner = findViewById(R.id.username_spinner);
        Spinner password_spinner = findViewById(R.id.password_spinner);

        // Pre-defined wordlists
        String[] wordlists = new String[]{"Single", "Generate",
                "tr-10000.txt", "nmap-5000.txt",
                "top-25-pass.txt", "top-100-pass.txt",
                "rockyou-50.txt", "rockyou-1000.txt", "rockyou-10000.txt",
                "name-male-1000.txt", "name-female-1000.txt"};

        // Custom wordlist permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String path = Environment.getExternalStorageDirectory().toString() + "/ABF";
            File dir = new File(path);

            PermissionListener permissionlistener = new PermissionListener() {
                @Override
                public void onPermissionGranted() {
                    if (!dir.exists()) {
                        boolean done = dir.mkdir();
                        if (done) {
                            Toast.makeText(getApplicationContext(), "ABF/ directory created at " + path + " , you can place your wordlists there.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "An error occurred while creating ABF/ directory.", Toast.LENGTH_LONG).show();
                        }
                    }

                    File[] files = dir.listFiles();
                    if (files != null) {
                        int j = wordlists.length - 1;
                        for (File file : files) {
                            wordlists[j] = file.getName();
                            j++;
                        }
                    }
                }

                @Override
                public void onPermissionDenied(ArrayList<String> deniedPermissions) {

                }

            };

            //check all needed permissions together
            TedPermission.with(this)
                    .setPermissionListener(permissionlistener)
                    .setDeniedMessage("You need to give read & write permission to use custom wordlists.")
                    .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .check();


        } else {
            // < SDK 23 accepts while installing, do what you gotta do!
            String path = Environment.getExternalStorageDirectory().toString() + "/ABF";
            File dir = new File(path);

            if (!dir.exists()) {
                boolean done = dir.mkdir();
                if (!done) {
                    Toast.makeText(getApplicationContext(), "An error occurred while creating ABF/ directory.", Toast.LENGTH_LONG).show();
                }
            }

            File[] files = dir.listFiles();
            if (files != null) {
                int j = wordlists.length - 1;
                for (File file : files) {
                    wordlists[j] = file.getName();
                    j++;
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wordlists);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        username_spinner.setAdapter(adapter);
        password_spinner.setAdapter(adapter);

        String[] useragents = getResources().getStringArray(R.array.user_agents);
        AutoCompleteTextView useragent_txt = findViewById(R.id.useragent_txt);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, useragents);
        useragent_txt.setAdapter(adapter2);
        // ArrayAdapters

        start_btn.setOnClickListener(v -> {

            // Connection
            String url = url_txt.getText().toString();
            String url_ = url;
            String body = body_txt.getText().toString();
            String body_ = body;

            // Username
            String username_spin_txt = username_spinner.getSelectedItem().toString();
            String username = username_txt.getText().toString();
            String username_chrst = username_chrst_txt.getText().toString();
            String username_min_s = username_min_txt.getText().toString();
            String username_max_s = username_max_txt.getText().toString();
            int username_min = 0;
            int username_max = 0;
            String username_pre = username_pre_txt.getText().toString();
            String username_suf = username_suf_txt.getText().toString();

            // Password
            String password_spin_txt = password_spinner.getSelectedItem().toString();
            String password = password_txt.getText().toString();
            String password_chrst = password_chrst_txt.getText().toString();
            String password_min_s = password_min_txt.getText().toString();
            String password_max_s = password_max_txt.getText().toString();
            int password_min = 0;
            int password_max = 0;
            String password_pre = password_pre_txt.getText().toString();
            String password_suf = password_suf_txt.getText().toString();

            // Configuration
            String user_agent = useragent_txt.getText().toString();
            String timeout = (timeout_txt.getText().toString().isEmpty()) ? "0" : timeout_txt.getText().toString();
            String cookie = cook_txt.getText().toString();
            String regex = rgx_txt.getText().toString();
            // BEEP and FAIL is not here because of runtime change!
            boolean type_c = type_switch.isChecked();
            String type = (type_c) ? "1" : "2";


            // AsyncTask
            @SuppressLint("StaticFieldLeak")
            class wl_conn extends AsyncTask<String, String, String> {

                private String user;
                private String pass;
                private String user_counter;
                private String pass_counter;
                String response;

                @Override
                protected String doInBackground(String... values) {
                    if (FOUND != 1) {
                        String url = values[0];
                        String body = values[1];
                        String type = values[2];
                        String timeout = values[3];
                        String useragent = values[4];
                        String cookie = values[5];

                        this.pass_counter = values[6];
                        this.user_counter = values[7];

                        this.pass = values[8];
                        this.user = values[9];
                        return response = executeReq(url, body, type, timeout, useragent, cookie);
                    }
                    return "DONE";
                }

                @Override
                protected void onPostExecute(String values) {
                    super.onPostExecute(values);
                    response = values;

                    // Check if done
                    if ("DONE".equals(response)) return;

                    // Check conn is ok
                    if ("ERROR".equals(response)) {
                        try_txt.setText("Problem with connection...");
                        return;
                    }

                    if (FOUND != 1) {
                        // Check regex is ok or not
                        if (by_switch.isChecked()) {
                            if (Pattern.compile(regex).matcher(response).find()) {

                                try_txt.setText("FOUND: " + user + ":" + pass);
                                FOUND = 1;

                                if (beep_switch.isChecked()) {
                                    MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.go);
                                    mediaPlayer.start();
                                }

                                txtcount.setText(user_counter + ":" + pass_counter);
                                return;
                            }
                        } else {
                            if (!Pattern.compile(regex).matcher(response).find()) {

                                try_txt.setText("FOUND: " + user + ":" + pass);
                                FOUND = 1;

                                if (beep_switch.isChecked()) {
                                    MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.go);
                                    mediaPlayer.start();
                                }

                                txtcount.setText(user_counter + ":" + pass_counter);
                                return;
                            }
                        }
                        try_txt.setText(user + ":" + pass);
                        txtcount.setText(user_counter + ":" + pass_counter);
                        // Check
                    }
                }
            }

            // Empty check
            if (url.isEmpty() || regex.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please fill all fields.", Toast.LENGTH_LONG).show();
                if (url.isEmpty()) {
                    url_txt.requestFocus();
                    return;
                }
                rgx_txt.requestFocus(); // If it's not URL it must be regex.
                return;
            }

            // SCHEMA check
            if (!url.contains("://")) {
                Toast.makeText(getApplicationContext(), "Please use scheme http[s]:// for URL.", Toast.LENGTH_SHORT).show();
                return;
            }

            // POST check
            if (type_c && body.isEmpty()) {
                Toast.makeText(getApplicationContext(), "You must fill body if you're going to make a POST request.", Toast.LENGTH_SHORT).show();
                type_switch.requestFocus();
                return;
            }


            // Single username check (Username)
            if ("Single".equals(username_spin_txt) && username.length() <= 0) {
                Toast.makeText(getApplicationContext(), "You need to give a username.", Toast.LENGTH_SHORT).show();
                username_txt.requestFocus();
                return;
            }

            // Generate wordlist check (Username)
            if ("Generate".equals(username_spin_txt) && username_chrst.length() <= 0) {
                Toast.makeText(getApplicationContext(), "You need to select charset for generate a userlist.", Toast.LENGTH_SHORT).show();
                username_chrst_txt.requestFocus();
                return;
            }

            // Min & Max checks (Username)
            if ("Generate".equals(username_spin_txt)) {

                // Min & Max check
                if (username_min_s.isEmpty() || username_max_s.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please fill minimum and maximum fields for userlist.", Toast.LENGTH_SHORT).show();
                    if (username_min_s.isEmpty()) {
                        username_min_txt.requestFocus();
                        return;
                    }
                    username_max_txt.requestFocus();
                    return;
                } else {
                    // Can parse after check
                    username_min = Integer.parseInt(username_min_s);
                    username_max = Integer.parseInt(username_max_s);
                }

                // Min & Max check 2
                if (username_min > username_max) {
                    Toast.makeText(getApplicationContext(), "Minimum cannot be higher than maximum (username).", Toast.LENGTH_SHORT).show();
                    username_min_txt.requestFocus();
                    return;
                }
            }


            // Single password check (Password)
            if ("Single".equals(password_spin_txt) && password.length() <= 0) {
                Toast.makeText(getApplicationContext(), "You need to give a password.", Toast.LENGTH_SHORT).show();
                password_txt.requestFocus();
                return;
            }

            // Generate wordlist check (Password)
            if ("Generate".equals(password_spin_txt) && password_chrst.length() <= 0) {
                Toast.makeText(getApplicationContext(), "You need to select charset for generate a wordlist.", Toast.LENGTH_SHORT).show();
                password_chrst_txt.requestFocus();
                return;
            }

            // Min & Max checks (Password)
            if ("Generate".equals(password_spin_txt)) {

                // Min & Max check
                if (password_min_s.isEmpty() || password_max_s.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please fill minimum and maximum fields for wordlist.", Toast.LENGTH_SHORT).show();
                    if (password_min_s.isEmpty()) {
                        password_min_txt.requestFocus();
                        return;
                    }
                    password_max_txt.requestFocus();
                    return;
                } else {
                    // Can parse after check
                    password_min = Integer.parseInt(password_min_s);
                    password_max = Integer.parseInt(password_max_s);
                }

                // Min & Max check 2
                if (password_min > password_max) {
                    Toast.makeText(getApplicationContext(), "Minimum cannot be higher than maximum (password).", Toast.LENGTH_SHORT).show();
                    password_min_txt.requestFocus();
                    return;
                }

            }

            // Reset
            try_txt.setText("");    // Clear
            txtcount.setText("");
            try_txt.requestFocus(); // Gain Focus
            FOUND = 0;              // Reset Found

            // PASSWORD
            if ("Generate".equals(password_spin_txt)) {
                // Generate
                String pass_reg = "[" + escapeMetaCharacters(password_chrst) + "]" + "{" + password_min + "," + password_max + "}";
                Generex pass_gen = new Generex(pass_reg);
                int password_counter = 0;
                int username_counter = 0;

                for (String pass : pass_gen.getAllMatchedStrings()) {

                    if (FOUND != 1) {
                        // Password things
                        password_counter++;
                        pass = password_pre + pass + password_suf;

                        // USERNAME
                        if ("Generate".equals(username_spin_txt)) {
                            String user_reg = "[" + escapeMetaCharacters(username_chrst) + "]" + "{" + username_min + "," + username_max + "}";
                            Generex user_gen = new Generex(user_reg);
                            // Generate
                            for (String user : user_gen.getAllMatchedStrings()) {
                                username_counter++;
                                user = username_pre + user + username_suf;

                                if (type_c) {
                                    body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                } else {
                                    url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                }

                                // Call AsyncTask
                                wl_conn wl_connection = new wl_conn();
                                wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);
                            }
                        } else if ("Single".equals(username_spin_txt)) {
                            // Single
                            String user = username;
                            username_counter = 1;
                            user = username_pre + user + username_suf;

                            if (type_c) {
                                body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                            } else {
                                url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                            }

                            // Call AsyncTask
                            wl_conn wl_connection = new wl_conn();
                            wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                        } else {
                            // Wordlist.

                            String path = Environment.getExternalStorageDirectory().toString() + "/ABF/" + username_spin_txt;
                            File file = new File(path);

                            if (file.exists()) {

                                try (BufferedReader user_reader = new BufferedReader(new FileReader(file))) {
                                    String user;
                                    username_counter = 0;
                                    while ((user = user_reader.readLine()) != null) {
                                        username_counter++;
                                        user = username_pre + user + username_suf;

                                        if (type_c) {
                                            body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                        } else {
                                            url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                        }

                                        // Call AsyncTask
                                        wl_conn wl_connection = new wl_conn();
                                        wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            } else {

                                try (BufferedReader user_reader = new BufferedReader(new InputStreamReader(getAssets().open(username_spin_txt)))) {
                                    String user;
                                    username_counter = 0;
                                    while ((user = user_reader.readLine()) != null) {
                                        username_counter++;
                                        user = username_pre + user + username_suf;

                                        if (type_c) {
                                            body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                        } else {
                                            url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                        }

                                        // Call AsyncTask
                                        wl_conn wl_connection = new wl_conn();
                                        wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    } else {
                        // Stop
                        break;
                    }

                }

            } else if ("Single".equals(password_spin_txt)) {
                // Single
                int password_counter = 0;
                int username_counter = 0;
                String pass = password;

                // Password Things
                password_counter++;
                pass = password_pre + pass + password_suf;

                if ("Generate".equals(username_spin_txt)) {
                    String user_reg = "[" + escapeMetaCharacters(username_chrst) + "]" + "{" + username_min + "," + username_max + "}";
                    Generex user_gen = new Generex(user_reg);
                    // Generate
                    for (String user : user_gen.getAllMatchedStrings()) {
                        username_counter++;
                        user = username_pre + user + username_suf;

                        if (type_c) {
                            body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                        } else {
                            url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                        }

                        // Call AsyncTask
                        wl_conn wl_connection = new wl_conn();
                        wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);
                    }
                } else if ("Single".equals(username_spin_txt)) {
                    // Single
                    String user = username;
                    username_counter = 1;
                    user = username_pre + user + username_suf;

                    if (type_c) {
                        body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                    } else {
                        url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                    }

                    // Call AsyncTask
                    wl_conn wl_connection = new wl_conn();
                    wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                } else {
                    // Wordlist.

                    String path = Environment.getExternalStorageDirectory().toString() + "/ABF/" + username_spin_txt;
                    File file = new File(path);

                    if (file.exists()) {

                        try (BufferedReader user_reader = new BufferedReader(new FileReader(file))) {
                            String user;
                            username_counter = 0;
                            while ((user = user_reader.readLine()) != null) {
                                username_counter++;
                                user = username_pre + user + username_suf;

                                if (type_c) {
                                    body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                } else {
                                    url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                }

                                // Call AsyncTask
                                wl_conn wl_connection = new wl_conn();
                                wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    } else {

                        try (BufferedReader user_reader = new BufferedReader(new InputStreamReader(getAssets().open(username_spin_txt)))) {
                            String user;
                            username_counter = 0;
                            while ((user = user_reader.readLine()) != null) {
                                username_counter++;
                                user = username_pre + user + username_suf;

                                if (type_c) {
                                    body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                } else {
                                    url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                }

                                // Call AsyncTask
                                wl_conn wl_connection = new wl_conn();
                                wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }

            } else {
                // Wordlist.
                int password_counter = 0;
                int username_counter = 0;

                String path = Environment.getExternalStorageDirectory().toString() + "/ABF/" + password_spin_txt;
                File file = new File(path);

                if (file.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String pass;
                        while ((pass = reader.readLine()) != null) {

                            // Password things
                            password_counter++;
                            pass = password_pre + pass + password_suf;

                            if ("Generate".equals(username_spin_txt)) {
                                String user_reg = "[" + escapeMetaCharacters(username_chrst) + "]" + "{" + username_min + "," + username_max + "}";
                                Generex user_gen = new Generex(user_reg);
                                // Generate
                                for (String user : user_gen.getAllMatchedStrings()) {
                                    username_counter++;
                                    user = username_pre + user + username_suf;

                                    if (type_c) {
                                        body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                    } else {
                                        url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                    }

                                    // Call AsyncTask
                                    wl_conn wl_connection = new wl_conn();
                                    wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);
                                }
                            } else if ("Single".equals(username_spin_txt)) {
                                // Single
                                String user = username;
                                username_counter = 1;
                                user = username_pre + user + username_suf;

                                if (type_c) {
                                    body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                } else {
                                    url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                }

                                // Call AsyncTask
                                wl_conn wl_connection = new wl_conn();
                                wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                            } else {
                                // Wordlist.
                                path = Environment.getExternalStorageDirectory().toString() + "/ABF/" + username_spin_txt;
                                file = new File(path);

                                if (file.exists()) {

                                    try (BufferedReader user_reader = new BufferedReader(new FileReader(file))) {
                                        String user;
                                        username_counter = 0;
                                        while ((user = user_reader.readLine()) != null) {
                                            username_counter++;
                                            user = username_pre + user + username_suf;

                                            if (type_c) {
                                                body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                            } else {
                                                url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                            }

                                            // Call AsyncTask
                                            wl_conn wl_connection = new wl_conn();
                                            wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                } else {

                                    try (BufferedReader user_reader = new BufferedReader(new InputStreamReader(getAssets().open(username_spin_txt)))) {
                                        String user;
                                        username_counter = 0;
                                        while ((user = user_reader.readLine()) != null) {
                                            username_counter++;
                                            user = username_pre + user + username_suf;

                                            if (type_c) {
                                                body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                            } else {
                                                url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                            }

                                            // Call AsyncTask
                                            wl_conn wl_connection = new wl_conn();
                                            wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(password_spin_txt)))) {
                        String pass;
                        while ((pass = reader.readLine()) != null) {

                            // Password things
                            password_counter++;
                            pass = password_pre + pass + password_suf;

                            if ("Generate".equals(username_spin_txt)) {
                                String user_reg = "[" + escapeMetaCharacters(username_chrst) + "]" + "{" + username_min + "," + username_max + "}";
                                Generex user_gen = new Generex(user_reg);
                                // Generate
                                for (String user : user_gen.getAllMatchedStrings()) {
                                    username_counter++;
                                    user = username_pre + user + username_suf;

                                    if (type_c) {
                                        body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                    } else {
                                        url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                    }

                                    // Call AsyncTask
                                    wl_conn wl_connection = new wl_conn();
                                    wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);
                                }
                            } else if ("Single".equals(username_spin_txt)) {
                                // Single
                                String user = username;
                                username_counter = 1;
                                user = username_pre + user + username_suf;

                                if (type_c) {
                                    body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                } else {
                                    url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                }

                                // Call AsyncTask
                                wl_conn wl_connection = new wl_conn();
                                wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                            } else {
                                // Wordlist.
                                path = Environment.getExternalStorageDirectory().toString() + "/ABF/" + username_spin_txt;
                                file = new File(path);

                                if (file.exists()) {

                                    try (BufferedReader user_reader = new BufferedReader(new FileReader(file))) {
                                        String user;
                                        username_counter = 0;
                                        while ((user = user_reader.readLine()) != null) {
                                            username_counter++;
                                            user = username_pre + user + username_suf;

                                            if (type_c) {
                                                body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                            } else {
                                                url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                            }

                                            // Call AsyncTask
                                            wl_conn wl_connection = new wl_conn();
                                            wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                } else {

                                    try (BufferedReader user_reader = new BufferedReader(new InputStreamReader(getAssets().open(username_spin_txt)))) {
                                        String user;
                                        username_counter = 0;
                                        while ((user = user_reader.readLine()) != null) {
                                            username_counter++;
                                            user = username_pre + user + username_suf;

                                            if (type_c) {
                                                body_ = body.replace("^USER^", user).replace("^PASS^", pass);
                                            } else {
                                                url_ = url.replace("^USER^", user).replace("^PASS^", pass);
                                            }

                                            // Call AsyncTask
                                            wl_conn wl_connection = new wl_conn();
                                            wl_connection.execute(url_, body_, type, timeout, user_agent, cookie, password_counter + "", username_counter + "", pass, user);

                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        by_switch.setOnClickListener(v -> {
            if (by_switch.isChecked()) {
                by_switch.setText("Success :");
                rgx_txt.setHint("Regex... Welcome.*?\\.");
            } else {
                by_switch.setText("Fail :");
                rgx_txt.setHint("Regex... (Wrong|Timeout)");
            }
        });

        type_switch.setOnClickListener(v -> {
            if (type_switch.isChecked()) {
                type_switch.setText("POST");
                body_txt.setEnabled(true);
                body_txt.setHint("Body... name=^USER^&pass=^PASS^");
                url_txt.setHint("URL... http://127.0.0.1[:8000]/login.php");
            } else {
                type_switch.setText("GET");
                body_txt.setEnabled(false);
                body_txt.setHint(null);
                url_txt.setHint("URL... http://127.0.0.1[:8000]/login.php?user=^USER^&secret=^PASS^");
            }
        });

        try_txt.setOnLongClickListener(v -> {
            if (!try_txt.getText().toString().isEmpty()) {
                String msg;
                ClipData clip;
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

                if (try_txt.getText().toString().contains("FOUND:")) {
                    clip = ClipData.newPlainText("label", try_txt.getText().toString().replace("FOUND: ", ""));
                    msg = "Result copied to clipboard.";
                } else {
                    clip = ClipData.newPlainText("label", try_txt.getText().toString());
                    msg = "Content copied to clipboard.";
                }
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        password_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if ("Generate".equals(password_spinner.getSelectedItem().toString())) {
                    password_num_chk.setEnabled(true);
                    password_az_chk.setEnabled(true);
                    password_AZ_chk.setEnabled(true);
                    password_spec_chk.setEnabled(true);

                    password_chrst_txt.setEnabled(true);
                    password_min_txt.setEnabled(true);
                    password_max_txt.setEnabled(true);
                } else {
                    password_num_chk.setEnabled(false);
                    password_az_chk.setEnabled(false);
                    password_AZ_chk.setEnabled(false);
                    password_spec_chk.setEnabled(false);

                    password_chrst_txt.setEnabled(false);
                    password_min_txt.setEnabled(false);
                    password_max_txt.setEnabled(false);
                }

                // Open it
                password_txt.setEnabled("Single".equals(password_spinner.getSelectedItem().toString()));
            }
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        username_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if ("Generate".equals(username_spinner.getSelectedItem().toString())) {
                    username_num_chk.setEnabled(true);
                    username_az_chk.setEnabled(true);
                    username_AZ_chk.setEnabled(true);
                    username_spec_chk.setEnabled(true);

                    username_chrst_txt.setEnabled(true);
                    username_min_txt.setEnabled(true);
                    username_max_txt.setEnabled(true);
                } else {
                    username_num_chk.setEnabled(false);
                    username_az_chk.setEnabled(false);
                    username_AZ_chk.setEnabled(false);
                    username_spec_chk.setEnabled(false);

                    username_chrst_txt.setEnabled(false);
                    username_min_txt.setEnabled(false);
                    username_max_txt.setEnabled(false);
                }

                username_txt.setEnabled("Single".equals(username_spinner.getSelectedItem().toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        password_num_chk.setOnClickListener(v -> {
            if (password_num_chk.isChecked()) {
                PASSWORD_ALPHABET += "0123456789";
            } else {
                PASSWORD_ALPHABET = PASSWORD_ALPHABET.replace("0123456789", "");
            }
            password_chrst_txt.setText(PASSWORD_ALPHABET);
        });

        username_num_chk.setOnClickListener(v -> {
            if (username_num_chk.isChecked()) {
                USERNAME_ALPHABET += "0123456789";
            } else {
                USERNAME_ALPHABET = USERNAME_ALPHABET.replace("0123456789", "");
            }
            username_chrst_txt.setText(USERNAME_ALPHABET);
        });

        password_az_chk.setOnClickListener(v -> {
            if (password_az_chk.isChecked()) {
                PASSWORD_ALPHABET += "abcdefghijklmnopqrstuvwxyz";
            } else {
                PASSWORD_ALPHABET = PASSWORD_ALPHABET.replace("abcdefghijklmnopqrstuvwxyz", "");
            }
            password_chrst_txt.setText(PASSWORD_ALPHABET);
        });

        username_az_chk.setOnClickListener(v -> {
            if (username_az_chk.isChecked()) {
                USERNAME_ALPHABET += "abcdefghijklmnopqrstuvwxyz";
            } else {
                USERNAME_ALPHABET = USERNAME_ALPHABET.replace("abcdefghijklmnopqrstuvwxyz", "");
            }
            username_chrst_txt.setText(USERNAME_ALPHABET);
        });

        password_AZ_chk.setOnClickListener(v -> {
            if (password_AZ_chk.isChecked()) {
                PASSWORD_ALPHABET += "ABCDEFGHIJKLMNOPRQSTUVWXYZ";
            } else {
                PASSWORD_ALPHABET = PASSWORD_ALPHABET.replace("ABCDEFGHIJKLMNOPRQSTUVWXYZ", "");
            }
            password_chrst_txt.setText(PASSWORD_ALPHABET);
        });

        username_AZ_chk.setOnClickListener(v -> {
            if (username_AZ_chk.isChecked()) {
                USERNAME_ALPHABET += "ABCDEFGHIJKLMNOPRQSTUVWXYZ";
            } else {
                USERNAME_ALPHABET = USERNAME_ALPHABET.replace("ABCDEFGHIJKLMNOPRQSTUVWXYZ", "");
            }
            username_chrst_txt.setText(USERNAME_ALPHABET);
        });

        password_spec_chk.setOnClickListener(v -> {
            if (password_spec_chk.isChecked()) {
                PASSWORD_ALPHABET += "!@#$%^&*()-_+=~`[]{}|\\:;\"'<>,.?/";
            } else {
                PASSWORD_ALPHABET = PASSWORD_ALPHABET.replace("!@#$%^&*()-_+=~`[]{}|\\:;\"'<>,.?/", "");
            }
            password_chrst_txt.setText(PASSWORD_ALPHABET);
        });

        username_spec_chk.setOnClickListener(v -> {
            if (username_spec_chk.isChecked()) {
                USERNAME_ALPHABET += "!@#$%^&*()-_+=~`[]{}|\\\\:;\\\"'<>,.?/\"";
            } else {
                USERNAME_ALPHABET = USERNAME_ALPHABET.replace("!@#$%^&*()-_+=~`[]{}|\\\\:;\\\"'<>,.?/\"", "");
            }
            username_chrst_txt.setText(USERNAME_ALPHABET);
        });

        useragent_txt.setOnItemClickListener((parent, view, position, id) -> {
            // Set to start of the line
            useragent_txt.setSelection(0);
        });

        txt3.setOnLongClickListener(v -> {
            // About intent
            Intent ma2 = new Intent(ABF.this, About.class);
            startActivity(ma2);
            return false;
        });

    }

}
